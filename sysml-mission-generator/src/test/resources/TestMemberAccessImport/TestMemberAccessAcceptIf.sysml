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
package VacuumGripperSystem {
    private import ScalarValues::*;
    private import Common::*;

    package VacuumGripper {
        //private import VacuumGripperSystem::VacuumGripperCommands::*;

        part def VacuumGripperMachine specializes Machine {
            // perform action goToPosition : GoToPosition;
            // perform action move : Move;
            // perform action pick : Pick;
            // perform action place : Place;
            // perform action setup : Setup;
            // perform action statusRequest : StatusRequest;
            // perform action grip : Grip;
            // perform action release : Release;
            // perform action stop : Stop;
            // perform action moveToSafePosition : MoveToSafePosition;
            // perform action retractArm : RetractArm;

            // attribute currentCommand : VacuumGripperCommandKind;
            // attribute executionStatus : ExecutionStatusKind;

            attribute verticalEncoder : Real;
            attribute armEncoder : Real;
            attribute rotEncoder : Real;

            attribute expectedVerticalEncoderValue : Real;
            attribute expectedArmEncoderValue : Real;
            attribute expectedRotationEncoderValue : Real;

            attribute vacuumActCompressorOn : Boolean;
            attribute vacuumActValve : Boolean;
        }
    }
}
package SortingLineMissions {
    private import Common::Messages::*;
    private import SortingLineSystem::*;
    private import SortingLineSystem::SortingLineCommands::*;
    private import SortingLineSystem::SortingLineMessages::*;

    doc
    /* Nominal mission scenario for SortingLine */
    state def SortingLineNominalMission {
        in sortingLine : SortingLineSystem::SortingLine::SortingLineMachine;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;

        entry;
        then Idle;

        state Idle;
        accept when sortingLine.sensor_SL_in == true then SortCMD;

        state SortCMD;
        accept CommandSuccessEventMessage
            if sortingLine.sensor_SL_blue == true
                do send new TokenAvailableBufferedMessage(color=Color::BLUE) to vacuumGripper
            then Idle;
        accept CommandSuccessEventMessage
            if sortingLine.sensor_SL_white == true
                do send new TokenAvailableBufferedMessage(color=Color::WHITE) to vacuumGripper
            then Idle;
        accept CommandSuccessEventMessage
            if sortingLine.sensor_SL_red == true
                do send new TokenAvailableBufferedMessage(color=Color::RED) to vacuumGripper
            then Idle;
    }
}

package VacuumGripperMissions {
    private import VacuumGripperSystem::*;
    // private import VacuumGripperSystem::VacuumGripperCommands::*;
    // private import VacuumGripperSystem::VacuumGripperMessages::CommandSuccess;
    private import SortingLineSystem::SortingLineCommands::*;
    private import SortingLineSystem::SortingLineMessages::*;
    private import ZonesSystem::Zone;
    private import ZonesSystem::ZonesMessages::*;
//    private import MultiProcessingStationSystem::MultiProcessingStationMessages::*;

    doc
    /* Nominal mission scenario for VacuumGripper n°1 */
    state def VacuumGripper1NominalMission {
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in zoneCB : ZonesSystem::Zone;

        action goToStandby {}

        action pickBlue {}

        action pickWhite {}

        action pickRed {}

        action placeConveyoBeltFeed {}

        entry action setup;
        then SetupCMD;

        state SetupCMD;
        accept CommandSuccessEventMessage do goToStandby then StandbyCMD;

        state StandbyCMD;
        accept CommandSuccessEventMessage
            do send new ReleaseRequestEventMessage() to zoneCB
            then Idle;

        state Idle;
        accept tokenAvailableMessage : TokenAvailableBufferedMessage
            if tokenAvailableMessage == Color::BLUE or tokenAvailableMessage == Color::WHITE do pickBlue
            then PickColorCMD;
        accept tokenAvailableMessage : TokenAvailableBufferedMessage
            if tokenAvailableMessage.color == Color::WHITE do pickWhite   // syson importer fails on this line because of the member access syntax in the 'if' clause
            then PickColorCMD;
        accept tokenAvailableMessage : TokenAvailableBufferedMessage
            if tokenAvailableMessage.color == Color::RED do pickRed  // syson importer fails on this line because of the member access syntax in the 'if' clause
            then PickColorCMD;

        state PickColorCMD;
        accept CommandSuccessEventMessage
            do send new AcquireRequestBufferedMessage(sender=vacuumGripper) to zoneCB
            then IdlePicked;

        state IdlePicked;
        accept AcquireResponseEventMessage do placeConveyoBeltFeed then PlaceConveyorBeltFeed;

        state PlaceConveyorBeltFeed;
        accept CommandSuccessEventMessage do goToStandby then StandbyCMD;
    }
}