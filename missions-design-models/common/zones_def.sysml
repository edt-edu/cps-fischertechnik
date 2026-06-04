package ZonesSystem {
    private import ScalarValues::*;
    private import Common::Messages::*;
    private import Common::*;

    package ZonesMessages {
        item def AcquireRequestEventMessage specializes EventMessage {
            attribute sender : Machine;
        }

        item def AcquireResponseEventMessage specializes EventMessage;
        item def ReleaseRequestEventMessage specializes EventMessage;

        calc def GetMachine {
            metadata Workaround {
                description =
                    "SysOn does not currently support using the member access syntax (request.sender) in the 'in' clause of an action or  in the 'send to' statement.";
            }
            doc
            /* This calculation extracts the sender machine from the AcquireRequestEventMessage. It is used as a workaround for current limitations in SysOn regarding direct access to message attributes in action inputs and send statements. */
            in value : ZonesMessages::AcquireRequestEventMessage;
            return : Machine = value.sender;
        }
    } // package ZonesMessages

    part def Zone {
        attribute isOccupied : Boolean = false;
    }

    doc
    /* Mission for managing zone acquirement on Conveyor Belt between VGR1 and VGR2 for nominal mission */
    state def ZoneMissionNominal {
        private import ZonesSystem::ZonesMessages::*;

        action Act1 {}
        action def Act2 {}

        entry action act1 : Act2;
        then IdleFree;

        state IdleFree;
        accept request : AcquireRequestEventMessage
            // do send new AcquireResponseEventMessage() to request.sender
            do send new AcquireResponseEventMessage() to GetMachine(request)
            then IdleBusy;

        state IdleBusy;
        accept request : ReleaseRequestEventMessage then IdleFree;
    }
} // package ZonesSystem
