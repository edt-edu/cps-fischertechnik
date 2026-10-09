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

        // Oven and saw times and output of the process: see the mission actions implementation.
        action processPayload : Process;

        entry multiProcessingStation.setup;
        then Idle;

        // A payload at the input is only processed when the output is free: the MPS takes its zone (shared with VGR2,
        // which places the payloads at the input) and keeps it until the payload is delivered at the output, so that no
        // new payload can be placed meanwhile. The next one then waits at the input until the output is cleared.
        state Idle;
        accept when
                multiProcessingStation.sensor_MPS_in == true and
                multiProcessingStation.sensor_MPS_out == false
            do send new AcquireRequestEventMessage(sender=multiProcessingStation) to zoneMPS
            then WaitForZone;

        state WaitForZone;
        accept AcquireResponseEventMessage via zoneMPS do processPayload then ProcessCMD;

        // Payload delivered at the output: release the zone.
        state ProcessCMD;
        accept MPSCommandSuccessEventMessage via multiProcessingStation
            do send new ReleaseRequestEventMessage() to zoneMPS
            then Idle;
    }
} // package MultiProcessingStationMissions
