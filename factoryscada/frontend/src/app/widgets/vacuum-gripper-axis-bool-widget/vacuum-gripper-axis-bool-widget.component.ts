import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CheckboxModule} from "primeng/checkbox";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";


@Component({
  selector: 'app-vacuum-gripper-axis-bool-widget',
  standalone: true,
  imports: [
    CheckboxModule,
    ReactiveFormsModule
  ],
  templateUrl: './vacuum-gripper-axis-bool-widget.component.html',
  styleUrl: './vacuum-gripper-axis-bool-widget.component.scss'
})
export class VacuumGripperAxisBoolWidgetComponent {

  @Input()  title?: string;
  @Output() onValuesChanged = new EventEmitter<{ rotationBool: boolean, verticalBool: boolean, horizontalBool: boolean }>();


  parametersForm: FormGroup;

  constructor() {
    this.parametersForm = new FormGroup({
      rotationGroup: new FormGroup({ rotationBool: new FormControl<boolean>(true, Validators.required) }),
      verticalGroup : new FormGroup({ verticalBool: new FormControl<boolean>(true, Validators.required) }),
      horizontalGroup : new FormGroup({ horizontalBool: new FormControl<boolean>(true, Validators.required) })
    });
  }

  emitValues(){
    const values = {
      rotationBool: this.parametersForm.value.rotationGroup.rotationBool,
      verticalBool: this.parametersForm.value.verticalGroup.verticalBool,
      horizontalBool: this.parametersForm.value.horizontalGroup.horizontalBool
    };
    this.onValuesChanged.emit(values);
  }

}
