class JSONOutput:
    """
    Class that holds all the information from the JSON strings
    """

    def __init__(self, topicName, timestamp, message):
        if (topicName is None) or (timestamp is None) or (message is None):
            raise TypeError("Missing arguments")
        self.__topicName = topicName
        self.__timestamp = timestamp
        self.__message = message

    @property
    def message(self):
        return self.__message

    @property
    def topicName(self):
        return self.__topicName

    @property
    def timestamp(self):
        return self.__timestamp

    def __repr__(self):
        return str(self.__topicName) + " " + str(self.__timestamp) + " " + str(self.__message)

    def to_dict(self):
        return {"topicName": self.__topicName, "timestamp": self.__timestamp, "message": self.__message.to_dict()}
