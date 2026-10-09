# Token injector: placing tokens in a simulated factory

The simple machine simulators (`*SimpleSimulator.py`) move motors and encoders, but they never change a light
barrier on their own. A simulated factory therefore stays idle: a SortingLine `EJECT` waits forever for its middle
light barrier, a ConveyorBelt `MOVE_TO_SENSOR` never sees its target, a VacuumGripper picks up nothing.

The **token injector** fills that gap. While the simulated PLC is running, you place a token (a workpiece) on a
light barrier over MQTT. From then on the token **moves with the machines**: a running belt carries it to the next
barrier, an ejector pushes it into its chute, a VacuumGripper picks it up and puts it down. Every light barrier is
recomputed from the tokens, and the PLC publishes the changes like real sensor changes, so the gateway, the SCADA
and the digital twin all see them as real.

| File | Role |
|---|---|
| `TokenInjector.py` | MQTT front end: receives requests, applies them in the controller main loop, publishes the state |
| `TokenWorld.py` | Where the tokens are and how the machines move them (belts, ejectors, gripper slots) |
| `tokens_cli.py` | Command line client (`list`, `place`, `remove`, `pulse`, `clear`) |

## 1. Prerequisites

- An MQTT broker reachable by both the simulated PLC and you (`mqtt.server` / `mqtt.port` in the PLC `config.yml`).
  Without an MQTT server the injector logs `token injection disabled` and does nothing.
