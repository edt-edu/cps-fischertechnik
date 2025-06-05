import { Component, inject, Input } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { FormArray, FormGroup } from "@angular/forms";

@Component({
  selector: 'app-vacuum-gripper-status',
  standalone: true,
  imports: [],
  templateUrl: './vacuum-gripper-status.component.html',
  styleUrl: './vacuum-gripper-status.component.scss'
})
export class VacuumGripperStatusComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  parameterForm: FormGroup;

  constructor() {
    this.parameterForm = new FormGroup({
      parameters: new FormArray([])
    });
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
        parameters: this.parameterForm.value.parameters
      }
    };

    this.myRxStompService.publish({
      destination: `/app/${type}/${name}/command/status`,
      body: JSON.stringify(payload)
    });
  }

}
