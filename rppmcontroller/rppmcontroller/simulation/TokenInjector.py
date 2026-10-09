"""Remote token injection for simulated machines.

The simple simulators never change their light barriers on their own, so a simulated factory stays idle
(e.g. a SortingLine EJECT waits forever for its middle light barrier). The TokenInjector lets a test tool
place / remove tokens on the light barriers over MQTT, while the controller keeps running. The placed tokens then
move with the machines (see TokenWorld): a running belt carries them to the next light barrier, an ejector pushes
them into its chute, ...

    request  (in)            SIM/<plcId>/tokens/request   {"id": "...", "action": "place|remove|pulse|clear",
                                                           "sensor": "sl-input", "duration": 1.0, "color": "RED"}
    response (out)           SIM/<plcId>/tokens/response  {"id": "...", "ok": true, "message": "..."}
    state    (retained)      SIM/<plcId>/tokens/sensors   {"sensors": [{"alias", "machine", "attribute", "tokenPresent"}],
                                                           "tokens": [{"id", "color", "track", "position", "at",
                                                                       "heldBy", "pulseEndsAt"}], "timestamp"}

``pulse`` places a token and removes it after ``duration`` seconds, wherever it is by then.

Requests are received on the MQTT network thread but only queued there: they are applied by
``applyPendingRequests()`` and tokens are moved by ``simulateMovement()``, both called from the controller main
loop, so machine attributes are only ever modified by the main loop thread.

Light barriers are active-low: ``False`` means a token interrupts the beam, ``True`` means the beam is clear.
The changed sensor values are then published by the controller like any other input change
(PLC/<plcId>/<type>/<id>/measurements/input/...), so the gateway and the digital twin see them as real.
"""

import json
import logging
import queue
import time
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

import paho.mqtt.client as mqtt

from rppmcontroller.simulation.TokenWorld import COLORS, GripperTrack, Token, TokenWorld


MAX_MOVEMENT_STEP_SECONDS = 0.5
"""Longest belt travel applied in one cycle, so a stalled main loop doesn't teleport tokens."""


def utcIsoTimestamp(epochSeconds: float) -> str:
    return datetime.fromtimestamp(epochSeconds, timezone.utc).isoformat().replace("+00:00", "Z")


def newMqttClient(clientId: str) -> mqtt.Client:
    """paho-mqtt 2.x requires choosing a callback API version; 1.x (used on the RevPis) doesn't know it."""
    if hasattr(mqtt, "CallbackAPIVersion"):
        return mqtt.Client(mqtt.CallbackAPIVersion.VERSION1, client_id=clientId)
    return mqtt.Client(client_id=clientId)


