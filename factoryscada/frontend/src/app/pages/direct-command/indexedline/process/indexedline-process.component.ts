import {Component, inject, Input} from '@angular/core';
import {MyRxStompService} from "../../../../services/my-rx-stomp.service";
import {Machine} from "../../../../models/i-factory-configuration";
import {ReactiveFormsModule} from "@angular/forms";
import {FieldsetModule} from "primeng/fieldset";
import {Button} from "primeng/button";
import {
  IndexedLineProcessWidgetComponent
} from "../../../../widgets/indexedline-process-widget/indexedline-process-widget.component";

@Component({
  selector: 'app-indexedline-process',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FieldsetModule,
    Button,
    IndexedLineProcessWidgetComponent
  ],
  templateUrl: './indexedline-process.component.html',
  styleUrl: './indexedline-process.component.scss'
})
export class IndexedLineProcessComponent {

  myRxStompService = inject(MyRxStompService);

  @Input() machine: Machine | undefined;
  @Input() placeholder: any;

  endPositionValues = {mill: 0, drill: 0, waitForPayload: false};

  constructor() {
  }

  handleValuesChanged(values: { mill: number, drill: number, waitForPayload: boolean }) {
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
              "number": this.endPositionValues.mill
            }
          },
          {
            "passableType": "NUMBERNATURAL",
            "passable": {
              "number": this.endPositionValues.drill
            }
          }, {
            "passableType": "BOOL",
            "passable": {
              "bool": this.endPositionValues.waitForPayload
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

