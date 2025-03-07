class MachineFeedback:
    """
    Class that holds the body in JSON-format of the Feedback message for machine state.
    """

    def __init__(self, jsonType, status, info):
        self.__jsonType = jsonType
        self.__status = status
        self.__info = info

    def to_dict(self):
        return {"jsonType": self.__jsonType, "status": self.__status, "info": self.__info}

    def __repr__(self):
        return str(self.__jsonType) + " " + str(self.__status) + " " + str(self.__info)
