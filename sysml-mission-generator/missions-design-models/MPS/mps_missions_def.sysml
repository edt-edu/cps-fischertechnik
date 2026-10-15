package MultiProcessingStationMissions {
    private import Common::*;
    private import MultiProcessingStationSystem::*;
    private import MultiProcessingStationSystem::MultiProcessingStationCommands::*;
    private import MultiProcessingStationSystem::MultiProcessingStationMessages::*;
    private import ZonesSystem::*;
    private import ZonesSystem::ZonesMessages::*;

    state def MultiProcessingStationNominalMission {
        doc
        /* Nominal mission for the multi-processing station */

        @MainMachineMetadata about multiProcessingStation;
        in multiProcessingStation
            : MultiProcessingStationSystem::MultiProcessingStation::MultiProcessingStationMachine;
        in zoneMPS : ZonesSystem::Zone;

        action processPayload : Process;
        action checkPayloadAtInput;

        entry multiProcessingStation.setup;
        then Idle;

        state Idle;
        accept when multiProcessingStation.sensor_MPS_in == true
            do send new AcquireRequestEventMessage(sender=multiProcessingStation) to zoneMPS
            then WaitForZone;

        state WaitForZone;
        accept AcquireResponseEventMessage via zoneMPS do checkPayloadAtInput then CheckInput;

        state CheckInput;
        accept PayloadAtInputEventMessage via multiProcessingStation do processPayload then ProcessCMD;
        accept OutputBusyEventMessage via multiProcessingStation then WaitForOutputFree;
        accept InputFreeEventMessage via multiProcessingStation
            do send new ReleaseRequestEventMessage() to zoneMPS
            then Idle;

        state WaitForOutputFree;
        accept when multiProcessingStation.sensor_MPS_out == false do checkPayloadAtInput then CheckInput;

        state ProcessCMD;
        accept MPSCommandSuccessEventMessage via multiProcessingStation
            do send new ReleaseRequestEventMessage() to zoneMPS
            then Idle;
    }
} // package MultiProcessingStationMissions
