from rppmcontroller.machine.MachineConfiguration import MachineConfiguration

class ConveyorBeltConfig(MachineConfiguration):

    def __init__(self, counterForward:int, counterBackward:int, detectFeed=False, detectSwap=False, pulseButton=False):
        self.__counterForward = counterForward
        self.__counterBackward = counterBackward
        self.__detectFeed = detectFeed
        self.__detectSwap = detectSwap
        self.__pulseButton = pulseButton


    def __eq__(self, other):
        return (self.__detectFeed == other.detectFeed and
                self.__detectSwap == other.detectSwap and
                self.__pulseButton == other.pulseButton and
                self.counterForward == other.counterForward and
                self.counterBackward == other.counterBackward)

    @property
    def counterForward(self):
        return self.__counterForward

    @property
    def counterBackward(self):
        return self.__counterBackward

    @property
    def detectFeed(self):
        return self.__detectFeed

    @property
    def detectSwap(self):
        return self.__detectSwap

    @property
    def pulseButton(self):
        return self.__pulseButton