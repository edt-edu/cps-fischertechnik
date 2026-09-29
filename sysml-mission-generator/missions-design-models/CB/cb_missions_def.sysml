package ConveyorBeltMissions {
    private import Common::*;
    private import ConveyorBeltSystem::*;
    private import ConveyorBeltSystem::ConveyorBeltCommands::*;
    private import ConveyorBeltSystem::ConveyorBeltMessages::*;

    state def ConveyorBeltNominalMission {
        doc
        /* Nominal mission for the conveyor belt */

        @MainMachineMetadata about conveyorBelt;
        in conveyorBelt : ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine;
        in vacuumGripper1 : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in vacuumGripper2 : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;

        entry;
        then Idle;

        state Idle;
        // light barriers are active-low (false = token present): token at the feed and swap free
        accept when conveyorBelt.conveyorSensFeed == false and conveyorBelt.conveyorSensSwap == true
            do conveyorBelt.moveToSensor
            then MovingToSensor;

        state MovingToSensor;
        accept CBCommandSuccessEventMessage
            do action notifyVgr1AndVgr2 {
                send new FeedFreeEventMessage() to vacuumGripper1;
                send new SwapBusyEventMessage() to vacuumGripper2;
            }
            then Idle;
    }
} // package ConveyorBeltMissions
