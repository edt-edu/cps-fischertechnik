package ConveyorBeltSystem {
    private import ScalarValues::*;
    private import Common::*;
    private import Common::Messages::*;

    package ConveyorBeltCommands {
        enum def ConveyorCommandKind {
            MOVE_TO_SENSOR;
            MOVE_OUT;
            MOVE_NB_STEPS;
            STOP;
            STATUS_REQUEST;
        }

        enum def DirectionKind {
            FORWARD;
            BACKWARD;
        }

        action def MoveToSensor {
            in direction : DirectionKind;
        }

        action def MoveOut {
            in direction : DirectionKind;
        }

        action def MoveNbSteps {
            in steps : Integer;
            in direction : DirectionKind;
        }

        action def Stop {}

        action def StatusRequest {}

        item def CommandSuccess;
    } // package ConveyorBeltCommands

    package ConveyorBeltMessages {
        private import ConveyorBeltSystem::ConveyorBeltCommands::*;

        item def FeedFreeEventMessage :> EventMessage;
        item def SwapBusyEventMessage :> EventMessage;

        item def CBCommandSuccessEventMessage :> EventMessage;
    } // package ConveyorBeltMessages

    package ConveyorBelt {
        private import ConveyorBeltSystem::ConveyorBeltCommands::*;

        part def ConveyorBeltMachine specializes Machine {
            action moveToSensor : MoveToSensor;
            action moveOut : MoveOut;
            action moveNbSteps : MoveNbSteps;
            action stop : Stop;
            action statusRequest : StatusRequest;

            attribute currentCommand : ConveyorCommandKind;
            attribute direction : DirectionKind;

            attribute currentStepCount : Integer;
            attribute targetStepCount : Integer;

            attribute conveyorSensFeed : Boolean;
            attribute conveyorSensSwap : Boolean;
            attribute conveyorSensImpulse : Integer;
        }
    } // package ConveyorBelt
} // package ConveyorBeltSystem
