package SortingLineSystem {
    private import ScalarValues::*;
    private import Common::*;

    package SortingLineCommands {
        enum def SortingLineCommandKind {
            SORT;
            STOP;
        }

        enum def ExecutionStatusKind {
            MUST_CONTINUE;
            DONE;
        }

        enum def Color {
            RED;
            BLUE;
            WHITE;
            AUTO;
        }

        action def Stop {}

        action def Eject {
            in color : Color;
        }
    }

    package SortingLineMessages {
        private import Messages::*;
        private import SortingLineSystem::SortingLineCommands::*;

        item def TokenAvailableBufferedMessage :> BufferedMessage {
            attribute color : Color;
        }

        item def CommandSuccessEventMessage :> EventMessage;
    }

    package SortingLine {
        private import SortingLineSystem::SortingLineCommands::*;

        part def SortingLineMachine specializes Machine {
            perform action eject : Eject;
            perform action stop : Stop;

            attribute sensor_SL_in : Boolean = false;
            attribute sensor_SL_blue : Boolean = false;
            attribute sensor_SL_white : Boolean = false;
            attribute sensor_SL_red : Boolean = false;
        }
    }
}
