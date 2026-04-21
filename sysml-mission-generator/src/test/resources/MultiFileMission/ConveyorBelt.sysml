package MultiFileMission_ConveyorBelt {

  private import ScalarValues::*;

    private import MultiFileMission_ConveyorBeltCommands::*;

    part def ConveyorBeltMachine {
      doc /* Definition of Conveyor Belt Machine */
      
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

    }
}