import {Component, EventEmitter, Input, Output} from '@angular/core';
import {InputNumberModule} from "primeng/inputnumber";
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";


@Component({
  selector: 'app-highbay-warehouse-position-widget',
  standalone: true,
  imports: [
    InputNumberModule,
    ReactiveFormsModule
  ],
  templateUrl: './highbay-warehouse-position-widget.component.html',
  styleUrl: './highbay-warehouse-position-widget.component.scss'
})
export class HighBayWarehousePositionWidgetComponent {

  @Input()  title?: string;
  @Output() onValuesChanged = new EventEmitter<{ row: number, column: number }>();


  parametersForm: FormGroup;

  constructor() {
    this.parametersForm = new FormGroup({
      rowGroup: new FormGroup({ row: new FormControl<number>(1, Validators.required) }),
      columnGroup : new FormGroup({ column: new FormControl<number>(1, Validators.required) })
    });
  }

  emitValues(){
    const values = {
      row: this.parametersForm.value.rowGroup.row,
      column: this.parametersForm.value.columnGroup.column
    };
    this.onValuesChanged.emit(values);
  }

}
