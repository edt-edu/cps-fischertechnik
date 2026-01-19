import unittest
from dataclasses import dataclass
from typing import Optional

from rppmcontroller.machine.Timer import Timer
from rppmcontroller.machine.indexedline.IndexedLine import IndexedLine


@dataclass
class ActState:
    act_feed_conveyor = False
    act_slider1_forward = False
    act_slider1_backward = False
    act_milling_conveyor = False
    act_milling = False
    act_drilling_conveyor = False
    act_drilling = False
    act_slider2_forward = False
    act_slider2_backward = False
    act_swap_conveyor = False


class IndexedLineTestCase(unittest.TestCase):
    def setUp(self):
        i_line = IndexedLine("1")
        i_line.indexedLineSensSlider1Rear = True
        i_line.indexedLineSensSlider2Rear = True
        self.indexedLine = i_line
        Timer.custom_current_time = 0

    def test_process1(self):
        i_line = self.indexedLine
        runner = i_line.process1_Command()
        expected_state = ActState()

        # runner should wait for a payload
        runner.run()
        self.assert_act_state(expected_state, "not waiting for payload")

        # time progression should change nothing
        Timer.custom_current_time += 5
        runner.run()
        self.assert_act_state(expected_state, "did not wait for payload")

        # put a payload on the feeder
        i_line.indexedLineSensLoading = False

        # nothing should change immediately
        runner.run()
        self.assert_act_state(expected_state,
                              "not waiting after registering payload")

        # the runner should wait for 0.5 seconds
        Timer.custom_current_time += 0.4
        runner.run()
        self.assert_act_state(expected_state, "state changed too early")
        Timer.custom_current_time += 0.1

        # the runner should then start the feed conveyor
        expected_state.act_feed_conveyor = True
        runner.run()
        self.assert_act_state(expected_state, "didn't start the feed conveyor")

        # state should not change when the loading sensor is left
        i_line.indexedLineSensLoading = True  # the payload has left the sensor by now
        runner.run()
        self.assert_act_state(expected_state,
                              "should keep transporting the payload")

        # until the light barrier is reached, nothing should change
        Timer.custom_current_time += 5
        runner.run()
        self.assert_act_state(expected_state,
                              "should keep transporting the payload")

        # the payload has reached the light barrier at slider 1
        i_line.indexedLineSensSlider1 = False

        # the runner shouldn't change state for another second
        runner.run()
        self.assert_act_state(expected_state, "changed state too early")

        # leaving the sensor should not change the state
        i_line.indexedLineSensSlider1 = True
        runner.run()
        self.assert_act_state(expected_state, "leaving slider 1 sensor changed the state")

        # The runner should maintain state for a second
        Timer.custom_current_time += 0.9
        runner.run()
        self.assert_act_state(expected_state, "state changed too early")
        Timer.custom_current_time += 0.1

        # now slider 1 should push the payload onto the milling conveyor
        expected_state.act_feed_conveyor = False
        expected_state.act_slider1_forward = True
        expected_state.act_milling_conveyor = True
        runner.run()
        self.assert_act_state(expected_state,
                              "not moving payload onto milling conveyor")

        # state should be mostly maintained when slider 1 is fully extended
        i_line.indexedLineSensSlider1Rear = False
        i_line.indexedLineSensSlider1Front = True
        expected_state.act_slider1_forward = False
        runner.run()
        self.assert_act_state(expected_state,
                              "not continuing to move payload onto milling conveyor")

        # runner should maintain state until payload reaches the milling sensor
        Timer.custom_current_time += 5
        runner.run()
        self.assert_act_state(expected_state,
                              "changed state without payload reaching milling sensor")

        # payload reaches the milling sensor; milling conveyor should stop and slider 1 retract
        i_line.indexedLineSensMilling = False
        expected_state.act_milling_conveyor = False
        expected_state.act_slider1_backward = True
        runner.run()
        self.assert_act_state(expected_state,
                              "not stopping at milling station")

        # slider 1 fully retracts; runner should start milling for 2 seconds
        i_line.indexedLineSensSlider1Rear = True
        i_line.indexedLineSensSlider1Front = False
        expected_state.act_slider1_backward = False
        expected_state.act_milling = True
        runner.run()
        self.assert_act_state(expected_state,"not milling")

        # milling state should be maintained for 2 seconds
        Timer.custom_current_time += 1.9
        runner.run()
        self.assert_act_state(expected_state,"stopped milling too early")
        Timer.custom_current_time += 0.1

        # runner should stop milling now and activate conveyors towards drill
        expected_state.act_milling = False
        expected_state.act_milling_conveyor = True
        expected_state.act_drilling_conveyor = True
        runner.run()
        self.assert_act_state(expected_state, "not moving towards drill")

        # nothing should change if the runner now leaves the milling sensor
        i_line.indexedLineSensMilling = True
        runner.run()
        self.assert_act_state(expected_state, "changed state by leaving milling sensor")

        # nothing should change until the drill sensor is reached
        Timer.custom_current_time += 5
        runner.run()
        self.assert_act_state(expected_state, "changed state without reaching drill sensor")

        # drill sensor is reached; conveyors should be stopped and drilling should start
        i_line.indexedLineSensDrilling = False
        expected_state.act_milling_conveyor = False
        expected_state.act_drilling_conveyor = False
        expected_state.act_drilling = True
        runner.run()
        self.assert_act_state(expected_state, "not drilling")

        # drilling should stop after 2 seconds
        Timer.custom_current_time += 1.9
        runner.run()
        self.assert_act_state(expected_state, "stopped drilling too early")
        Timer.custom_current_time += 0.1

        # drilling should stop now; start moving to slider 2
        expected_state.act_drilling = False
        expected_state.act_drilling_conveyor = True
        runner.run()
        self.assert_act_state(expected_state, "not moving to slider 2")

        # state should not change when payload leaves drilling sensor
        i_line.indexedLineSensDrilling = True
        runner.run()
        self.assert_act_state(expected_state, "changed state by leaving drilling sensor")

        # state should not change for 1.5 seconds
        Timer.custom_current_time += 1.4
        runner.run()
        self.assert_act_state(expected_state, "changed state too early")
        Timer.custom_current_time += 0.1

        # the payload should be pushed to the swap station until it hits the next sensor
        expected_state.act_drilling_conveyor = False
        expected_state.act_slider2_forward = True
        expected_state.act_swap_conveyor = True
        runner.run()
        self.assert_act_state(expected_state, "not moving payload onto swap station")

        # state should be mostly maintained when slider 2 is fully extended
        i_line.indexedLineSensSlider2Front = True
        i_line.indexedLineSensSlider2Rear = False
        expected_state.act_slider2_forward = False
        runner.run()
        self.assert_act_state(expected_state, "not continuing to move payload onto swap station")

        # runner should maintain state until payload reaches the swap sensor
        Timer.custom_current_time += 5
        runner.run()
        self.assert_act_state(expected_state, "state changed without payload reaching swap sensor")

        # payload reaches the swap sensor; slider 2 should retract but swap conveyor should remain active for another second
        expected_state.act_slider2_backward = True
        runner.run()
        self.assert_act_state(expected_state, "only slider 2 should have been retracted")

        # slider 2 reaching the end should largely maintain the state
        i_line.indexedLineSensSlider2Front = False
        i_line.indexedLineSensSlider2Rear = True
        expected_state.act_slider2_backward = False

        # runner should remain like this for a second
        Timer.custom_current_time += 0.9
        runner.run()
        self.assert_act_state(expected_state, "state changed too early")
        Timer.custom_current_time += 0.1

        # runner should stop now
        expected_state.act_swap_conveyor = False
        runner.run()
        self.assert_act_state(expected_state, "did not stop")

    def assert_act_state(self, act_state: ActState, msg: Optional[str] = None):
        i_line = self.indexedLine
        self.assertEqual(act_state.act_feed_conveyor,
                         i_line.indexedLineActFeedConveyor,
                         join_reasons(msg, "bad feed conveyor state"))
        self.assertEqual(act_state.act_slider1_forward,
                         i_line.indexedLineActSlider1Forward,
                         join_reasons(msg, "bad slider 1 forward state"))
        self.assertEqual(act_state.act_slider1_backward,
                         i_line.indexedLineActSlider1Backward,
                         join_reasons(msg, "bad slider 1 backward state"))
        self.assertEqual(act_state.act_milling_conveyor,
                         i_line.indexedLineActMillingConveyor,
                         join_reasons(msg, "bad milling conveyor state"))
        self.assertEqual(act_state.act_milling, i_line.indexedLineActMilling,
                         join_reasons(msg, "bad milling state"))
        self.assertEqual(act_state.act_drilling_conveyor,
                         i_line.indexedLineActDrillingConveyor,
                         join_reasons(msg, "bad drilling conveyor state"))
        self.assertEqual(act_state.act_drilling, i_line.indexedLineActDrilling,
                         join_reasons(msg, "bad drilling state"))
        self.assertEqual(act_state.act_slider2_forward,
                         i_line.indexedLineActSlider2Forward,
                         join_reasons(msg, "bad slider 2 forward state"))
        self.assertEqual(act_state.act_slider2_backward,
                         i_line.indexedLineActSlider2Backward,
                         join_reasons(msg, "bad slider 2 backward state"))
        self.assertEqual(act_state.act_swap_conveyor,
                         i_line.indexedLineActSwapConveyor,
                         join_reasons(msg, "bad swap conveyor state"))


def join_reasons(msg: Optional[str], cause: str) -> str:
    return f"{msg}: {cause}" if msg is not None else cause


if __name__ == '__main__':
    unittest.main()
