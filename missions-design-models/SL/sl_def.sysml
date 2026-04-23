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
        calc def GetTokenAvailableBufferedMessageColor { 
            metadata Workaround{description="SysOn does not currently support using the member access syntax (request.sender) in the 'in' clause of an action or  in the 'send to' statement. when coming from an accept";}
            doc /* This calculation extracts the color from the TokenAvailableBufferedMessage. It is used as a workaround for current limitations in SysOn regarding direct access to message attributes in action inputs and send statements, particularly when the message is received through an accept statement. */
            in value : TokenAvailableBufferedMessage; 
            return : Color = value.color;
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
