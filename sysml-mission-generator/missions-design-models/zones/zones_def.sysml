package ZonesSystem {
    private import ScalarValues::*;
    private import Common::Messages::*;
    private import Common::*;

    package ZonesMessages {
        item def AcquireRequestEventMessage :> EventMessage {
            ref sender : Machine;
        }

        item def AcquireResponseEventMessage :> EventMessage;
        item def ReleaseRequestEventMessage :> EventMessage;

        // Dead conveyor belt (ZoneMissionDeadCB): the belt doesn't move, its feed and swap positions are two slots
        // shared by the grippers (VacuumGripper1CBFailureMission, VacuumGripper2CBFailureMission). The zone knows which
        // slots hold a token, in which order: the grippers ask it, on the zone, where to place or pick, and tell it
        // when they are done (out of the belt again).

        // VGR1 holds a token: where to place it? Answered by PlaceOnFeed, PlaceOnSwap or NoCBSlotFree.
        item def CBSlotRequestEventMessage :> EventMessage;
        item def PlaceOnFeedEventMessage :> EventMessage;
        item def PlaceOnSwapEventMessage :> EventMessage;
        item def NoCBSlotFreeEventMessage :> EventMessage;
        // VGR1 placed its token and retracted its arm
        item def TokenPlacedOnCBEventMessage :> EventMessage;
        // to VGR1, after NoCBSlotFree: a slot may be free now, ask again
        item def CBSlotFreeEventMessage :> EventMessage;

        // VGR2 is free: where to pick a token? Answered by PickFromFeed, PickFromSwap or NoTokenOnCB.
        item def CBTokenRequestEventMessage :> EventMessage;
        item def PickFromFeedEventMessage :> EventMessage;
        item def PickFromSwapEventMessage :> EventMessage;
        item def NoTokenOnCBEventMessage :> EventMessage;
        // VGR2 picked its token and retracted its arm
        item def TokenPickedFromCBEventMessage :> EventMessage;
        // to VGR2, after NoTokenOnCB: a token may wait on the belt now, ask again
        item def TokenOnCBEventMessage :> EventMessage;
    } // package ZonesMessages

    part def Zone {
        attribute isOccupied : Boolean = false;
    }
} // package ZonesSystem
