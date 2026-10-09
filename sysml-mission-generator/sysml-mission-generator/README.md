# SysML Mission Generator

A code generator that transforms **SysML v2** state machine models into executable **Java** state machine implementations. Built on [Eclipse SySon](https://eclipse.dev/syson/) for SysML parsing and [JavaPoet](https://github.com/palantir/javapoet) for code generation.

## Overview

This tool reads `.sysml` files describing missions as state machines (states, transitions, triggers, guards, actions) and produces ready-to-use Java classes implementing those missions. The generated code relies on a minimal runtime framework that handles event dispatch and state transitions.

## Requirements

- **Java 21+**
- **Maven 3.8+**

## Quick start

### Build

```bash
mvn package -DskipTests
```

This produces a fat CLI jar via `maven-shade-plugin` at `target/sysml-mission-generator-*-cli.jar`.

### Run (CLI)

```bash
java -jar target/sysml-mission-generator-*-cli.jar \
    -i path/to/model.sysml \
    -p com.example.mission \
    -o ./generated-sources
```

| Option | Description |
|--------|-------------|
| `-i`, `--input` | Path(s) to SysML file(s) (repeatable) |
| `-p`, `--package` | Base Java package name for generated classes |
| `-o`, `--output` | Output folder for generated Java sources |
| `-h`, `--help` | Display usage help |

### Run (script)

A convenience wrapper script handles building, glob resolution, and directory scanning:

```bash
./scripts/generate-from-sysml.sh <output-dir> <package-name> <sysml-inputs...>
```

Inputs can be individual files, directories (searched recursively for `*.sysml`), or glob patterns. See [`scripts/README.md`](scripts/README.md) for details.

## Project structure

```
src/main/java/fr/inria/mbdo/mission/
├── SysmlMissionGeneratorCLI.java   # CLI entry point (picocli)
├── SysmlMissionGenerator.java      # Orchestrates the 3-pass pipeline
├── SysMLResourceSetProvider.java   # EMF ResourceSet setup for SySon
├── importer/
│   └── SysmlImporter.java          # Parses .sysml text into EMF resources
├── switchs/
│   ├── IndexerSwitch.java          # Pass 1: indexes all SysML definitions
│   └── ToIrSwitch.java             # Pass 2: converts SysML AST to IR
├── ir/                             # Intermediate Representation model
│   ├── ElementIR.java              # Base class (name, qualifiedName, docs)
│   ├── MachineMissionIR.java       # A mission (state machine composition)
│   ├── MachineIR.java              # A machine (interface contract + attributes)
│   ├── StateIR.java                # A state with outgoing transitions
│   ├── TransitionIR.java           # Transition (from, to, trigger, guard, action)
│   ├── TransitionTriggerIR.java    # Trigger base
│   ├── TransitionTriggerSimpleIR   # Message-based trigger
│   ├── TransitionTriggerWhenIR     # Accept-when conditional trigger (expression tree + associated machine)
│   ├── TriggerExpressionIR.java    # Accept-when expression tree base
│   ├── TriggerOperatorExpressionIR # Binary operator node (==, !=, and, or, …)
│   ├── TriggerMachineAttributeExpressionIR # Leaf: machine.attribute reference
│   ├── TriggerLiteralExpressionIR  # Leaf: boolean / integer / string literal
│   ├── TransitionActionIR.java     # Action base
│   ├── TransitionActionCustomIR    # Custom code action (delegates to Actions interface)
│   ├── TransitionActionMachineIR   # Direct machine method call
│   ├── TransitionActionSendToIR    # Send-to-machine action
│   ├── EnumerationIR.java          # Enum definition
│   ├── MachineActionIR.java        # Action owned by a machine (name + parameters)
│   ├── MachineAttributeIR.java     # Attribute owned by a machine (name + type)
│   ├── Ref.java                    # Lazy reference by qualified name
│   └── ...
├── generators/
│   ├── IrRepository.java           # Central registry for all IR elements
│   ├── SymbolIndex.java            # Symbol table (definitions by qualified name)
│   ├── JavaLinker.java             # Pass 3a: resolves cross-references, builds TypeTable
│   ├── TypeTable.java              # Maps qualified names → JavaPoet TypeNames
│   └── JavaTransformer.java        # Pass 3b: IR → Java source files
├── runtime/
│   ├── api/
│   │   ├── Machine.java            # Marker interface
│   │   ├── MachineAdapter.java     # publish/subscribe/shutdown contract
│   │   ├── MachineMissionStrategy  # Strategy interface (start/stop/onEvent)
│   │   ├── AbstractMissionStrategy # Base class for generated missions
│   │   └── AbstractAdapter.java    # Base adapter implementation (pub/sub engine)
│   └── rtc/
│       ├── def/
│       │   ├── RuntimeState.java       # State definition for runtime
│       │   └── RuntimeTransition.java  # Transition definition for runtime
│       ├── event/
│       │   ├── Event.java              # Event marker interface
│       │   └── CompletionEvent.java    # Auto-fired completion event
│       └── exec/
│           ├── RuntimeInstance.java    # State machine executor
│           ├── RuntimeDefinition.java  # Definition abstraction
│           ├── SimpleRuntimeDefinition # Linear state list implementation
│           ├── RuntimeAction.java      # Functional action interface
│           ├── RuntimeGuard.java       # Functional guard interface
│           └── ...
└── utils/
    ├── FileUtils.java
    ├── ImportUtils.java             # SySon workaround transformations
    ├── StringUtils.java
    └── SysmlToJavaUtils.java        # Qualified-name → Java package/class mapping
```

## Architecture

### Processing pipeline

The generator follows a **3-pass compiler architecture**:

```
┌──────────────┐     ┌───────────────┐     ┌────────────────┐     ┌─────────────┐
│  .sysml file │───▶ │  Pass 1       │───▶ │  Pass 2        │───▶ │  Pass 3     │
│  (text)      │     │  Index types  │     │  Build IR      │     │  Link +     │
│              │     │  (SymbolIndex)│     │  (IrRepository)│     │  Generate   │
└──────────────┘     └───────────────┘     └────────────────┘     └─────────────┘
        │                                                                  │
        ▼                                                                  ▼
 SysmlImporter                                                      .java files
 (SySon parser)                                                     (JavaPoet)
```

1. **Import**: SysML text is parsed by Eclipse SySon into an EMF model (`SysmlImporter`).
2. **Pass 1 — Index**: `IndexerSwitch` traverses the EMF tree and registers all definitions (parts, enums, items, states, actions, attributes) into a `SymbolIndex`.
3. **Pass 2 — IR conversion**: `ToIrSwitch` walks the same tree, building a typed Intermediate Representation (`IrRepository`) using the symbol table for lookups. `accept when` expressions are parsed recursively into a `TriggerExpressionIR` tree and linked to the machine they reference.
4. **Pass 3 — Link & Generate**: `JavaLinker` validates cross-references and builds a `TypeTable` mapping qualified names to Java types. `JavaTransformer` then produces all Java source files via JavaPoet.

### Generated artifacts

For each SysML model, the generator produces the following artifacts. Packages follow `<basePackage>.<sysmlNamespace>`:

| Artifact | One per | Description |
|----------|---------|-------------|
| **Mission class** | `state def` with states | Extends `AbstractMissionStrategy`; builds `RuntimeState`/`RuntimeTransition` objects and wires event subscriptions in its constructor |
| **Actions interface** | Mission | Declares one method per custom transition action; developers implement this to provide the business logic |
| **Machine interface** | `part def` | Extends `MachineAdapter`; declares getters/setters for attributes and abstract methods for machine actions |
| **Abstract machine adapter** | `part def` | Extends `AbstractAdapter` and implements the machine interface; manages `protected volatile` attribute fields and automatically fires accept-when events when their conditions become true — developers extend this instead of `AbstractAdapter` directly |
| **Accept-when event class** | `accept when` trigger | Implements `Event`; published by the abstract adapter whenever the associated boolean condition is satisfied |
| **Message class** | `item def` extending `EventMessage` | Implements `Event`; used as typed event payloads between machines |
| **Enumeration** | `enum def` | Plain Java enum |

### Runtime framework

Generated missions extend `AbstractMissionStrategy` and use `RuntimeInstance` for execution:

- **Event dispatch**: incoming events are matched against transitions leaving the active state.
- **Completion transitions**: after each state change, guard-less transitions (completions) fire automatically.
- **Pub/Sub**: machines communicate via `MachineAdapter.publish()` / `subscribe()`.

### Accept-when event flow

`accept when` expressions in SysML (e.g., `accept when machine.attr == true`) produce two generated artifacts that work together:

1. A **typed event class** (e.g., `AcceptWhenMachineAttrEqualstrueEvent`) placed in a `customevents` sub-package of the mission.
2. An entry in the **abstract machine adapter** that evaluates the condition after every relevant setter call and publishes the event when it becomes true:

```java
// generated in AbstractConveyorBeltMachineAdapter
@Override
public void setConveyorSensFeed(boolean conveyorSensFeed) {
    this.conveyorSensFeed = conveyorSensFeed;
    checkAndFireAcceptWhenEvents();
}

private void checkAndFireAcceptWhenEvents() {
    if ((this.conveyorSensFeed == true) && (this.conveyorSensSwap == false)) {
        publish(new AcceptWhenConveyorBeltConveyorSensFeedEqualstrueAndConveyorBeltConveyorSensSwapEqualsfalseEvent());
    }
}
```

The adapter developer only needs to call the attribute setters (e.g., from MQTT or OPC-UA message handlers) — event firing is handled automatically.

## Developer workflow

For each machine, the expected implementation pattern is:

```
Generated (do not edit)                     Hand-written
─────────────────────────────────           ──────────────────────────────
ConveyorBeltMachine         (interface)
        │
        ▼
AbstractConveyorBeltMachineAdapter          ConveyorBeltAdapterImpl
  - volatile attribute fields                 extends AbstractConveyorBeltMachineAdapter
  - getters / setters                         - MQTT/OPC-UA subscription
  - checkAndFireAcceptWhenEvents()            - calls setXxx() on incoming data
  - abstract action methods                   - implements abstract action methods
```

For each mission, the expected implementation pattern is:

```
Generated (do not edit)                     Hand-written
─────────────────────────────────           ──────────────────────────────
ConveyorBeltNominalMission  (class)         ConveyorBeltNominalMissionActionsImpl
ConveyorBeltNominalMissionActions (iface)     implements ConveyorBeltNominalMissionActions
                                              - implements each custom action method
```

## Design choices

### Intermediate Representation (IR)

Instead of generating Java directly from the SysML AST, the project uses a dedicated IR layer. This:

- Decouples SysML metamodel specifics from code generation logic.
- Allows validation and cross-reference resolution before generation.
- Makes the generator extensible to other target languages in the future.

### Lazy references (`Ref<T>`)

IR elements reference each other by qualified name (`Ref<T>` record). Resolution happens at link time, making it possible to handle forward references and separate concerns between passes.

### Visitor-based traversal

Both `IndexerSwitch` and `ToIrSwitch` extend SySon's `SysmlSwitch<Void>`, leveraging the built-in EMF visitor pattern for type-safe traversal of the SysML metamodel.

### Actions interface for custom code

Rather than attempting to generate complex action logic, the generator produces an **Actions interface** that application developers implement manually in plain Java. Each custom transition action body (expressed in SysML as an inline `action { … }` block on a transition) becomes one method in this interface, documented with its SysML source body when available.

### Abstract machine adapters for accept-when automation

`accept when` conditions are converted into an expression tree in the IR (`TriggerExpressionIR`). The generator uses this tree to:

- Derive the set of machine attributes that each condition depends on.
- Emit a `checkAndFireAcceptWhenEvents()` helper in the abstract adapter.
- Call that helper from the setter of every attribute that appears in at least one condition.

The condition itself is recursively translated to a Java boolean expression (SysML `and` → Java `&&`, SysML `or` → `||`, `==`/`!=`/`<`/`>` pass through). This removes a class of boilerplate that every adapter author would otherwise have to write manually and get right.

### Fat jar distribution

The `maven-shade-plugin` produces a single executable jar containing all dependencies (SySon, JavaPoet, picocli, EMF, etc.), simplifying distribution and usage.

## Key dependencies

| Dependency | Purpose |
|------------|---------|
| [Eclipse SySon](https://eclipse.dev/syson/) `2026.3.0` | SysML v2 metamodel, parser, and import |
| [JavaPoet](https://github.com/palantir/javapoet) (Palantir fork) `0.14.0` | Java source code generation |
| [picocli](https://picocli.info/) `4.7.6` | CLI argument parsing |
| [Lombok](https://projectlombok.org/) `1.18.46` | Boilerplate reduction (`@Getter`) |
| [Logback](https://logback.qos.ch/) `1.5.6` | Logging |
| [JUnit 5](https://junit.org/junit5/) `5.11.0` | Testing |
| [Mockito](https://site.mockito.org/) `5.11.0` | Mocking in tests |
| [JaCoCo](https://www.jacoco.org/) `0.8.12` | Code coverage |

## Testing

Tests use JUnit 5. There are two complementary styles:

- **Golden file tests**: expected generator output is stored in `src/test/resources/golden/`. Tests run the generator on sample inputs and compare output with the golden baseline. Update after intentional changes with `mvn test -Dupdate.golden=true`, then review the diff.
- **Integration checks** (`SysmlJavaTransformerIntegrationChecks`): run the full pipeline and assert focused properties on the generated output (file existence, key content) without maintaining a full golden snapshot.

```bash
# Run all tests
mvn test

# Update golden files after intentional output changes
mvn test -Dupdate.golden=true
```

Input models for tests are resolved relative to `../missions-design-models/` (the sibling directory in the repository). Required files follow the pattern:

```
common/common_def.sysml
zones/zones_def.sysml
zones/zones_missions_def.sysml
CB/cb_def.sysml  +  CB/cb_missions_def.sysml
MPS/mps_def.sysml  +  MPS/mps_missions_def.sysml
SL/sl_def.sysml   +  SL/sl_missions_def.sysml
VGR/vgr_def.sysml +  VGR/vgr_missions_def.sysml
```

## SysML mapping

| SysML concept | Generated Java |
|---------------|----------------|
| `state def` (with states) | Mission class extending `AbstractMissionStrategy` |
| `state` (usage) | `RuntimeState` instance wired in the mission constructor |
| `transition` | `RuntimeTransition` connecting states, with typed trigger, guard, and action |
| `part def` | Machine interface extending `MachineAdapter` + abstract adapter base class |
| `attribute` on `part def` | `protected volatile` field + getter/setter in the abstract adapter |
| `action def` / `perform action` | Abstract method in both the machine interface and the abstract adapter |
| `item def :> EventMessage` | Message class implementing `Event` |
| `enum def` | Java enum |
| `accept <MessageType>` | `RuntimeTransition` triggered by that message class |
| `accept <MessageType> via <machine>` | Subscription on that machine only, and a `RuntimeTransition` with that machine as `source`: it only fires on the messages that machine publishes, even if another mission machine publishes the same type |
| `accept when <expr>` | Typed event class + condition check in abstract adapter that publishes the event when true |
| inline `action { … }` on transition | Method in the mission's Actions interface (Javadoc shows the SysML body) |
| `send new M() to machine` | `publish(new M())` lambda in the mission constructor |
| `do machine.action` | Direct method-reference lambda (`event -> this.machine.action()`) in the mission constructor |
