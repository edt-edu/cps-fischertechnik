package MultiFileMission_ConveyorBeltCommands {

  private import ScalarValues::*;
  enum def ConveyorCommandKind {
    MOVE_TO_SENSOR;
    MOVE_OUT;
    MOVE_NB_STEPS;
    STOP;
    STATUS_REQUEST;
  }

  enum def DirectionKind {
    FORWARD;
    BACKWARD;
  }


  action def MoveToSensor {
    doc /* Moves the conveyor in the given direction until the conveyed object reaches the sensor */
    in direction : DirectionKind;
  }

  action def MoveOut {
    doc /* Moves the conveyor in the given direction until the conveyed object reaches the sensor and is ejected of the Conveyor belt*/
    in direction : DirectionKind;
  }

  action def MoveNbSteps {
    in steps : Integer;
    in direction : DirectionKind;
  }

  action def Stop {
  }

  action def StatusRequest {
  }
}

  
