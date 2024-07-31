
import logging
import time

from rppmcontroller.protocol.JSONReader import JSONReader
from rppmcontroller.protocol.JSONParser import JSONParser
from rppmcontroller.protocol.JSONOutput import JSONOutput
from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController

def readNotification(controller : RevPiPyMachineController ) -> str :
    """ simple notification reader for test purposes

    get the output buffer content as string
    it doesn't block on the queue 

    Returns:
        str:  the queue content (string) if it contains data or an empty string
    """
    if controller.outputBuffer.qsize() > 0:
        notificationSent = controller.outputBuffer.get(block=False)
        logging.debug(f'notification sent={notificationSent}')
        # msg = JSONParser.parse(notificationSent)            
        # logging.debug(f'notification sent={msg}')
        if not "COMMAND_FEEDBACK" in notificationSent:
                    return str(notificationSent)
    else:
        logging.debug(f'no notification sent')
        return ""

def sendMessage(controller : RevPiPyMachineController, machineId : str, message) -> None :
    """ send a message after encoding it as a JSONOutput to the inputBuffer queue
    """
    jsonOutput = JSONOutput(machineId, time.time(), message)    
    controller.inputBuffer.put(jsonOutput)
    # we use a multithread Queue in a mono thread, makes sure the message is queued 
    time.sleep(0.1)