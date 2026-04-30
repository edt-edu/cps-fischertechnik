package io.github.mbdo.factoryscada.mission.extension;


/**
 * discovery interface provided by actual Mission to the spring application 
 */
public interface MissionSpringExtension {

	/**
	 * name of the mission
	 * @return
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
}
