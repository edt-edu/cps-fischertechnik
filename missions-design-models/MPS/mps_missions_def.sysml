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

        action broadcastCompletion {
            doc
            /* This action is responsible for broadcasting the completion of the processing task to any interested parties. It can be used to trigger subsequent actions in other machines or to update the system state. */
            // In a real implementation, this might involve sending specific messages or updating shared state to indicate that the processing is complete.

            send new ReleaseRequestEventMessage() to zoneMPS;
            send new InputFreeEventMessage() to multiProcessingStation;
            send new OutputBusyEventMessage() to multiProcessingStation;
        }

        entry multiProcessingStation.setup;
        then Idle;

        state Idle;
        accept when
                multiProcessingStation.sensor_MPS_in == true and
                multiProcessingStation.sensor_MPS_out == false
            do send new AcquireRequestEventMessage(sender=multiProcessingStation) to zoneMPS
            then ZoneAcquired;

        state ZoneAcquired;
        accept AcquireResponseEventMessage do multiProcessingStation.process then ProcessCMD;

        state ProcessCMD;
        accept MPSCommandSuccessEventMessage do action broadcastCompletion then Idle;
    }
} // package MultiProcessingStationMissions
