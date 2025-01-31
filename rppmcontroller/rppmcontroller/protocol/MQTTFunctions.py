import time
from datetime import datetime, timezone
import paho.mqtt.client as mqtt
import json
import logging

from rppmcontroller.machine.StatusKind import StatusKind

from typing import Any, Dict

class MQTTFunctions:
    def __init__(self, server, port, keepalive):
        self.server = server
        self.port = port
        self.keepalive = keepalive
        self.client = mqtt.Client()
        self.connected = False

    def connect(self):
        try:
            self.client.connect(self.server, self.port, self.keepalive)
            self.client.loop_start()
            self.connected = True
            logging.debug("Connected to MQTT server")
        except Exception as e:
            logging.error(f"Failed to connect to MQTT server: {e}")
            self.connected = False

    def disconnect(self):
        if self.connected:
            self.client.loop_stop()
            self.client.disconnect()
            self.connected = False
            logging.debug("Disconnected from MQTT server")

    def publish_message(self, machineType, machineId, parameter, value):
        try:
            if not self.connected:
                self.connect()
            # Determine the topic based on the parameter
            if 'Motor' in parameter or 'Valve' in parameter or 'Compressor' in parameter:
                topic = f"{machineType}/{machineId}/action/{parameter}"
            elif 'Step' in parameter:
                topic = f"{machineType}/{machineId}/counter/{parameter}"
            else:
                topic = f"{machineType}/{machineId}/sensor/{parameter}"

            payload = {
                "value": value,
                "timestamp": datetime.now(timezone.utc).isoformat() + 'Z'  # Current timestamp in ISO 8601 format
            }
            self.client.publish(topic, json.dumps(payload),2,True) #With QOS2 and message retention 
            logging.debug(f"Published message to topic {topic}: {payload}")

        except Exception as e:
            logging.error(f"Failed to publish message: {e}")

    def publishStatus(self, plcId : str, machineType : str, machineId : str, statusKind : StatusKind, status : Dict[str, Any], parent_key = ''):
        """Publish the status dict in dedicated topics
        """
        try:
            if status:
                if not self.connected:
                    self.connect()
                # recursively compute topics and send status in these topics
                for k, v in status.items():
                    full_key = f"{parent_key}/{k}" if parent_key else k
                    if isinstance(v, dict):
                        self.publishStatus( plcId, machineType, machineId, statusKind, v, full_key)
                    else:
                        topic = f"PLC/{plcId}/{machineType}/{machineId}/{statusKind.name.lower()}/{full_key}"
                        payload = {
                            "value": v,
                            "timestamp": datetime.now(timezone.utc).isoformat() + 'Z'  # Current timestamp in ISO 8601 format
                        }
                        self.client.publish(topic, json.dumps(payload),2,True) #With QOS2 and message retention
        except Exception as e:
            logging.error(f"Failed to publish message: {e}")