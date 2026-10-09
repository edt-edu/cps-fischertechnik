"""Where the tokens of a simulated factory are, and how the machines move them.

Every light barrier sits on a track. A token is always on a track, at a position along it:

- a **moving track** is a belt (SortingLine, ConveyorBelt). While its motor actuators are on, the tokens on it
  move along it, and a token carried past an end falls off the belt and disappears;
- a **static track** is a place where a token just sits (SortingLine colour chutes, MultiProcessing and HighBay
  light barriers), until it is removed;
- a **gripper track** holds the token a VacuumGripper picked up: opening its vacuum valve over a slot that holds a
  token picks it up, closing the valve over a slot puts the token down there (anywhere else, it is dropped and
  disappears). Slots are recognized from the gripper's rotation and arm encoders.

Positions are measured in seconds of belt travel, so the layout matches the time-based machine commands: e.g. the
SortingLine ejectors sit at "middle light barrier + the machine's ejector delay", which makes a time-based EJECT fire
exactly when the token is in front of the ejector.

Light barriers are active-low. After each change, every barrier is recomputed from the tokens: ``False`` if a token
is within its window, ``True`` otherwise. The tokens are the only source of truth for these attributes.
"""

import logging
from dataclasses import dataclass
from typing import Any, Dict, List, Optional

BELT_SPEED = 1.0
"""Belt travel per second of motor activation (the position unit)."""

SENSOR_HALF_WIDTH = 0.15
"""A light barrier sees a token within this distance of its center (0.3 s of belt travel, ~3 controller cycles)."""

BELT_END_MARGIN = 0.5
"""Distance past the last light barrier after which a token falls off a belt."""

# SortingLine layout, from the input light barrier.
SL_COLOR_SENSOR_POSITION = 0.4
SL_MIDDLE_POSITION = 0.8
SL_EJECTOR_OFFSET = 0.15
"""Commands react to the middle barrier one or two controller cycles after the token enters it."""
SL_EJECTOR_TOLERANCE = 0.3
"""An ejector pushes a token into its chute only if the token is this close to it."""

# ConveyorBelt layout: feed light barrier at 0, swap light barrier at CB_LENGTH.
CB_LENGTH = 3.0

COLORS = ("WHITE", "RED", "BLUE")

VGR_SLOT_TOLERANCE = 40
"""A gripper is over a slot when its rotation and arm encoders are within this many counts of the slot's."""

# Where each VacuumGripper picks / places tokens: slot alias -> name of the gripper's named position
# (VacuumGripperParameters.named_positions, the positions the SCADA missions send by name).
VGR_SLOT_POSITION_NAMES: List[Dict[str, str]] = [
    {"sl-white": "SL_OUTPUT_WHITE", "sl-red": "SL_OUTPUT_RED", "sl-blue": "SL_OUTPUT_BLUE", "cb-feed": "CB"},
    {"cb-swap": "ALT_CB", "mps-input": "MPS_INPUT"},
]

# Fallback slot coordinates, (rotation, arm) encoder counts, for a gripper without these named positions.
VGR_SLOTS: List[Dict[str, tuple]] = [
    {"sl-white": (2400, 500), "sl-red": (2270, 900), "sl-blue": (2135, 1500), "cb-feed": (1870, 1400)},
    {"cb-swap": (1155, 1810), "mps-input": (2050, 1890)},
]


@dataclass
class SensorSpot:
    alias: str
    name: str
    attribute: Optional[str]
    """Light barrier attribute of the machine, None for a slot without light barrier"""
    center: float

    def covers(self, position: float) -> bool:
        return abs(position - self.center) <= SENSOR_HALF_WIDTH


@dataclass
class Token:
    id: int
    color: Optional[str]
    track: "Track"
    position: float
    pulseEndsAt: Optional[float] = None

    @property
    def spot(self) -> Optional[SensorSpot]:
        return self.track.spotAt(self.position)

    def describe(self) -> str:
        color = f" ({self.color})" if self.color else ""
        return f"token {self.id}{color}"


class Track:
    """A static track: tokens stay where they are placed."""

    def __init__(self, name: str, machine: Any, machineType: str, spots: List[SensorSpot]):
        self.name = name
        self.machine = machine
        self.machineType = machineType
        self.spots = spots

    def spotAt(self, position: float) -> Optional[SensorSpot]:
        for spot in self.spots:
            if spot.covers(position):
                return spot
        return None

    def speed(self) -> float:
        return 0.0

    def isOnTrack(self, position: float) -> bool:
        return True

    def afterMove(self, world: "TokenWorld") -> None:
        pass

    def updateSensors(self, tokens: List["Token"]) -> None:
        for spot in self.spots:
            if spot.attribute is None:
                continue
            present = any(token.track is self and spot.covers(token.position) for token in tokens)
            setattr(self.machine, spot.attribute, not present)


