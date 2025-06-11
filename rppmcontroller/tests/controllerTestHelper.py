
import logging
import time
from typing import Callable, Optional

from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.protocol.JSONOutput import JSONOutput

pending_non_conform_notifications: list[str] = []
"""a list of read non-conform notifications, in case we want to read those later"""

def clearPendingNotifications():
    """Removes all notifications which were read, but non-conform from the buffer"""
    pending_non_conform_notifications.clear()


def readNotification(controller: RevPiPyMachineController, notification_filter: Optional[Callable[[str], bool]] = None) -> str:
    """ simple notification reader for test purposes

    get the output buffer content as string
    it doesn't block on the queue

    Returns:
        str:  the queue content (string) if it contains data or an empty string
    """
    # first we check whether there is still pending previous non-conform feedback, which might now be conform
    for notification in pending_non_conform_notifications:
        if notification_filter is None or notification_filter(notification):
            pending_non_conform_notifications.remove(notification)
            logging.debug(f"found previous non-conform notification: {notification}")
            return notification

    # if we didn't find anything, we now check for new feedback
    while not controller.outputBuffer.empty():
        notification_sent = controller.outputBuffer.get(block=False)
        logging.debug(f"notification sent: {notification_sent}")
        notification_sent = str(notification_sent) # we want to be sure we are working with strings in the following
        if notification_filter is None or notification_filter(notification_sent):
            logging.debug("notification is conform")
            return notification_sent

        logging.debug(f"adding non-conform message to pending: {notification_sent}")
        pending_non_conform_notifications.append(notification_sent)

    logging.debug("no notification sent")
    return ""

def readMachineFeedbackNotification(controller: RevPiPyMachineController) -> str:
    """ simple notification reader for test purposes

    get the output buffer content as string
    it doesn't block on the queue

    this function act like a filter to receive only MACHINE_FEEDBACK notification

    Returns:
        str:  the queue content (string) if it contains data or an empty string
    """
    return readNotification(controller, lambda notification: "MACHINE_FEEDBACK" in notification)


def readCommandFeedbackNotification(controller: RevPiPyMachineController) -> str:
    """ simple notification reader for test purposes

    get the output buffer content as string
    it doesn't block on the queue

    this function act like a filter to receive only COMMAND_FEEDBACK notification

    Returns:
        str:  the queue content (string) if it contains data or an empty string
    """
    return readNotification(controller, lambda notification: "COMMAND_FEEDBACK" in notification)


def sendMessage(controller: RevPiPyMachineController, machineId: str, message) -> None:
    """ send a message after encoding it as a JSONOutput to the inputBuffer queue
    """
    jsonOutput = JSONOutput(machineId, time.time(), message)
    controller.inputBuffer.put(jsonOutput)
    # we use a multithread Queue in a mono thread, makes sure the message is queued
    time.sleep(0.1)
