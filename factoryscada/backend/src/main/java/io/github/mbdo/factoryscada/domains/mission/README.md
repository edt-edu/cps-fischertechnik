## Architecture Overview

The FactoryMissionsParallelized_dto structure is constructed by the FactoryScada from a corresponding configuration file.

Once built:

- The InitializerVisitor is invoked to initialize node attributes.
- The ExecuterVisitor then executes the missions described in the FactoryMissionsParallelized_dto.


## Data Structure: FactoryMissionsParallelized_dto

The following diagram illustrates the class hierarchy and composition of the parallelized mission execution model:

```mermaid
classDiagram
    class FactoryMissionsParallelized_dto {
        +String name
        +List<MissionParallelized_dto> missions
    }

    class MissionParallelized_dto {
        +String name
        +String description
        +List<Node_dto> nodes
    }

    class Node_dto {
        <<abstract>>
        -String id
        -String description
        -List<String> outputs
        +List<Node_dto> outputNodes
        +abstract void accept(Visitor v)
    }

    class ActionNode_dto {
        <<abstract>>
    }

    class RawMachineCommand_dto {
        -String placeholder
    }

    class WaitAction_dto {
        -int time
    }

    class ControlNode_dto {
        <<abstract>>
    }

    class Fork_dto {
    }

    class Join_dto {
        -Integer numberInputs
    }

    class Entry_dto {
    }

    FactoryMissionsParallelized_dto --> MissionParallelized_dto : contains
    MissionParallelized_dto --> Node_dto : contains

    Node_dto <|-- ActionNode_dto
    Node_dto <|-- ControlNode_dto

    ActionNode_dto <|-- RawMachineCommand_dto
    ActionNode_dto <|-- WaitAction_dto

    ControlNode_dto <|-- Fork_dto
    ControlNode_dto <|-- Join_dto
    ControlNode_dto <|-- Entry_dto
```