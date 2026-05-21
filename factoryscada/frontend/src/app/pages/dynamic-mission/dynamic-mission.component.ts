import {Component, DestroyRef, inject, OnInit} from '@angular/core';
import {MyRxStompService} from "../../services/my-rx-stomp.service";
import {Message} from "@stomp/stompjs";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {IFactoryInstance} from "../../models/i-factory-instance";
import {IConfiguration, Machine} from "../../models/i-factory-configuration";
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {OverlayPanelModule} from 'primeng/overlaypanel';
import {PanelModule} from 'primeng/panel';
import {FieldsetModule} from 'primeng/fieldset';
import {AccordionModule} from 'primeng/accordion';
import {DropdownModule} from 'primeng/dropdown';
import {ScrollPanelModule} from 'primeng/scrollpanel';
import {MachineStatusWidgetComponent} from "../../widgets/machine-status-widget/machine-status-widget.component";
import {CommandStatusWidgetComponent} from "../../widgets/command-status-widget/command-status-widget.component";
import {LogTableWidgetComponent} from "../../widgets/log-table-widget/log-table-widget.component";
import {DynamicMission} from "../../models/i-factory-dynamic_missions";

@Component({
  selector: 'app-dynamic-mission',
  standalone: true,
  imports: [
    AccordionModule,
    ButtonModule,
    DropdownModule,
    FieldsetModule,
    FormsModule,
    InputTextModule,
    LogTableWidgetComponent,
    OverlayPanelModule,
    PanelModule,
    ReactiveFormsModule,
    ScrollPanelModule,
    MachineStatusWidgetComponent,
    CommandStatusWidgetComponent
  ],
  templateUrl: './dynamic-mission.component.html',
  styleUrls: ['./dynamic-mission.component.scss']
})
export class DynamicMissionComponent implements OnInit {
  selectedMission?: DynamicMission;
  configuration?: IConfiguration;
  instance?: IFactoryInstance;
  missions?: DynamicMission[];

  missionDescription: string = '';

  private readonly myRxStompService = inject(MyRxStompService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.subscribeToTopics();
    this.requestInitialData();
  }

  onMissionSelected(): void {
    if (!this.selectedMission) {
      return;
    }

    this.missionDescription = this.selectedMission?.description;
  }

  onStartMission(): void {
    if (!this.selectedMission) {
      return;
    }

    const destination = `/app/dynamic-mission/command/start/${encodeURIComponent(this.selectedMission.name)}`;
    this.myRxStompService.publish({destination, body: ""});
  }

  /**
   * Call backend to stop any running mission
   */
  onStopMission(): void {
    if (!this.selectedMission) {
      return;
    }

    const destination = `/app/dynamic-mission/command/stop`;
    this.myRxStompService.publish({destination, body: ""});
  }

  private requestInitialData(): void {
    this.myRxStompService.publish({destination: '/app/factory/configuration'});
    this.myRxStompService.publish({destination: '/app/factory/instance'});
    this.myRxStompService.publish({destination: '/app/dynamic-mission/missions'})
  }

  private subscribeToTopics(): void {
    this.subscribeToTopic('/topic/factory-instance', (message: Message) => {
      this.instance = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/factory-configuration', (message: Message) => {
      this.configuration = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/dynamic-missions', (message: Message)=> {
      this.missions = this.parseMessage(message);
    })
  }

  canStartMission(): boolean {
    return this.selectedMission != undefined;
  }

  getMissions(): DynamicMission[] {
    return this.missions ?? [];
  }

  getMachinesInMission(): Machine[] {
    if (!this.selectedMission) {
      return [];
    }

    return (this.configuration?.controllers ?? [])
      .flatMap(controller => controller.machines)
      .filter(machine => this.selectedMission?.involvedMachineNames?.includes(machine.name));
  }

  private subscribeToTopic(destination: string, callback: (message: Message) => void): void {
    this.myRxStompService.watch(destination)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(callback);
  }

  private parseMessage(message: Message): any {
    try {
      return JSON.parse(message.body);
    } catch (error) {
      console.error('Failed to parse message', error);
      return undefined;
    }
  }
}

