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
│   ├── MachineIR.java              # A machine (interface contract)
│   ├── StateIR.java                # A state with outgoing transitions
│   ├── TransitionIR.java           # Transition (from, to, trigger, guard, action)
│   ├── TransitionTriggerIR.java    # Trigger base
│   ├── TransitionTriggerSimpleIR   # Message-based trigger
│   ├── TransitionTriggerWhenIR     # Accept-when conditional trigger
│   ├── TransitionActionIR.java     # Action base
│   ├── TransitionActionCustomIR    # Custom code action
│   ├── TransitionActionMachineIR   # Machine call action
│   ├── TransitionActionSendToIR    # Send-to-machine action
│   ├── EnumerationIR.java          # Enum definition
│   ├── ActionIR.java               # Reusable action definition
│   ├── Ref.java                    # Lazy reference by qualified name
│   └── ...
├── generators/
│   ├── IrRepository.java           # Central registry for all IR elements
│   ├── SymbolIndex.java            # Symbol table (definitions by qualified name)
│   ├── Linker.java                 # Pass 3a: resolves references, validates
│   ├── TypeTable.java              # Maps qualified names → JavaPoet TypeNames
│   └── JavaTransformer.java        # Pass 3b: IR → Java source files
├── runtime/
│   ├── api/
│   │   ├── Machine.java            # Marker interface
│   │   ├── MachineAdapter.java     # publish/subscribe/shutdown contract
│   │   ├── MachineMissionStrategy  # Strategy interface (start/stop/onEvent)
│   │   ├── AbstractMissionStrategy # Base class for generated missions
│   │   └── AbstractAdapter.java    # Base adapter implementation
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
    └── SymlUtils.java
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
3. **Pass 2 — IR conversion**: `ToIrSwitch` walks the same tree, building a typed Intermediate Representation (`IrRepository`) using the symbol table for lookups.
4. **Pass 3 — Link & Generate**: `Linker` validates cross-references and builds a `TypeTable` mapping qualified names to Java types. `JavaTransformer` then produces Java source files via JavaPoet.

### Generated artifacts

For each SysML mission, the generator produces:

| Artifact | Description |
|----------|-------------|
| **Mission class** | Extends `AbstractMissionStrategy`, builds states and transitions, wires event subscriptions |
| **Machine interfaces** | One per `PartDefinition` — defines the adapter contract (methods from actions/attributes) |
| **Enumerations** | One per `EnumerationDefinition` |
| **Message classes** | One per `ItemDefinition` — event payloads |
| **Accept-event classes** | One per `accept when` trigger — implements `Event` |
| **Actions utility interface** | Custom action callbacks for manual implementation |

### Runtime framework

Generated missions extend `AbstractMissionStrategy` and use `RuntimeInstance` for execution:

- **Event dispatch**: incoming events are matched against transitions leaving the active state.
- **Completion transitions**: after each state change, guard-less transitions (completions) fire automatically.
- **Pub/Sub**: machines communicate via `MachineAdapter.publish()` / `subscribe()`.

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

### Utils interface for custom code

Rather than attempting to generate complex action logic, the generator produces a **Utils interface** that application developers implement manually in plain Java. This handles:

- Custom transition actions (business logic).
- Custom `accept when` guard evaluation.

This keeps the generator deterministic while allowing full flexibility in the hand-written parts.

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

Tests use JUnit 5 with a **golden file** strategy:

- Expected generator output is stored in `src/test/resources/golden/`.
- Tests run the generator on sample inputs and compare with the golden baseline.
- To update golden files after intentional changes: `mvn test -Dupdate.golden=true`, then review the diff.

```bash
# Run tests
mvn test

# Update golden files
mvn test -Dupdate.golden=true
```

## SysML mapping

| SysML concept | Generated Java |
|---------------|----------------|
| `state def` (with states) | Mission class extending `AbstractMissionStrategy` |
| `state` (usage) | `RuntimeState` constant in the mission class |
| `transition` | `RuntimeTransition` connecting states |
| `part def` | Machine interface extending `MachineAdapter` |
| `item def` | Message/event class implementing `Event` |
| `enum def` | Java enum |
| `action def` | Method in the machine interface |
| `accept when` | Event class + trigger-based subscription |
