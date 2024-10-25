import logging
import multiprocessing 
from multiprocessing import Process
from multiprocessing import Queue
from queue import Empty
import signal
import socket
import sys
import time
import json
import os


import rppmcontroller
import rppmcontroller.machine
import rppmcontroller.machine.vacuumgripper
from rppmcontroller.protocol import socketConnexionHelper
from rppmcontroller.protocol.JSONParser import JSONParser
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.protocol.MachineStatusRequestAnswer import MachineStatusRequestAnswer
from rppmcontroller.machine.StatusKind import StatusKind
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.example.VacuumGripperController import VacuumGripperController
from rppmcontroller.machine.vacuumgripper.VacuumGripperSimpleSimulator import VacuumGripperSimpleSimulator


class SimulatedVacuumGripperController(VacuumGripperController):
    """
    Class allowing to stream commands to and from  a simulated vacuum gripper
    """

    def __init__(self, configurationFile : str = ""):
        """
        Init method of this class, starts all threads and everything is ready for receiving commands via Sockets and executing them
        """

        super().__init__(simulatedRevPiModIO=True, configurationFile=configurationFile)
        
        # TODO read from a configuration file
        #the list of all machines that are connected to this core
        self.machines = []
        #dict, which keys are the machines, than there is a tuple holding the function currently executed ([0]) and the id it was sent with ([1])
        self.currentlyExecuting = {}
        self.vacuumGripperMachine = VacuumGripper("VacuumGripper01")
        self.machines = [self.vacuumGripperMachine]
        self.currentlyExecuting = {
            self.vacuumGripperMachine: [None, None]
        }

        self.feedback = {
            self.vacuumGripperMachine: None
        }


        self.vaccumGripperSimulator = VacuumGripperSimpleSimulator(self.vacuumGripperMachine)
        """Simulator for the Vacuum Gripper"""

    def read(self) -> None:
        self.vaccumGripperSimulator.simulatedRead()
        currentInput = self.vacuumGripperMachine.inputStatus()
        if self.previousInputStatus != currentInput:
            logging.info(f'VGR inputs are differents ')
            self.MQTT.publishStatus(self.plcId, "VacuumGripper", self.vacuumGripperMachine.id, StatusKind.INPUT ,self.vacuumGripperMachine.inputStatus())
        self.previousInputStatus = currentInput

    def write(self) -> None:
        self.vaccumGripperSimulator.simulatedWrite()

        currentInternalStatus = self.vacuumGripperMachine.internalStatus()
        if self.previousInternalStatus != currentInternalStatus:
            self.MQTT.publishStatus(self.plcId, "VacuumGripper", self.vacuumGripperMachine.id, StatusKind.INTERNAL ,self.vacuumGripperMachine.internalStatus())
        self.previousInternalStatus = currentInternalStatus
        
        currentOutput = self.vacuumGripperMachine.outputStatus()
        if self.previousOuputStatus != currentOutput:
            self.MQTT.publishStatus(self.plcId, "VacuumGripper", self.vacuumGripperMachine.id, StatusKind.OUTPUT ,self.vacuumGripperMachine.outputStatus())
        self.previousOuputStatus = currentOutput
   
    def reset(self) -> None:
        vg = self.vacuumGripperMachine.executeHelper()
        if vg[0]:
            self.vaccumGripperSimulator.simulatedReset()

if __name__ == "__main__":
    logging.basicConfig(format='%(asctime)s %(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s', 
                        level=logging.DEBUG,
                        datefmt='%Y-%m-%d %H:%M:%S')
    handler = logging.FileHandler("logfile.log")
    logFormatter = logging.Formatter("%(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s")
    handler.setFormatter(logFormatter)
    logging.getLogger().addHandler(handler)
    # Start VacuumGripperStreamer app
    root = SimulatedVacuumGripperController("config.yml")
    
    # start communication threads and main control loop
    root.start()