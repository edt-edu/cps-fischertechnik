import { Component, inject, Input } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { ReactiveFormsModule } from "@angular/forms";
import {
  VacuumGripperPositionWidgetComponent
} from "../../../../widgets/vacuum-gripper-position-widget/vacuum-gripper-position-widget.component";
import {FieldsetModule} from "primeng/fieldset";
import {Button} from "primeng/button";

@Component({
  selector: 'app-vacuum-gripper-pick',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    VacuumGripperPositionWidgetComponent,
    FieldsetModule,
    Button
  ],
  templateUrl: './vacuum-gripper-pick.component.html',
  styleUrl: './vacuum-gripper-pick.component.scss'
})
export class VacuumGripperPickComponent {
  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  startPositionValues = { rotation: 0, vertical: 0, horizontal: 0 };

  constructor() {

  }


  handleValuesChanged(values: { rotation: number, vertical: number, horizontal: number }) {
    this.startPositionValues = values;
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
              "meaning": "START",
              "vertical": this.startPositionValues.vertical,
              "horizontal": this.startPositionValues.horizontal,
              "rot": this.startPositionValues.rotation
          }}
        ]
      }
    };

    console.debug(JSON.stringify(payload))
    this.myRxStompService.publish({
      destination: `/app/${type}/${name}/command/pick`,
      body: JSON.stringify(payload)
    });
  }
}
