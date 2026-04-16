package ConveyorBeltSystem {

  import ScalarValues::*;

  package ConveyorBeltCommands {

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
      in direction : DirectionKind;
    }

    action def MoveOut {
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

  package ConveyorBelt {

    private import ConveyorBeltSystem::ConveyorBeltCommands::*;

    part def ConveyorBeltMachine {

      perform action moveToSensor : MoveToSensor;
      perform action moveOut : MoveOut;
      perform action moveNbSteps : MoveNbSteps;
      perform action stop : Stop;
      perform action statusRequest : StatusRequest;

      attribute currentCommand : ConveyorCommandKind;
      attribute direction : DirectionKind;

      attribute currentStepCount : Integer;
      attribute targetStepCount : Integer;

      attribute conveyorSensFeed : Boolean;
      attribute conveyorSensSwap : Boolean;
      attribute conveyorSensImpulse : Integer;

    state cbMission: ConveyorBeltMission {
    }

    state def ConveyorBeltMission {

        state Idle;
        state MovingToSensor;
        state MovingOut;
        state MovingNbSteps;

        entry; then Idle;

        transition t_start_move_to_sensor
            first Idle
            accept currentCommand = ConveyorCommandKind::MOVE_TO_SENSOR
            then MovingToSensor;

        transition t_start_move_out
            first Idle
            accept currentCommand = ConveyorCommandKind::MOVE_OUT
            then MovingToSensor;

        transition t_start_move_nb_steps
            first Idle
            accept currentCommand = ConveyorCommandKind::MOVE_NB_STEPS
            then MovingNbSteps;

        transition t_sensor_reached_forward
            first MovingToSensor
            accept direction = DirectionKind::FORWARD
            and conveyorSensSwap = false
            and currentCommand = ConveyorCommandKind::MOVE_TO_SENSOR
            then Idle;

        transition t_sensor_reached_backward
            first MovingToSensor
            accept direction = DirectionKind::BACKWARD
            and conveyorSensFeed = false
            and currentCommand = ConveyorCommandKind::MOVE_TO_SENSOR
            then Idle;

        transition t_move_out_after_sensor_forward
            first MovingToSensor
            accept direction = DirectionKind::FORWARD
            and conveyorSensSwap = false
            and currentCommand = ConveyorCommandKind::MOVE_OUT
            then MovingOut;

        transition t_move_out_after_sensor_backward
            first MovingToSensor
            accept direction = DirectionKind::BACKWARD
            and conveyorSensFeed = false
            and currentCommand = ConveyorCommandKind::MOVE_OUT
            then MovingOut;

        transition t_nb_steps_reached
            first MovingNbSteps
            accept currentStepCount = targetStepCount
            then Idle;

        transition t_move_out_done
            first MovingOut
            accept currentCommand = ConveyorCommandKind::STOP
            then Idle;

        transition t_stop_from_sensor_move
            first MovingToSensor
            accept currentCommand = ConveyorCommandKind::STOP
            then Idle;

        transition t_stop_from_nb_steps
            first MovingNbSteps
            accept currentCommand = ConveyorCommandKind::STOP
            then Idle;
    }
      
    }
  }
}