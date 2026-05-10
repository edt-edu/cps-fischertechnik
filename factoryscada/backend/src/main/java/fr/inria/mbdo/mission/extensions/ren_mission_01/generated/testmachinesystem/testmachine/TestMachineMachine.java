package fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachine;

import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinecommands.TestCommandKind;
import fr.inria.mbdo.mission.runtime.api.MachineAdapter;

/**
 * From TestMachineSystem::TestMachine::TestMachineMachine
 */
public interface TestMachineMachine extends MachineAdapter {
    TestCommandKind getCurrentCommand();

    void setCurrentCommand(TestCommandKind currentCommand);

    void setCommandSuccess(TestCommandKind command);

    int getAttA();

    void setAttA(int attA);

    int getAttB();

    void setAttB(int attB);

    /**
     * From TestMachineSystem::TestMachine::TestMachineMachine::actionA
     */
    void actionA();

    /**
     * From TestMachineSystem::TestMachine::TestMachineMachine::actionB
     */
    void actionB();
}
