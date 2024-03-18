class MachineStatusRequest:
    """
    Class that holds the request body of strings received in JSON-fromat. Used as an attribute in JSONOutput
    """

    def __init__(self, jsonType, mtype, requestId, params):
        self.__jsonType = jsonType
        if not self.__jsonType == "STATUSREQUEST":
            raise TypeError("not a request")
        self.__type = mtype
        self.__requestId = requestId
        self.__params = params

    @property
    def jsonType(self):
        return self.__jsonType

    @property
    def type(self):
        return self.__type

    @property
    def requestId(self):
        return self.__requestId

    @property
    def parameters(self):
        return self.__params
