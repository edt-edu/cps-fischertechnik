class MachineCommand(object):
    """
    Class that holds the command body of strings received in JSON-fromat. Used as an attribute in JSONOutput
    """

    def __init__(self, jsonType, mtype, commandId, name, parameters):
        if (jsonType is None) or (mtype is None) or (commandId is None) or (name is None) or (parameters is None):
            raise TypeError("Missing arguments")
        self.__jsonType = jsonType
        if not self.__jsonType == "COMMAND":
            raise TypeError("not a command, but a " + str(self.__jsonType))
        self.__type = mtype
        self.__commandId = commandId
        self.__name = name
        self.__parameters = parameters

    def __repr__(self):
        return str(self.__jsonType) + " " + str(self.__type) + " " + str(self.__commandId) + " " + str(self.__name) + " " + str(self.__parameters)

    def to_dict(self):
        return {"jsonType": self.__jsonType, "type": str(self.__type), "commandId": self.__commandId, "name": self.__name, "parameters": self.__parameters}
    
    @property
    def jsonType(self):
        return self.__jsonType

    @property
    def commandId(self):
        return self.__commandId

    @property
    def name(self):
        return self.__name

    @property
    def type(self):
        return self.__type

    @property
    def parameters(self):
        return self.__parameters

