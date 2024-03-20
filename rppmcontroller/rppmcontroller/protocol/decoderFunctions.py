import logging

from rppmcontroller.protocol.MachineCommand import MachineCommand
from rppmcontroller.protocol.MachineStatusRequest import MachineStatusRequest
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.machine.Position import Position
from rppmcontroller.machine.BoxNumber import BoxNumber
from rppmcontroller.machine.Direction import Direction
from rppmcontroller.machine.Colour import Colour
from rppmcontroller.protocol.RequestedParameter import RequestedParameter


def customDecoder(idict):
    """
    decoder to decode a json String representing JSONOutput.class into JSONOutput.py
    :param idict: the jsonString as a dictionary
    :return: the JSONOutput object filled with data
    """
    # typical header for all JSONOutput received
    if 'topicName' and 'timestamp' and 'message' in idict:
        topicName = idict['topicName']
        timestamp = idict['timestamp']
        message = idict['message']
        # further information contained in the message body

        # case command
        if 'jsonType' in message and message['jsonType'] == 'COMMAND':
            logging.debug("command type")
            jsonType = message['jsonType']
            type = message['type']
            commandId = message['outputId']
            name = message['name']
            parameters = message['parameters']
            parameterList = []
            # map parameters to the correct classes according to their type in the message
            for param in parameters:
                if 'passableType' and 'passable' in param and param['passableType'] == 'POSITIONPARAMETERTHREED':
                    passable = param['passable']
                    meaning = passable['meaning']
                    vertical = passable['vertical']
                    rot = passable['rot']
                    horizontal = passable['horizontal']
                    parameterList.append(Position(meaning, vertical, rot, horizontal))
                elif 'passableType' and 'passable' in param and param['passableType'] == 'BOXNUMBER':
                    passable = param['passable']
                    parameterList.append(BoxNumber[passable])
                elif 'passableType' and 'passable' in param and param['passableType'] == 'COLOUR':
                    passable = param['passable']
                    parameterList.append(Colour[passable])
                elif 'passableType' and 'passable' in param and param['passableType'] == 'NUMBERNATURAL':
                    passable = param['passable']
                    numberstring = passable['number']
                    # already implicitly converted to int by reader
                    parameterList.append(numberstring)
                elif 'passableType' and 'passable' in param and param['passableType'] == 'DIRECTION':
                    passable = param['passable']
                    parameterList.append(Direction[passable])
                elif 'passableType' and 'passable' in param and param['passableType'] == 'TURTLEBOTPOSITION':
                    passable = param['passable']
                    meaning = passable['meaning']
                    numberstring = passable['coordinateX']
                    numberstring = passable['coordinateY']
                    parameterList.append(numberstring)
                elif 'passableType' and 'passable' in param and param['passableType'] == 'TURTLEBOTGRIPPER':
                    passable = param['passable']
                    meaning = passable['meaning']
            # create the python-objects from the information gathered and return them
            m = MachineCommand(jsonType, type, commandId, name, parameterList)
            jsonOutput = JSONOutput(topicName, timestamp, m)
            return jsonOutput

        #case request
        if 'jsonType' in message and message['jsonType'] == 'STATUSREQUEST':
            logging.debug("Request type")
            jsonType = message['jsonType']
            type = message['type']
            requestId = message['requestId']
            params = message['params']
            requestedList = []
            # extract the request Parameters into a list
            for requ in params:
                requestedList.append(RequestedParameter[requ])
            # create the python-objects from the information gathered and return them
            r = MachineStatusRequest(jsonType, type, requestId, requestedList)
            jsonOutput = JSONOutput(topicName, timestamp, r)
            return jsonOutput

    return idict
