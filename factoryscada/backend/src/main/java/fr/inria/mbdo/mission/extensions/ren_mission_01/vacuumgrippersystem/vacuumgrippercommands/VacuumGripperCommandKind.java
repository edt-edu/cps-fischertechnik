package fr.inria.mbdo.mission.extensions.ren_mission_01.vacuumgrippersystem.vacuumgrippercommands;

/**
 * From VacuumGripperSystem::VacuumGripperCommands::VacuumGripperCommandKind
 */
public enum VacuumGripperCommandKind {
  GO_TO_POSITION,

  MOVE,

  PICK,

  PLACE,

  SETUP,

  STATUS_REQUEST,

  GRIP,

  RELEASE,

  STOP,

  MOVE_TO_SAFE_POSITION,

  RETRACT_ARM
}
