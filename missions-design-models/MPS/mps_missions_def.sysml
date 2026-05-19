package MultiProcessingStationMissions {
    private import MultiProcessingStationSystem::*;
    private import MultiProcessingStationSystem::MultiProcessingStationCommands::*;
    private import MultiProcessingStationSystem::MultiProcessingStationMessages::*;
    private import ZonesSystem::*;
    private import ZonesSystem::ZonesMessages::*;

    doc
    /* Nominal mission for the multi-processing station */
    state def MultiProcessingStationNominalMission {
        in multiProcessingStation
            : MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine;
        in zoneMPS : ZonesSystem::Zone;

        entry action setup;
        then SetupCMD;

        state SetupCMD;
        accept CommandSuccessEventMessage then Idle;

        state Idle;
        accept when
                multiProcessingStation.sensor_MPS_in == true and
                multiProcessingStation.sensor_MPS_out == false
            do send new AcquireRequestBufferedMessage(sender=multiProcessingStation) to zoneMPS
            then ZoneAcquired;

        state ZoneAcquired;
        accept AcquireResponseEventMessage do multiProcessingStation.process then ProcessCMD;

        state ProcessCMD;
        accept CommandSuccessEventMessage
            do action {
                send new ReleaseRequestEventMessage() to zoneMPS;
                send new InputFreeBufferedMessage() to multiProcessingStation;
                send new OutputBusyBufferedMessage() to multiProcessingStation;
            }
            then Idle;
    }
} // package MultiProcessingStationMissions
