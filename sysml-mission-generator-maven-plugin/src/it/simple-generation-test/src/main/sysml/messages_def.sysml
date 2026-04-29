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
