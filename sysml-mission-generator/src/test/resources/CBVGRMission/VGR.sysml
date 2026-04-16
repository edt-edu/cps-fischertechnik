package REN_1V_01_VGRSystemModel {

  import ScalarValues::*;

  package VGRCommands {

    enum def VGRCommandKind {
      GO_TO_POSITION;
      MOVE;
      PICK;
      PLACE;
      SETUP;
      STATUS_REQUEST;
      GRIP;
      RELEASE;
      STOP;
      MOVE_TO_SAFE_POSITION;
      RETRACT_ARM;
    }

    enum def ExecutionStatusKind {
      MUST_CONTINUE;
      DONE;
    }

    attribute def Position3D {
      attribute vertical : Real;
      attribute horizontal : Real;
      attribute rot : Real;
    }

    action def GoToPosition {
      in targetPosition : Position3D;
    }

    action def Move {
      in startPosition : Position3D;
      in endPosition : Position3D;
    }

    action def Pick {
      in targetPosition : Position3D;
        doc /* Moves to the pickup area, activates gripping, and lifts the payload. */
    }

    action def Place {
      in targetPosition : Position3D;
        doc /* Moves to the drop-off area, releases the payload, and retreats. */
    }

    action def Setup {
      (0,0,0)
    }


    action def Grip {
      out attribute vacuumActValve : Boolean = true;
      out attribute vacuumActCompressorOn : Boolean = true;
    }

    action def Release {
      out attribute vacuumActValve : Boolean = false;
      out attribute vacuumActCompressorOn : Boolean = false;
    }

    action def Stop {
    }

    action def MoveToSafePosition {
    }

    action def RetractArm {
    }
  }

  package VGR {

    private import REN_1V_01_VGRSystemModel::VGRCommands::*;

    part def VacuumGripperSystem {

      perform action goToPosition : GoToPosition;
      perform action move : Move;
      perform action pick : Pick;
      perform action place : Place;
      perform action setup : Setup;
      perform action statusRequest : StatusRequest;
      perform action grip : Grip;
      perform action release : Release;
      perform action stop : Stop;
      perform action moveToSafePosition : MoveToSafePosition;
      perform action retractArm : RetractArm;

      attribute currentCommand : VGRCommandKind;
      attribute executionStatus : ExecutionStatusKind;

      attribute verticalEncoder : Real;
      attribute armEncoder : Real;
      attribute rotEncoder : Real;

      attribute expectedVerticalEncoderValue : Real;
      attribute expectedArmEncoderValue : Real;
      attribute expectedRotationEncoderValue : Real;

      attribute vacuumActCompressorOn : Boolean;
      attribute vacuumActValve : Boolean;

      state vgrobotMission: VGRMission {}
    

      state def VGRMission {

        state Idle;

        state Pick_HoverOverPayload;
        state Pick_PreparePickup;
        state Pick_GrabPayload;
        state Pick_LiftPayload;
        state Pick_Complete;

        state Place_HoverOverDropOff;
        state Place_PrepareDropOff;
        state Place_ReleasePayload;
        state Place_RetreatFromDropOff;
        state Place_Complete;

        entry; then Idle;

        transition Pick_t0
          first Idle
          accept currentCommand = VGRCommandKind::PICK
            and executionStatus = ExecutionStatusKind::MUST_CONTINUE
          then Pick_HoverOverPayload;

        transition Pick_t1
          first Pick_HoverOverPayload
          accept currentCommand = VGRCommandKind::PICK
            and verticalEncoder = expectedVerticalEncoderValue
            and armEncoder = expectedArmEncoderValue
            and rotEncoder = expectedRotationEncoderValue
          then Pick_PreparePickup;

        transition Pick_t2
          first Pick_PreparePickup
          accept currentCommand = VGRCommandKind::PICK
            and verticalEncoder = expectedVerticalEncoderValue
            and armEncoder = expectedArmEncoderValue
            and rotEncoder = expectedRotationEncoderValue
          then Pick_GrabPayload;

        transition Pick_t3
          first Pick_GrabPayload
          accept currentCommand = VGRCommandKind::PICK
            and vacuumActCompressorOn = true
            and vacuumActValve = true
          then Pick_LiftPayload;

        transition Pick_t4
          first Pick_LiftPayload
          accept currentCommand = VGRCommandKind::PICK
            and vacuumActCompressorOn = true
            and vacuumActValve = true
            and verticalEncoder = expectedVerticalEncoderValue
            and armEncoder = expectedArmEncoderValue
            and rotEncoder = expectedRotationEncoderValue
          then Pick_Complete;

        transition Pick_t5
          first Pick_Complete
          accept currentCommand = VGRCommandKind::PICK
            and executionStatus = ExecutionStatusKind::DONE
          then Idle;

        transition Place_t0
          first Idle
          accept currentCommand = VGRCommandKind::PLACE
            and executionStatus = ExecutionStatusKind::MUST_CONTINUE
          then Place_HoverOverDropOff;

        transition Place_t1
          first Place_HoverOverDropOff
          accept currentCommand = VGRCommandKind::PLACE
            and verticalEncoder = expectedVerticalEncoderValue
            and armEncoder = expectedArmEncoderValue
            and rotEncoder = expectedRotationEncoderValue
          then Place_PrepareDropOff;

        transition Place_t2
          first Place_PrepareDropOff
          accept currentCommand = VGRCommandKind::PLACE
            and verticalEncoder = expectedVerticalEncoderValue
            and armEncoder = expectedArmEncoderValue
            and rotEncoder = expectedRotationEncoderValue
          then Place_ReleasePayload;

        transition Place_t3
          first Place_ReleasePayload
          accept currentCommand = VGRCommandKind::PLACE
            and vacuumActCompressorOn = false
            and vacuumActValve = false
          then Place_RetreatFromDropOff;

        transition Place_t4
          first Place_RetreatFromDropOff
          accept currentCommand = VGRCommandKind::PLACE
            and verticalEncoder = expectedVerticalEncoderValue
            and armEncoder = expectedArmEncoderValue
            and rotEncoder = expectedRotationEncoderValue
          then Place_Complete;

        transition Place_t5
          first Place_Complete
          accept currentCommand = VGRCommandKind::PLACE
            and executionStatus = ExecutionStatusKind::DONE
          then Idle;
      }
    }
  }
}