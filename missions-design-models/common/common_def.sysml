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

    metadata def MainMachineMetadata {
        doc
        /* This metadata indicates that the related machine is the main machine of a mission. It is used to determine the entry point of the mission when multiple machines are involved. */
    }

    metadata def MissionConfig {
        doc
        /* This metadata indicates that the related machine is the main machine of a mission. It is used to determine the entry point of the mission when multiple machines are involved. */
        attribute id : ScalarValues::String;
    }
} // package Common
