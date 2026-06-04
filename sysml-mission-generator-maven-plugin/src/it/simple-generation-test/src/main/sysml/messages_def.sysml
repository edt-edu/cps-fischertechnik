package Common {
    part def Machine;

    package Messages {
        item def EventMessage;
    } // package Messages

    metadata def Workaround {
        doc
        /* This metadata indicates that the related element is part of a workaround for current limitation (usually in SysOn or in SysIDE). */
        attribute description : ScalarValues::String;
    }
} // package Common
