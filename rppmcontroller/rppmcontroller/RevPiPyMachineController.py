from __future__ import annotations

import inspect
import json as json
import logging
import multiprocessing
import os
import select
import signal
import socket
import sys
import time
from abc import abstractmethod, ABC
from dataclasses import dataclass
from multiprocessing import Process
from multiprocessing import Queue
from queue import Empty
from typing import Any, Dict, List, Optional, Callable, cast

import yaml

from rppmcontroller import __version__, TRACE
from rppmcontroller.behavior.CycleStepCommand import CycleStepCommand
from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
# noinspection PyDeprecation
from rppmcontroller.machine.EventKind import EventKind
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.MachineStatus import MachineStatus
from rppmcontroller.machine.NamedPosition import NamedPosition
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.StatusKind import StatusKind
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.highbay.HighBay import HighBay
from rppmcontroller.machine.indexedline.IndexedLine import IndexedLine
from rppmcontroller.machine.multiprocessing.MultiProcessing import \
    MultiProcessing
from rppmcontroller.machine.punchingmachine.PunchingMachine import \
    PunchingMachine
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.protocol import socketConnexionHelper
from rppmcontroller.protocol.CommandFeedback import CommandFeedback
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.protocol.JSONParser import JSONParser
from rppmcontroller.protocol.JSONReader import JSONReader
from rppmcontroller.protocol.MQTTFunctions import MQTTFunctions
from rppmcontroller.protocol.MachineFeedback import MachineFeedback
from rppmcontroller.protocol.MachineStatusRequestAnswer import \
    MachineStatusRequestAnswer


@dataclass(frozen=True)
class CommandResult:
    """Class representing the result of a command"""
    command: CycleStepCommand
    result: CycleStepResult

# commandServer will be on PORT_BASE+1
# notificationServer will be on PORT_BASE+11

