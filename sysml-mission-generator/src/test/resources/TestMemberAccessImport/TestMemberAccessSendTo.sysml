package Common {
    part def Machine;

    package Messages {
        item def BufferedMessage;
        item def EventMessage;
    }

    metadata def Workaround {
        doc /* This metadata indicates that the related element is part of a workaround for current limitation (usually in SysOn or in SysIDE).
             */
        attribute description : ScalarValues::String;
    }
}

package ZonesSystem {
    private import Common::Messages::*;
    private import Common::*;

    package ZonesMessages {
        item def AcquireRequestBufferedMessage specializes BufferedMessage {
            attribute sender : Machine;
        }

        item def AcquireResponseEventMessage specializes EventMessage;
        item def ReleaseRequestEventMessage specializes EventMessage;

        calc def GetMachine { 
            metadata Workaround{description="SysOn does not currently support using the member access syntax (request.sender) in the 'in' clause of an action or  in the 'send to' statement.";}
            doc /* This calculation extracts the sender machine from the AcquireRequestBufferedMessage. It is used as a workaround for current limitations in SysOn regarding direct access to message attributes in action inputs and send statements. */
            in value : ZonesMessages::AcquireRequestBufferedMessage; 
            return : Machine = value.sender;
        }
    }

    state def Zone {
        private import ZonesMessages::*;
        entry;
        then IdleFree;

        state IdleFree;
        accept request : AcquireRequestBufferedMessage
            do send new AcquireResponseEventMessage() to request.sender // fail on this line
            //do send new AcquireResponseEventMessage() to GetMachine(request)            
            then IdleBusy;

        state IdleBusy;
        accept request : ReleaseRequestEventMessage then IdleFree;
    }
}