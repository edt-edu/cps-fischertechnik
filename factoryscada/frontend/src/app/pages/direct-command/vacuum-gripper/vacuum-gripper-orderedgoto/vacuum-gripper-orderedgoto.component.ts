import { Component, inject, Input } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { ReactiveFormsModule } from "@angular/forms";
import {Button} from "primeng/button";
import {FieldsetModule} from "primeng/fieldset";
import {
    VacuumGripperPositionWidgetComponent
} from "../../../../widgets/vacuum-gripper-position-widget/vacuum-gripper-position-widget.component";
import {
    VacuumGripperAxisBoolWidgetComponent
} from "../../../../widgets/vacuum-gripper-axis-bool-widget/vacuum-gripper-axis-bool-widget.component";

@Component({
  selector: 'app-vacuum-gripper-orderedgoto',
  standalone: true,
    imports: [
        ReactiveFormsModule,
        Button,
        FieldsetModule,
        VacuumGripperPositionWidgetComponent,
        VacuumGripperAxisBoolWidgetComponent
    ],
  templateUrl: './vacuum-gripper-orderedgoto.component.html',
  styleUrl: './vacuum-gripper-orderedgoto.component.scss'
})
export class VacuumGripperOrderedgotoComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  endPositionValues = { rotation: 0, vertical: 0, horizontal: 0 };
  boolAxisValues = { rotationBool: true, verticalBool: true, horizontalBool: true };

  constructor() {
  }

  handleValuesChanged(values: { rotation: number, vertical: number, horizontal: number }) {
    this.endPositionValues = values;
  }
  handleBooleanValuesChanged(values: { rotationBool: boolean, verticalBool: boolean, horizontalBool: boolean }) {
    this.boolAxisValues = values;
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
            }},
          {"passableType":"AXISPRIORITIZED",
            "passable":{
              "vertical": this.boolAxisValues.verticalBool,
              "horizontal": this.boolAxisValues.horizontalBool,
              "rot": this.boolAxisValues.rotationBool
            }}
        ]
      }
    };

    this.myRxStompService.publish({
      destination: `/app/${type}/${name}/command/ordered_go_to`,
      body: JSON.stringify(payload) // Directly stringify the object
    });
  }

}
