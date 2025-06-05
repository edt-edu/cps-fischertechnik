import { Component, inject, Input } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import {
    VacuumGripperPositionWidgetComponent
} from "../../../../widgets/vacuum-gripper-position-widget/vacuum-gripper-position-widget.component";
import {FieldsetModule} from "primeng/fieldset";
import {Button} from "primeng/button";

@Component({
  selector: 'app-vacuum-gripper-place',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    VacuumGripperPositionWidgetComponent,
    FieldsetModule,
    Button
  ],
  templateUrl: './vacuum-gripper-place.component.html',
  styleUrl: './vacuum-gripper-place.component.scss'
})
export class VacuumGripperPlaceComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  endPositionValues = { rotation: 0, vertical: 0, horizontal: 0 };

  constructor() {

  }
  handleValuesChanged(values: { rotation: number, vertical: number, horizontal: number }) {
    this.endPositionValues = values;
  }
  onExecute() {
    const {name, type} = this.machine || {};
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
              "meaning": "END",
              "vertical": this.endPositionValues.vertical,
              "horizontal": this.endPositionValues.horizontal,
              "rot": this.endPositionValues.rotation
            }}
        ]
      }
    };

    this.myRxStompService.publish({
      destination: `/app/${type}/${name}/command/place`,
      body: JSON.stringify(payload) // Directly stringify the object
    });
  }
}