# noinspection PyMethodMayBeStatic
class RevPiPyMachineController(ABC):
    """
    Abstract Class allowing to stream commands to and from  machines controlled by a RevPi
    """

    def __init__(self, configurationFile : str = ""):
        """
        Init method of this class, prepares the configuration
        """
        logging.info('rppmcontroller version: ' + __version__)

        # init a buffer to store all incoming/outgoing messages, received in a different thread than the one executing the revpi functions
        # stored as python objects, so parse/deserialize before putting into buffer
        self.inputBuffer = multiprocessing.Queue()
        self.outputBuffer = multiprocessing.Queue()
        self.__parent_pid = os.getppid()

        #the list of all machines that are connected to this controller
        self.machines : List[Machine] = []
        #dict, which keys are the machines, and value is a CycleStepCommand holding the function currently executed ([0]) and the id it was sent with ([1])
        self.currentlyExecuting : Dict[Machine, Optional[CycleStepCommand ]  ]= {}
        #dict, which keys are the machines, machine_feedback as the values
        self.machineFeedback : Dict [Machine, Optional[MachineStatus]] = {}
        #dict, which keys are the machines, command_feedback as the values
        self.commandFeedback : Dict[Machine, Optional[CommandResult]]= {}


        self.previousInputStatus : Dict[str, Any]= {}
        """Dict resulting from Machine.inputStatus(), used to detect changes in the input"""

        self.previousOuputStatus : Dict[str, Any]= {}
        """Dict resulting from Machine.outputStatus(), used to detect changes in the output"""

        self.previousInternalStatus : Dict[str, Any]= {}
        """Dict resulting from Machine.internalStatus(), used to detect changes in the internal status"""

        # used to indicate to the notification socket to stop too even if no notification message needs to be send
        # use of multiprocessing.Value to cross processes
        self.brokenCommandSocketDetected = multiprocessing.Value('b', False)

        # read configuration from file
        self.controller_config = {}
        if os.path.isfile(configurationFile):
            logging.debug(f'reading configuration file {configurationFile}')
            with open(configurationFile, 'r') as file:
                self.controller_config = yaml.safe_load(file)
        else:
            logging.warning(f'configuration file {configurationFile} not found; stopping the process')
            sys.exit(1)

        self.plcId = self.controller_config.get('plc', {}).get('id', "PLC")
        self.host = self.controller_config.get('connection', {}).get('host', socket.gethostname()+ ".local")
        self.command_port =  self.controller_config.get('connection', {}).get('command_port', 6001)
        self.notification_port =  self.controller_config.get('connection', {}).get('notification_port', 6011)
        self.mainLoopDelay =   self.controller_config.get('controller', {}).get('mainLoopDelay', 0.25)

        self.MQTT = MQTTFunctions(self.controller_config.get('mqtt', {}).get('server', 'localhost'),
                                  self.controller_config.get('mqtt', {}).get('port', 1883),
                                  self.controller_config.get('mqtt', {}).get('keepalive', 60))
        logging.debug(f'plc= {self.plcId}, controller_sockets={self.host}:{self.command_port}/{self.notification_port}, mqtt={self.MQTT.server}:{self.MQTT.port}')

    def receiveCommandMessages(self, s: socket.socket) -> None:
        """
        receive command message from the provided socket
        WARNING: runs in dedicated Process
        """
        signal.signal(signal.SIGINT, lambda sig, frame: signal_custom_handler(sig, frame, "receiveCommandMessages Process"))
        socket_list.append(s)
        isBrokenConnection = False
        self.brokenCommandSocketDetected.value = False

        try:
            # use makefile to read line by line
            f = s.makefile('r', encoding='utf-8')
            while not isBrokenConnection:
                line = f.readline()
                if not line:
                    logging.info("receiveCommandMessages socket connection broken")
                    isBrokenConnection = True
                    self.brokenCommandSocketDetected.value = True
                    time.sleep(self.mainLoopDelay) # wait enough before possible connection so that sendNotificationMessages has time to consider the brokenCommandSocketDetected flag
                elif line.strip('\n').startswith('WATCHDOG'):
                    logging.info(f"IGNORED Received {line!r}")
                elif not line.isspace():
                    logging.debug(f"Received {line!r}")
                    self.MQTT.publishEvent(self.plcId, '', '', EventKind.RECEIVED, "message", f"{line!r}")
                    objdata = JSONReader.read(line)
                    logging.debug(f"-> as object data: {objdata!r}")
                    self.inputBuffer.put(objdata)
        except Exception as e:
            logging.error(f"Error in receiveCommandMessages: {e}")
            isBrokenConnection = True
            self.brokenCommandSocketDetected.value = True

        # reset boolean (required if notificationSocket is opened first)
        self.brokenCommandSocketDetected.value = False


    def sendNotificationMessages(self, s: socket.socket) -> None:
        """
        send notification message to the provided socket
        WARNING: runs in dedicated Process
        """
        signal.signal(signal.SIGINT, lambda sig, frame: signal_custom_handler(sig, frame, "sendNotificationMessages Process"))
        socket_list.append(s)
        isBrokenConnection = False
        while not isBrokenConnection:
            try:
                if not self.outputBuffer.empty():
                    logging.debug("self.outputBuffer not empty!!!")
                messageSend = self.outputBuffer.get(block=False)
                message = JSONParser.parse(messageSend)
                message = message + "\n"
                s.sendall(bytes(message, "utf-8"))
                logging.debug(messageSend)
            except Empty:
                if self.brokenCommandSocketDetected.value:
                    logging.info(f"sendNotificationMessages socket connection closed - cause: receiveCommandMessages socket connection broken")
                    s.close()
                    isBrokenConnection = True
                else:
                    readable, writable, exceptional = select.select([s], [s], [s], 0)
                    if exceptional:
                        logging.info(f"sendNotificationMessages socket connection broken - cause: exceptional={exceptional}")
                        isBrokenConnection = True
                    elif s.fileno() == -1:
                        logging.info("sendNotificationMessages socket connection broken = cause: fileno() == -1")
                        isBrokenConnection = True
                    else:
                        # logging.debug("nothing in queue to send")
                        time.sleep(self.mainLoopDelay) # TO DO  find a way to make sure that we don't spend to much time in the loop, we should block on the buffer ...



    def processJson(self, inputBuffer: Queue) -> None:
        """
        gets the JSONOutput-objects out of the input buffer and decides
        which command function to execute
        The function to execute must use the name of the command lowercase
        with a "_Command" postfix,
        a command function must return a lambda pointing to a "_cycleStep"
        function
        :param inputBuffer: The command input buffer queue
        :return: `None`
        """
        try:
            input_buffer_item = inputBuffer.get(block=False)
        except Empty:
            return

        message = input_buffer_item.message
        topic_name = input_buffer_item.topicName

        # find a matching machine
        machine = self.__find_machine(topic_name)
        if machine is None:
            logging.warning(f"unknown topic name: {topic_name}")
            self.__publish_received_message_event("ignored", message, machine)
            return

        # handle different json-types
        json_type = message.jsonType

        if json_type == "STATUSREQUEST":
            self.__handle_status_request(machine, message)
        elif json_type == "COMMAND":
            self.__handle_command(machine, message, topic_name)
        else:
            # ignore messages which are not status requests or commands
            self.__publish_received_message_event("ignored",
                                                  message,
                                                  machine)

    def __find_machine(self, machine_id: str) -> Machine | None:
        """
        Attempts to find a machine with the given id.

        :param machine_id: The id of the machine to find.
        :return: The respective machine or `None` if no matching machine was
        found.
        """
        logging.debug(f"looking for {machine_id} in following machines: "
                      f"{self.machines}")
        machine = next((machine for machine in self.machines if
                        machine.id == machine_id), None)
        return machine

    def __handle_status_request(self, machine: Machine, message: Any) -> None:
        """
        Handles an incoming status request.

        :param machine: The machine for which the status was requested
        :param message: The message which requested the status
        :return: None
        """
        logging.debug("Handling status request")

        self.__publish_received_message_event("request",
                                              message,
                                              machine)

        # request status and append it to the outputBuffer
        try:
            requested_parameter_values = machine.request(message.parameters)
            answer = MachineStatusRequestAnswer("STATUSANSWER",
                                                message.requestId,
                                                requested_parameter_values)
            json_output = JSONOutput(machine.id, time.time(), answer)
            self.outputBuffer.put(json_output)
        except AttributeError:
            # TODO change:
            # raise JSONCommandNotSupportedOnThisMachineException()
            print("status not supported")

    def __handle_command(self,
                         machine: Machine,
                         message: Any,
                         topic_name: str) -> None:
        """
        Handles an incoming command.

        :param machine: The machine at which the command was targeted
        :param message: The message which sent the command
        :param topic_name: The topic name which selected the machine
        :return: None
        """
        message_type = message.type
        message_name = message.name
        parameters = message.parameters
        logging.debug(f'Handling command: {message_type} {message_name}')
        logging.debug(f"message: {message!r}")

        self.__publish_received_message_event("command",
                                              message,
                                              machine)

        logging.debug("Determining machine class")
        machine_class = self.__get_machine_class(message_type)
        if machine_class is None:
            logging.warning(f"invalid JSON command: unsupported machine "
                            f"type: {message_type}")
            return

        logging.debug(f"Determined machine class to be {machine_class}")

        if not isinstance(machine, machine_class):
            # the machine is determined by the topic name, so it is better to
            # log the topic name here instead of the selected machine
            logging.warning(f"invalid JSON command: missmatch between "
                            f"topic name and message type: {topic_name} "
                            f"incompatible with {message_type}")
            return

        # find the correct command function
        command_function_name = f"{str.lower(message_name)}_Command"
        command_function = self.__find_command_function(machine_class,
                                                        command_function_name)

        if command_function is None:
            logging.warning(f"command not supported: cannot find "
                            f"function"
                            f" {message_type}.{command_function_name}")
            self.send_command_feedback(machine,
                                       message.commandId,
                                       abort(f"Invalid Command "
                                             f"{message_name} "
                                             f"{message.commandId}"))
            return

        # apply parameter modifications
        logging.debug("Applying parameter modifications")
        try:
            self.__replace_named_positions(machine, parameters)
        except UnknownNamedPosition as e:
            named_position = e.named_position
            logging.warning(f"Cannot resolve named position '"
                            f"{named_position}' for {machine.id}")
            self.send_command_feedback(machine,
                                       message.commandId,
                                       abort(f"Unknown named position "
                                             f"{named_position}"))
            return

        logging.debug("Applying machine-specific parameter modifications")
        self.__apply_machine_specific_parameter_modifications(message_type,
                                                              parameters)

        # call command function
        logging.debug("Calling command function")
        command_function_return_value = self.__call_command_function(
            command_function,
            command_function_name,
            machine,
            parameters)
        if command_function_return_value is None:
            logging.warning(f"bad parameters for method "
                            f"{command_function_name}: {parameters}")
            # TODO activate:
            # raise JSONCommandNotSupportedOnThisMachineException()
            return

        # store the cycleStep function currently executed on each machine
        cycle_step_function = self.cast_to_callable(command_function_return_value)
        if cycle_step_function is None:
            # abort if the function did not return a callable
            self.send_command_feedback(machine,
                                       message.commandId,
                                       abort(f"Invalid Command "
                                             f"{message_name} "
                                             f"{message.commandId}"))
            return

        # send interruption feedback for the previously running
        # command on the machine
        logging.debug("Interrupting currently running command")
        self.__interrupt_currently_running_command(machine, message)

        # store the new command on the machine
        self.__set_currently_executing_command(machine,
                                               cycle_step_function,
                                               message)

    def __set_currently_executing_command(self,
                                          machine: Machine,
                                          cycle_step_function: Callable[
                                              [], CycleStepResult],
                                          message: Any) -> None:
        """
        Will set the executing command of a machine to the provided
        `cycle_step_function`. This method does NOT automatically interrupt
        the currently running command on the machine.

        :param machine: The machine for which to set the executing command
        :param cycle_step_function: A cycle step function to set as executing
            command
        :param message: The message which caused the command
        :return: None
        """
        try:
            source = inspect.getsource(cycle_step_function)
        except (OSError, TypeError):
            # use a str repr of the function if we failed to determine its
            # source
            source = f"{cycle_step_function}"
        display_name = f"{message.name} [{source.strip()}]"

        self.currentlyExecuting[machine] = CycleStepCommand(
            cycle_step_function,
            display_name,
            message.commandId)

    def __interrupt_currently_running_command(self,
                                              machine: Machine,
                                              message: Any) -> None:
        """
        Interrupts the currently running command of a machine

        :param machine: The machine for which to interrupt the currently
            running command
        :param message: The message which caused the interrupt
        :return: None
        """
        currently_executing_command = self.currentlyExecuting[machine]
        if currently_executing_command is not None:
            logging.debug(f'interrupting currently running command')
            self.sendCommandFeedbackOnChange(machine,
                                             currently_executing_command,
                                             CycleStepResult(
                                                 CycleStepResultEnum.INTERRUPTED,
                                                 f"Interrupted by "
                                                 f"Command "
                                                 f"{message.name} "
                                                 f"{message.commandId}"))
            machine.processSequenceContext = None

    def __call_command_function(self,
                                command_function: Any | None,
                                command_function_name: str,
                                machine: Machine,
                                parameters: list[Any]) \
        -> Callable[[], CycleStepResult] | None:
        """
        Calls a command function on a machine
        :param command_function: The command function to call
        :param command_function_name: The name of the command function
        :param machine: The machine on which to call the command function
        :param parameters: The parameters to pass to the command function
        :return: The return value of the command function or `None` if the
            parameters were not applicable to the provided command function
        """
        # Call the command function: it must return a lambda that calls a
        # cycleStep method (i.e., a method intended to run in the main
        # loop during the exLoop).
        logging.debug(f"calling function {command_function_name} with "
                      f"{len(parameters)} parameters")
        machine.incrementNbMinimumRequiredExecutionCycles()
        try:
            return command_function(machine, *parameters)
        except TypeError:
            return None

    def __replace_named_positions(self, machine: Machine, parameters: list[Any]) -> None:
        """
        Replaces named positions in the parameters for the given machine.

        :param machine: The machine for which to replace the named positions
        :param parameters: The parameters to replace named positions in
        :return: None
        :raise UnknownNamedPosition: If a named position cannot be resolved
        """

        machine_parameters = machine.parameters
        named_positions = machine_parameters.named_positions \
            if machine_parameters is not None else {}

        for parameter_index, parameter in enumerate(parameters):
            if isinstance(parameter, NamedPosition):
                resolved = named_positions[parameter.name] \
                    if parameter.name in named_positions \
                    else None
                if resolved is None:
                    raise UnknownNamedPosition(parameter)

                parameters[parameter_index] = resolved

    def __apply_machine_specific_parameter_modifications(self,
                                                         machine_type: str,
                                                         parameters: list[Any]):
        """
        For certain machines and methods the parameters need to be modified
        before they are passed to the command function. This method
        handles that modification.
        :param machine_type: The machine type as provided by a JSON message
        :param parameters: The (mutable) parameters which will be passed to the
            command function
        :return: None
        """
        if ((machine_type == "GRIPPER" or machine_type == "VACUUM") and
            len(parameters) > 1 and
            isinstance(parameters[0], Position) and
            isinstance(parameters[1], Position) and
            parameters[0].meaning == "END" and
            parameters[1].meaning == "START"):
            # swap the first two parameters if START and END are swapped
            parameters[0], parameters[1] = parameters[1], parameters[0]

    def __find_command_function(self,
                                machine_class: type[Machine],
                                command_function_name: str) -> Any | None:
        """
        Tries to find a method with a given name on a machine class.

        Note: This is done using reflection.

        :param machine_class: The class of the machine where to look for the
            method
        :param command_function_name: The name of the function to look for
        :return: The found function or `None` if no function with the given
            name was found
        """
        try:
            return getattr(machine_class, command_function_name)
        except AttributeError:
            return None

    def __get_machine_class(self, machine_type) -> type[Machine] | None:
        """
        Gets the class of a machine for the specified type name.

        Currently supported type names are:

        - VACUUM
        - WAREHOUSE
        - SORTING
        - INDEXEDLINE
        - MULTIPROCESSING
        - CONVEYOR
        - PUNCHING

        :param machine_type: A machine type name
        :return: The class of the associated machine or `None` for an
            unsupported type name
        """
        machine_type_to_class_mapping = {
            "VACUUM": VacuumGripper,
            "WAREHOUSE": HighBay,
            "SORTING": SortingLine,
            "INDEXEDLINE": IndexedLine,
            "MULTIPROCESSING": MultiProcessing,
            "CONVEYOR": ConveyorBelt,
            "PUNCHING": PunchingMachine,
        }
        return machine_type_to_class_mapping[machine_type] if machine_type in machine_type_to_class_mapping else None

    def __publish_received_message_event(self,
                                         event_group: str,
                                         message: Any,
                                         machine: Optional[Machine] = None) -> None:
        """
        Publish the event of a received message to MQTT

        :param event_group: The event group
        :param message: The received message
        :param machine: Which machine was targeted by the message, if known
        :return: None
        """
        machine_type_name = machine.machineTypeName() if machine is not None else ''
        machine_id = machine.id if machine is not None else ''
        event = json.dumps(message, default=str)

        self.MQTT.publishEvent(self.plcId,
                               machine_type_name,
                               machine_id,
                               EventKind.RECEIVED,
                               event_group,
                               event)

    @abstractmethod
    def read(self) -> None:
        """
        Set the internal machine parameters according to the RevPi inputs
        Ie.  maps the DIO bus values to our python objects
        DIO IO variables names must conforms to the physical configuration
        """
        pass

    @abstractmethod
    def write(self) -> None:
        """
        Set the RevPi output according to the internal machine parameters
        Ie.  maps the DIO bus values to our python objects
        DIO IO variables names must conforms to the physical configuration
        """
        pass

    @abstractmethod
    def reset(self) -> None:
        """
        Reset encoder values to 0 after the setup of the individual machine connected to
        these ports is finished
        DIO IO variables names must conform to the physical configuration
        """

    def exLoop(self) -> None:
        """
        The execute-loop, which activates all the necessary "_cycleStep" functions on each machine
        a "_cycleStep" function is maintained in the currentlyExecuting map until it returns
        """
        for key in self.currentlyExecuting.keys():
            # call method
            cycleStepCommand = self.currentlyExecuting[key]
            if cycleStepCommand is not None:
                #logging.debug(f'currentlyExecuting {key}.{cycleStepCommand.displayName}')
                try:
                    ret = cycleStepCommand.cycleStep()
                except Exception as e:
                    logging.exception("Error while executing cycleStepCommand", exc_info=e)
                    continue

                # removes currentlyExecuting function once it indicates it is finished
                # logging.debug(f"{ret}")
                self.sendCommandFeedbackOnChange(key, cycleStepCommand, ret)
                # we use "not must continue" since is_done() wouldn't handle the abort case
                if not ret.must_continue():
                    logging.debug(f'removing {cycleStepCommand.displayName} from currentlyExecuting')
                    self.currentlyExecuting[key] = None
                    logging.debug(f'isExecuting = {key.isExecuting}')
            # logging.debug("after command evaluation")
            # LEGACY :  TO BE REMOVED AFTER FULL REFACTORY remove currentlyExecuting function once it is finished
            if (key.machineFeedback() == MachineStatus.INITIALIZED_IDLE or key.machineFeedback() == MachineStatus.UNINITIALIZED_IDLE) and cycleStepCommand is not None:
                logging.warning(f'LEGACY: DEPRECATED, removing {cycleStepCommand.displayName} from currentlyExecuting due to MachineStatus.IDLE')
                self.currentlyExecuting[key] = None

    def cast_to_callable(self, var: Any) -> Optional[Callable[[], CycleStepResult]]:
        """Casts a variable to Callable[[], CycleStepResult] if it's compatible, otherwise returns None."""
        if not callable(var):
            logging.warning(f'{var} is not callable')
            return None

        return cast(Callable[[], CycleStepResult], var)

    def createMachineFeedbackOnChange(self) -> None:
        """Whenever the state of the machine changes, feedback is created
        a machine can be in several states as defined in the ExecutionStatus enum
        (currently, only INACTION and FINISHED are used)
        Also update the self.feedback[m] dictionary
        """
        # logging.debug(f"machines: {self.machines}")
        for m in self.machines:
            # logging.debug(f"checking feedback for {m}")
            current_feedback = m.machineFeedback()
            # logging.debug(f"current feedback: {feedback}")
            cached_feedback = self.machineFeedback[m] if m in self.machineFeedback else None
            # logging.debug(f"cached feedback: {cached_feedback}")
            if current_feedback != cached_feedback:
                # logging.debug("feedback changed")
                self.machineFeedback[m] = current_feedback
                # append feedback to outputBuffer
                f = MachineFeedback("MACHINE_FEEDBACK",  current_feedback.name,  "")
                j = JSONOutput(m.id, time.time(), f)
                #logging.debug("created Feedback")
                self.outputBuffer.put(j, block=False)
                self.MQTT.publishEvent(self.plcId, m.machineTypeName(), m.id, EventKind.EMITTED, "machine_feedback", JSONParser.parse(f))

    def sendCommandFeedbackOnChange(self, machine: Machine, lastCommand: CycleStepCommand | None, lastResult: CycleStepResult) -> None:
        """
        Whenever the result of the last executed command changes, feedback is created
        If lastCommand is None, it means that no command was running on the machine or that the command was invalid and must be sent
        """
        # a change is detected if the result is different from the previous one
        cached_result = self.commandFeedback[machine] if machine in self.commandFeedback else None
        if cached_result is None or lastCommand is None:
            # first time
            result_changed = True
        else:
            # use is_equivalent_result to consider only changes related to the result and info in case of Runner
            result_differs = \
                not lastResult.is_equivalent_result(cached_result.result)
            if result_differs:
                logging.debug(f"Last result differs from cached result: "
                              f"{lastResult!r} != {cached_result.result!r}")
            cached_command_id = cached_result.command.commandId
            last_command_id = lastCommand.commandId
            command_id_differs = last_command_id != cached_command_id
            if command_id_differs:
                logging.debug(f"Last command id differs from cached command "
                              f"id: {last_command_id} != {cached_command_id}")
            result_changed = result_differs or command_id_differs
        if result_changed:
            cycleStepCommand = self.currentlyExecuting[machine]
            if cycleStepCommand is not None:
                self.send_command_feedback(machine,
                                           cycleStepCommand.commandId,
                                           lastResult)

        else:
            logging.debug(f'identical CycleStepResult for machine {machine.id} {self.commandFeedback[machine]} == {lastResult}')
        if lastCommand is not None:
            # clone result here since it might be a runner, which is mutable
            # -> we prefer clone over deepcopy, since that is more predictable
            #  with subclasses like runners
            result = lastResult.clone()
            self.commandFeedback[machine] = CommandResult(lastCommand, result)
            logging.debug(f'stored CommandResult for machine {machine.id} {lastCommand} {result}')

    def send_command_feedback(self,
                              machine: Machine,
                              command_id: str,
                              result: CycleStepResult) -> None:
        """
        Send command feedback for a specific command

        :param machine: The machine that the command was meant for
        :param command_id: The id of the command
        :param result: The feedback to send
        :return: None
        """
        sub_result = result.subCycleStepResult
        info = f"{result.info}"
        if sub_result is not None:
            info += (f"\nSubCycleStepResult: {sub_result[1].info} "
                     f"{sub_result[1].result}")
        command_feedback = CommandFeedback("COMMAND_FEEDBACK",
                                           command_id,
                                           result.result.name,
                                           info)
        logging.debug(f"CommandFeedback: {command_feedback}")
        json_output = JSONOutput(machine.id, time.time(), command_feedback)
        self.outputBuffer.put(json_output, block=False)
        self.MQTT.publishEvent(self.plcId,
                               machine.machineTypeName(),
                               machine.id,
                               EventKind.EMITTED,
                               "command_feedback",
                               JSONParser.parse(command_feedback))

    def publishMQTTMeasurementStatus(self) -> None:
        """for each machine publish the input, output, and internal measurements/status to MQTT if the MQTT is set
        """
        for m in self.machines:
            currentInputStatus = m.inputStatus()
            if self.previousInputStatus.get(m.id, None) != currentInputStatus:
                logging.debug(f'publishing {m.id} inputStatus to MQTT {self.previousInputStatus.get(m.id, None)} != {currentInputStatus}')
                self.MQTT.publishMeasurementStatus(self.plcId, m.machineTypeName(), m.id, StatusKind.INPUT, currentInputStatus)
                self.previousInputStatus[m.id] =  currentInputStatus

            currentInternalStatus = m.internalStatus()
            if self.previousInternalStatus.get(m.id, None) != currentInternalStatus:
                logging.debug(f'publishing {m.id} internalStatus to MQTT')
                self.MQTT.publishMeasurementStatus(self.plcId, m.machineTypeName(), m.id, StatusKind.INTERNAL, currentInternalStatus)
                self.previousInternalStatus[m.id] = currentInternalStatus

            currentOutputStatus = m.outputStatus()
            if self.previousOuputStatus.get(m.id, None) != currentOutputStatus:
                logging.debug(f'publishing {m.id} outputStatus to MQTT')
                self.MQTT.publishMeasurementStatus(self.plcId, m.machineTypeName(), m.id, StatusKind.OUTPUT, currentOutputStatus)
                self.previousOuputStatus[m.id] = currentOutputStatus

    def start(self):
        """
        Starts communication threads for receiving commands via Sockets and executing them
        Initiate the main loop
        """
        logging.debug('start')

        # TODO manage connectinOpening modes : opend by controller or by orchestrator
        # listen for connection and process commandMessages in a dedicated Process
        # start the function socketConnexionHelper.listenSocket("localhost", 8888, self.receiveCommandMessage) in a Process
        Process(target=socketConnexionHelper.listenSocket, args=[self.host, self.command_port, self.receiveCommandMessages, True]).start()
        #Process(target=socketConnexionHelper.connectSocket, args=["localhost", self.command_port, self.receiveCommandMessages]).start()

        # listen for connection and send notification in a dedicated Process
        Process(target=socketConnexionHelper.listenSocket, args=[self.host, self.notification_port, self.sendNotificationMessages, True]).start()
        #Process(target=socketConnexionHelper.connectSocket, args=["localhost", self.command_port, self.sendNotificationMessages]).start()


        logging.debug('all threads started')
        signal.signal(signal.SIGINT, lambda sig, frame: signal_custom_handler(sig, frame, "Main"))
        while True:
            try:
                self.mainLoopIteration()
            except Exception as e:
                logging.error("Encountered an unexpected error during the main loop iteration", exc_info=e)

    def mainLoopIteration(self):
        logging.log(TRACE, "[main loop] Processing json...")
        self.processJson(self.inputBuffer)
        logging.log(TRACE, "[main loop] Reading...")
        self.read()
        logging.log(TRACE, "[main loop] Executing loop...")
        self.exLoop()
        logging.log(TRACE, "[main loop] Writing...")
        self.write()
        logging.log(TRACE, "[main loop] Publishing MQTT measurement status...")
        self.publishMQTTMeasurementStatus()
        logging.log(TRACE, "[main loop] Resetting...")
        self.reset()
        logging.log(TRACE, "[main loop] Creating machine feedback...")
        self.createMachineFeedbackOnChange()
        logging.log(TRACE, "[main loop] Decrementing required execution cycles...")
        # if a machine was executing some command, we are now sure that it was taken into account (incl. write, reset, and feedback)
        for m in self.machines:
            m.decrementNbMinimumRequiredExecutionCycles()
        logging.log(TRACE, "[main loop] Sleeping...")
        time.sleep(self.mainLoopDelay)

def abort(info: str) -> CycleStepResult:
    return CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, info)

# Create an empty list
socket_list : List[socket.socket] = []

def signal_custom_handler(sig, frame, name: str):
    process_id = os.getpid()
    logging.debug(f"Signal '{sig}' received in process {name} (PID: {process_id}).")
    for s in socket_list:
        logging.info(f"Closing socket {s}")
        s.close()
    sys.exit(0)

class UnknownNamedPosition(Exception):
    def __init__(self, named_position: NamedPosition):
        self.__named_position = named_position

    @property
    def named_position(self) -> NamedPosition:
        return self.__named_position
