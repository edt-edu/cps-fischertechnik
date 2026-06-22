# Mission Extension Architecture (ren_mission_01)

## Overview

This package provides the bridge between the **mission state machine runtime** (event-driven,
defined in `fr.inria.mbdo.mission.extensions.ren_mission_01`) and the **Spring application context**.

## Layered Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Frontend (React/Angular)                                       │
│  REST: /api/mission-extension   STOMP: /app/mission-extension/* │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│  MissionExtensionController + MissionExtensionService           │
│  Manages mission lifecycle (start/stop/override)                │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│  MissionExtensionRegistry                                       │
│  Holds the MissionBindingCatalog, creates mission instances     │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│  ExtensionMissionAutoConfig (this package)                      │
│  Declares adapter beans + action beans + catalog assembly       │
└──────┬──────────────────────────────────────────┬───────────────┘
       │                                          │
┌──────▼──────────┐                    ┌──────────▼──────────────┐
│ Adapters Impls  │                    │  Action Impls           │
│ (Spring beans)  │                    │  (Spring beans)         │
│ implement the   │                    │  implement mission      │
│ machine ifaces  │                    │  action interfaces      │
└──────┬──────────┘                    └─────────────────────────┘
       │
       │  (future: bridge to legacy FactoryScada/SocketProtocol)
       │
┌──────▼──────────────────────────────────────────────────────────┐
│  Physical Hardware via MQTT                                     │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### Machine Adapters Implementations

Each adapter:

- Extends `AbstractAdapter` (provides event pub/sub infrastructure)
- Implements the corresponding machine interface (e.g. `ConveyorBeltMachine`)
- Logs all commands via SLF4J
- Maintains volatile local state for sensor/actuator values

**Current adapters:**

| Bean Name                       | Interface                       | Physical Machine     |
|---------------------------------|---------------------------------|----------------------|
| `conveyorBeltAdapter`           | `ConveyorBeltMachine`           | Conveyor Belt        |
| `sortingLineAdapter`            | `SortingLineMachine`            | Sorting Line         |
| `multiProcessingStationAdapter` | `MultiProcessingStationMachine` | Multi-Processing Stn |
| `vacuumGripper1Adapter`         | `VacuumGripperMachine`          | Vacuum Gripper 1     |
| `vacuumGripper2Adapter`         | `VacuumGripperMachine`          | Vacuum Gripper 2     |
| `zoneCBAdapter`                 | `Zone`                          | CB Exclusion Zone    |
| `zoneMPSAdapter`                | `Zone`                          | MPS Exclusion Zone   |

### Mission Action Implementations (NoOp*)

Each NoOp action class implements the custom action interface for a mission.
They log invocations and can be replaced with real business logic.

### MissionBindingCatalog

The catalog wires everything together:

- **MissionTemplates** — named state machine instances with a factory for fresh copies
- **MachineMissionBindings** — which missions are valid for which machine
- **GlobalMissions** — orchestrations that start all machines with coordinated missions

## Connecting to Real Hardware

To bridge a stub adapter to the legacy `FactoryScada` TCP socket layer:

1. Inject `FactoryScada` into the adapter (or create a separate `@Service` bridge)
2. In command methods (e.g. `moveToSensor()`), delegate to the legacy machine's command
3. Subscribe to feedback from the socket layer and call `this.publish(new CBCommandSuccessEventMessage())` to advance
   the mission state machine

Example:

```java

@Override
public void moveToSensor() {
    // Delegate to legacy layer
    legacyConveyorBelt.executeCommand("moveToSensor", new GenericMachineCommandDTO<>(...))
}

// Called when TCP feedback arrives
public void onHardwareFeedback(FeedbackMessage msg) {
    if (msg.isSuccess()) {
        publish(new CBCommandSuccessEventMessage());
    }
}
```

## Adding New Missions

1. Create a new `*Mission.java` class extending `AbstractMissionStrategy` in the `ren_mission_01` package
2. Create the corresponding `*MissionActions.java` interface
3. Add a `NoOp*Actions` implementation in this package
4. Register a new `MissionTemplate` in `ExtensionMissionAutoConfig.missionBindingCatalog()`
5. Add the mission name to the relevant `MachineMissionBinding`
6. Optionally add it to a `GlobalMission`
