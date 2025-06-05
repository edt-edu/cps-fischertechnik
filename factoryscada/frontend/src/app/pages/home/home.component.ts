import {Component} from '@angular/core';
import {MessageService} from 'primeng/api';
import {PanelModule} from "primeng/panel";
import {
  ConfigurationStatusViewComponent
} from "../../views/configuration-status-view/configuration-status-view.component";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    PanelModule,
    ConfigurationStatusViewComponent
  ],
  templateUrl: './home.component.html',
  providers: [MessageService],
  styleUrl: './home.component.scss'
})

export class HomeComponent  {

}
