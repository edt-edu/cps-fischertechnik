import unittest
#from time import time
from time import sleep
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
import inspect

from rppmcontroller.protocol import socketConnexionHelper

class SocketConnexionHelperTestCase(unittest.TestCase):
    def setUp(self):
        dictMap = {}
        

    def testListenSocket_noReconnect(self):        
        logging.debug(f'{inspect.stack()[0][3]} start')

        myServerClass  = MyServerClass(False)
        server = Process(target=myServerClass.run)
        server.start()
        time.sleep(0.5)
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            client_socket.connect(("localhost", 8889))

            # Send some message
            client_socket.sendall("test message".encode("utf-8"))
            # Receive the response from the server
            received_data = client_socket.recv(1024).decode("utf-8")
            logging.info(f"Received response: {received_data}")
            self.assertEqual(received_data, "test response")
            client_socket.close()
 
        # not configured for allowing more connections, the server should have ended       
        server.join(timeout=3)
        assert(not server.is_alive())
        time.sleep(0.5)
        
    def testListenSocket_reconnect(self):        
        logging.debug(f'{inspect.stack()[0][3]} start')

        myServerClass  = MyServerClass(True)
        server = Process(target=myServerClass.run)
        server.start()
        time.sleep(0.5)
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            client_socket.connect(("localhost", 8889))

            # Send some message
            client_socket.sendall("test message".encode("utf-8"))
            # Receive the response from the server
            received_data = client_socket.recv(1024).decode("utf-8")
            logging.info(f"Received response: {received_data}")
            self.assertEqual(received_data, "test response")
            client_socket.close()
        
        # configured for allowing more connections, the server should not have ended       
        assert(server.is_alive())
 
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            client_socket.connect(("localhost", 8889))

            # Send some message
            client_socket.sendall("test message".encode("utf-8"))
            # Receive the response from the server
            received_data = client_socket.recv(1024).decode("utf-8")
            logging.info(f"Received response: {received_data}")
            self.assertEqual(received_data, "test response")
            client_socket.close()
        
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client_socket:
            client_socket.connect(("localhost", 8889))

            # Send some message
            client_socket.sendall("test message".encode("utf-8"))
            client_socket.close()

        server.kill()
        time.sleep(0.5)


    def testConnectSocket_noReconnect(self):        
        logging.debug(f'{inspect.stack()[0][3]} start')

        myClientClass  = MyClientClass(False)
        client = Process(target=myClientClass.run)
        client.start()
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
            # allows quicker reuse of the port, otherwise a new bind will have to wait
            server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

            server_socket.bind(("localhost", 8888))
            server_socket.listen(1)

            logging.info(f"testConnectSocket_noReconnect listening on localhost:8888")
            client_socket, _ = server_socket.accept()
            logging.info(f"Accepted connection from {_}")
            # Send some message
            client_socket.sendall("test message".encode("utf-8"))
            # Receive the response from the server
            received_data = client_socket.recv(1024).decode("utf-8")
            logging.info(f"Received response: {received_data}")
            self.assertEqual(received_data, "test response")
 
            # not configured for allowing more connections, the client should have ended       
            client.join(timeout=3)
            assert(not client.is_alive())
            client_socket.close()
        client.kill()
        time.sleep(0.5)

 
            

class MyClientClass():
    def __init__(self, autoreconnect: bool) -> None:
        self.autoreconnect = autoreconnect
        self.funcMustExit = False
    def my_function(self, s: socket.socket) -> None:
        # Your implementation here
        if not self.funcMustExit:
            received_data = s.recv(1024).decode("utf-8")
            if received_data:
                
                logging.info(f"MyClientClass received data: {received_data}")

                s.sendall("test response".encode("utf-8"))
            else:
                # Client closed the connection
                logging.info('Server closed the connection')
        else:
            logging.info(f"MyClientClass must exit")
        time.sleep(0.2)
    
    def run(self):
        time.sleep(0.2)
        socketConnexionHelper.connectSocket("localhost", 8888, self.my_function)
        logging.info("MyClientClass end of process")

class MyServerClass():
    def __init__(self, autoreconnect: bool) -> None:
        self.autoreconnect = autoreconnect
        
    def my_function(self, s: socket.socket) -> None:
        # Your implementation here
        received_data = s.recv(1024).decode("utf-8")
        if received_data:
            logging.info(f"MyServerClass received data: {received_data}")
            s.sendall("test response".encode("utf-8"))
        else:
            # Client closed the connection
            logging.info('Client closed the connection')
        time.sleep(0.2)
    
    def run(self):
        socketConnexionHelper.listenSocket("localhost", 8889, self.my_function, self.autoreconnect)
        logging.info("MyServerClass end of process")


if __name__ == '__main__':
    logging.basicConfig(format='%(levelname)s: %(module)s,%(lineno)s: %(message)s', level=logging.DEBUG)
    # logging.debug('debug level messages displayed')
    # logging.info('info level messages displayed')
    # logging.warning('warning level messages displayed')
    # logging.error('error level messages displayed')
    unittest.main()
