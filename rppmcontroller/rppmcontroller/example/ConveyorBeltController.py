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
import ctypes

import revpimodio2

import rppmcontroller
import rppmcontroller.machine
import rppmcontroller.machine.conveyorbelt
from rppmcontroller.protocol import socketConnexionHelper
from rppmcontroller.protocol.JSONParser import JSONParser
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.protocol.MachineStatusRequestAnswer import MachineStatusRequestAnswer
from rppmcontroller.machine.conveyorbelt.ConveyorBelt import ConveyorBelt
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController


class ConveyorBeltController(RevPiPyMachineController):
    """
    Class allowing to stream commands to and from a conveyor belt
    """

    def __init__(self, simulatedRevPiModIO: bool = False, configurationFile : str = ""):
        """
        Init method of this class, starts all threads and everything is ready for receiving commands via Sockets and executing them
        """

        super().__init__(configurationFile)
        
        # Instantiate RevPiModIO
        if(not simulatedRevPiModIO):
            self.rpi = revpimodio2.RevPiModIO(autorefresh=True)

        # TODO find a way to read from a configuration file
        #the list of all machines that are connected to this core
        self.machines = []
        #dict, which keys are the machines, than there is a tuple holding the function currently executed ([0]) and the id it was sent with ([1])
        self.currentlyExecuting = {}
        self.conveyorBeltMachine = ConveyorBelt("ConveyorBelt01")
        self.machines = [self.conveyorBeltMachine]
        self.currentlyExecuting = {
            self.conveyorBeltMachine: [None, None]
        }

        self.feedback = {
            self.conveyorBeltMachine: None
        }

    def read(self):
        assert self.rpi.io is not None
        self.conveyorBeltMachine.conveyorSensFeed = self.rpi.io.dio2_I_1.value
        self.conveyorBeltMachine.conveyorSensSwap = self.rpi.io.dio2_I_2.value 
       
if __name__ == "__main__":
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)
    handler = logging.FileHandler("logfile.log")
    logFormatter = logging.Formatter("%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s")
    handler.setFormatter(logFormatter)
    logging.getLogger().addHandler(handler)
    # Start ConveyorBeltStreamer app
    root = ConveyorBeltController(configurationFile="config.yml")
    # start communication threads and main control loop
    root.start()