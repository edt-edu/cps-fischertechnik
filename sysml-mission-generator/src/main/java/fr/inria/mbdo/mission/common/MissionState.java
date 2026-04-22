package fr.inria.mbdo.mission.common;

public interface MissionState {
    public void enter();
    public MissionState exit();
}
