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
import rppmcontroller.machine.vacuumgripper
from rppmcontroller.protocol import socketConnexionHelper
from rppmcontroller.protocol.JSONParser import JSONParser
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.protocol.MachineStatusRequestAnswer import MachineStatusRequestAnswer
from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController


class VacuumGripperController(RevPiPyMachineController):
    """
    Class allowing to stream commands to and from  a vacuum gripper
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
        self.vacuumGripperMachine = VacuumGripper("VacuumGripper01")
        self.machines = [self.vacuumGripperMachine]
        self.currentlyExecuting = {
            self.vacuumGripperMachine: [None, None]
        }

        self.feedback = {
            self.vacuumGripperMachine: None
        }
        self.commandFeedback = {
            self.vacuumGripperMachine: None
        }

    def read(self):
        # TODO find a way to read from a configuration file
        assert self.rpi.io is not None
        self.vacuumGripperMachine.vacuumSensVerticalEndUp = self.rpi.io.dio1_I_1.value
        self.vacuumGripperMachine.vacuumSensArmEndIn = self.rpi.io.dio1_I_2.value
        self.vacuumGripperMachine.vacuumSensRotEnd = self.rpi.io.dio1_I_3.value
        # use signed int32 to deal with possible negative values of the encoders
        self.vacuumGripperMachine.vacuumSensVerticalEncoderCounter = ctypes.c_int32(self.rpi.io.dio1_Counter_5.value).value
        self.vacuumGripperMachine.vacuumSensArmEncoderCounter = ctypes.c_int32(self.rpi.io.dio1_Counter_7.value).value
        # note: the rotation encoder counts in negative when going counterclockwise
        self.vacuumGripperMachine.vacuumSensRotEncoderCounter = -ctypes.c_int32(self.rpi.io.dio1_Counter_9.value).value



        # once setup: maximum physical observed values are:
        # -12 <= vacuumSensVerticalEncoderCounter <= 1779
        # -1 <= vacuumSensArmEncoderCounter <= 2017
        # -1 <= vacuumSensRotEncoderCounter <= 3053


    def write(self):
        # TODO find a way to read from a configuration file
        assert self.rpi.io is not None
        self.rpi.io.dio1_O_1.value = self.vacuumGripperMachine.vacuumActVerticalUp
        self.rpi.io.dio1_O_2.value = self.vacuumGripperMachine.vacuumActVerticalDown
        self.rpi.io.dio1_O_3.value = self.vacuumGripperMachine.vacuumActArmIn
        self.rpi.io.dio1_O_4.value = self.vacuumGripperMachine.vacuumActArmOut
        self.rpi.io.dio1_O_5.value = self.vacuumGripperMachine.vacuumActRotRight
        self.rpi.io.dio1_O_6.value = self.vacuumGripperMachine.vacuumActRotLeft
        self.rpi.io.dio1_O_7.value = self.vacuumGripperMachine.vacuumActCompressorOn
        self.rpi.io.dio1_O_8.value = self.vacuumGripperMachine.vacuumActValve


         
    
    def reset(self) -> None:
        # TODO find a way to read from a configuration file
        assert self.rpi.io is not None
        vg = self.vacuumGripperMachine.executeHelper()
        if vg[0]:
            self.rpi.io.dio1_Counter_5.reset()
            self.rpi.io.dio1_Counter_7.reset()
            self.rpi.io.dio1_Counter_9.reset()

if __name__ == "__main__":
    logging.basicConfig(format='%(asctime)s %(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s', 
                        level=logging.DEBUG,
                        datefmt='%Y-%m-%d %H:%M:%S')
    handler = logging.FileHandler("logfile.log")
    logFormatter = logging.Formatter("%(levelname)-5s: %(module)-30s,%(lineno)-3s: %(message)s")
    handler.setFormatter(logFormatter)
    logging.getLogger().addHandler(handler)
    # Start VacuumGripperStreamer app
    root = VacuumGripperController(configurationFile="config.yml")
    # start communication threads and main control loop
    root.start()