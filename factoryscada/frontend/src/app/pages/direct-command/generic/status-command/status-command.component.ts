import {Component, inject, Input} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import {MyRxStompService} from "../../../../services/my-rx-stomp.service";
import {Machine} from "../../../../models/i-factory-configuration";
import { MultiSelectModule } from 'primeng/multiselect';
import {FieldsetModule} from "primeng/fieldset";
import {Button} from "primeng/button";

@Component({
  selector: 'app-generic-status-command',
  standalone: true,
  imports: [
    MultiSelectModule,
    ReactiveFormsModule,
    FieldsetModule,
    FormsModule,
    Button
  ],
  templateUrl: './status-command.component.html',
  styleUrl: './status-command.component.scss'
})
export class GenericStatusCommandComponent {


  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;
  @Input() requestStatusParameters: {name: string, code: string}[] = [];
  @Input() title: String = "";
  @Input() description: String = "";
  @Input() destinationSuffix: String = "/command/debug";  // by default use the debug route

  selectedStatusParameters: {name: string, code: string}[] = [];

  onExecute() {
    const {name, type} = this.machine || {};
    const payload = {
      ...JSON.parse(this.placeholder),
      topicName: name,
      timestamp: Date.now(),
      message: {
        ...JSON.parse(this.placeholder).message,
        outputId: "AUTO_ID",
        params: this.selectedStatusParameters.map(param => param.code)
      }
    };

    console.log(JSON.stringify(payload))
    this.myRxStompService.publish({
      destination: `/app/${type}/${name}${this.destinationSuffix}`,
      body: JSON.stringify(payload)
    });
  }
}
