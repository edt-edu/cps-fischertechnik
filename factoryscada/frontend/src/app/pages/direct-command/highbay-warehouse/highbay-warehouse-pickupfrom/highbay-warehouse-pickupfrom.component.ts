import { Component, inject, Input } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import {
    HighBayWarehousePositionWidgetComponent
} from "../../../../widgets/highbay-warehouse-position-widget/highbay-warehouse-position-widget.component";
import {FieldsetModule} from "primeng/fieldset";
import {Button} from "primeng/button";

@Component({
  selector: 'app-highbay-warehouse-pickupfrom',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    HighBayWarehousePositionWidgetComponent,
    FieldsetModule,
    Button
  ],
  templateUrl: './highbay-warehouse-pickupfrom.component.html',
  styleUrl: './highbay-warehouse-pickupfrom.component.scss'
})
export class HighBayWarehousePickupFromComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  endPositionValues = { row: 1, column: 1 };

  constructor() {

  }
  handleValuesChanged(values: { row: number, column: number }) {
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
        "parameters": [
            {
                "passableType": "NUMBERNATURAL",
                "passable": {
                    "number": this.endPositionValues.row
                }
            },
            {
                "passableType": "NUMBERNATURAL",
                "passable": {
                    "number": this.endPositionValues.column
                }
            }
        ]
      }
    };

    this.myRxStompService.publish({
      destination: `/app/${type}/${name}/command/pickup_from`,
      body: JSON.stringify(payload) // Directly stringify the object
    });
  }
}
