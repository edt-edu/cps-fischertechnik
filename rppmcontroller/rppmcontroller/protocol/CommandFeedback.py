class CommandFeedback:
    """
    Class that holds the body in JSON-format of the Feedback message for commands.
    status possible values are defined in CommandExecutionStatus
    """
    def __init__(self, jsonType, commandId, status, info):
        self.__jsonType = jsonType
        self.__commandId = commandId
        self.__status = status
        self.__info = info

    def to_dict(self):
        return {"jsonType": self.__jsonType, "commandId": self.__commandId, "status": self.__status, "info": self.__info}

    def __repr__(self):
        return str(self.__jsonType) + " " + str(self.__commandId) + " " + str(self.__status) + " " + str(self.__info)
