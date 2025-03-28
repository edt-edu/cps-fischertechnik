from rppmcontroller.machine.MachineConfiguration import MachineConfiguration


class MultiProcessingConfig(MachineConfiguration):
    def __init__(self):
        # TODO make the following state representable
        # tt pos (3) (conv implies feeder, saw implies saw)
        # conv
        # oven (implies lamp and door)
        # vac state (3: at tt, at ov, pickup)
        pass