class TokenInjector:

    def __init__(self, plcId: str, server: Optional[str], port: int, keepalive: int,
                 machinesByType: Dict[str, List[Any]]):
        self.plcId = plcId
        self.server = server
        self.port = port
        self.keepalive = keepalive
        self.topicPrefix = f"SIM/{plcId}/tokens"
        self.requestTopic = f"{self.topicPrefix}/request"
        self.responseTopic = f"{self.topicPrefix}/response"
        self.sensorsTopic = f"{self.topicPrefix}/sensors"

        self.world = TokenWorld(machinesByType)
        self.pendingRequests: "queue.Queue[Dict[str, Any]]" = queue.Queue()
        self.client: Optional[mqtt.Client] = None
        self.lastMovementAt: Optional[float] = None
        self.publishedState: Optional[Any] = None

    # ------------------------------------------------------------------------------------------------------
    # MQTT
    # ------------------------------------------------------------------------------------------------------

    def start(self) -> None:
        if self.server is None:
            logging.warning("TokenInjector: no MQTT server configured, token injection disabled")
            return

        self.client = newMqttClient(f"{self.plcId}-token-injector")
        # clears the retained sensor states if the controller dies, so tools don't read a stale catalog
        self.client.will_set(self.sensorsTopic, payload=None, qos=1, retain=True)
        self.client.on_connect = self._onConnect
        self.client.on_message = self._onMessage
        try:
            self.client.connect(self.server, self.port, self.keepalive)
        except Exception as e:
            logging.error(f"TokenInjector: failed to connect to MQTT server {self.server}:{self.port}: {e}")
            self.client = None
            return
        self.client.loop_start()
        logging.info(f"TokenInjector: listening on {self.requestTopic}, sensors: {', '.join(self.world.spots)}")

    def _onConnect(self, client, userdata, flags, rc) -> None:
        if rc != 0:
            logging.error(f"TokenInjector: MQTT connection refused (rc={rc})")
            return
        client.subscribe(self.requestTopic, qos=1)
        self.publishSensorStates(force=True)

    def _onMessage(self, client, userdata, message) -> None:
        try:
            request = json.loads(message.payload.decode("utf-8"))
            if not isinstance(request, dict):
                raise ValueError("payload must be a JSON object")
        except ValueError as e:
            self._respond(None, False, f"invalid request payload: {e}")
            return
        self.pendingRequests.put(request)

    def _respond(self, requestId: Optional[str], ok: bool, message: str) -> None:
        (logging.info if ok else logging.warning)(f"TokenInjector: {message}")
        if self.client is not None:
            payload = {"id": requestId, "ok": ok, "message": message}
            self.client.publish(self.responseTopic, json.dumps(payload), qos=1)

    def _stateSignature(self) -> Any:
        """What the published state shows, except exact belt positions: changes when a token reaches or leaves
        a light barrier, not on every cycle of a running belt."""
        return tuple((token.id, token.color, token.track.name, None if token.spot is None else token.spot.alias,
                      token.pulseEndsAt) for token in self.world.tokens)

    def publishSensorStates(self, force: bool = False) -> None:
        signature = self._stateSignature()
        if self.client is None or (not force and signature == self.publishedState):
            return
        self.publishedState = signature
        payload = {
            "plcId": self.plcId,
            "sensors": [
                {
                    "alias": spot.alias,
                    "machine": self.world.trackBySpotAlias[alias].machine.id,
                    "machineType": self.world.trackBySpotAlias[alias].machineType,
                    "attribute": spot.attribute,
                    "tokenPresent": self.world.tokenPresent(spot),
                }
                for alias, spot in self.world.spots.items()
            ],
            "tokens": [
                {
                    "id": token.id,
                    "color": token.color,
                    "track": token.track.name,
                    "position": round(token.position, 2),
                    "at": None if token.spot is None else token.spot.alias,
                    "heldBy": token.track.machine.id if isinstance(token.track, GripperTrack) else None,
                    "pulseEndsAt": None if token.pulseEndsAt is None
                    else utcIsoTimestamp(time.time() + token.pulseEndsAt - time.monotonic()),
                }
                for token in self.world.tokens
            ],
            "timestamp": utcIsoTimestamp(time.time()),
        }
        self.client.publish(self.sensorsTopic, json.dumps(payload), qos=1, retain=True)

    # ------------------------------------------------------------------------------------------------------
    # Main loop side
    # ------------------------------------------------------------------------------------------------------

    def applyPendingRequests(self) -> None:
        """Applies queued requests and ends elapsed pulses. Must be called from the controller main loop."""
        self._endElapsedPulses()

        while True:
            try:
                request = self.pendingRequests.get(block=False)
            except queue.Empty:
                break
            try:
                message = self._apply(request)
                self._respond(request.get("id"), True, message)
            except ValueError as e:
                self._respond(request.get("id"), False, str(e))

        self.publishSensorStates()

    def simulateMovement(self) -> None:
        """Moves the tokens with the machines' actuators. Must be called from the controller main loop, after the
        commands have set the actuators of this cycle."""
        now = time.monotonic()
        if self.lastMovementAt is not None:
            self.world.step(min(now - self.lastMovementAt, MAX_MOVEMENT_STEP_SECONDS))
        self.lastMovementAt = now
        self.publishSensorStates()

    def _endElapsedPulses(self) -> None:
        now = time.monotonic()
        for token in list(self.world.tokens):
            if token.pulseEndsAt is not None and now >= token.pulseEndsAt:
                self.world.removeToken(token)
                logging.info(f"TokenInjector: pulse ended, {token.describe()} removed")

    def _apply(self, request: Dict[str, Any]) -> str:
        action = str(request.get("action", "")).lower()

        if action == "clear":
            self.world.clear()
            return "all tokens removed"

        spot = self.world.findSpot(request.get("sensor"))
        color = self._parseColor(request.get("color"))
        where = f"{spot.alias} ({self.world.trackBySpotAlias[spot.alias].machine.id}.{spot.attribute})"

        if action == "place":
            token = self.world.place(spot, color)
            return f"{token.describe()} placed on {where}"

        if action == "remove":
            removed = self.world.remove(spot)
            if not removed:
                raise ValueError(f"no token on {where}")
            return f"{', '.join(token.describe() for token in removed)} removed from {where}"

        if action == "pulse":
            duration = self._parseDuration(request.get("duration", 1.0))
            token: Token = self.world.place(spot, color)
            token.pulseEndsAt = time.monotonic() + duration
            return f"{token.describe()} placed on {where} for {duration}s"

        raise ValueError(f"unknown action '{action}', expected one of: place, remove, pulse, clear")

    @staticmethod
    def _parseColor(color: Any) -> Optional[str]:
        if color is None or color == "":
            return None
        normalized = str(color).strip().upper()
        if normalized not in COLORS:
            raise ValueError(f"unknown color '{color}', expected one of: {', '.join(COLORS)}")
        return normalized

    @staticmethod
    def _parseDuration(duration: Any) -> float:
        try:
            value = float(duration)
        except (TypeError, ValueError):
            raise ValueError(f"invalid duration '{duration}', expected a number of seconds")
        if value <= 0:
            raise ValueError(f"invalid duration '{duration}', expected a positive number of seconds")
        return value
