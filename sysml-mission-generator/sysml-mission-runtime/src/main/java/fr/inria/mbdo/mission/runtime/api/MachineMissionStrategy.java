package fr.inria.mbdo.mission.runtime.api;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;

import java.util.List;

public interface MachineMissionStrategy {

    /**
     * name of the mission
     *
     * @return the name of the mission
     */
    String getName();

    /**
     * Returns the list of machines involved in the mission.
     *
     * @return the list of machines
     */
    List<MachineAdapter> getMachines();

    /**
     * start the mission
     */
    void start();

    /**
     * start the mission directly in the given state, without running its entry action: used to migrate a machine
     * from another mission once its state was reconciled with that state.
     *
     * @param stateName name of the state to start in
     * @throws IllegalArgumentException if the mission has no such state
     */
    void startAt(String stateName);

    /**
     * @return true between {@link #start()} (or {@link #startAt(String)}) and {@link #stop()}
     */
    boolean isRunning();

    /**
     * @return the names of the states of the mission, in discovery order from the initial state
     */
    List<String> getStateNames();

    /**
     * gracefully stop all the ongoing commands and stop the mission control
     */
    void stop();

    /**
     * stop the mission control (statemachine) without stopping the ongoing commands
     */
    void forceStop();

    /**
     * handle an incoming event. The mission control will dispatch the event to the
     * appropriate state and execute the corresponding actions.
     *
     * @param event the incoming event
     */
    void onEvent(Event event);

    /**
     * Returns the active state name.
     *
     * @return the name of the active state
     */
    String getActiveStateName();

    /**
     * @return the mission short description.
     */
    String getDescription();

    /**
     * Returns a graphviz représentation of the State Machine.
     *
     * @return the content of the .dot graph.
     */
    String getDotGraph();
}
