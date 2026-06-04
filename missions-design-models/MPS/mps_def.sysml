package MultiProcessingStationSystem {
    private import ScalarValues::*;
    private import Common::*;
    private import Common::Messages::*;

    package MultiProcessingStationCommands {
        enum def MultiProcessingStationCommandKind {
            SETUP;
            STOP;
            PROCESS1;
            PROCESS;
            MOVE_TO_SAFE_POSITION;
            OVEN_LOAD;
            OVEN_UNLOAD;
            OVEN_HEAT;
            OVEN_PROCESS;
            ARM_MOVE;
            ARM_PICK;
            ARM_PLACE;
            TURNTABLE_ROTATE;
            TURNTABLE_EJECT;
            CONVEYOR_MOVE_TO_SENSOR;
            CONVEYOR_MOVE_OUT;
            SAW_CUT;
        }

        action def Setup {}

        action def Stop {}

        action def Process1 {}

        action def Process {}

        action def MoveToSafePosition {}

        action def OvenLoad {}

        action def OvenUnload {}

        action def OvenHeat {}

        action def OvenProcess {}

        action def ArmMove {}

        action def ArmPick {}

        action def ArmPlace {}

        action def TurntableRotate {}

        action def TurntableEject {}

        action def ConveyorMoveToSensor {}

        action def ConveyorMoveOut {}

        action def SawCut {}
    } // package MultiProcessingStationCommands

    package MultiProcessingStationMessages {
        private import MultiProcessingStationSystem::MultiProcessingStationCommands::*;

        item def InputFreeEventMessage :> EventMessage;
        item def OutputBusyEventMessage :> EventMessage;
        item def MPSCommandSuccessEventMessage :> EventMessage;
    } // package MultiProcessingStationMessages

    package MultiProcessingStation {
        private import MultiProcessingStationSystem::MultiProcessingStationCommands::*;

        part def MultiProcessingStationMachine specializes Machine {
            perform action setup : Setup;
            perform action stop : Stop;
            perform action process1 : Process1;
            perform action process : Process;
            perform action moveToSafePosition : MoveToSafePosition;
            perform action ovenLoad : OvenLoad;
            perform action ovenUnload : OvenUnload;
            perform action ovenHeat : OvenHeat;
            perform action ovenProcess : OvenProcess;
            perform action armMove : ArmMove;
            perform action armPick : ArmPick;
            perform action armPlace : ArmPlace;
            perform action turntableRotate : TurntableRotate;
            perform action turntableEject : TurntableEject;
            perform action conveyorMoveToSensor : ConveyorMoveToSensor;
            perform action conveyorMoveOut : ConveyorMoveOut;
            perform action sawCut : SawCut;

            attribute currentCommand : MultiProcessingStationCommandKind;

            attribute sensor_MPS_in : Boolean = false;
            attribute sensor_MPS_out : Boolean = true;
        }
    } // package MultiProcessingStation
} // package MultiProcessingStationSystem
