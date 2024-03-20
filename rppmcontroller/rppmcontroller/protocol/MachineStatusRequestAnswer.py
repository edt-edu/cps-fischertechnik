import logging

from rppmcontroller.protocol.ParameterRequestAnswer import ParameterRequestAnswer


class MachineStatusRequestAnswer:

    def __init__(self, jsonType, requestId, answer):
        self.__jsonType = jsonType
        self.__requestId = requestId
        self.__answer = answer

    def to_dict(self):
        l = []
        for p in self.__answer:
            l.append(p.to_dict())
            #l.append(p)
        logging.debug("created answer list")
        return {"jsonType": self.__jsonType, "requestId": self.__requestId, "answer": l}

    def __repr__(self):
        return str(self.__jsonType) + " " + str(self.__requestId) + " " + str(self.__answer)