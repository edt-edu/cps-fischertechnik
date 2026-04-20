package ZonesSystem {
    private import Common::Messages::*;
    private import Common::*;

    package ZonesMessages {
        item def AcquireRequestBufferedMessage specializes BufferedMessage {
            attribute sender : Machine;
        }

        item def AcquireResponseEventMessage specializes EventMessage;
        item def ReleaseRequestEventMessage specializes EventMessage;
    }

    state def Zone {
        private import ZonesMessages::*;

        entry;
        then IdleFree;

        state IdleFree;
        accept request : AcquireRequestBufferedMessage
            do send new AcquireResponseEventMessage() to request.sender
            then IdleBusy;

        state IdleBusy;
        accept request : ReleaseRequestEventMessage then IdleFree;
    }
}
