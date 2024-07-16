import time
from datetime import datetime, timezone
import paho.mqtt.client as mqtt
import json
import logging

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
