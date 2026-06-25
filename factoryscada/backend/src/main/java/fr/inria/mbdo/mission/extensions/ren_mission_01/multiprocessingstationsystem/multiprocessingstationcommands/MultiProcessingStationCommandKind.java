package fr.inria.mbdo.mission.extensions.ren_mission_01.multiprocessingstationsystem.multiprocessingstationcommands;

/**
 * From MultiProcessingStationSystem::MultiProcessingStationCommands::MultiProcessingStationCommandKind
 */
public enum MultiProcessingStationCommandKind {
  SETUP,

  STOP,

  PROCESS1,

  PROCESS,

  MOVE_TO_SAFE_POSITION,

  OVEN_LOAD,

  OVEN_UNLOAD,

  OVEN_HEAT,

  OVEN_PROCESS,

  ARM_MOVE,

  ARM_PICK,

  ARM_PLACE,

  TURNTABLE_ROTATE,

  TURNTABLE_EJECT,

  CONVEYOR_MOVE_TO_SENSOR,

  CONVEYOR_MOVE_OUT,

  SAW_CUT
}
