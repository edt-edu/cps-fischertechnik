class ParameterRequestAnswer:

    def __init__(self, requestedParameter, value, valueClass):
        self.__requestedParameter = requestedParameter
        self.__value = value
        self.__valueClass = valueClass

    def to_dict(self):
        return {"requestedParameter": self.__requestedParameter, "value": self.__value, "valueClass": self.__valueClass}

    def __repr__(self):
        return str(self.__requestedParameter) + " " + str(self.__value) + " " + str(self.__valueClass)