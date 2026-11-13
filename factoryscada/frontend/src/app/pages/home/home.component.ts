import {Component} from '@angular/core';
import {MessageService} from 'primeng/api';
import {PanelModule} from "primeng/panel";
import {
  ConfigurationStatusViewComponent
} from "../../views/configuration-status-view/configuration-status-view.component";
import { Button } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { AccordionModule } from 'primeng/accordion';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    PanelModule,
    ConfigurationStatusViewComponent,
    Button,
    OverlayPanelModule,
    AccordionModule
],
  templateUrl: './home.component.html',
  providers: [MessageService],
  styleUrl: './home.component.scss'
})

export class HomeComponent  {

}
