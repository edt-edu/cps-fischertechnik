import copy
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
from abc import abstractmethod
from dataclasses import dataclass
from multiprocessing import Process
from multiprocessing import Queue
from queue import Empty
from typing import Any, Dict, List, Optional, Callable, cast

import yaml

from rppmcontroller import __version__
from rppmcontroller.behavior.CycleStepCommand import CycleStepCommand
from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.EventKind import EventKind
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.MachineStatus import MachineStatus
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

class RevPiPyMachineController:
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

        while not isBrokenConnection:
                data = s.recv(1024)
                if data == b'':
                    logging.info("receiveCommandMessages socket connection broken")
                    isBrokenConnection = True
                    self.brokenCommandSocketDetected.value = True
                    time.sleep(self.mainLoopDelay) # wait enough before possible connection so that sendNotificationMessages has time to consider the brokenCommandSocketDetected flag
                else:
                    if data.decode().strip('\n').startswith('WATCHDOG'):
                        logging.info(f"IGNORED Received {data!r}")
                    elif not data.isspace():
                        logging.debug(f"Received {data!r}")
                        self.MQTT.publishEvent(self.plcId, '', '', EventKind.RECEIVED, "message", f"{data!r}")
                        objdata = JSONReader.read(data)
                        self.inputBuffer.put(objdata)
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
                if self.brokenCommandSocketDetected.value == True:
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
        gets the JSONOutput-objects out of the input buffer and decides which command function to execute
        The function to execute must use the name of the command lowercase with a "_Command" postfix,
        a command function must return a lambda pointing to a "_cycleStep" function
        :param inputBuffer: The command input buffer queue
        :return: `None`
        """
        # logging.debug("processingJson")
        try:
            input_buffer_item = inputBuffer.get(block=False)
        except Empty:
            # logging.debug("nothing in queue")
            return

        message = input_buffer_item.message
        topic_name = input_buffer_item.topicName

        # find a matching machine
        logging.debug(f"machines: {self.machines}")
        machine = next((machine for machine in self.machines if
                        machine.id == topic_name), None)
        if machine is None:
            logging.warning(f"unknown id: {topic_name}")
            self.MQTT.publishEvent(self.plcId,
                                   '',
                                   '',
                                   EventKind.RECEIVED,
                                   "ignored",
                                   json.dumps(message, default=str))
            return

        # handle different json-types
        json_type = message.jsonType
        machine_type_name = machine.machineTypeName()
        machine_id = machine.id
        if json_type == "STATUSREQUEST":
            logging.debug("Status")

            self.MQTT.publishEvent(self.plcId,
                                   machine_type_name,
                                   machine_id,
                                   EventKind.RECEIVED,
                                   "request",
                                   json.dumps(message, default=str))
            # request status and append it to the outputBuffer
            try:
                requested_parameter_values = machine.request(
                    message.parameters)
                answer = MachineStatusRequestAnswer("STATUSANSWER",
                                                    message.requestId,
                                                    requested_parameter_values)
                json_output = JSONOutput(machine_id, time.time(), answer)
                self.outputBuffer.put(json_output)
            except AttributeError:
                #TODO change:
                #raise JSONCommandNotSupportedOnThisMachineException()
                print("status not supported")

        elif json_type == "COMMAND":
            func = None
            ret = None
            logging.debug(f'Command {message.type} {message.name}')
            # self.MQTT.publishEvent(self.plcId, m.machineTypeName(), m.id, EventKind.RECEIVED, "command", JSONParser.parse(inputBufferItem.message))
            self.MQTT.publishEvent(self.plcId,
                                   machine_type_name,
                                   machine_id, EventKind.RECEIVED, "command", json.dumps(
                message, default=str))
            try:
                # find a function in the machine class with name = "{message.name}_Command"
                if message.type == "VACUUM" and isinstance(machine, VacuumGripper):
                    func = getattr(VacuumGripper, f'{str.lower(message.name)}_Command')
                # elif inputBufferItem.message.type == "GRIPPER" and isinstance(m, Robot):
                #     func = getattr(Robot, str.lower(inputBufferItem.message.name))
                elif message.type == "WAREHOUSE" and isinstance(machine, HighBay):
                    func = getattr(HighBay, f'{str.lower(message.name)}_Command')
                elif message.type == "SORTING" and isinstance(machine, SortingLine):
                    func = getattr(SortingLine, f'{str.lower(message.name)}_Command')
                elif message.type == "INDEXEDLINE" and isinstance(machine, IndexedLine):
                    func = getattr(IndexedLine, f'{str.lower(message.name)}_Command')
                elif message.type == "MULTIPROCESSING" and isinstance(machine, MultiProcessing):
                    func = getattr(MultiProcessing, f'{str.lower(message.name)}_Command')
                elif message.type == "CONVEYOR" and isinstance(machine, ConveyorBelt):
                    func = getattr(ConveyorBelt, f'{str.lower(message.name)}_Command')
                elif message.type == "PUNCHING" and isinstance(machine, PunchingMachine):
                    func = getattr(PunchingMachine, f'{str.lower(message.name)}_Command')
                else:
                    #TODO raise an exception here
                    #TODO send a COMMAND_FEEDBACK  IGNORED message
                    logging.error(f"Invalid json command. Cannot find function {message.type}.{message.name}")
                # Call the command function: it must return either None if nothing else is required
                #  return a lambda that calls a cycleStep method (ie. a method intended to run in the main loop during the exLoop)
                # Arrange the function parameters in the correct order and match them with the function.
                if func is not None and (
                    message.type == "GRIPPER" or message.type == "VACUUM"):
                    pos = message.parameters
                    i = len(pos)
                    if i == 0:
                        logging.debug("function called with no args")
                        #m.setupFirst = True unschön

                        machine.incrementNbMinimumRequiredExecutionCycles()
                        ret = func(machine)
                    if i == 1:
                        # just assume that start/end is correct
                        # TODO get rid of assumption
                        logging.debug("function called with one arg")
                        machine.incrementNbMinimumRequiredExecutionCycles()
                        ret = func(machine, pos[0])
                    if i == 2:
                        #for the ordered move command
                        if isinstance(pos[1].horizontal, bool) :
                            logging.debug("function called with two args")
                            machine.incrementNbMinimumRequiredExecutionCycles()
                            ret = func(machine, pos[0], pos[1])
                        elif pos[0].meaning == "START" and pos[1].meaning == "END":
                            logging.debug("function called with two args")
                            machine.incrementNbMinimumRequiredExecutionCycles()
                            ret = func(machine, pos[0], pos[1])
                        elif pos[0].meaning == "END" and pos[1].meaning == "START":
                            logging.debug("function called with two args")
                            machine.incrementNbMinimumRequiredExecutionCycles()
                            ret = func(machine, pos[1], pos[0])
                        else:
                            logging.error("command not supported - params")
                            # TODO activate:
                            # raise JSONCommandNotSupportedOnThisMachineException()
                elif func is not None and message.type == "WAREHOUSE":
                    box = message.parameters
                    i = len(box)
                    logging.debug(f"func: {func}, number of parameters: {i}")
                    if i == 0:
                        ret = func(machine)
                    elif i == 1:
                        ret = func(machine, box[0])
                    elif i == 2:
                        ret = func(machine, box[0], box[1])
                    else:
                        logging.warning(f"unsupported number of parameters: {i}")
                elif func is not None and message.type == "SORTING":
                    color = message.parameters
                    i = len(color)
                    if i == 0:
                        ret = func(machine)
                    if i == 1:
                        ret = func(machine, color[0])
                elif func is not None and message.type == "INDEXEDLINE":
                    params = message.parameters
                    try:
                        ret = func(machine, *params)
                    except TypeError:
                        logging.warning(f"unsupported number of parameters: {len(params)}")
                elif func is not None and message.type == "MULTIPROCESSING":
                    parameters = message.parameters
                    logging.debug(message)
                    i = len(parameters)
                    if i == 0:
                        ret = func(machine)
                    elif i == 1:
                        ret = func(machine, parameters[0])
                    elif i == 3:
                        ret = func(machine, parameters[0], parameters[1], parameters[2])
                    else:
                        logging.warning(f"unsupported number of parameters: {i}")
                        logging.warning(parameters)
                elif func is not None and message.type == "PUNCHING":
                   ret = func(machine)
                elif func is not None and (message.type == "CONVEYOR"):
                    mix = message.parameters
                    i = len(mix)
                    if i == 0:
                        ret = func(machine)
                    if i == 1:
                        ret = func(machine, mix[0])
                    if i == 2:
                        if mix[0] == Direction.BACKWARD or mix[0] == Direction.FORWARD:
                            ret = func(machine, mix[0], mix[1])
                        else:
                            ret = func(machine, mix[1], mix[0])

                # store the cycleStep function that is currently executed on each machine
                cycleStepFunction = self.cast_to_callable(ret)
                if cycleStepFunction is not None:
                    # logging.debug(f'cycleStepFunction is not None')
                    if machine in self.currentlyExecuting :
                        command = self.currentlyExecuting[machine]
                        if command is not None:
                            logging.debug(f'self.currentlyExecuting[m] is not None')
                            # send interruption feedback for the previously running command on the machine
                            self.sendCommandFeedbackOnChange(machine, command, CycleStepResult(CycleStepResultEnum.INTERRUPTED, f"Interrupted by Command {message.name} {message.commandId}"))
                            machine.processSequenceContext = None
                    # logging.debug("survived execution check")
                    message_name = message.name
                    # logging.debug(f"message_name: {message_name}")
                    try:
                        source = inspect.getsource(cycleStepFunction)
                    except:
                        # logging.debug("failed to determine source of cycleStepFunction")
                        source = f"{cycleStepFunction}"
                    # logging.debug(f"source: {source}")
                    display_name = f"{message_name} [{source.strip()}]"
                    # logging.warning(f"display_name: {display_name}")
                    command_id = message.commandId
                    # logging.debug(f"command_id: {command_id}")
                    self.currentlyExecuting[machine] = CycleStepCommand(cycleStepFunction,
                                                                  display_name,
                                                                  command_id)
                    # logging.debug("survived execution update")
                else:
                    self.sendCommandFeedbackOnChange(machine, None, CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, f"Invalid Command {message.name} {message.commandId}"))
                    # an invalid command doesn't interrupt currentlyRunning command
                return
            except AttributeError as e:
                logging.warning(f"command not supported: Cannot find function {message.type}.{message.name}_Command:\n{e}")
                self.sendCommandFeedbackOnChange(machine, None, CycleStepResult(CycleStepResultEnum.ABORTED_ERROR, f"Invalid Command {message.name} {message.commandId}"))
                return
        else:
            self.MQTT.publishEvent(self.plcId,
                                   machine_type_name,
                                   machine_id, EventKind.RECEIVED, "ignored", json.dumps(
                message, default=str))

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
        DIO IO variables names must conforms to the physical configuration
        """

    def exLoop(self) -> None:
        """
        The execute loop, which activates all the necessary "_cycleStep" functions on each machine
        a "_cycleStep" function is maintained in the currentlyExecuting map until it returns
        """
        for key in self.currentlyExecuting.keys():
            # call method
            cycleStepCommand = self.currentlyExecuting[key]
            if cycleStepCommand is not None:
                #logging.debug(f'currentlyExecuting {key}.{cycleStepCommand.displayName}')
                ret = cycleStepCommand.cycleStep()
                # removes currentlyExecuting function once it indicates it is finished
                # logging.debug(f"{ret}")
                if ret.is_done():
                    self.sendCommandFeedbackOnChange(key, cycleStepCommand, ret)
                    logging.debug(f'removing {cycleStepCommand.displayName} from currentlyExecuting')
                    self.currentlyExecuting[key] = None
                    logging.debug(f'isExecuting = {key.isExecuting}')
                else:
                    # continue
                    # maybe the res is different from previous, so it should be published
                    self.sendCommandFeedbackOnChange(key, cycleStepCommand, ret)
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

    def sendCommandFeedbackOnChange(self, machine: Machine, lastCommand: Optional[CycleStepCommand], lastResult: CycleStepResult) -> None:
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
            result_changed = not lastResult.is_equivalent_result(cached_result.result) or lastCommand.commandId != cached_result.command.commandId
            logging.debug(f'lastResult.is_equivalent_result(cached_result.result) {lastResult.is_equivalent_result(cached_result.result)}')
            logging.debug(f'lastCommand.commandId {lastCommand.commandId} != cached_result.command.commandId {cached_result.command.commandId}')
        if result_changed :
            cycleStepCommand = self.currentlyExecuting[machine]
            if cycleStepCommand is not None:
                jsonid = cycleStepCommand.commandId
                subResult = lastResult.subCycleStepResult
                if subResult is not None:
                    info = f'{lastResult.info}\nLast subCycleStepResult: {subResult[1].info} {subResult[1].result}'
                else:
                    info = f'{lastResult.info}'
                f = CommandFeedback("COMMAND_FEEDBACK", jsonid, lastResult.result.name,  info)
                logging.debug(f'CommandFeedback {f}')
                j = JSONOutput(machine.id, time.time(), f)
                self.outputBuffer.put(j, block=False)
                self.MQTT.publishEvent(self.plcId, machine.machineTypeName(), machine.id, EventKind.EMITTED, "command_feedback", JSONParser.parse(f))

        else:
            logging.debug(f'identical CycleStepResult for machine {machine.id} {self.commandFeedback[machine]} == {lastResult}')
        if lastCommand is not None:
            self.commandFeedback[machine] = CommandResult(copy.deepcopy(lastCommand), copy.deepcopy(lastResult))
            logging.debug(f'stored CommandResult for machine {machine.id} {self.commandFeedback[machine].command} {self.commandFeedback[machine].result}')

    def publishMQTTMeasurementStatus(self) -> None:
        """for each machines publish the input, output and internal measurements/status to MQTT if the MQTT is set
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
            self.mainLoopIteration()

    def mainLoopIteration(self):
        # logging.debug(f'main loop - self.inputBuffer.empty()={self.inputBuffer.empty()}')
        self.processJson(self.inputBuffer)
        # logging.debug("after processJson")
        self.read()
        # logging.debug("after read")
        self.exLoop()
        # logging.debug("after exLoop")
        self.write()
        # logging.debug("after write")
        self.publishMQTTMeasurementStatus()
        # logging.debug("after publish")
        self.reset()
        # logging.debug("after reset")
        # # logging.debug(self.currentlyExecuting)
        self.createMachineFeedbackOnChange()
        # logging.debug("after feedback")
        # self.createCommandFeedbackOnChange()
        # if a machine was executing some command, we are now sure that it was taken into account (incl. write, reset, and feedback)
        for m in self.machines:
            m.decrementNbMinimumRequiredExecutionCycles()
        # logging.debug("after decrements")
        time.sleep(self.mainLoopDelay)
        # logging.debug("after sleep")

# Create an empty list
socket_list : List[socket.socket] = []

def signal_custom_handler(sig, frame, name: str):
    process_id = os.getpid()
    logging.debug(f"Signal '{sig}' received in process {name} (PID: {process_id}).")
    for s in socket_list:
        logging.info(f"Closing socket {s}")
        s.close()
    sys.exit(0)
