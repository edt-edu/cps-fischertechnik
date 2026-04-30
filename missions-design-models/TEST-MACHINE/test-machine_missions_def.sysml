package TestMachineMissions {
    private import TestMachineSystem::*;
    private import TestMachineSystem::TestMachineCommands::*;
    private import TestMachineSystem::TestMachineMessages::*;

    doc
    /* Mission Alpha drives command A and signals Mission Beta on success. */
    state def TestMissionAlpha {
        in testMachine1 : TestMachineSystem::TestMachine::TestMachineMachine;
        in testMachine2 : TestMachineSystem::TestMachine::TestMachineMachine;

        entry testMachine1.actionA;
        then Setup;

        state Setup;
        accept CommandSuccessEventMessage then Idle;

        state Idle;
        accept when testMachine1.attA > 10
            do action {
                send new EventMessage1() to testMachine2;
            }
            then WaitingForB;

        state WaitingForB;
        accept EventMessage2 then Idle;
    }

    doc
    /* Mission Beta reacts to Alpha's event and drives command B. */
    state def TestMissionBeta {
        in testMachine1 : TestMachineSystem::TestMachine::TestMachineMachine;
        in testMachine2 : TestMachineSystem::TestMachine::TestMachineMachine;

        action def testAction {
            doc
            /* This action represents the processing that Mission Beta performs upon receiving the event from Mission Alpha. In this simplified example, it does not have any internal logic, but in a real mission, it could include computations, state updates, or other operations before sending the command to the machine. */
            in tmp : TestMachineSystem::TestMachine::TestMachineMachine;
            perform tmp.actionB;
        }

        entry;
        then Standby;

        state Standby;
        accept EventMessage1 do testMachine1.actionB then WaitingForA;

        state WaitingForA;
        accept CommandSuccessEventMessage
            do action {
                send new EventMessage2() to testMachine2;
            }
            then Standby;
    }
}
