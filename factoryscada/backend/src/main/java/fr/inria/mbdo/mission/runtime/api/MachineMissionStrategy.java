package fr.inria.mbdo.mission.runtime.api;

import fr.inria.mbdo.mission.runtime.rtc.event.Event;

public interface MachineMissionStrategy {

    /**
     * name of the mission
     *
     * @return the name of the mission
     */
    String getName();

    /**
     * start the mission
     */
    void start();

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
}
