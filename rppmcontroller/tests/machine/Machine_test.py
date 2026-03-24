import unittest
# from time import time
from time import sleep
from typing import Dict, Any

from rppmcontroller.behavior.CycleStepResult import CycleStepResult
from rppmcontroller.behavior.CycleStepResultEnum import CycleStepResultEnum
from rppmcontroller.machine.Machine import Machine


class MachineTestCase(unittest.TestCase):
    def setUp(self):
        dictMap = {}
        self.machine1 = GenericMachine("1", dictMap)

    def testTimeSinceExecution(self):
        self.machine1.isExecuting = True
        self.assertEqual(self.machine1.timeSinceExecution(), 0)
        self.machine1.isExecuting = False
        sleep(2)
        self.assertAlmostEqual(self.machine1.timeSinceExecution(), 2, 1)
        self.machine1.isExecuting = True
        self.assertEqual(self.machine1.timeSinceExecution(), 0)

class GenericMachine(Machine):
    def __init__(self, machine_id: str, dictMap: Dict[str, Any]):
        super().__init__(machine_id, dictMap)

    def sensorStatusString(self) -> str:
        return ""

    def actuatorStatusString(self) -> str:
        return ""

    def inputStatus(self) -> Dict[str, Any]:
        return {}

    def outputStatus(self) -> Dict[str, Any]:
        return {}

    def internalStatus(self) -> Dict[str, Any]:
        return {}

    def stop_CycleStep(self) -> CycleStepResult:
        return CycleStepResult(CycleStepResultEnum.DONE)


if __name__ == '__main__':
    unittest.main()
