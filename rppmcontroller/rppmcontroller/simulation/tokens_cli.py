"""Command line client of the TokenInjector: place / remove tokens on the light barriers of a simulated PLC.

    python -m rppmcontroller.simulation.tokens_cli --config <PLC config.yml> list
    python -m rppmcontroller.simulation.tokens_cli --config <PLC config.yml> place sl-input --color red
    python -m rppmcontroller.simulation.tokens_cli --config <PLC config.yml> pulse sl-middle 1.5
    python -m rppmcontroller.simulation.tokens_cli --config <PLC config.yml> remove sl-input
    python -m rppmcontroller.simulation.tokens_cli --config <PLC config.yml> clear

The PLC id and MQTT broker are read from the PLC configuration file; --plc-id / --broker / --port override them.
"""

import argparse
import json
import sys
import threading
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, Optional

import yaml

from rppmcontroller.simulation.TokenInjector import newMqttClient

RESPONSE_TIMEOUT_SECONDS = 3.0


class TokenClient:

    def __init__(self, plcId: str, broker: str, port: int):
        self.topicPrefix = f"SIM/{plcId}/tokens"
        self.plcId = plcId
        self.broker = broker
        self.port = port
        self.client = newMqttClient(f"{plcId}-tokens-cli-{uuid.uuid4().hex[:8]}")
        self.subscribed = threading.Event()
        self.received = threading.Event()
        self.payload: Optional[Dict[str, Any]] = None

    def _waitFor(self, topic: str, matches, publish=None) -> Optional[Dict[str, Any]]:
        def onConnect(client, userdata, flags, rc):
            if rc == 0:
                client.subscribe(topic, qos=1)

        def onSubscribe(client, userdata, mid, grantedQos):
            self.subscribed.set()

        def onMessage(client, userdata, message):
            if not message.payload:
                return
            try:
                payload = json.loads(message.payload.decode("utf-8"))
            except ValueError:
                return
            if matches(payload):
                self.payload = payload
                self.received.set()

        self.client.on_connect = onConnect
        self.client.on_subscribe = onSubscribe
        self.client.on_message = onMessage
        try:
            self.client.connect(self.broker, self.port, 30)
        except Exception as e:
            sys.exit(f"cannot connect to MQTT broker {self.broker}:{self.port}: {e}")
        self.client.loop_start()
        try:
            if not self.subscribed.wait(RESPONSE_TIMEOUT_SECONDS):
                sys.exit(f"no answer from MQTT broker {self.broker}:{self.port}")
            if publish is not None:
                self.client.publish(f"{self.topicPrefix}/request", json.dumps(publish), qos=1)
            self.received.wait(RESPONSE_TIMEOUT_SECONDS)
            return self.payload
        finally:
            self.client.loop_stop()
            self.client.disconnect()

    def request(self, request: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        request = dict(request, id=uuid.uuid4().hex)
        return self._waitFor(f"{self.topicPrefix}/response",
                             lambda payload: payload.get("id") == request["id"],
                             publish=request)

    def sensorStates(self) -> Optional[Dict[str, Any]]:
        # retained message, delivered right after subscribing while the controller is running
        return self._waitFor(f"{self.topicPrefix}/sensors", lambda payload: "sensors" in payload)


def printSensorStates(states: Dict[str, Any]) -> None:
    rows = states["sensors"]
    aliasWidth = max([len("SENSOR")] + [len(row["alias"]) for row in rows])
    machineWidth = max([len("MACHINE")] + [len(row["machine"]) for row in rows])
    print(f"{'SENSOR':<{aliasWidth}}  {'MACHINE':<{machineWidth}}  {'TOKEN':<7}  ATTRIBUTE")
    for row in rows:
        token = "present" if row["tokenPresent"] else "-"
        print(f"{row['alias']:<{aliasWidth}}  {row['machine']:<{machineWidth}}  {token:<7}  {row['attribute'] or '(no light barrier)'}")

    tokens = states.get("tokens", [])
    print()
    if not tokens:
        print("no token")
        return
    print("TOKEN  COLOR  WHERE")
    for token in tokens:
        if token.get("heldBy"):
            where = f"held by {token['heldBy']}"
        else:
            where = token["at"] or f"on {token['track']}, between sensors"
            where += f" (position {token['position']})"
        if token.get("pulseEndsAt"):
            endsAt = datetime.strptime(token["pulseEndsAt"], "%Y-%m-%dT%H:%M:%S.%fZ").replace(tzinfo=timezone.utc)
            remaining = max(0.0, (endsAt - datetime.now(timezone.utc)).total_seconds())
            where += f", removed in {remaining:.1f}s"
        print(f"{token['id']:<5}  {token['color'] or '-':<5}  {where}")


def parseArguments(argv) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="tokens",
        description="Place / remove tokens on the light barriers of a simulated PLC (TokenInjector).")
    parser.add_argument("--config", help="PLC configuration file (reads plc.id and mqtt.server/port)")
    parser.add_argument("--plc-id", help="PLC id (default: plc.id from --config)")
    parser.add_argument("--broker", help="MQTT broker host (default: mqtt.server from --config, else localhost)")
    parser.add_argument("--port", type=int, help="MQTT broker port (default: mqtt.port from --config, else 1883)")

    commands = parser.add_subparsers(dest="command", metavar="command")
    commands.required = True

    commands.add_parser("list", help="list the sensors and whether a token is on them")

    place = commands.add_parser("place", help="place a token on a sensor; it then moves with the machines")
    place.add_argument("sensor", help="sensor alias, e.g. sl-input (see 'list')")
    place.add_argument("--color", help="token color (white, red, blue), seen by the SortingLine color sensor")

    remove = commands.add_parser("remove", help="remove the tokens on a sensor")
    remove.add_argument("sensor", help="sensor alias, e.g. sl-input (see 'list')")

    pulse = commands.add_parser("pulse", help="place a token on a sensor, removed after DURATION seconds wherever it is")
    pulse.add_argument("sensor", help="sensor alias, e.g. sl-input (see 'list')")
    pulse.add_argument("duration", nargs="?", type=float, default=1.0, help="seconds (default: 1.0)")
    pulse.add_argument("--color", help="token color (white, red, blue), seen by the SortingLine color sensor")

    commands.add_parser("clear", help="remove every token")

    return parser.parse_args(argv)


def main(argv=None) -> None:
    args = parseArguments(argv)

    config: Dict[str, Any] = {}
    if args.config:
        with open(args.config, "r") as file:
            config = yaml.safe_load(file) or {}
    plcId = args.plc_id or config.get("plc", {}).get("id")
    if not plcId:
        sys.exit("unknown PLC id: pass --config <PLC config.yml> or --plc-id")
    broker = args.broker or config.get("mqtt", {}).get("server") or "localhost"
    port = args.port or config.get("mqtt", {}).get("port") or 1883

    client = TokenClient(plcId, broker, port)

    if args.command == "list":
        states = client.sensorStates()
        if states is None:
            sys.exit(f"no sensor states on {client.topicPrefix}/sensors - is the simulated PLC '{plcId}' running?")
        printSensorStates(states)
        return

    request: Dict[str, Any] = {"action": args.command}
    if args.command != "clear":
        request["sensor"] = args.sensor
    if getattr(args, "color", None):
        request["color"] = args.color
    if args.command == "pulse":
        request["duration"] = args.duration

    response = client.request(request)
    if response is None:
        sys.exit(f"no response on {client.topicPrefix}/response - is the simulated PLC '{plcId}' running?")
    print(response["message"])
    if not response["ok"]:
        sys.exit(1)


if __name__ == "__main__":
    main()