class BeltTrack(Track):
    """A moving track between ``minPosition`` and ``maxPosition``."""

    def __init__(self, name: str, machine: Any, machineType: str, spots: List[SensorSpot],
                 minPosition: float, maxPosition: float):
        super().__init__(name, machine, machineType, spots)
        self.minPosition = minPosition
        self.maxPosition = maxPosition

    def isOnTrack(self, position: float) -> bool:
        return self.minPosition <= position <= self.maxPosition


class SortingLineTrack(BeltTrack):
    """Input -> colour sensor -> middle -> white / red / blue ejectors -> end of the line."""

    def __init__(self, prefix: str, machine: Any, chutes: Dict[str, Track]):
        parameters = machine.parameters
        # position of the token when the middle barrier first sees it, i.e. when the ejector timers start
        middleEntry = SL_MIDDLE_POSITION - SENSOR_HALF_WIDTH
        self.ejectorPositions: Dict[str, float] = {
            "WHITE": middleEntry + parameters.white_ejector_delay + SL_EJECTOR_OFFSET,
            "RED": middleEntry + parameters.red_ejector_delay + SL_EJECTOR_OFFSET,
            "BLUE": middleEntry + parameters.blue_ejector_delay + SL_EJECTOR_OFFSET,
        }
        endOfLine = middleEntry + parameters.pass_through_delay + BELT_END_MARGIN
        super().__init__(f"{prefix}-belt", machine, "SortingLine", [
            SensorSpot(f"{prefix}-input", "input", "sortingLineSensInputLightBarrier", 0.0),
            SensorSpot(f"{prefix}-middle", "middle", "sortingLineSensMiddleLightBarrier", SL_MIDDLE_POSITION),
        ], minPosition=-BELT_END_MARGIN, maxPosition=endOfLine)
        self.chutes = chutes

    def speed(self) -> float:
        return BELT_SPEED if self.machine.sortingLineActMotorConveyor else 0.0

    def afterMove(self, world: "TokenWorld") -> None:
        ejectorActive = {
            "WHITE": self.machine.sortingLineActWhiteEjector,
            "RED": self.machine.sortingLineActRedEjector,
            "BLUE": self.machine.sortingLineActBlueEjector,
        }
        for color, active in ejectorActive.items():
            if not active:
                continue
            ejectorPosition = self.ejectorPositions[color]
            candidates = [token for token in world.tokens
                          if token.track is self and abs(token.position - ejectorPosition) <= SL_EJECTOR_TOLERANCE]
            if not candidates:
                continue
            token = min(candidates, key=lambda candidate: abs(candidate.position - ejectorPosition))
            chute = self.chutes[color]
            logging.info(f"TokenWorld: {token.describe()} ejected by the {color.lower()} ejector "
                         f"of {self.machine.id} into {chute.spots[0].alias}")
            world.moveToken(token, chute, chute.spots[0].center)

    def updateSensors(self, tokens: List["Token"]) -> None:
        super().updateSensors(tokens)
        seenColor = None
        for token in tokens:
            if token.track is self and token.color and abs(token.position - SL_COLOR_SENSOR_POSITION) <= SENSOR_HALF_WIDTH:
                seenColor = token.color
        self.machine.sortingLineSensColorDetector = seenColor is not None
        self.machine.sortingLineSensWhiteDetector = seenColor == "WHITE"
        self.machine.sortingLineSensRedDetector = seenColor == "RED"
        self.machine.sortingLineSensBlueDetector = seenColor == "BLUE"


class ConveyorBeltTrack(BeltTrack):
    """Feed (0) <-> swap (CB_LENGTH). FORWARD moves tokens from the feed to the swap."""

    def __init__(self, prefix: str, machine: Any):
        super().__init__(f"{prefix}-belt", machine, "ConveyorBelt", [
            SensorSpot(f"{prefix}-feed", "feed", "conveyorSensFeed", 0.0),
            SensorSpot(f"{prefix}-swap", "swap", "conveyorSensSwap", CB_LENGTH),
        ], minPosition=-BELT_END_MARGIN, maxPosition=CB_LENGTH + BELT_END_MARGIN)

    def speed(self) -> float:
        forward = self.machine.conveyorActForward
        backward = self.machine.conveyorActBackward
        if forward and not backward:
            return BELT_SPEED
        if backward and not forward:
            return -BELT_SPEED
        return 0.0


