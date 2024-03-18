from abc import abstractmethod
import logging
from multiprocessing import Process
import signal
import socket
import sys
import time
import json
import os
from  typing import Callable


def listenSocket(host: str, port: int, func: Callable[[socket.socket], None], auto_reconnect : bool= False):
    """ listen for connexion on the socket
    when connected, call the func() with the socket
    if auto_reconnect, will listen continuously for new connexion
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        # allows quicker reuse of the port, otherwise a new bind will have to 
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        server_socket.bind((host, port))
        server_socket.listen(1)

        nbconnexions = 0
        while nbconnexions < 1 or auto_reconnect:
            logging.info(f"ComnandServer listening on {host}:{port}")
            client_socket, _ = server_socket.accept()
            logging.info(f"Accepted connection from {_}")
            nbconnexions = nbconnexions + 1
            
            # self.process_commands(server_socket)
            
            func(client_socket)
            client_socket.close()
        

def connectSocket(host: str, port: int, func: Callable[[socket.socket], None]):
    """  connect to the socket
    when connected, applies the func on the socket
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
        client_socket.connect((host, port))
        
        logging.info(f"connected to {host}:{port}")
        # do some work on the socket
        func(client_socket)

        

