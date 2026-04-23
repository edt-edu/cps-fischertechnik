import {Component, DestroyRef, ElementRef, inject, OnInit, ViewChild} from '@angular/core';
import {MyRxStompService} from "../../services/my-rx-stomp.service";
import {Message} from "@stomp/stompjs";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {IFactoryInstance} from "../../models/i-factory-instance";
import {
  IFactoryParallelizedMissionsConfiguration,
  MissionParallelized,
  Nodes
} from "../../models/i-factory-paralelized_missions";
import {IConfiguration} from "../../models/i-factory-configuration";
import {ICommandPlaceholder} from "../../models/i-command-placeholder";
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
import {getMachinesInMission, getMissions} from "../../utilities/utils";

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
  @ViewChild('commandExecuteLog', {static: true}) commandExecuteLog!: ElementRef<HTMLDivElement>;

  selectedMission?: MissionParallelized;
  placeholder?: ICommandPlaceholder;
  configuration?: IConfiguration;
  missionConfiguration?: IFactoryParallelizedMissionsConfiguration;
  instance?: IFactoryInstance;

  actualMissionsExecuted: Nodes[] = [];

  protected readonly getMissions = getMissions;
  protected readonly getMachinesInMission = getMachinesInMission;

  missionDescription: string = '';

  private readonly myRxStompService = inject(MyRxStompService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.subscribeToTopics();
    this.requestInitialData();
  }

  onMissionSelected(): void {
    if (this.selectedMission) {
      this.missionDescription = this.selectedMission?.description;
    }
  }

  onStartMission(): void {
    if (this.selectedMission) {
      const destination = `/app/factoryMission/command/start/${encodeURIComponent(this.selectedMission.name)}`;
      const body = "";
      let publishParams = {destination, body};
      if (publishParams) {
        this.myRxStompService.publish(publishParams);
      } else {
        console.error('Error processing stop mission', publishParams);
      }
    }
  }

  /**
   * Call backend to stop any runnning mission
   */
  onStopMission(): void {
    if (this.selectedMission) {
      const destination = `/app/factoryMission/command/stop`;
      const body = "";
      let publishParams = {destination, body};
      if (publishParams) {
        this.myRxStompService.publish(publishParams);
      } else {
        console.error('Error processing stop mission', publishParams);
      }
    }
  }

  private requestInitialData(): void {
    this.myRxStompService.publish({destination: '/app/factory/configuration'});
    this.myRxStompService.publish({destination: '/app/factory/instance'});
    this.myRxStompService.publish({destination: '/app/factoryMission/mission-configuration'});
    this.myRxStompService.publish({destination: '/app/factoryMission/actual-command-executing'});
  }

  private subscribeToTopics(): void {

    this.subscribeToTopic('/topic/factory-instance', (message: Message) => {
      this.instance = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/factory-configuration', (message: Message) => {
      this.configuration = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/mission-configuration', (message: Message) => {
      this.missionConfiguration = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/actual-command-executing', (message: Message) => {
      this.actualMissionsExecuted = this.parseMessage(message);
    });
  }

  canStartMission(): boolean {
    return this.selectedMission != undefined;
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

