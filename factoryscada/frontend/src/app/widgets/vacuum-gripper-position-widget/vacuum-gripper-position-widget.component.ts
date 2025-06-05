import {Component, EventEmitter, Input, Output} from '@angular/core';
import {InputNumberModule} from "primeng/inputnumber";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";


@Component({
  selector: 'app-vacuum-gripper-position-widget',
  standalone: true,
  imports: [
    InputNumberModule,
    ReactiveFormsModule
  ],
  templateUrl: './vacuum-gripper-position-widget.component.html',
  styleUrl: './vacuum-gripper-position-widget.component.scss'
})
export class VacuumGripperPositionWidgetComponent {

  @Input()  title?: string;
  @Output() onValuesChanged = new EventEmitter<{ rotation: number, vertical: number, horizontal: number }>();


  parametersForm: FormGroup;

  constructor() {
    this.parametersForm = new FormGroup({
      rotationGroup: new FormGroup({ rotation: new FormControl<number>(0, Validators.required) }),
      verticalGroup : new FormGroup({ vertical: new FormControl<number>(0, Validators.required) }),
      horizontalGroup : new FormGroup({ horizontal: new FormControl<number>(0, Validators.required) })
    });
  }

  emitValues(){
    const values = {
      rotation: this.parametersForm.value.rotationGroup.rotation,
      vertical: this.parametersForm.value.verticalGroup.vertical,
      horizontal: this.parametersForm.value.horizontalGroup.horizontal
    };
    this.onValuesChanged.emit(values);
  }

}
