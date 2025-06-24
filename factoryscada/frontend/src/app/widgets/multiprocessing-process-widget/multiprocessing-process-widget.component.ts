import {Component, EventEmitter, Input, Output} from '@angular/core';
import {InputNumberModule} from "primeng/inputnumber";
import { DropdownModule } from 'primeng/dropdown';
import { FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";


@Component({
  selector: 'app-multiprocessing-process-widget',
  standalone: true,
  imports: [
    InputNumberModule,
    ReactiveFormsModule,
    DropdownModule
  ],
  templateUrl: './multiprocessing-process-widget.component.html',
  styleUrl: './multiprocessing-process-widget.component.scss'
})
export class MultiProcessingProcessWidgetComponent {

  @Input()  title?: string;
  @Output() onValuesChanged = new EventEmitter<{ oven: number, saw: number, output: string }>();

  outputs = [
    {name: 'Oven', code: 'OVEN'},
    {name: 'Conveyor', code: 'CONVEYOR'}
  ];

  parametersForm: FormGroup;

  constructor() {
    this.parametersForm = new FormGroup({
      ovenGroup: new FormGroup({ oven: new FormControl<number>(0, Validators.required) }),
      sawGroup : new FormGroup({ saw: new FormControl<number>(0, Validators.required) }),
      outputGroup : new FormGroup({ output: new FormControl<string>("OVEN", Validators.required) })
    });
  }

  emitValues(){
    const values = {
      oven: this.parametersForm.value.ovenGroup.oven,
      saw: this.parametersForm.value.sawGroup.saw,
      output: this.parametersForm.value.outputGroup.output
    };
    this.onValuesChanged.emit(values);
  }

}
