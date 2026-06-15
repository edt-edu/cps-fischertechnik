package VacuumGripperMissions {
    private import Common::*;
    private import VacuumGripperSystem::*;
    private import VacuumGripperSystem::VacuumGripperCommands::*;
    private import VacuumGripperSystem::VacuumGripperMessages::*;
    private import SortingLineSystem::SortingLineCommands::*;
    private import SortingLineSystem::SortingLineMessages::*;
    private import ZonesSystem::Zone;
    private import ZonesSystem::ZonesMessages::*;
    private import MultiProcessingStationSystem::MultiProcessingStationMessages::*;
    private import ScalarValues::*;

    state def VacuumGripper1NominalMission {
        doc
        /* Nominal mission scenario for VacuumGripper n°1 */

        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in sortingLine : SortingLineSystem::SortingLine::SortingLineMachine;
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

        entry vacuumGripper.setup;
        then SetupCMD;

        state SetupCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper do goToStandby then StandbyCMD;

        state StandbyCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new ReleaseRequestEventMessage() to zoneCB
            then Idle;

        state Idle;
        accept BlueTokenAvailableEventMessage do pickBlue then PickColorCMD;
        accept WhiteTokenAvailableEventMessage do pickWhite then PickColorCMD;
        accept RedTokenAvailableEventMessage do pickRed then PickColorCMD;

        state PickColorCMD;
        accept VGRCommandSuccessEventMessage via vacuumGripper
            do send new AcquireRequestEventMessage() to zoneCB
            then IdlePicked;

        state IdlePicked;
        accept AcquireResponseEventMessage via zoneCB
            do placeConveyoBeltFeed
            then PlaceConveyorBeltFeed;

        state PlaceConveyorBeltFeed;
        accept VGRCommandSuccessEventMessage via vacuumGripper do goToStandby then StandbyCMD;
    }

    state def VacuumGripper2NominalMission {
        doc
        /* Nominal mission scenario for VacuumGripper n°2 */

        @MainMachineMetadata about vacuumGripper;
        in vacuumGripper : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
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

        action releaseCBZoneAndAcquireMPSZone {
            send new ReleaseRequestEventMessage() to zoneCB;
            send new AcquireRequestEventMessage(sender=vacuumGripper) to zoneMPS;
        }

        entry vacuumGripper.setup;
        then SetupIdle;

        state SetupIdle;
        accept VGRCommandSuccessEventMessage do goToStandby then GotoStandbyCMD;

        state GotoStandbyCMD;
        accept VGRCommandSuccessEventMessage then Idle;

        state Idle;
        accept InputFreeEventMessage
            do send new AcquireRequestEventMessage(sender=vacuumGripper) to zoneCB
            then WaitForCBZoneAcquisition;

        state WaitForCBZoneAcquisition;
        accept AcquireResponseEventMessage if true == false do pickCBswap then PickCBswapCMD;

        state PickCBswapCMD;
        accept VGRCommandSuccessEventMessage do goToStandby then ReleaseCBZone;

        state ReleaseCBZone;
        accept VGRCommandSuccessEventMessage
            do releaseCBZoneAndAcquireMPSZone
            then WaitForMPSZoneAcquisition;

        state WaitForMPSZoneAcquisition;
        accept AcquireResponseEventMessage do placeMPSin then PlaceMPSInCMD;

        state PlaceMPSInCMD;
        accept VGRCommandSuccessEventMessage do goToStandby then ReleaseMPSZone;

        state ReleaseMPSZone;
        accept VGRCommandSuccessEventMessage
            do send new ReleaseRequestEventMessage() to zoneMPS
            then GotoStandbyCMD;
    }
} // package VacuumGripperMissions
