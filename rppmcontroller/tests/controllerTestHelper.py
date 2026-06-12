
import inspect
import logging
import time
from typing import Callable, Optional, List

from rppmcontroller.RevPiPyMachineController import RevPiPyMachineController
from rppmcontroller.protocol.JSONOutput import JSONOutput

pending_non_conform_notifications: List[str] = []
"""a list of read non-conform notifications, in case we want to read those later"""

def clearPendingNotifications():
    """Removes all notifications that were read, but non-conform from the buffer"""
    pending_non_conform_notifications.clear()


def readNotification(controller: RevPiPyMachineController, notification_filter: Optional[Callable[[str], bool]] = None) -> str:
    """ simple notification reader for test purposes

    get the output buffer content as string
    it doesn't block on the queue

    Returns:
        str: the queue content (string) if it contains data or an empty string
    """
    # first we check whether there is still pending previous non-conform feedback, which might now conform
    for notification in pending_non_conform_notifications:
        if notification_filter is None or notification_filter(notification):
            pending_non_conform_notifications.remove(notification)
            logging.warning(f"found previous non-conform notification: {notification}")
            return notification

    # if we didn't find anything, we now check for new feedback
    while not controller.outputBuffer.empty():
        notification_sent = controller.outputBuffer.get(block=False)
        notification_sent = str(notification_sent) # we want to be sure we are working with strings in the following
        if notification_filter is None or notification_filter(notification_sent):
            logging.info(f"notification is conform: {notification_sent}")
            return notification_sent

        if notification_filter is not None:
            try:
                logging.debug(f"adding non-conform message to pending: {notification_sent} (was expecting {inspect.getsource(notification_filter)})")
            except OSError:
                logging.debug(f"adding non-conform message to pending: {notification_sent} (was expecting [Source not available (maybe defined in REPL or compiled)]")
                print("Source not available (maybe defined in REPL or compiled).")

        else:
            logging.debug(f"adding non-conform message to pending: {notification_sent}")
        pending_non_conform_notifications.append(notification_sent)

    logging.debug("no notification sent")
    return ""

def readMachineFeedbackNotification(controller: RevPiPyMachineController) -> str:
    """ simple notification reader for test purposes

    get the output buffer content as string
    it doesn't block on the queue

    this function acts like a filter to receive only MACHINE_FEEDBACK notification

    Returns:
        str: the queue content (string) if it contains data or an empty string
    """
    return readNotification(controller, lambda notification: "MACHINE_FEEDBACK" in notification)


def readCommandFeedbackNotification(controller: RevPiPyMachineController) -> str:
    """ simple notification reader for test purposes

    get the output buffer content as string
    it doesn't block on the queue

    this function acts like a filter to receive only COMMAND_FEEDBACK notification

    Returns:
        str: the queue content (string) if it contains data or an empty string
    """
    return readNotification(controller, lambda notification: "COMMAND_FEEDBACK" in notification)


def sendMessage(controller: RevPiPyMachineController, machineId: str, message) -> None:
    """ send a message after encoding it as a JSONOutput to the inputBuffer queue
    """
    jsonOutput = JSONOutput(machineId, time.time(), message)
    controller.inputBuffer.put(jsonOutput)
    # we use a multithread Queue in a mono thread, makes sure the message is queued
    time.sleep(0.1)
