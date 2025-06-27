import { Component, inject, Input, output } from '@angular/core';
import { MyRxStompService } from "../../../../services/my-rx-stomp.service";
import { Machine } from "../../../../models/i-factory-configuration";
import { ReactiveFormsModule } from "@angular/forms";
import {Button} from "primeng/button";
import {FieldsetModule} from "primeng/fieldset";
import {
    MultiProcessingProcessWidgetComponent
} from "../../../../widgets/multiprocessing-process-widget/multiprocessing-process-widget.component";

@Component({
  selector: 'app-multiprocessing-process',
  standalone: true,
    imports: [
        ReactiveFormsModule,
        Button,
        FieldsetModule,
        MultiProcessingProcessWidgetComponent
    ],
  templateUrl: './multiprocessing-process.component.html',
  styleUrl: './multiprocessing-process.component.scss'
})
export class MultiProcessingProcessComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  endPositionValues = { oven: 0, saw: 0, output: "OVEN" };

  constructor() {
  }

  handleValuesChanged(values: { oven: number, saw: number, output: string }) {
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
        parameters: [
          {
                "passableType": "NUMBERNATURAL",
                "passable": {
                    "number": this.endPositionValues.oven
                }
            },
            {
                "passableType": "NUMBERNATURAL",
                "passable": {
                    "number": this.endPositionValues.saw
                }
            },{
                "passableType": "MPSOUTPUT",
                "passable": {
                    "output": this.endPositionValues.output
                }
            }
        ]
      }
    };

    this.myRxStompService.publish({
      destination: `/app/${type}/${name}/command/process`,
      body: JSON.stringify(payload) // Directly stringify the object
    });
  }

}