class GripperTrack(Track):
    """The token held by a VacuumGripper."""

    def __init__(self, prefix: str, machine: Any, slots: Dict[str, tuple]):
        super().__init__(f"{prefix}-gripper", machine, "VacuumGripper", [])
        self.slots = slots
        self.valveWasOpen = False

    def slotUnderGripper(self) -> Optional[str]:
        rotation = self.machine.vacuumSensRotEncoderCounter
        arm = self.machine.vacuumSensArmEncoderCounter
        for alias, (slotRotation, slotArm) in self.slots.items():
            if abs(rotation - slotRotation) <= VGR_SLOT_TOLERANCE and abs(arm - slotArm) <= VGR_SLOT_TOLERANCE:
                return alias
        return None

    def afterMove(self, world: "TokenWorld") -> None:
        valveOpen = bool(self.machine.vacuumActValve and self.machine.vacuumActCompressorOn)
        if valveOpen == self.valveWasOpen:
            return
        self.valveWasOpen = valveOpen
        alias = self.slotUnderGripper()
        held = [token for token in world.tokens if token.track is self]

        if valveOpen:
            if held or alias is None or alias not in world.spots:
                return
            spot = world.spots[alias]
            slotTrack = world.trackBySpotAlias[alias]
            candidates = [token for token in world.tokens if token.track is slotTrack and spot.covers(token.position)]
            if candidates:
                logging.info(f"TokenWorld: {candidates[0].describe()} picked up from {alias} by {self.machine.id}")
                world.moveToken(candidates[0], self, 0.0)
            return

        for token in held:
            if alias is None or alias not in world.spots:
                logging.warning(f"TokenWorld: {token.describe()} released by {self.machine.id} outside any slot, "
                                f"it falls off the line")
                world.tokens.remove(token)
                continue
            spot = world.spots[alias]
            logging.info(f"TokenWorld: {token.describe()} placed on {alias} by {self.machine.id}")
            world.moveToken(token, world.trackBySpotAlias[alias], spot.center)


ALIAS_PREFIX_BY_MACHINE_TYPE: Dict[str, str] = {
    "SortingLine": "sl",
    "ConveyorBelt": "cb",
    "MultiProcessing": "mps",
    "HighBay": "hb",
    "VacuumGripper": "vgr",
}

# Light barriers without a simulated drive yet: name -> machine attribute.
STATIC_SENSORS_BY_MACHINE_TYPE: Dict[str, Dict[str, str]] = {
    "MultiProcessing": {
        "end": "multiProcessingSensEndConveyor",
    },
    "HighBay": {
        "inside": "highbaySensInside",
        "outside": "highbaySensOutside",
    },
}


