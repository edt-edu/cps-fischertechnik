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

  logs: any[] = [];

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
    this.myRxStompService.publish({ destination: '/app/logs/request' });
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

      if (this.selectedMission != undefined) {
        this.missionsGraph = this.buildMermaidDiagramFromMission(this.selectedMission);
      }
    });

    this.subscribeToTopic('/topic/controller-feedbacks', (message: Message) => {
      //this.addExecutionLog(message.body);
    });

    this.subscribeToTopic('/topic/frontend-logs', (message: Message) => {
      this.logs = this.parseLogsMessage(message);

      const logElement = document.querySelector('.console-log') as HTMLElement;
      if (logElement) {
        logElement.textContent = this.logs.join('\n');
      }
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

  private parseLogsMessage(message: Message): any {
    const messages: any[] = [];

    try {
      console.log(message);
      const rawBody = message.body;
      const lines = rawBody.trim().split('\n');

      for (const line of lines) {
        if (line.trim()) {
          messages.push(line);
        }
      }
      return messages;
    } catch (e) {
      console.error('Failed to parse message', e);
      return undefined;
    }
  }

  private buildMermaidDiagramFromMission(mission: MissionParallelized): string {
    /*
    This function i used to build the mermaid graph for visualizing the mission currently running
    It build a list of lines wich are the mermaid code and then concatenate them
    */
    const lines: string[] = [];
    const sanitize = (id: string) => id.replace(/\s+/g, '_');

    //For a flow graph from Left to Right
    lines.push("flowchart LR");

    //Add each nodes except the Forks (waste of space)
    for (const node of mission.nodes) {
      if (!["Fork"].includes(node.type)) {
        lines.push(`${sanitize(node.id)}[${(node.description ?? node.id).trim().replace(/;/g, ":").replace(/[\[\]]/g, "")}]`);
      }
    }

    //Include the transitions
    for (const node of mission.nodes) {
      if (!["Fork"].includes(node.type)) {
        const outputs = this.getoutputsNodes(node);
        for (var output of outputs) {
          lines.push(`${sanitize(node.id)} --> ${sanitize(output)}`);
        }
      }
    }

    //Add the surveillance on currently visited nodes
    lines.push(`classDef surveillance fill:#ffcccc,stroke:#ff0000,stroke-width:2px;`)
    for (const node of this.actualMissionsExecuted) {
      lines.push(`class ${node.id} surveillance`)
    }

    //Return the graph
    return lines.join("\n");
  }

  private getoutputsNodes(node: Nodes): string[] {
    /*
    This function is used recursively to search for all the outputs of the node provided, excluding the forks
    :return: A list of string, which contains all id of output nodes
    */

    var ret: string[] = [];

    for (var output of node.outputNodes) {
      if (!["Fork"].includes(output.type)) {
        ret.push(output.id);
      } else {
        ret = ret.concat(this.getoutputsNodes(output));
      }
    }
    return ret;
  }

}

