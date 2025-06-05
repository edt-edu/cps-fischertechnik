import { Component, inject, Input } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { FieldsetModule } from 'primeng/fieldset';
import { PanelModule } from 'primeng/panel';
import { Machine } from "../../../../models/i-factory-configuration";
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";

@Component({
  selector: 'app-generic-direction-command',
  standalone: true,
  imports: [
    ButtonModule,
    DropdownModule,
    FieldsetModule,
    FormsModule,
    PanelModule,
    ReactiveFormsModule
  ],
  templateUrl: './generic-direction-command.component.html',
  styleUrl: './generic-direction-command.component.scss'
})
export class GenericDirectionCommandComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;
  @Input() title: String = "";
  @Input() description: String = "";
  @Input() destinationSuffix: String = "/command/debug";  // by default use the debug route

  
  directions: any[] = [ 
      {name: "Forward", value: "FORWARD"},
      {name: "Bacward", value: "BACKWARD"}
  ];

  selectedDirection : any = this.directions[0];

  constructor() {

  }

  onExecute() {
    const { name, type } = this.machine || {};

    // use the template as a base msg
    const payload = {
      ...JSON.parse(this.placeholder),
      topicName: name,
      timestamp: Date.now(),
      message: {
        ...JSON.parse(this.placeholder).message,
        outputId: "AUTO_ID",
        parameters: [ // rewrite the paramters part with the values from the fields
          { "passableType":"DIRECTION",
            "passable":{
              "direction": this.selectedDirection.value
            }
          }
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
