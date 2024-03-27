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

import revpimodio2

import rppmcontroller
import rppmcontroller.machine
import rppmcontroller.machine.vacuumgripper
from rppmcontroller.protocol import socketConnexionHelper
from rppmcontroller.protocol.JSONParser import JSONParser
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.protocol.MachineStatusRequestAnswer import MachineStatusRequestAnswer
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.machine.vacuumgripper.VacuumGripperSimpleSimulator import VacuumGripperSimpleSimulator


class SimulatedVacuumGripperController(RevPiPyMachineController):
    """
    Class allowing to stream commands to and from  a vacuum gripper
    """

    def __init__(self, simulatedRevPiModIO: bool = False):
        """
        Init method of this class, starts all threads and everything is ready for receiving commands via Sockets and executing them
        """

        logging.debug('init started')

        super().__init__()
        

        # Instantiate RevPiModIO
        if(not simulatedRevPiModIO):
            self.rpi = revpimodio2.RevPiModIO(autorefresh=True)

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

    def write(self) -> None:
        self.vaccumGripperSimulator.simulatedWrite()
   
    def reset(self) -> None:
        # TODO find a way to simulated reset
        vg = self.vacuumGripperMachine.executeHelper()
        if vg[0]:
            self.vaccumGripperSimulator.simulatedReset()

if __name__ == "__main__":
    logging.basicConfig(format='%(levelname)-5s: %(module)-20s,%(lineno)-3s: %(message)s', level=logging.DEBUG)
    logging.debug('main')
    # Start VacuumGripperStreamer app
    root = SimulatedVacuumGripperController(simulatedRevPiModIO=True)
    # start communication threads and main control loop
    root.start()