package VacuumGripperSystem {
    private import ScalarValues::*;
    private import Common::*;

    package VacuumGripperCommands {
        enum def VacuumGripperCommandKind {
            GO_TO_POSITION;
            MOVE;
            PICK;
            PLACE;
            SETUP;
            STATUS_REQUEST;
            GRIP;
            RELEASE;
            STOP;
            MOVE_TO_SAFE_POSITION;
            RETRACT_ARM;
        }

        enum def ExecutionStatusKind {
            MUST_CONTINUE;
            DONE;
        }

        attribute def Position3D {
            attribute vertical : Real;
            attribute horizontal : Real;
            attribute rot : Real;
        }

        action def GoToPosition {
            in targetPosition : Position3D;
        }

        action def Move {
            in startPosition : Position3D;
            in endPosition : Position3D;
        }

        action def Pick {
            in targetPosition : Position3D;
            doc
            /* Moves to the pickup area, activates gripping, and lifts the payload. */
        }

        action def Place {
            in targetPosition : Position3D;
            doc
            /* Moves to the drop-off area, releases the payload, and retreats. */
        }

        action def Setup {}

        action def Grip {
            out attribute vacuumActValve : Boolean = true;
            out attribute vacuumActCompressorOn : Boolean = true;
        }

        action def Release {
            out attribute vacuumActValve : Boolean = false;
            out attribute vacuumActCompressorOn : Boolean = false;
        }

        action def Stop {}

        action def MoveToSafePosition {}

        action def RetractArm {}

        action def StatusRequest {}
    } // package VacuumGripperCommands

    package VacuumGripperMessages {
        private import VacuumGripperSystem::VacuumGripperCommands::*;

        item def VGRCommandSuccessEventMessage;
    } // package VacuumGripperMessages

    package VacuumGripper {
        private import VacuumGripperSystem::VacuumGripperCommands::*;

        part def VacuumGripperMachine specializes Machine {
            perform action goToPosition : GoToPosition;
            perform action move : Move;
            perform action pick : Pick;
            perform action place : Place;
            perform action setup : Setup;
            perform action statusRequest : StatusRequest;
            perform action grip : Grip;
            perform action release : Release;
            perform action stop : Stop;
            perform action moveToSafePosition : MoveToSafePosition;
            perform action retractArm : RetractArm;

            attribute currentCommand : VacuumGripperCommandKind;
            attribute executionStatus : ExecutionStatusKind;

            attribute verticalEncoder : Real;
            attribute armEncoder : Real;
            attribute rotEncoder : Real;

            attribute expectedVerticalEncoderValue : Real;
            attribute expectedArmEncoderValue : Real;
            attribute expectedRotationEncoderValue : Real;

            attribute vacuumActCompressorOn : Boolean;
            attribute vacuumActValve : Boolean;
        }
    } // package VacuumGripper
} // package VacuumGripperSystem
