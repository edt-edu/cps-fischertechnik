import unittest

from rppmcontroller.machine.Timer import Timer
from rppmcontroller.machine.indexedline.IndexedLine import IndexedLine


class IndexedLineTestCase(unittest.TestCase):
    def setUp(self):
        self.indexedLine = IndexedLine("1")
        Timer.custom_current_time = 0

    def test_process1(self):
        runner = self.indexedLine.process1_Command()

        # runner should wait for a payload
        runner.run()

        # TODO assert machine is stopped
        self.assertEqual(False, self.indexedLine.indexedLineActFeedConveyor)

        for _ in range(5):
            Timer.custom_current_time += 1
            runner.run()
            self.assertEqual(False,
                             self.indexedLine.indexedLineActFeedConveyor)

        # TODO assert machine is still stopped

        # TODO test further



if __name__ == '__main__':
    unittest.main()
