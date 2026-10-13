
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
}

package DoorControllerSystem {
    // -------------------------------------------------
    // Common imports
    // -------------------------------------------------
    private import ScalarValues::*;
    private import Common::*;
    private import Common::Messages::*;

    // -------------------------------------------------
    // Commands and actions
    // -------------------------------------------------
    package DoorCommands {
        enum def DoorCommandKind {
            OPEN;
            CLOSE;
            STOP;
        }

        action def OpenDoor {}
        action def CloseDoor {}
        action def StopDoor {}
    } // package DoorCommands

    // -------------------------------------------------
    // Door 
    // -------------------------------------------------
    package Door {
        private import DoorControllerSystem::DoorCommands::*;

        part def DoorMachine specializes Machine {
            // Available actions
            perform action openDoor  : OpenDoor;
            perform action closeDoor : CloseDoor;
            perform action stopDoor  : StopDoor;

            // Command received by the controller
            attribute currentCommand : DoorCommandKind;

            // Door position sensors
            attribute isFullyOpen   : Boolean;
            attribute isFullyClosed : Boolean;
        }
    } // package Door
} // package DoorControllerSystem


// ===================================================================
// Missions
// ===================================================================
package DoorControllerMissions {
    private import Common::*;
    private import DoorControllerSystem::*;
    private import DoorControllerSystem::DoorCommands::*;
    private import DoorControllerSystem::Door::*;

    /**
     * Simple mission for a motorized door.
     *
     * - Initially, the door is Closed.
     * - OPEN starts opening the door.
     * - CLOSE starts closing the door.
     * - Position sensors indicate when movement is complete.
     * - STOP terminates the mission.
     *
     * Assumption: the door is initially fully closed.
     */
    state def DoorMission {
        @MainMachineMetadata about door;
        in door : DoorControllerSystem::Door::DoorMachine;

        entry;
        then Closed;

        // -------------------------------------------------
        // Closed state
        // -------------------------------------------------
        state Closed;
            accept when door.currentCommand == DoorCommandKind::OPEN
                do door.openDoor
                then Opening;

            accept when door.currentCommand == DoorCommandKind::STOP
                do door.stopDoor
                then Stopped;

        // -------------------------------------------------
        // Opening state
        // -------------------------------------------------
        state Opening;
            accept when door.isFullyOpen
                then Open;

            accept when door.currentCommand == DoorCommandKind::STOP
                do door.stopDoor
                then Stopped;

        // -------------------------------------------------
        // Open state
        // -------------------------------------------------
        state Open;
            accept when door.currentCommand == DoorCommandKind::CLOSE
                do door.closeDoor
                then Closing;

            accept when door.currentCommand == DoorCommandKind::STOP
                do door.stopDoor
                then Stopped;

        // -------------------------------------------------
        // Closing state
        // -------------------------------------------------
        state Closing;
            accept when door.isFullyClosed
                then Closed;

            accept when door.currentCommand == DoorCommandKind::STOP
                do door.stopDoor
                then Stopped;

        // -------------------------------------------------
        // Final state
        // -------------------------------------------------
        state Stopped;
    }
} // package DoorControllerMissions