- A simulated PLC controller that wires the injector in. The reference one is
  `dt-setups/VIRTUAL_Setups/VIRTUAL_1C1H1M1S2V_01/config/PLC1/SimulatedMachines_Controller.py`
  (see [section 7](#7-wiring-the-injector-into-your-own-simulated-controller) for another controller).
- The `rppmcontroller` conda env (Python 3.7, `paho-mqtt` 1.x or 2.x, `pyyaml`).

## 2. Quick start

Terminal 1, start the simulated PLC:

```bash
conda activate rppmcontroller
cd dt-setups/VIRTUAL_Setups/VIRTUAL_1C1H1M1S2V_01/config/PLC1
python3 SimulatedMachines_Controller.py
```

The log shows `TokenInjector: listening on SIM/virtualmachines-plc-1/tokens/request, sensors: sl-input, ...`.

Terminal 2, from the same folder:

```bash
alias tokens='python -m rppmcontroller.simulation.tokens_cli --config config.yml'

tokens list                          # every sensor, and every token with where it is
tokens place sl-input --color red    # a red workpiece on the SortingLine input
tokens list
```

Then send a SortingLine `EJECT` (from the SCADA or a mission): the belt starts, the token passes the colour sensor,
reaches the middle barrier, and the red ejector pushes it into `sl-red`. `tokens list` now shows it on `sl-red`.

`--config` is only used to read `plc.id` and `mqtt.server` / `mqtt.port`. Without it, pass them explicitly:

```bash
python -m rppmcontroller.simulation.tokens_cli --plc-id virtualmachines-plc-1 --broker localhost --port 1883 list
```

Defaults: broker `localhost`, port `1883`. The PLC id is mandatory (from `--config` or `--plc-id`).

## 3. CLI commands

| Command | Effect |
|---|---|
| `list` | Prints every sensor (alias, machine, token present or not, machine attribute), then every token: id, colour, and where it is (on a sensor, between sensors on a belt with its position, or held by a gripper), plus the time left for a pulse |
| `place <sensor> [--color C]` | Places a new token on the sensor. It then moves with the machines |
| `remove <sensor>` | Removes every token currently on that sensor. Fails if there is none |
| `pulse <sensor> [duration] [--color C]` | Places a token and removes it after `duration` seconds (default `1.0`), **wherever it is by then** (it may have been carried away by a belt) |
| `clear` | Removes every token, including the ones held by a gripper |

- `<sensor>` is an alias from `list` (case-insensitive, e.g. `sl-input`), or `<machineId>/<attribute>`
  (case-sensitive, e.g. `SortingLine01/sortingLineSensInputLightBarrier`).
- `--color` is `white`, `red` or `blue` (case-insensitive). It only matters on the SortingLine, whose colour sensor
  reports it when the token passes (used by `EJECT` with colour `AUTO`). A token without colour is not seen by the
  colour sensor.
- Several tokens may be placed on the same sensor; `remove` removes all of them.
- The CLI waits 3 s for an answer, prints the PLC's message, and exits with status 1 if the request was refused.
  `no response on SIM/<plcId>/tokens/response` means the simulated PLC is not running, uses another broker, or
  has another `plc.id`.

## 4. Sensor aliases

For the default VIRTUAL_1C1H1M1S2V_01 setup:

| Alias | Machine attribute (active-low) | Track |
|---|---|---|
| `sl-input` | `SortingLine01.sortingLineSensInputLightBarrier` | SortingLine belt, position 0 |
| `sl-middle` | `SortingLine01.sortingLineSensMiddleLightBarrier` | SortingLine belt, position 0.8 |
| `sl-white`, `sl-red`, `sl-blue` | `SortingLine01.sortingLineSens{White,Red,Blue}LightBarrier` | SortingLine colour chutes (static) |
| `cb-feed` | `ConveyorBelt01.conveyorSensFeed` | ConveyorBelt, position 0 |
| `cb-swap` | `ConveyorBelt01.conveyorSensSwap` | ConveyorBelt, position 3.0 |
| `mps-input` | `MultiProcessing01.multiProcessingSensOven` | MPS oven feeder (static), where VGR2 places tokens |
| `mps-end` | `MultiProcessing01.multiProcessingSensEndConveyor` | static |
| `hb-inside`, `hb-outside` | `HighBay01.highbaySens{Inside,Outside}` | static |

- Further instances of a machine type get a numbered prefix: `sl2-input`, `cb2-feed`, ...
- Light barriers are **active-low**: a token sets the attribute to `false`, no token sets it to `true`.
- The VacuumGrippers have no sensor alias; a held token shows as `held by VacuumGripper01` in `list`.

## 5. How tokens move

Positions along a belt are measured in **seconds of belt travel** (1 unit = 1 s of motor on). A light barrier sees a
token within ±0.15 of its centre. The constants are at the top of `TokenWorld.py`.

### SortingLine

```
 -0.5    0     0.4      0.8           1.3        2.35        3.4       4.65
  |------+------+--------+-------------+-----------+-----------+---------|
  fall  input  colour  middle        white        red        blue    fall off
  off          sensor               ejector     ejector     ejector  (end)
```

(ejector and end positions shown for the default `SortingLineParameters`: they are
`middle barrier entry (0.65) + <colour>_ejector_delay + 0.15`, and the end is `0.65 + pass_through_delay + 0.5`)

- While `sortingLineActMotorConveyor` is on, every token on the belt moves forward at 1 unit/s.
- When a coloured token is within ±0.15 of the colour sensor, `sortingLineSensColorDetector` and the matching
  `sortingLineSens{White,Red,Blue}Detector` are `true`.
- While an ejector is active, the token closest to it (within ±0.3) is pushed into its chute (`sl-white`, `sl-red`,
  `sl-blue`), where it stays until removed or picked up by a gripper. Because ejectors sit at "middle + ejector delay",
  a time-based `EJECT` fires exactly when the token is in front of the right ejector.
- A token that is not ejected falls off the end of the belt and disappears.

### ConveyorBelt

```
 -0.5   0                     3.0   3.5
  |-----+----------------------+-----|
  fall  feed                  swap  fall off
```

- `conveyorActForward` moves tokens from `cb-feed` towards `cb-swap`, `conveyorActBackward` the other way
  (both on, or none: no movement).
- `MOVE_TO_SENSOR` therefore stops with the token on the target barrier; `MOVE_OUT` carries it off the end.

### VacuumGripper

Each gripper has pick-up / drop **slots** at fixed `(rotation, arm)` encoder positions:

| Gripper | Slot | Named position used |
|---|---|---|
| VGR1 (1st VacuumGripper) | `sl-white`, `sl-red`, `sl-blue`, `cb-feed` | `SL_OUTPUT_WHITE`, `SL_OUTPUT_RED`, `SL_OUTPUT_BLUE`, `CB` |
| VGR2 (2nd VacuumGripper) | `cb-swap`, `mps-input` | `ALT_CB`, `MPS_INPUT` |

The slot coordinates are taken from the gripper's named positions (`vacuumGrippers.<id>.namedPositions` in
`config.yml`), so a token is picked exactly where the missions send the gripper. A gripper is "over" a slot when its
rotation **and** arm encoders are both within ±40 counts of it (the vertical axis is ignored).

- **Pick**: when the suction turns on (`vacuumActValve` and `vacuumActCompressorOn` both become on) over a slot that
  holds a token, the gripper takes that token. It holds at most one token.
- **Place**: when the suction turns off over a slot, the held token is put on that slot's barrier.
- **Drop**: when the suction turns off anywhere else, the token falls and disappears (warning in the PLC log).

So a mission `pick SL_OUTPUT_BLUE` → `place CB` moves a token from `sl-blue` to `cb-feed` with no manual step.

### MultiProcessing, HighBay

No movement yet: a token stays on `mps-input`, `mps-end`, `hb-inside` or `hb-outside` until it is removed (or, for
`mps-input`, until VGR2 picks it up).

### What the simulator does not model

- Belt travel per cycle is capped at 0.5 s: if the PLC main loop stalls, the tokens lag behind the time-based
  command timers instead of jumping, and an `EJECT` may then miss its token.
- The SortingLine ejector positions are computed once, at controller start-up, from the `SortingLineParameters`.
- Tokens on the belts don't collide; two tokens can share a position.

## 6. MQTT protocol (for other tools)

All topics are under `SIM/<plcId>/tokens`, where `<plcId>` is `plc.id` of the PLC configuration.

### Request — `SIM/<plcId>/tokens/request` (you → PLC)

```json
{"id": "a1b2", "action": "place", "sensor": "sl-input", "color": "RED"}
{"id": "a1b3", "action": "pulse", "sensor": "sl-middle", "duration": 1.5}
{"id": "a1b4", "action": "remove", "sensor": "sl-input"}
{"id": "a1b5", "action": "clear"}
```

| Field | Required for | Notes |
|---|---|---|
| `id` | – | Any string, echoed in the response so you can match it |
| `action` | all | `place`, `remove`, `pulse`, `clear` (case-insensitive) |
| `sensor` | `place`, `remove`, `pulse` | Alias or `<machineId>/<attribute>` |
| `color` | – | `WHITE`, `RED`, `BLUE` (case-insensitive), or omitted |
| `duration` | – (`pulse` only) | Positive number of seconds, default `1.0` |

Requests are queued when received and applied at the start of the next PLC main loop cycle (≈ `mainLoopDelay`,
0.1 s), so the machines see the change in that cycle.

### Response — `SIM/<plcId>/tokens/response` (PLC → you)

```json
{"id": "a1b2", "ok": true,  "message": "token 3 (RED) placed on sl-input (SortingLine01.sortingLineSensInputLightBarrier)"}
{"id": "a1b4", "ok": false, "message": "no token on sl-input (SortingLine01.sortingLineSensInputLightBarrier)"}
```

Subscribe to it **before** publishing the request. A payload that is not a JSON object gets a response with
`"id": null`.

### State — `SIM/<plcId>/tokens/sensors` (PLC → you, retained)

```json
{
  "plcId": "virtualmachines-plc-1",
  "sensors": [
    {"alias": "sl-input", "machine": "SortingLine01", "machineType": "SortingLine",
     "attribute": "sortingLineSensInputLightBarrier", "tokenPresent": true}
  ],
  "tokens": [
    {"id": 3, "color": "RED", "track": "sl-belt", "position": 0.0, "at": "sl-input",
     "heldBy": null, "pulseEndsAt": null}
  ],
  "timestamp": "2026-10-09T08:53:20.123456Z"
}
```

- Published on connection, then each time a token is added, removed, reaches or leaves a sensor, or is picked /
  placed — **not** on every cycle of a running belt, so `position` of a token between sensors may be stale.
- `at` is the sensor alias the token is on (`null` between sensors), `heldBy` the gripper id holding it,
  `pulseEndsAt` the UTC time a pulsed token will be removed.
- The PLC registers an empty retained message as its MQTT will: if it dies, the retained state is cleared
  (an empty payload), so tools don't read a stale catalog.

### Example with mosquitto clients

```bash
mosquitto_sub -h localhost -t 'SIM/virtualmachines-plc-1/tokens/#' -v &
mosquitto_pub -h localhost -t SIM/virtualmachines-plc-1/tokens/request \
  -m '{"id":"1","action":"place","sensor":"cb-feed","color":"white"}'
```

### Python example

```python
import json, uuid
import paho.mqtt.client as mqtt

PREFIX = "SIM/virtualmachines-plc-1/tokens"
requestId = uuid.uuid4().hex

def onConnect(client, userdata, flags, rc):
    client.subscribe(f"{PREFIX}/response", qos=1)

def onSubscribe(client, userdata, mid, grantedQos):
    client.publish(f"{PREFIX}/request", json.dumps(
        {"id": requestId, "action": "place", "sensor": "sl-input", "color": "BLUE"}), qos=1)

def onMessage(client, userdata, message):
    response = json.loads(message.payload)
    if response["id"] == requestId:
        print(response["ok"], response["message"])
        client.disconnect()

client = mqtt.Client()   # paho-mqtt 2.x: mqtt.Client(mqtt.CallbackAPIVersion.VERSION1)
client.on_connect, client.on_subscribe, client.on_message = onConnect, onSubscribe, onMessage
client.connect("localhost", 1883)
client.loop_forever()
```

## 7. Wiring the injector into your own simulated controller

`TokenInjector` takes the machines grouped by type and needs two calls per main loop cycle:

```python
from rppmcontroller.simulation.TokenInjector import TokenInjector

class MySimulatedController(RevPiPyMachineController):
    def __init__(self, configurationFile):
        super().__init__(configurationFile=configurationFile)
        ...  # create machines and their SimpleSimulators first
        self.tokenInjector = TokenInjector(self.plcId, self.MQTT.server, self.MQTT.port, self.MQTT.keepalive,
                                           {"SortingLine": [...], "ConveyorBelt": [...], "MultiProcessing": [...],
                                            "HighBay": [...], "VacuumGripper": [...]})

    def start(self):
        self.tokenInjector.start()          # connects to MQTT, subscribes, publishes the retained state
        super().start()

    def read(self):
        self.tokenInjector.applyPendingRequests()   # before the simulated reads: injected tokens seen this cycle
        for simulator in self.simulators:
            simulator.simulatedRead()

    def write(self):
        self.tokenInjector.simulateMovement()       # after the commands set the actuators of this cycle
        for simulator in self.simulators:
            simulator.simulatedWrite()
```

- Create the `TokenInjector` **after** the SimpleSimulators: their constructors reset the light barriers to `true`,
  the injector then owns them.
- Once the injector is wired in, the tokens are the only source of truth for the light barriers and SortingLine
  colour detectors: any other write to them (e.g. a simulator's `fakeSensor`) is overwritten on the next cycle.
- Machine attributes are only modified from the main loop (`applyPendingRequests` / `simulateMovement`); the MQTT
  thread only queues requests.
- In unit tests, pass `server=None` and put requests directly in `tokenInjector.pendingRequests` (see
  `tests/simulation/TokenFlowIntegration_test.py`).

## 8. Typical scenarios

**Sort a red workpiece**

```bash
tokens place sl-input --color red
# SCADA: SortingLine01 EJECT colour AUTO   → token ends on sl-red
```

**Feed the ConveyorBelt and move it to the swap side**

```bash
tokens place cb-feed
# SCADA: ConveyorBelt01 MOVE_TO_SENSOR forward → token ends on cb-swap
```

**Full VGR1 transfer from the blue chute to the belt**

```bash
tokens place sl-blue
# mission: VacuumGripper01 pick SL_OUTPUT_BLUE, place CB → token ends on cb-feed
```

**Simulate a workpiece briefly passing a barrier** (e.g. to trigger an edge)

```bash
tokens pulse sl-middle 0.5
```

**Reset between test runs**

```bash
tokens clear
```

## 9. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `no sensor states on .../sensors` or `no response on .../response` | PLC not running, different broker, or different `plc.id`. Check the PLC log for `TokenInjector: listening on ...` |
| PLC log: `no MQTT server configured, token injection disabled` | `mqtt.server` missing in the PLC `config.yml` |
| `unknown sensor 'x', expected one of: ...` | Use an alias printed by `list` |
| The token is not ejected | The belt did not run long enough, or the main loop stalled (see section 5). Check the token position with `list` |
| The gripper does not pick the token | The gripper's rotation / arm encoders are not within ±40 of the slot, or the slot holds no token at that moment. Check the named positions in `config.yml` |
| The token "falls off the line" when placed by a gripper | The suction was released outside any slot of that gripper |
