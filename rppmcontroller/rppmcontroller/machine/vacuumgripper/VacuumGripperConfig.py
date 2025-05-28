from dataclasses import dataclass

from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


@dataclass
class VacuumGripperConfig(MachineConfiguration):

    def __init__(self, counterVertical:int, counterRot:int, counterArm:int, gripperActive:bool, endVertical=False, endRot=False, endArm=False):
        self._counterVertical = counterVertical
        self._counterRot = counterRot
        self._counterArm = counterArm
        self._gripperActive = gripperActive
        self._endVertical = endVertical
        self._endRot = endRot
        self._endArm = endArm

    def __eq__(self, other):
        return (self._endArm == other.endArm and
                self._endRot == other.endRot and
                self._endVertical == other.endVertical and
                self.counterArm == other.counterArm and
                self.gripperActive == other.gripperActive and
                self.counterRot == other.counterRot and
                self.counterVertical == other.counterVertical)

    @property
    def counterVertical(self):
        return self._counterVertical

    @property
    def counterRot(self):
        return self._counterRot

    @property
    def counterArm(self):
        return self._counterArm

    @property
    def gripperActive(self):
        return self._gripperActive

    @property
    def endVertical(self):
        return self._endVertical

    @property
    def endRot(self):
        return self._endRot

    @property
    def endArm(self):
        return self._endArm
