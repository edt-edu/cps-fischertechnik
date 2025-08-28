import {Component, inject, Input} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { FieldsetModule } from 'primeng/fieldset';
import { PanelModule } from 'primeng/panel';
import { Machine } from "../../../../models/i-factory-configuration";
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";

@Component({
  selector: 'app-mps-turntable-position',
  standalone: true,
  imports: [
    ButtonModule,
    DropdownModule,
    FieldsetModule,
    FormsModule,
    PanelModule,
    ReactiveFormsModule
  ],
  templateUrl: './multiprocessing-turntable-position.component.html',
  styleUrl: './multiprocessing-turntable-position.component.scss'
})
export class MultiProcessingTurntablePositionComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine?: Machine;
  @Input() placeholder: any;
  @Input() destinationSuffix: String = "/command/debug";  // by default use the debug route

  positions: any[] = [
    {name: 'Arm', value: 'ARM'},
    {name: 'Saw', value: 'SAW'},
    {name: 'Conveyor', value: 'CONVEYOR'}
  ];

  selectedPosition : any = this.positions[0];

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
          { "passableType":"MPSTURNTABLEPOSITION",
            "passable":{
              "destination": this.selectedPosition.value
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