class TokenWorld:

    def __init__(self, machinesByType: Dict[str, List[Any]]):
        self.tracks: List[Track] = []
        self.tokens: List[Token] = []
        self.nextTokenId = 1

        for machineType in ("SortingLine", "ConveyorBelt", "MultiProcessing", "HighBay"):
            for index, machine in enumerate(machinesByType.get(machineType, [])):
                # first instance: "sl-input", next ones: "sl2-input", "sl3-input", ...
                prefix = ALIAS_PREFIX_BY_MACHINE_TYPE[machineType] + ("" if index == 0 else str(index + 1))
                self.tracks.extend(self._buildTracks(machineType, prefix, machine))
        # MPS input, where VGR2 places the token: the oven feeder, seen by the light barrier in front of the oven
        if machinesByType.get("MultiProcessing"):
            self.tracks.append(Track("mps-input", machinesByType["MultiProcessing"][0], "MultiProcessing",
                                     [SensorSpot("mps-input", "input", "multiProcessingSensOven", 0.0)]))
        for index, machine in enumerate(machinesByType.get("VacuumGripper", [])):
            self.tracks.append(GripperTrack(f"vgr{index + 1}", machine, self._gripperSlots(index, machine)))

        self.spots: Dict[str, SensorSpot] = {}
        self.trackBySpotAlias: Dict[str, Track] = {}
        for track in self.tracks:
            for spot in track.spots:
                self.spots[spot.alias] = spot
                self.trackBySpotAlias[spot.alias] = track

        self.updateSensors()

    @staticmethod
    def _gripperSlots(index: int, machine: Any) -> Dict[str, tuple]:
        """Slot alias -> (rotation, arm) encoder counts, from the gripper's named positions when it has them."""
        slots = dict(VGR_SLOTS[index]) if index < len(VGR_SLOTS) else {}
        named_positions = getattr(getattr(machine, "parameters", None), "named_positions", None) or {}
        for alias, position_name in (VGR_SLOT_POSITION_NAMES[index] if index < len(VGR_SLOT_POSITION_NAMES) else {}).items():
            position = named_positions.get(position_name)
            if position is not None:
                slots[alias] = (position.rot, position.horizontal)
        return slots

    @staticmethod
    def _buildTracks(machineType: str, prefix: str, machine: Any) -> List[Track]:
        if machineType == "SortingLine":
            chutes = {
                color: Track(f"{prefix}-{color.lower()}-chute", machine, machineType, [
                    SensorSpot(f"{prefix}-{color.lower()}", color.lower(),
                               f"sortingLineSens{color.capitalize()}LightBarrier", 0.0)])
                for color in COLORS
            }
            # alias order: input, middle, white, blue, red
            return [SortingLineTrack(prefix, machine, chutes), chutes["WHITE"], chutes["BLUE"], chutes["RED"]]
        if machineType == "ConveyorBelt":
            return [ConveyorBeltTrack(prefix, machine)]
        return [
            Track(f"{prefix}-{name}", machine, machineType, [SensorSpot(f"{prefix}-{name}", name, attribute, 0.0)])
            for name, attribute in STATIC_SENSORS_BY_MACHINE_TYPE[machineType].items()
        ]

    # ------------------------------------------------------------------------------------------------------

    def findSpot(self, name: Any) -> SensorSpot:
        if not name:
            raise ValueError(f"missing 'sensor', expected one of: {', '.join(self.spots)}")
        key = str(name).strip()
        if key.lower() in self.spots:
            return self.spots[key.lower()]
        # also accept "<machineId>/<attribute>", e.g. "SortingLine01/sortingLineSensInputLightBarrier"
        for alias, spot in self.spots.items():
            if key == f"{self.trackBySpotAlias[alias].machine.id}/{spot.attribute}":
                return spot
        raise ValueError(f"unknown sensor '{key}', expected one of: {', '.join(self.spots)}")

    def place(self, spot: SensorSpot, color: Optional[str]) -> Token:
        token = Token(self.nextTokenId, color, self.trackBySpotAlias[spot.alias], spot.center)
        self.nextTokenId += 1
        self.tokens.append(token)
        self.updateSensors()
        return token

    def remove(self, spot: SensorSpot) -> List[Token]:
        track = self.trackBySpotAlias[spot.alias]
        removed = [token for token in self.tokens if token.track is track and spot.covers(token.position)]
        self.tokens = [token for token in self.tokens if token not in removed]
        self.updateSensors()
        return removed

    def removeToken(self, token: Token) -> None:
        self.tokens = [other for other in self.tokens if other is not token]
        self.updateSensors()

    def clear(self) -> None:
        self.tokens = []
        self.updateSensors()

    def moveToken(self, token: Token, track: Track, position: float) -> None:
        token.track = track
        token.position = position

    def step(self, elapsedSeconds: float) -> None:
        """Moves the tokens on running belts by ``elapsedSeconds`` of belt travel, then applies ejections."""
        for track in self.tracks:
            speed = track.speed()
            if speed == 0.0:
                continue
            for token in [token for token in self.tokens if token.track is track]:
                spotBefore = token.spot
                token.position += speed * elapsedSeconds
                if not track.isOnTrack(token.position):
                    logging.info(f"TokenWorld: {token.describe()} fell off the end of {track.name}")
                    self.tokens.remove(token)
                    continue
                spotAfter = token.spot
                if spotAfter is not None and spotAfter is not spotBefore:
                    logging.info(f"TokenWorld: {token.describe()} reached {spotAfter.alias}")
        for track in self.tracks:
            track.afterMove(self)
        self.updateSensors()

    def updateSensors(self) -> None:
        for track in self.tracks:
            track.updateSensors(self.tokens)

    def tokenPresent(self, spot: SensorSpot) -> bool:
        track = self.trackBySpotAlias[spot.alias]
        return any(token.track is track and spot.covers(token.position) for token in self.tokens)
