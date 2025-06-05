import { Component, inject, Input } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { FormArray, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ButtonModule } from 'primeng/button';
import { FieldsetModule } from 'primeng/fieldset';

@Component({
  selector: 'app-generic-no-param-command',
  standalone: true,
  imports: [
    ButtonModule,
    FieldsetModule,
    ReactiveFormsModule
  ],
  templateUrl: './generic-no-param-command.component.html',
  styleUrl: './generic-no-param-command.component.scss'
})
export class GenericNoParamCommandComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;
  @Input() title: String = "";
  @Input() description: String = "";
  @Input() destinationSuffix: String = "/command/debug";  // by default use the debug route

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
        outputId: "AUTO_ID"
      }
    };

    this.myRxStompService.publish({
      destination: `/app/${type}/${name}${this.destinationSuffix}`,
      body: JSON.stringify(payload)
    });
  }
}
