import {Component, EventEmitter, Input, Output} from '@angular/core';
import {InputNumberModule} from "primeng/inputnumber";
import {InputSwitchModule} from "primeng/inputswitch";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";


@Component({
  selector: 'app-indexedline-process-widget',
  standalone: true,
  imports: [
    InputNumberModule,
    ReactiveFormsModule,
    InputSwitchModule
  ],
  templateUrl: './indexedline-process-widget.component.html',
  styleUrl: './indexedline-process-widget.component.scss'
})
export class IndexedLineProcessWidgetComponent {

  @Input() title?: string;
  @Output() onValuesChanged = new EventEmitter<{ mill: number, drill: number, waitForPayload: boolean }>();

  parametersForm: FormGroup;

  constructor() {
    this.parametersForm = new FormGroup({
      millGroup: new FormGroup({mill: new FormControl<number>(0, Validators.required)}),
      drillGroup: new FormGroup({drill: new FormControl<number>(0, Validators.required)}),
      waitForPayloadGroup: new FormGroup({waitForPayload: new FormControl<boolean>(false, Validators.required)})
    });
  }

  emitValues() {
    const values = {
      mill: this.parametersForm.value.millGroup.mill,
      drill: this.parametersForm.value.drillGroup.drill,
      waitForPayload: this.parametersForm.value.waitForPayloadGroup.waitForPayload
    };
    this.onValuesChanged.emit(values);
  }

}
