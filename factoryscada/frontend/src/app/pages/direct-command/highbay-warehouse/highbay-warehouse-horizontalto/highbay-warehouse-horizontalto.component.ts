import { Component, inject, Input } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import {
    CommandSingleIntWidgetComponent
} from "../../../../widgets/command-single-int-widget/command-single-int-widget.component";
import {FieldsetModule} from "primeng/fieldset";
import {Button} from "primeng/button";

@Component({
  selector: 'app-highbay-warehouse-horizontal-to',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommandSingleIntWidgetComponent,
    FieldsetModule,
    Button
  ],
  templateUrl: './highbay-warehouse-horizontalto.component.html',
  styleUrl: './highbay-warehouse-horizontalto.component.scss'
})
export class HighBayWarehouseHorizontalToComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  endValues = { integer: 0 };

  constructor() {

  }
  handleValuesChanged(values: { integer: number }) {
    this.endValues = values;
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
                    "number": this.endValues.integer
                }
            }
        ]
      }
    };

    this.myRxStompService.publish({
      destination: `/app/${type}/${name}/command/horizontal_to`,
      body: JSON.stringify(payload) // Directly stringify the object
    });
  }
}
