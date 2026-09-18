package fr.inria.mbdo.mission.runtime.api;

public interface MissionState {
    public void enter();
    public MissionState exit();
}
