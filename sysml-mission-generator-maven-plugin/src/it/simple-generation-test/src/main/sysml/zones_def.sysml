package ZonesSystem {
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

    part def Zone {}

    doc
    /* This is a simple mission definition for a system that manages access to a shared resource (e.g., a machine in a workshop) using acquire and release messages. The mission has two states: IdleFree, where the resource is available, and IdleBusy, where the resource is occupied.
     * The mission handles requests to acquire and release the resource, ensuring that only one requester can hold it at a time.
     */
    state def ZoneCBMission {
        private import ZonesMessages::*;
        entry;
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
