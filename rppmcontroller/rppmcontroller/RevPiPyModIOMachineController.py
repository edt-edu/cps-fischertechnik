import logging
import ctypes

import revpimodio2

from rppmcontroller.machine.vacuumgripper.VacuumGripper import VacuumGripper
from rppmcontroller.machine.sortingLine.SortingLine import SortingLine
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.protocol.MQTTFunctions import MQTTFunctions
from rppmcontroller.machine.Type import Type



class RevPiPyModIOMachineController(RevPiPyMachineController):
    """
     Intermediate class for not simulated controllers
    """

    def __init__(self, configurationFile : str = ""):
        """
        Init method of this class, starts all threads and everything is ready for receiving commands via Sockets and executing them
        """
        super().__init__(configurationFile)
        
        self.rpi = revpimodio2.RevPiModIO(autorefresh=True)

        self.machines = []
        self.MQTT : MQTTFunctions

    def updateValueRead(self, machineType, machineNumber, valName, valDIO, parameter, typeOfValue=Type.BOOLEAN):
        """
        Called by read() in the controller. This function update the machine attribute and send a message via MQTT
        """
        valBefore = getattr(self.machines[machineNumber], valName)
        if typeOfValue == Type.POSITIVEINT32:
            valAfter = ctypes.c_int32(getattr(self.rpi.io, valDIO).value).value
        elif typeOfValue == Type.NEGATIVEINT32:
            valAfter = -ctypes.c_int32(getattr(self.rpi.io, valDIO).value).value
        else:
            valAfter = getattr(self.rpi.io, valDIO).value

        if valBefore != valAfter:
            setattr(self.machines[machineNumber], valName, valAfter)
            self.MQTT.publish_message(machineType, self.machines[machineNumber].id, str(parameter), valAfter)

    def updateValueWrite(self, machineType, machineNumber, valName, valDIO, parameter):
        """
        Called by write() in the controller. This function update the value on the DIO and send a message via MQTT
        """
        valBefore = getattr(self.rpi.io, valDIO).value
        valAfter = getattr(self.machines[machineNumber], valName)
        if valBefore != valAfter:
            setattr(getattr(self.rpi.io, valDIO), 'value', valAfter)
            self.MQTT.publish_message(machineType, self.machines[machineNumber].id, str(parameter), valAfter)