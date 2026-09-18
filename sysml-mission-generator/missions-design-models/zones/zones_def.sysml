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
    } // package ZonesMessages

    part def Zone {
        attribute isOccupied : Boolean = false;
    }
} // package ZonesSystem
