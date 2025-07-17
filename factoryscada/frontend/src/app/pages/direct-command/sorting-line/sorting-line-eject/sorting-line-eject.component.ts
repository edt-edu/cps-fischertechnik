import {Component, inject, Input} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { FieldsetModule } from 'primeng/fieldset';
import { PanelModule } from 'primeng/panel';
import { Machine } from "../../../../models/i-factory-configuration";
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";

@Component({
  selector: 'app-sorting-line-eject',
  standalone: true,
  imports: [
    ButtonModule,
    DropdownModule,
    FieldsetModule,
    FormsModule,
    PanelModule,
    ReactiveFormsModule
  ],
  templateUrl: './sorting-line-eject.component.html',
  styleUrl: './sorting-line-eject.component.scss'
})
export class SortingLineEjectComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine?: Machine;
  @Input() placeholder: any;
  @Input() destinationSuffix: String = "/command/debug";  // by default use the debug route

  colors: any[] = [
    {name: "Red", value: "RED"},
    {name: "White", value: "WHITE"},
    {name: "Blue", value: "BLUE"},
    {name: "Auto", value: "AUTO"}
  ];

  selectedColor : any = this.colors[0];

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
          { "passableType":"COLOR",
            "passable":{
              "color": this.selectedColor.value
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
