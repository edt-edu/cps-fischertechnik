# Endpoint naming inconsistencies (WebSocket destinations, topics, REST paths)

This file lists the naming inconsistencies observed in the communication API of the factoryscada backend:
STOMP destinations (`/app/...`), STOMP topics (`/topic/...`) and REST paths (`/api/...`).
It was written while working on [#234](https://github.com/edt-edu/cps-fischertechnik/issues/234),
from the generated documentation (`docgen/websocket-endpoints-topics.adoc`, and `docgen/rest-endpoints.adoc` from PR #293)
and from the Angular frontend.

It does **not** propose a naming convention: each item needs a more detailed analysis, and may become a separate issue.
When an item is turned into an issue, link the issue in the item.

Endpoints are given without the `/app` prefix for incoming STOMP destinations, as in the generated documentation.

**Who is affected by a rename** (the "Affects" lines):

- **backend**: `@MessageMapping` / `@SendTo` / `@WebSocketPublish` / `@RequestMapping` values.
- **frontend**: hard-coded destinations and topics in `factoryscada/frontend/src` (components, widgets, services).
- **config**: the machine `type` values of `configuration.yml` (and the user configurations derived from it).
- **external**: clients outside this repository, e.g. the digital twin using `/api/replanning`. They cannot be found by searching this repository.

## Summary

| # | Inconsistency | Affects |
|---|---|---|
| 1 | Case of the first path segment: camelCase vs kebab-case | backend, frontend, config |
| 2 | Case of action names: snake_case vs kebab-case | backend, frontend |
| 3 | Requests mixed with commands under `/command/` | backend, frontend |
| 4 | Broken destination: vacuum gripper status | backend, frontend |
| 5 | Request destination vs reply topic names | backend, frontend |
| 6 | Topics without namespace, and the `/topic/{name}/...` namespace shared by machines and PLCs | backend, frontend |
| 7 | Three mission features with unrelated names | backend, frontend, external |
| 8 | Position of the mission name in command paths | backend, frontend, external |
| 9 | Meaning of `stop` | backend, frontend, external |
| 10 | Logs destinations | backend, frontend |
| 11 | Machine action names differ from the PLC command names | backend, frontend |
| 12 | The word "controller" | backend (doc), frontend |
| 13 | Catch-all `**` handlers listed as incoming topics | backend (doc generator) |

## 1. Case of the first path segment: camelCase vs kebab-case

Machine and some other prefixes are camelCase, the more recent ones are kebab-case:

- camelCase: `/conveyorBelt/...`, `/highBayWarehouse/...`, `/indexedLine/...`, `/multiProcessingStation/...`,
  `/punchingMachine/...`, `/sortingLine/...`, `/vacuumGripper/...`, `/factoryMission/...`
- kebab-case: `/dynamic-mission/...`, `/mission-extension/...`, `/api/dynamic-mission/...`, `/api/mission-extension/...`
- topics are kebab-case: `/topic/factory-configuration`, `/topic/dynamic-missions`...

The machine prefixes are not arbitrary: they are the machine `type` values of `configuration.yml`
(`type: "sortingLine"`), and the frontend builds the destinations from them (`/app/${type}/${name}/command/...`).

- **Affects:** backend, frontend, config.
- **Open question:** should the machine type stay the destination prefix (one vocabulary, but tied to the configuration format),
  or should the prefix be mapped from the type (allowing kebab-case everywhere)? Is `/factoryMission` a machine-like prefix or a feature one?

## 2. Case of action names: snake_case vs kebab-case

- snake_case for machine commands: `/conveyorBelt/{machineName}/command/move_to_sensor`, `/highBayWarehouse/{machineName}/command/go_to_column`,
  `/vacuumGripper/{machineName}/command/move_to_safe_position`...
- kebab-case for everything else: `/{type}/{machineName}/command-status`, `/{type}/{machineName}/machine-status`,
  `/factoryMission/actual-command-executing`, `/factoryMission/mission-configuration`, `/mission-extension/stop-active`, `/plc/{plcName}/plc-connection`.

The snake_case names follow the command names of the rppmcontroller (see `factoryscada/README.md`, "The naming convention should follow the one in the rppmcontroller").

- **Affects:** backend, frontend.
- **Open question:** is "a STOMP command name is exactly the PLC command name" a rule worth keeping (see also item 11)?
  If yes, it should be written down as the reason for the exception.

## 3. Requests mixed with commands under `/command/`

Under a machine, the path tells apart commands and requests in three different ways:

- `/{type}/{machineName}/command/<action>`: commands sent to the machine (`move_out`, `stop`...), but also
  `/{type}/{machineName}/command/debug` (a raw JSON command, possibly a request);
- `/{type}/{machineName}/command-status` and `/{type}/{machineName}/machine-status`: requests for the last known statuses, at the same level as `/command`;
- `/vacuumGripper/{machineName}/request/status`: a request to the machine, under `/request/` (only for the vacuum gripper).

- **Affects:** backend, frontend.
- **Open question:** should requests have their own segment (e.g. `/request/...`) for all machines, and should
  "ask the backend for the last known status" be distinguished from "ask the machine for its status"?

## 4. Broken destination: vacuum gripper status

The backend listens to `/vacuumGripper/{machineName}/request/status`, but:

- the frontend component `vacuum-gripper-status` publishes to `/app/${type}/${name}/command/status`
  (it is currently commented out in `direct-command.component.html`, so the bug is hidden);
- the backend handler logs `Received request on /vacuumGripper/{}/command/status`.

A message to `/vacuumGripper/{machineName}/command/status` falls into the catch-all `/vacuumGripper**` handler.

- **Affects:** backend, frontend.
- **Open question:** fix the frontend (or the backend) now, independently of the naming discussion, or remove the unused component?

## 5. Request destination vs reply topic names

There is no rule to derive the reply topic from the request destination:

| Request (`/app` + ...) | Reply / related topic |
|---|---|
| `/factory/configuration` | `/topic/factory-configuration` |
| `/factory/instance` | `/topic/factory-instance` |
| `/factory/placeholder` | `/topic/command-placeholder` |
| `/factoryMission/mission-configuration` | `/topic/mission-configuration` |
| `/factoryMission/list` | `/topic/mission-configuration` (same topic as above) |
| `/factoryMission/status` | `/topic/mission-status` |
| `/factoryMission/actual-command-executing` | `/topic/actual-command-executing` |
| `/dynamic-mission/missions` | `/topic/dynamic-missions` |
| `/mission-extension/configuration` | `/topic/mission-extension/configuration` |
| `/mission-extension/logs` | `/topic/mission-extension/logs` |
| `/{type}/{machineName}/command-status` | `/topic/{machineName}/command-status` |
| `/plc/{plcName}/plc-connection` | `/topic/{plcName}/plc-connection-status` |
| `/logs/request` | `/topic/frontend-logs` |

Sometimes the segments are joined with `-` (`factory-configuration`), sometimes they stay segments (`mission-extension/configuration`);
the prefix is sometimes dropped (`/factoryMission/status` → `/topic/mission-status`, `/{type}/...` → `/topic/{machineName}/...`)
and sometimes renamed (`placeholder` → `command-placeholder`, `plc-connection` → `plc-connection-status`).

- **Affects:** backend, frontend.
- **Open question:** should the reply topic always be `/topic` + the request path (as for `/mission-extension/...`)?

## 6. Topics without namespace, and the `/topic/{name}/...` namespace shared by machines and PLCs

- Most topics are flat (`/topic/mission-status`, `/topic/mission-configuration`, `/topic/actual-command-executing`, `/topic/controller-feedbacks`),
  only the mission extension uses a sub-namespace (`/topic/mission-extension/...`).
- Machine topics are `/topic/{machineName}/...` and PLC topics are `/topic/{plcName}/plc-connection-status`:
  machines and PLCs share the same first segment, without their type. A machine and a PLC with the same name would share it.
- The same PLC topic is declared with two variable names: `/topic/{controllerName}/plc-connection-status` (`@WebSocketPublish`)
  and `/topic/{plcName}/plc-connection-status` (`@SendTo`), so the generated documentation lists it twice.

- **Affects:** backend, frontend.
- **Open question:** should topics mirror the incoming destinations (e.g. `/topic/{type}/{machineName}/...`, `/topic/plc/{plcName}/...`)?

## 7. Three mission features with unrelated names

Three mission features exist side by side, and their names don't tell what distinguishes them:

- `/factoryMission/...` (DSL based missions), with topics `/topic/mission-status`, `/topic/mission-configuration`, `/topic/actual-command-executing`;
- `/dynamic-mission/...` and `/api/dynamic-mission/...`, with topic `/topic/dynamic-missions`;
- `/mission-extension/...` and `/api/mission-extension/...` (state machine missions per machine, and global missions), with topics `/topic/mission-extension/...`;
- `/api/replanning/...` also acts on the mission extension missions (swap, stop), but is named after its client (the replanning of the digital twin) rather than after the missions it acts on.

The generic topics `/topic/mission-status` and `/topic/mission-configuration` only concern the `/factoryMission` ones.

- **Affects:** backend, frontend, external (`/api/replanning`, `/api/dynamic-mission`).
- **Open question:** which names should these features have in the domain vocabulary, and should all their endpoints and topics be prefixed with them?
  Are all three still needed?

## 8. Position of the mission name in command paths

- verb then name: `/factoryMission/command/start/{missionName}`, `/dynamic-mission/command/start/{missionName}`, `/api/dynamic-mission/command/start/{missionName}`;
- name then verb: `/api/mission-extension/{missionName}/stop`;
- name in the payload: `/mission-extension/start`, `/mission-extension/stop` (STOMP), `/api/mission-extension/start`, and all `/api/replanning/...` endpoints
  (machine names contain `/`, so they can't be path variables);
- `/command/` segment for some features (`/dynamic-mission/command/start/...`) and not others (`/mission-extension/start`).

For REST, `/api/mission-extension/{missionName}/stop` (resource then action) and `/api/dynamic-mission/command/start/{missionName}` follow two different styles.

- **Affects:** backend, frontend, external.
- **Open question:** one style for STOMP and one for REST, or the same path shape for both?

## 9. Meaning of `stop`

- `/dynamic-mission/command/stop`, `/factoryMission/command/stop`: stop *the* active mission (no name);
- `/mission-extension/stop` (STOMP, name in the payload) and `/api/mission-extension/{missionName}/stop`: stop *a given* mission;
- `/mission-extension/stop-active`, `/api/mission-extension/stop-active`: stop *all* active missions;
- `/mission-extension/global/stop`: stop the global mission *and the machines*;
- `/api/replanning/mission/stop`: stop the mission of *a machine*, without interrupting its command;
- `/{type}/{machineName}/command/stop`: stop a machine.

- **Affects:** backend, frontend, external.
- **Open question:** should the scope be explicit in the names (e.g. `stop-all`, `stop` + target) so that the same word always means the same thing?

## 10. Logs destinations

- `/logs/request` has no class-level `@MessageMapping` prefix, ends with a verb, and replies on `/topic/frontend-logs`;
- the mission extension logs are `/mission-extension/logs` → `/topic/mission-extension/logs`, and `GET /api/mission-extension/logs`.

- **Affects:** backend, frontend.
- **Open question:** `/logs` → `/topic/logs`, consistently with item 5?

## 11. Machine action names differ from the PLC command names

Some STOMP action names are not the name of the PLC command they send (according to their `@WebSocketDoc`):

- high-bay warehouse: `go_to_column` → `crane_goto_column`, `go_to_row` → `crane_goto_row`,
  `horizontal_to` → `crane_goto_horizontal_position`, `vertical_to` → `crane_goto_vertical_position`;
- spelling of "go to": `go_to_column`, `go_to_position` (vacuum gripper), `ordered_go_to` vs `crane_goto_*` (PLC);
- legacy duplicates: `process` and `process1` (indexed line, multi-processing station).

This conflicts with the rule quoted in item 2.

- **Affects:** backend, frontend (the command name is matched in `direct-command.component.html`).
- **Open question:** align with the PLC names, or document the mapping?

## 12. The word "controller"

"Controller" means three different things: the PLC programs (rppmcontroller, `ControllerConfiguration` in `configuration.yml`),
the Spring controllers, and in topics `/topic/controller-feedbacks` and `/topic/{controllerName}/plc-connection-status` (the PLC).
The same PLC is called `plc` in `/plc/{plcName}/plc-connection`.

- **Affects:** backend (documentation, variable names), frontend.
- **Open question:** use "PLC" in all endpoint and topic names?

## 13. Catch-all `**` handlers listed as incoming topics

Each machine controller and some others declare a catch-all `@MessageMapping("**")` (to log unknown destinations),
which the generated documentation lists as incoming topics (`/conveyorBelt**`, `/factory**`, `/plc**`...).
The resulting `/conveyorBelt**` is not a valid path pattern for a reader (missing `/`).

- **Affects:** backend (documentation generator).
- **Open question:** hide them from the generated documentation, or document them once as "unknown destinations are logged"?
