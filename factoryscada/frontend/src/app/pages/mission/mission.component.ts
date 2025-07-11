import { Component, DestroyRef, ElementRef, inject, OnInit, Renderer2, ViewChild } from '@angular/core';
import { MyRxStompService } from "../../services/my-rx-stomp.service";
import { Message } from "@stomp/stompjs";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { JsonPipe } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IFactoryInstance } from "../../models/i-factory-instance";
import { IFactoryMissionsConfiguration, Mission } from "../../models/i-factory-missions";
import { IFactoryParallelizedMissionsConfiguration, MissionParallelized, Nodes } from "../../models/i-factory-paralelized_missions";
import { IConfiguration, Machine } from "../../models/i-factory-configuration";
import { ICommandPlaceholder } from "../../models/i-command-placeholder";
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';
import { FieldsetModule } from 'primeng/fieldset';
import { AccordionModule } from 'primeng/accordion';
import { DropdownModule } from 'primeng/dropdown';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import {
  MachineStatusWidgetComponent
} from "../../widgets/machine-status-widget/machine-status-widget.component";
import {
  CommandStatusWidgetComponent
} from "../../widgets/command-status-widget/command-status-widget.component";
import {
  GraphMissionsView
} from "../../widgets/parallelized-missions-graph-widget/parallelized-missions-graph-widget.component";
import {
  getMissions,
  getMachinesInMission
} from "../../utilities/utils";

declare var $: any;

@Component({
  selector: 'app-mission',
  standalone: true,
  imports: [
    AccordionModule,
    ButtonModule,
    DropdownModule,
    FieldsetModule,
    FormsModule,
    InputTextModule,
    JsonPipe,
    OverlayPanelModule,
    PanelModule,
    ReactiveFormsModule,
    ScrollPanelModule,
    MachineStatusWidgetComponent,
    CommandStatusWidgetComponent,
    GraphMissionsView
  ],
  templateUrl: './mission.component.html',
  styleUrls: ['./mission.component.scss']
})
export class MissionComponent implements OnInit {
  @ViewChild('commandExecuteLog', { static: true }) commandExecuteLog!: ElementRef<HTMLDivElement>;

  selectedMission?: MissionParallelized;
  selectedMachine?: string;
  commandToSend?: string;

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
  private readonly renderer = inject(Renderer2);

  missionsGraph = `flowchart LR\n  Empty[No mission selected]`;

  ngOnInit(): void {
    //this.initializeSemanticJS();
    this.subscribeToTopics();
    this.requestInitialData();
  }

  onMissionSelected(): void {
    if (this.selectedMission) {
      this.missionDescription = this.selectedMission?.description;
      this.missionsGraph = this.buildMermaidDiagramFromMission(this.selectedMission);
    }
  }

  onStartMission(): void {
    if (this.selectedMission) {
      const destination = `/app/factoryMission/command/start/${encodeURIComponent(this.selectedMission.name)}`;
      const body = "";
      let publishParams = { destination, body };
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
      let publishParams = { destination, body };
      if (publishParams) {
        this.myRxStompService.publish(publishParams);
      } else {
        console.error('Error processing stop mission', publishParams);
      }
    }
  }

  private addExecutionLog(message: string): void {
    const div = this.renderer.createElement('div');
    this.renderer.addClass(div, 'item');
    const text = this.renderer.createText(`[${new Date().toLocaleString()}] > ${message}`);
    this.renderer.appendChild(div, text);
    this.renderer.appendChild(this.commandExecuteLog.nativeElement, div);
  }


  private requestInitialData(): void {
    this.myRxStompService.publish({ destination: '/app/factory/configuration' });
    this.myRxStompService.publish({ destination: '/app/factory/instance' });
    this.myRxStompService.publish({ destination: '/app/factoryMission/mission-configuration' });
    this.myRxStompService.publish({ destination: '/app/factoryMission/actual-command-executing' });
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
      // Save scroll position
      const el = this.commandExecuteLog.nativeElement;
      const scrollPos = el.scrollTop;

      // Update the data (replace or append)
      this.actualMissionsExecuted = this.parseMessage(message);

      // Optionally update your DOM / graph as you do:
      if (this.selectedMission != undefined) {
        this.missionsGraph = this.buildMermaidDiagramFromMission(this.selectedMission);
      }

      // Restore scroll position
      setTimeout(() => {
        el.scrollTop = scrollPos;
      });
    });

    this.subscribeToTopic('/topic/controller-feedbacks', (message: Message) => {
      this.addExecutionLog(message.body);
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

  private buildMermaidDiagramFromMission(mission: MissionParallelized): string {
    const lines: string[] = [];
    const sanitize = (id: string) => id.replace(/\s+/g, '_');

    lines.push("flowchart TD");

    for (const node of mission.nodes) {
      lines.push(`${sanitize(node.id)}[${(node.description ?? node.id).trim().replace(/;/g, ":").replace(/[\[\]]/g, "")}]`);
    }

    for (const node of mission.nodes) {
      for (const output of node.outputs ?? []) {
        lines.push(`${sanitize(node.id)} --> ${sanitize(output)}`);
      }
    }

    lines.push(`classDef surveillance fill:#ffcccc,stroke:#ff0000,stroke-width:2px;`)
    for (const node of this.actualMissionsExecuted) {
      lines.push(`class ${node.id} surveillance`)
    }

    console.log(lines.join("\n"));
    return lines.join("\n");
  }

}

