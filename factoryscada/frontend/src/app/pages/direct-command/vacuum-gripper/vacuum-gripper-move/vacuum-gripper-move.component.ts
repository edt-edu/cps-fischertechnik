import { Component, inject, Input } from '@angular/core';
import { ReactiveFormsModule } from "@angular/forms";
import { Machine } from "../../../../models/i-factory-configuration";
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import {Button} from "primeng/button";
import {FieldsetModule} from "primeng/fieldset";
import {
  VacuumGripperPositionWidgetComponent
} from "../../../../widgets/vacuum-gripper-position-widget/vacuum-gripper-position-widget.component";

@Component({
  selector: 'app-vacuum-gripper-move',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    Button,
    FieldsetModule,
    VacuumGripperPositionWidgetComponent
  ],
  templateUrl: './vacuum-gripper-move.component.html',
  styleUrl: './vacuum-gripper-move.component.scss'
})
export class VacuumGripperMoveComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  startPositionValues = { rotation: 0, vertical: 0, horizontal: 0 };
  endPositionValues = { rotation: 0, vertical: 0, horizontal: 0 };


  constructor() {

  }

  handleStartValuesChanged(values: { rotation: number, vertical: number, horizontal: number }) {
    this.endPositionValues = values;
  }
  handleEndValuesChanged(values: { rotation: number, vertical: number, horizontal: number }) {
    this.endPositionValues = values;
  }
  onExecute() {
    const {name, type} = this.machine || {};

    // create a command object by prefilling it with command placeholder,  (placeholder is assigned by direct-command.commandToSend on selection of the command selector)
    // and then replace some of its attributes with values from the forms
    // for convenience, the forms name structure (ie. this.parameterForm.value.parameters) is similar to the expected "message.paramters" field
    // NOTE: a precise destination /app/${type}/${name}/command/move is quite useless and a generic one would be enough in this case
    // since the message contains all the command details; TODO: plan to  create /app/${type}/${name}/command/generic_command (to be used by debug view)
    // and really implement /move, /pick, /place, on the backend
    const payload = {
      ...JSON.parse(this.placeholder),
      topicName: name,
      timestamp: Date.now(),
      message: {
        ...JSON.parse(this.placeholder).message,
        outputId: "AUTO_ID",
        parameters: [
          {"passableType":"POSITIONPARAMETERTHREED",
            "passable":{
              "meaning": "START",
              "vertical": this.startPositionValues.vertical,
              "horizontal": this.startPositionValues.horizontal,
              "rot": this.startPositionValues.rotation
            }},
          {"passableType":"POSITIONPARAMETERTHREED",
            "passable":{
              "meaning": "END",
              "vertical": this.endPositionValues.vertical,
              "horizontal": this.endPositionValues.horizontal,
              "rot": this.endPositionValues.rotation
            }}
        ]
      }
    };

    // serialize and send
    this.myRxStompService.publish({
      destination: `/app/${type}/${name}/command/move`,
      body: JSON.stringify(payload) // Directly stringify the object
    });
  }


}
