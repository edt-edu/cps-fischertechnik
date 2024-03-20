class MachineConfiguration:
    """
    Common functions for all MachineConfiguration
    """

    def __str__(self) -> str:
        """
        human representation of the configuration
        """
        
        className = type(self).__name__
        s = ', '.join(f"{attr}={getattr(self, attr)}" for attr in vars(self))

        return f"{className}({s.replace('_'+className, '')})"