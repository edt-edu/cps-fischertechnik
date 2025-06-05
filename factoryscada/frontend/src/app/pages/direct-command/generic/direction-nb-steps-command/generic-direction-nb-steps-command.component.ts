import { Component, inject, Input } from '@angular/core';
import { Machine } from "../../../../models/i-factory-configuration";
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";

import { FormArray, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from "@angular/forms";
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { FieldsetModule } from 'primeng/fieldset';
import { InputNumberModule } from 'primeng/inputnumber';

@Component({
  selector: 'app-generic-direction-nb-steps-command',
  standalone: true,
  imports: [
    ButtonModule,
    DropdownModule,
    FieldsetModule,
    FormsModule,
    InputNumberModule,
    ReactiveFormsModule
  ],
  templateUrl: './generic-direction-nb-steps-command.component.html',
  styleUrl: './generic-direction-nb-steps-command.component.scss'
})
export class GenericDirectionNbStepsCommandComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;
  @Input() title: String = "";
  @Input() description: String = "";
  @Input() destinationSuffix: String = "/command/debug";  // by default use the debug route

  parametersForm: FormGroup;


  //nbStepsForm: FormGroup;

  directions: any[] = [ 
    {name: "Forward", value: "FORWARD"},
    {name: "Bacward", value: "BACKWARD"}
  ];

  //selectedDirection : any = this.directions[0];

  //nbSteps : number = 10;

  constructor() {
    this.parametersForm = new FormGroup({
      directionGroup: new FormGroup({ direction: new FormControl(this.directions[0], Validators.required) }) ,
      stepsGroup : new FormGroup({ steps: new FormControl<number>(15, Validators.required) })
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
        parameters: [
          {"passableType":"NUMBERNATURAL", "passable":{"number": this.parametersForm.value.stepsGroup.steps}},
          {"passableType":"DIRECTION", "passable": {"direction":this.parametersForm.value.directionGroup.direction.value}}
        ]
      }
    };

    console.log(JSON.stringify(payload))
    this.myRxStompService.publish({
      destination: `/app/${type}/${name}${this.destinationSuffix}`,
      body: JSON.stringify(payload)
    });
  }
}
