package fr.inria.mbdo.mission.extensions.ren_mission_01.adapters;

import java.util.Objects;

import java.util.function.Consumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachine.TestMachineMachine;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinecommands.TestCommandKind;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.CommandSuccessEventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.TestMachineAttAAbove10EventMessage;
import fr.inria.mbdo.mission.extensions.ren_mission_01.generated.testmachinesystem.testmachinemessages.TestMachineCommandChanged;
import fr.inria.mbdo.mission.runtime.api.AbstractAdapter;
import fr.inria.mbdo.mission.runtime.rtc.event.Event;

/**
 * Simple adapter implementing TestMachineMachine used for local testing.
 * It publishes command changes and command completions to the provided event
 * bus.
 */
public final class TestMachineAdapter extends AbstractAdapter implements TestMachineMachine {

    private volatile TestCommandKind currentCommand;
    private volatile int attA;
    private volatile int attB;
    private static final Logger logger = LoggerFactory.getLogger(TestMachineAdapter.class);

    public TestMachineAdapter(String id) {
        super(id);
    }

    @Override
    public TestCommandKind getCurrentCommand() {
        return currentCommand;
    }

    @Override
    public void setCurrentCommand(TestCommandKind currentCommand) {
        this.currentCommand = currentCommand;
        logger.info("{}: setCurrentCommand -> {}", id, currentCommand);
        // TODO - send command
    }

    @Override
    public int getAttA() {
        return attA;
    }

    @Override
    public void setAttA(int attA) {
        int previousAttA = this.attA;
        logger.info("{}: setAttA -> {}", id, attA);
        this.attA = attA;
        if (attA != previousAttA && attA > 10) {
            publish(TestMachineAttAAbove10EventMessage.now(attA));
        }
    }

    @Override
    public void setCommandSuccess(TestCommandKind command) {
        logger.info("{}: manual acknowledge command success", id);
        this.setCurrentCommand(null);
        publish(CommandSuccessEventMessage.now());
    }

    @Override
    public int getAttB() {
        return attB;
    }

    @Override
    public void setAttB(int attB) {
        logger.info("{}: setAttB -> {}", id, attB);
        this.attB = attB;
    }

    @Override
    public void actionA() {
        logger.info("{}: actionA() called !", id);
        setCurrentCommand(TestCommandKind.A);
    }

    @Override
    public void actionB() {
        logger.info("{}: actionB() called !", id);
        setCurrentCommand(TestCommandKind.B);
    }

}
