
from abc import abstractmethod
import logging
import multiprocessing 
from multiprocessing import Process
from multiprocessing import Queue
import os
from queue import Empty
import signal
import socket
import sys
import time
import inspect
import yaml
from typing import List

from rppmcontroller.protocol import socketConnexionHelper
from rppmcontroller.protocol.JSONParser import JSONParser
from rppmcontroller.protocol.JSONReader import JSONReader
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.protocol.MachineStatusRequestAnswer import MachineStatusRequestAnswer
from rppmcontroller.protocol.MachineCommandFeedback import MachineCommandFeedback
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.machine.multiprocessing.MultiProcessing import MultiProcessing
from rppmcontroller.machine.Machine import Machine
from rppmcontroller.machine.ExecutionStatus import ExecutionStatus
from rppmcontroller.machine.Direction import Direction


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

        logging.debug('init started')
        
        # init a buffer to store all incoming/outgoing messages, received in a different thread than the one executing the revpi functions
        # stored as python objects, so parse/deserialize before putting into buffer
        self.inputBuffer = multiprocessing.Queue()
        self.outputBuffer = multiprocessing.Queue()
        self.__parent_pid = os.getppid()

        #the list of all machines that are connected to this core
        self.machines : List[Machine] = []
        #dict, which keys are the machines, than there is a tuple holding the function currently executed ([0]) and the id it was sent with ([1])
        self.currentlyExecuting = {}
        #dict, which keys are the machines, feedback as the values
        self.feedback = {}

        # read configuration from file
        self.controller_config = {}
        if os.path.isfile(configurationFile):
            logging.debug(f'reading configuration file {configurationFile}')
            with open(configurationFile, 'r') as file:
                self.controller_config = yaml.safe_load(file)
        else:
            logging.warning(f'configuration file {configurationFile} not found; using default values')

        self.host = self.controller_config.get('connection', {}).get('host', socket.gethostname()+ ".local")
        self.command_port =  self.controller_config.get('connection', {}).get('command_port', 6001)
        self.notification_port =  self.controller_config.get('connection', {}).get('notification_port', 6011)
        self.mainLoopDelay =   self.controller_config.get('controller', {}).get('mainLoopDelay', 0.25)

        

    def receiveCommandMessages(self, s: socket.socket) -> None:
        """
        receive command message from the provided socket
        """
        signal.signal(signal.SIGINT, lambda sig, frame: signal_custom_handler(sig, frame, "receiveCommandMessages Process"))

        isBrokenConnection = False

        while not isBrokenConnection:
                data = s.recv(1024)
                if data == b'':
                    logging.info("receiveCommandMessages socket connection broken")
                    isBrokenConnection = True
                else:
                    if not data.isspace():
                        logging.debug(f"Received {data!r}")
                        objdata = JSONReader.read(data)
                        self.inputBuffer.put(objdata)

    def sendNotificationMessages(self, s: socket.socket) -> None:
        """
        send notification message to the provided socket
        """
        signal.signal(signal.SIGINT, lambda sig, frame: signal_custom_handler(sig, frame, "sendNotificationMessages Process"))
        while True:
            try:
                if self.outputBuffer.qsize() > 0:
                    logging.debug("self.outputBuffer not empty!!!")
                messageSend = self.outputBuffer.get(block=False)
                message = JSONParser.parse(messageSend)
                message = message + "\n"
                s.sendall(bytes(message, "utf-8"))
                logging.debug(messageSend)
            except Empty:
                logging.debug("nothing in queue to send")
                time.sleep(self.mainLoopDelay) # TO DO  find a way to make sure that we don't spend to much time in the loop, we should block on the buffer ...


    def processJson(self, inputBuffer: Queue):
        #maybe output buffer als parameter übergeben wie inputbuffer???
        """
        gets the JSONOutput-objects out of the input buffer and decides which function to execute
        :param inputBuffer:
        :return:
        """
        foundMatchingMachine = False
        func = None
        ret = None
        inputBufferItem = None
        try:
            inputBufferItem = inputBuffer.get(block=False)
        except Empty:
            # logging.debug("nothing in queue")
            pass
        if inputBufferItem is not None:
            for m in self.machines:
                # überprüfe ob maschinen-id bekannt
                logging.debug(f"machines: {self.machines}")
                if m.id == inputBufferItem.topicName:
                    foundMatchingMachine = True
                    # find diff btw command and request
                    if inputBufferItem.message.jsonType == "STATUSREQUEST":
                        logging.debug("Status")
                        # Status abfragen und Ergebnis an outputBuffer anfügen
                        try:
                            statusanswer = m.request(inputBufferItem.message.parameters)
                            a = MachineStatusRequestAnswer("STATUSANSWER", inputBufferItem.message.requestId, statusanswer)
                            j = JSONOutput(m.id, time.time(), a)
                            self.outputBuffer.put(j)
                        except AttributeError:
                            #TODO change:
                            #raise JSONCommandNotSupportedOnThisMachineException()
                            print("status not supported")
                            break
                    elif inputBufferItem.message.jsonType == "COMMAND":
                        logging.debug("command")
                        try:
                            logging.debug(f"Nom de message : {inputBufferItem.message.name}")
                            # map between functions and the name of functions sent with the JSON
                            if inputBufferItem.message.type == "VACUUM" and isinstance(m, VacuumGripper):
                                func = getattr(VacuumGripper, str.lower(inputBufferItem.message.name))
                            # elif inputBufferItem.message.type == "GRIPPER" and isinstance(m, Robot):
                            #     func = getattr(Robot, str.lower(inputBufferItem.message.name))
                            # elif inputBufferItem.message.type == "WAREHOUSE" and isinstance(m, Warehouse):
                            #     func = getattr(Warehouse, str.lower(inputBufferItem.message.name))
                            elif inputBufferItem.message.type == "SORTING" and isinstance(m, SortingLine):
                                func = getattr(SortingLine, str.lower(inputBufferItem.message.name))
                            # elif inputBufferItem.message.type == "INDEXEDLINE" and isinstance(m, IndexedLine):
                            #     func = getattr(IndexedLine, str.lower(inputBufferItem.message.name))
                            elif inputBufferItem.message.type == "MULTIPROCESSING" and isinstance(m, MultiProcessing):
                                func = getattr(MultiProcessing, str.lower(inputBufferItem.message.name))
                            elif inputBufferItem.message.type == "CONVEYOR" and isinstance(m, ConveyorBelt):
                                func = getattr(ConveyorBelt, str.lower(inputBufferItem.message.name))
                            # elif inputBufferItem.message.type == "PUNCHING" and isinstance(m, PunchingMachine):
                            #     func = getattr(PunchingMachine, str.lower(inputBufferItem.message.name))
                            else:
                                #TODO raise an exception here
                                logging.error(f"Invalid json command. Cannot find function {inputBufferItem.message.type}.{inputBufferItem.message.name}")
                            # Funktionsparameter in korrekte Reihenfolge bringen und mit Funktion zusammenbringen
                            logging.debug(f"Type de message : {inputBufferItem.message.type}")
                            if inputBufferItem.message.type == "GRIPPER" or inputBufferItem.message.type == "VACUUM":
                                pos = inputBufferItem.message.parameters
                                i = len(pos)
                                if i == 0:
                                    logging.debug("function called with no args")
                                    #m.setupFirst = True unschön

                                    m.incrementNbMinimumRequiredExecutionCycles()
                                    ret = func(m)
                                if i == 1:
                                    # just assume that start/end is correct
                                    # TODO get rid of assumption
                                    logging.debug("function called with one arg")
                                    m.incrementNbMinimumRequiredExecutionCycles()
                                    ret = func(m, pos[0])
                                if i == 2:
                                    if pos[0].meaning == "START" and pos[1].meaning == "END":
                                        logging.debug("function called with two args")
                                        m.incrementNbMinimumRequiredExecutionCycles()
                                        ret = func(m, pos[0], pos[1])
                                    elif pos[0].meaning == "END" and pos[1].meaning == "START":
                                        logging.debug("function called with two args")
                                        m.incrementNbMinimumRequiredExecutionCycles()
                                        ret = func(m, pos[1], pos[0])
                                    else:
                                        logging.error("command not supported - params")
                                        # TODO activate:
                                        # raise JSONCommandNotSupportedOnThisMachineException()
                            # elif inputBufferItem.message.type == "WAREHOUSE":
                            #     box = inputBufferItem.message.parameters
                            #     i = len(box)
                            #     if i == 0:
                            #         ret = func(m)
                            #     if i == 1:
                            #         ret = func(m, box[0])
                            #     #TODO clarify in API wich box the object is put to and from which the object is retrieved
                            #     #currently: first arg: in, second argument: out
                            #     if i == 2:
                            #         ret = func(m, box[0], box[1])
                            elif inputBufferItem.message.type == "SORTING" or inputBufferItem.message.type == "INDEXEDLINE" or inputBufferItem.message.type == "MULTIPROCESSING":
                                color = inputBufferItem.message.parameters
                                i = len(color)
                                if i == 0:
                                    ret = func(m)
                                if i == 1:
                                    ret = func(m, color[0])
                            #elif inputBufferItem.message.type == "PUNCHING":
                            #    ret = func(m)
                            elif inputBufferItem.message.type == "CONVEYOR":
                                mix = inputBufferItem.message.parameters
                                i = len(mix)
                                if i == 0:
                                    ret = func(m)
                                if i == 1:
                                    ret = func(m, mix[0])
                                if i == 2:
                                    if mix[0] == Direction.BACKWARD or mix[0] == Direction.FORWARD:
                                        ret = func(m, mix[0], mix[1])
                                    else:
                                        ret = func(m, mix[1], mix[0])

                            # holds the function that is currently executed on each machine
                            self.currentlyExecuting[m] = [ret, inputBufferItem.message.commandId]
                            break
                        except AttributeError as e:
                            #TODO activate:
                            #raise JSONCommandNotSupportedOnThisMachineException()
                            logging.warning(f"command not supported: \n{e}")
                            break
            if not foundMatchingMachine : 
                logging.warning(f"unknown id: {inputBufferItem.topicName}")

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
        The execute loop, which activates all the necessary functions on each machine
        """
        for key  in self.currentlyExecuting.keys():
            # call method
            if not self.currentlyExecuting[key][0] is None:
                #print(key)
                
                logging.debug(f'currentlyExecuting {self.currentlyExecuting[key][0]} [{inspect.getsource(self.currentlyExecuting[key][0]).strip()}]')
                #print(self.currentlyExecuting[key][0])
                #if key == self.robot41:
                    #print(self.currentlyExecuting[key][0])
                # noinspection PyCallingNonCallable
                ret = self.currentlyExecuting[key][0]()
                logging.debug(f'result of  self.currentlyExecuting[key][0]() = {ret}')

            # remove currentlyExecuting function once it is finished
            if key.feedback() == ExecutionStatus.FINISHED and self.currentlyExecuting[key][0] != None:
                logging.debug(f'removing {self.currentlyExecuting[key][0]} from currentlyExecuting')
                self.currentlyExecuting[key][0] = None
            # DVK    
            # if key.fakeFeedback() == ExecutionStatus.FINISHED:
            
            # #if (key.fakeFeedback() == ExecutionStatus.FINISHED) and (key.feedback() == ExecutionStatus.FINISHED):
            #     #logging.debug(str(key) + 'finished execution')
            #     self.currentlyExecuting[key][0] = None

    def createFeedbackOnChange(self) -> None:
        """Whenever the state of the machine changes, feedback is created
        a machine can be in several states as definded in the ExecutionStatus enum
        (currently, only INACTION and FINISHED are used)
        Also update the self.feedback[m] dictionnary
        """
        for m in self.machines:
            if self.feedback[m] != m.feedback():
                self.feedback[m] = m.feedback()
                # for the first time send feedback without command as id 0 (possibly problematic because of undesired program
                # flow in later real execution in error cases, no problems known so far)
                if self.currentlyExecuting[m][1] is None:
                    jsonid = 0
                else:
                    jsonid = self.currentlyExecuting[m][1]
                # append feedback to outputBuffer
                f = MachineCommandFeedback("FEEDBACK", jsonid, m.feedback().name,  "")
                j = JSONOutput(m.id, time.time(), f)
                #logging.debug("created Feedback")
                self.outputBuffer.put(j, block=False)

    def start(self):
        """
        Starts communication threads for receiving commands via Sockets and executing them
        Initiate the main loop 
        """
        logging.debug('start')

        # TODO manage connectinOpening modes : opend by controller or by orchestrator
        # listen for connection and process commandMessages in a dedicated Process
        # start the function socketConnexionHelper.listenSocket("localhost", 8888, self.receiveCommandMessage) in a Process
        Process(target=socketConnexionHelper.listenSocket, args=[self.host, self.command_port, self.receiveCommandMessages]).start()
        #Process(target=socketConnexionHelper.connectSocket, args=["localhost", self.command_port, self.receiveCommandMessages]).start()
        
        # listen for connection and send notification in a dedicated Process
        Process(target=socketConnexionHelper.listenSocket, args=[self.host, self.notification_port, self.sendNotificationMessages]).start()
        #Process(target=socketConnexionHelper.connectSocket, args=["localhost", self.command_port, self.sendNotificationMessages]).start()
        

        logging.debug('all threads started')
        signal.signal(signal.SIGINT, lambda sig, frame: signal_custom_handler(sig, frame, "Main"))
        while True:
            self.mainLoopIteration()

    def mainLoopIteration(self):
        logging.debug(f'main loop - self.inputBuffer.empty()={self.inputBuffer.empty()}')
        self.processJson(self.inputBuffer)
        self.read()
        self.exLoop()
        self.write()
        self.reset()
        # # logging.debug(self.currentlyExecuting)
        self.createFeedbackOnChange()
        
        # if a machine was executing some command, we are now sure that it was taken into account (incl. write, reset, and feedback)
        for m in self.machines:
            m.decrementNbMinimumRequiredExecutionCycles()
        time.sleep(self.mainLoopDelay)

def signal_custom_handler(sig, frame, name: str):
    process_id = os.getpid()
    logging.debug(f"Signal '{sig}' received in process {name} (PID: {process_id}).")    
    sys.exit(0)
