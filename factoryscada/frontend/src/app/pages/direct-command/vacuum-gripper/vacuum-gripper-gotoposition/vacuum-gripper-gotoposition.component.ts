import { Component, inject, Input } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { ReactiveFormsModule } from "@angular/forms";
import {Button} from "primeng/button";
import {FieldsetModule} from "primeng/fieldset";
import {
    VacuumGripperPositionWidgetComponent
} from "../../../../widgets/vacuum-gripper-position-widget/vacuum-gripper-position-widget.component";

@Component({
  selector: 'app-vacuum-gripper-gotoposition',
  standalone: true,
    imports: [
        ReactiveFormsModule,
        Button,
        FieldsetModule,
        VacuumGripperPositionWidgetComponent
    ],
  templateUrl: './vacuum-gripper-gotoposition.component.html',
  styleUrl: './vacuum-gripper-gotoposition.component.scss'
})
export class VacuumGripperGoToPositionComponent {

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
      destination: `/app/${type}/${name}/command/go_to_position`,
      body: JSON.stringify(payload) // Directly stringify the object
    });
  }

}
