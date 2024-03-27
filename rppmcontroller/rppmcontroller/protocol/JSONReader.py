import json as json
from rppmcontroller.protocol.decoderFunctions import customDecoder
from rppmcontroller.protocol.JSONOutput import JSONOutput
from typing import Union


class JSONReader:
    """
    Reader that takes the json String and returns the corresponding object
    Currently only method defined: read, which only works with JSONOutput strings and objects
    """

    @staticmethod
    def read(jsonString: Union[str, bytes, bytearray]) -> JSONOutput:
        return json.loads(jsonString, object_hook=customDecoder)
