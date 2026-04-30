package VacuumGripperMissions {
    private import VacuumGripperSystem::*;
    private import VacuumGripperSystem::VacuumGripperCommands::*;
    private import VacuumGripperSystem::VacuumGripperMessages::CommandSuccess;
    private import SortingLineSystem::SortingLineCommands::*;
    private import SortingLineSystem::SortingLineMessages::*;
    private import ZonesSystem::Zone;
    private import ZonesSystem::ZonesMessages::*;
    private import MultiProcessingStationSystem::MultiProcessingStationMessages::*;

    doc
    /* Nominal mission scenario for VacuumGripper n°1 */
    state def VacuumGripper1NominalMission {
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in zoneCB : ZonesSystem::Zone;

        action goToStandby : GoToPosition {
            perform vacuumGripper.goToPosition : GoToPosition {
                in targetPosition : Position3D {
                    vertical = 0.0;
                    horizontal = 0.0;
                    rot = 0.0;
                }
            }
        }

        action pickBlue : Pick {
            perform vacuumGripper.pick : Pick {
                in targetPosition : Position3D {
                    vertical = 0.0;
                    horizontal = 0.0;
                    rot = 0.0;
                }
            }
        }

        action pickWhite : Pick {
            perform vacuumGripper.pick : Pick {
                in targetPosition : Position3D {
                    vertical = 0.0;
                    horizontal = 0.0;
                    rot = 0.0;
                }
            }
        }

        action pickRed : Pick {
            perform vacuumGripper.pick : Pick {
                in targetPosition : Position3D {
                    vertical = 0.0;
                    horizontal = 0.0;
                    rot = 0.0;
                }
            }
        }

        action placeConveyoBeltFeed : Place {
            perform vacuumGripper.place : Place {
                in targetPosition : Position3D {
                    vertical = 0.0;
                    horizontal = 0.0;
                    rot = 0.0;
                }
            }
        }

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
            if tokenAvailableMessage.color == Color::BLUE do pickBlue
            then PickColorCMD;
        accept tokenAvailableMessage : TokenAvailableBufferedMessage
            if tokenAvailableMessage.color == Color::WHITE do pickWhite
            then PickColorCMD;
        accept tokenAvailableMessage : TokenAvailableBufferedMessage
            if tokenAvailableMessage.color == Color::RED do pickRed
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

    doc
    /* Nominal mission scenario for VacuumGripper n°2 */
    state def VacuumGripper2NominalMission {
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in conveyorBelt : ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine;
        in zoneCB : ZonesSystem::Zone;
        in zoneMPS : ZonesSystem::Zone;

        action goToStandby : GoToPosition {
            perform vacuumGripper.goToPosition : GoToPosition {
                in targetPosition : Position3D {
                    vertical = 0.0;
                    horizontal = 0.0;
                    rot = 0.0;
                }
            }
        }

        action pickCBswap : Pick {
            perform vacuumGripper.pick : Pick {
                in targetPosition : Position3D {
                    vertical = 0.0;
                    horizontal = 0.0;
                    rot = 0.0;
                }
            }
        }

        action placeMPSin : Place {
            perform vacuumGripper.place : Place {
                in targetPosition : Position3D {
                    vertical = 0.0;
                    horizontal = 0.0;
                    rot = 0.0;
                }
            }
        }

        entry;
        then SetupIdle;

        state SetupIdle;
        accept CommandSuccessEventMessage do goToStandby then GotoStandbyCMD;

        state GotoStandbyCMD;
        accept CommandSuccessEventMessage then Idle;

        state Idle;
        accept InputFreeBufferedMessage
            do send new AcquireRequestBufferedMessage(sender=vacuumGripper) to zoneCB
            then WaitForCBZoneAcquisition;

        state WaitForCBZoneAcquisition;
        accept AcquireResponseEventMessage do pickCBswap then PickCBswapCMD;

        state PickCBswapCMD;
        accept CommandSuccessEventMessage do goToStandby then ReleaseCBZone;

        state ReleaseCBZone;
        accept CommandSuccessEventMessage
            do action {
                send new ReleaseRequestEventMessage() to zoneCB;
                send new AcquireRequestBufferedMessage(sender=vacuumGripper) to zoneMPS;
            }
            then WaitForMPSZoneAcquisition;

        state WaitForMPSZoneAcquisition;
        accept AcquireResponseEventMessage do placeMPSin then PlaceMPSInCMD;

        state PlaceMPSInCMD;
        accept CommandSuccessEventMessage do goToStandby then ReleaseMPSZone;

        state ReleaseMPSZone;
        accept CommandSuccessEventMessage
            do send new ReleaseRequestEventMessage() to zoneMPS
            then GotoStandbyCMD;
    }
}
