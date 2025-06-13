import {Component, EventEmitter, Input, Output} from '@angular/core';
import {InputNumberModule} from "primeng/inputnumber";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";


@Component({
  selector: 'app-single-int-widget',
  standalone: true,
  imports: [
    InputNumberModule,
    ReactiveFormsModule
  ],
  templateUrl: './command-single-int-widget.component.html',
  styleUrl: './command-single-int-widget.component.scss'
})
export class CommandSingleIntWidgetComponent {

  @Input()  title?: string;
  @Output() onValuesChanged = new EventEmitter<{ integer: number }>();


  parametersForm: FormGroup;

  constructor() {
    this.parametersForm = new FormGroup({
      intGroup: new FormGroup({ integer: new FormControl<number>(0, Validators.required) })
    });
  }

  emitValues(){
    const value = {
      integer: this.parametersForm.value.rowGroup.row
    };
    this.onValuesChanged.emit(value);
  }

}
