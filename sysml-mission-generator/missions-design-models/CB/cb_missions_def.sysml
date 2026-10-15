package ConveyorBeltMissions {
    private import Common::*;
    private import ConveyorBeltSystem::*;
    private import ConveyorBeltSystem::ConveyorBeltCommands::*;
    private import ConveyorBeltSystem::ConveyorBeltMessages::*;
    private import ZonesSystem::Zone;

    state def ConveyorBeltNominalMission {
        doc
        /* Nominal mission for the conveyor belt */

        @MainMachineMetadata about conveyorBelt;
        in conveyorBelt : ConveyorBeltSystem::ConveyorBelt::ConveyorBeltMachine;
        in vacuumGripper1 : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        in vacuumGripper2 : VacuumGripperSystem::VacuumGripper::VacuumGripperMachine;
        // the CB zone (ZoneMissionCBNominal) counts the tokens on the feed and the swap: told when a token moved
        in zoneCB : ZonesSystem::Zone;

        // reads the light barriers (active-low: false = token present): a token at the swap -> sends SwapBusy to
        // vacuumGripper2; else a token at the feed -> publishes TokenAtFeed on conveyorBelt
        action checkBelt;

        entry;
        then CheckBelt;

        // (Re)started with tokens already on the belt (e.g. after a repair): the light barrier edges that Idle
        // waits for already happened, so check them once.
        state CheckBelt;
        transition first CheckBelt do checkBelt then Idle;

        state Idle;
        // light barriers are active-low (false = token present): token at the feed and swap free
        accept when conveyorBelt.conveyorSensFeed == false and conveyorBelt.conveyorSensSwap == true
            do conveyorBelt.moveToSensor
            then MovingToSensor;
        accept TokenAtFeedEventMessage via conveyorBelt
            do conveyorBelt.moveToSensor
            then MovingToSensor;

        // The token left the feed and waits at the swap: the CB zone is told first (the feed is free for VGR1 again),
        // then VGR2, which asks the zone to pick it.
        state MovingToSensor;
        accept CBCommandSuccessEventMessage
            do action notifyTokenAtSwap {
                send new FeedFreeEventMessage() to zoneCB;
                send new FeedFreeEventMessage() to vacuumGripper1;
                send new SwapBusyEventMessage() to vacuumGripper2;
            }
            then Idle;
    }
} // package ConveyorBeltMissions
