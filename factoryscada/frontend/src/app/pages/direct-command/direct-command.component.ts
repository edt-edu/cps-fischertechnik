import { Component, DestroyRef, ElementRef, inject, OnInit, Renderer2, ViewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { SortingLineEjectComponent } from "./sorting-line/sorting-line-eject/sorting-line-eject.component";
import {
  VacuumGripperGoToPositionComponent
} from "./vacuum-gripper/vacuum-gripper-gotoposition/vacuum-gripper-gotoposition.component";
import { VacuumGripperMoveComponent } from "./vacuum-gripper/vacuum-gripper-move/vacuum-gripper-move.component";
import { VacuumGripperPickComponent } from "./vacuum-gripper/vacuum-gripper-pick/vacuum-gripper-pick.component";
import { VacuumGripperPlaceComponent } from "./vacuum-gripper/vacuum-gripper-place/vacuum-gripper-place.component";

import { HighBayWarehouseStoreComponent } from "./highbay-warehouse/highbay-warehouse-store/highbay-warehouse-store.component";
import { HighBayWarehouseRetrieveComponent } from "./highbay-warehouse/highbay-warehouse-retrieve/highbay-warehouse-retrieve.component";
import { HighBayWarehouseGoToColumnComponent } from "./highbay-warehouse/highbay-warehouse-gotocolumn/highbay-warehouse-gotocolumn.component";
import { HighBayWarehouseGoToRowComponent } from "./highbay-warehouse/highbay-warehouse-gotorow/highbay-warehouse-gotorow.component";
import { HighBayWarehouseHorizontalToComponent } from "./highbay-warehouse/highbay-warehouse-horizontalto/highbay-warehouse-horizontalto.component";
import { HighBayWarehouseVerticalToComponent } from "./highbay-warehouse/highbay-warehouse-verticalto/highbay-warehouse-verticalto.component";

import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Message } from "@stomp/stompjs";
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { MessagesModule } from 'primeng/messages';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import { ICommandPlaceholder } from "../../models/i-command-placeholder";
import { IConfiguration, Machine } from "../../models/i-factory-configuration";
import { IFactoryInstance } from "../../models/i-factory-instance";
import { MyRxStompService } from "../../services/my-rx-stomp.service";
import {
  beautifyJson,
  getDestinationSuffixFromJSONPlaceholder,
  getCommandNames,
  getCommandPlaceholder,
  getMachines,
  getMissionCommandDescription,
  getMissionCommandPlaceholder,
  getMissionCommandQualifiedNames,
  humanizeCommandName
} from "../../utilities/utils";
import { GenericDirectionCommandComponent } from "./generic/direction-command/generic-direction-command.component";
import { GenericDirectionNbStepsCommandComponent } from "./generic/direction-nb-steps-command/generic-direction-nb-steps-command.component";
import { GenericNoParamCommandComponent } from "./generic/no-param-command/generic-no-param-command.component";
import { GenericStatusCommandComponent } from "./generic/status-command/status-command.component";
import { VacuumGripperStatusComponent } from "./vacuum-gripper/vacuum-gripper-status/vacuum-gripper-status.component";
import { MachineStatusWidgetComponent } from "../../widgets/machine-status-widget/machine-status-widget.component";
import { CommandStatusWidgetComponent } from "../../widgets/command-status-widget/command-status-widget.component";
import { IFactoryMissionsConfiguration } from '../../models/i-factory-missions';

declare var $: any;

@Component({
  selector: 'app-direct-command',
  standalone: true,
  imports: [
    AccordionModule,
    ButtonModule,
    DropdownModule,
    FieldsetModule,
    FormsModule,
    GenericStatusCommandComponent,
    InputTextModule,
    MessagesModule,
    OverlayPanelModule,
    PanelModule,
    ReactiveFormsModule,
    ScrollPanelModule,
    VacuumGripperPlaceComponent,
    VacuumGripperPickComponent,
    VacuumGripperMoveComponent,
    VacuumGripperGoToPositionComponent,
    SortingLineEjectComponent,
    VacuumGripperStatusComponent,
    HighBayWarehouseStoreComponent,
    HighBayWarehouseRetrieveComponent,
    GenericNoParamCommandComponent,
    HighBayWarehouseGoToColumnComponent,
    HighBayWarehouseHorizontalToComponent,
    HighBayWarehouseVerticalToComponent,
    HighBayWarehouseGoToRowComponent,
    GenericDirectionCommandComponent,
    GenericDirectionNbStepsCommandComponent,
    CardModule,
    MachineStatusWidgetComponent,
    CommandStatusWidgetComponent
  ],
  templateUrl: './direct-command.component.html',
  styleUrl: './direct-command.component.scss'
})
export class DirectCommandComponent implements OnInit {

  @ViewChild('commandExecuteLog', { static: true }) commandExecuteLog!: ElementRef<HTMLDivElement>;

  selectedMachine?: Machine;
  selectedCommand?: string;
  selectedMissionCommand?: string;
  commandToSend?: string;

  placeholder?: ICommandPlaceholder;
  configuration?: IConfiguration;
  missionConfiguration?: IFactoryMissionsConfiguration;
  instance?: IFactoryInstance;

  protected readonly getMachines = getMachines;
  protected readonly humanizeCommandName = humanizeCommandName;
  protected readonly getCommandNames = getCommandNames;
  protected readonly getDestinationSuffixFromJSONPlaceholder = getDestinationSuffixFromJSONPlaceholder;
  protected readonly getMissionCommandDescription = getMissionCommandDescription;
  protected readonly getMissionCommandQualifiedNames = getMissionCommandQualifiedNames;

  private readonly myRxStompService = inject(MyRxStompService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly renderer = inject(Renderer2);

  ngOnInit(): void {
    this.subscribeToTopics();
    this.requestInitialData();
  }

  onCommandSelected(): void {
    try {
      const command = getCommandPlaceholder(
        this.placeholder,
        this.selectedMachine?.type,
        this.selectedCommand
      );
      if (command !== undefined) {
        this.commandToSend = beautifyJson(command, 4);
        // clear the mission command selection
        this.selectedMissionCommand =  undefined;
        return
      }
    } catch (e) {
      console.error('Error processing command', e);
    }
    this.commandToSend = undefined;
  }
  onMissionCommandSelected(): void {
    try {
      const command = getMissionCommandPlaceholder(
        this.missionConfiguration,
        this.selectedMissionCommand
      );
      console.info(`onMissionCommandSelected called command=${command}`);
      if (command !== undefined) {
        this.commandToSend = beautifyJson(command, 4);
        // clear the standard command selection
        this.selectedCommand =  undefined;
        return
      }
    } catch (e) {
      console.error('Error processing command', e);
    }
    this.commandToSend = undefined;
  }
  onMachineSelected(): void {
    this.selectedCommand = undefined;
    this.selectedMissionCommand = undefined;
  }

  private subscribeToTopics(): void {
    this.subscribeToTopic('/topic/command-placeholder', (message: Message) => {
      this.placeholder = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/factory-configuration', (message: Message) => {
      this.configuration = this.parseMessage(message);
    });
    this.subscribeToTopic('/topic/mission-configuration', (message: Message) => {
      this.missionConfiguration = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/factory-instance', (message: Message) => {
      this.instance = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/controller-feedbacks', (message: Message) => {
      this.addExecutionLog(message.body);
    });
  }

  getRequestStatusParameters(machineType :  string) :{name: string, code: string}[] {
    switch (machineType) {
      case "vacuumGripper": {
        return  [
          { name: 'REFERENCESWITCHVERTICALAXIS', code: 'REFERENCESWITCHVERTICALAXIS' },
          { name: 'REFERENCESWITCHHORIZONTALAXIS', code: 'REFERENCESWITCHHORIZONTALAXIS' },
          { name: 'REFERENCESWITCHROTATE', code: 'REFERENCESWITCHROTATE' },
          { name: 'VERTICALAXISSTEP', code: 'VERTICALAXISSTEP' },
          { name: 'HORIZONTALAXISSTEP', code: 'HORIZONTALAXISSTEP' },
          { name: 'ROTATESTEP', code: 'ROTATESTEP' },
          { name: 'MOTORVERTICALAXISUP', code: 'MOTORVERTICALAXISUP' },
          { name: 'MOTORVERTICALAXISDOWN', code: 'MOTORVERTICALAXISDOWN' },
          { name: 'MOTORHORIZONTALAXISBACKWARD', code: 'MOTORHORIZONTALAXISBACKWARD' },
          { name: 'MOTORHORIZONTALAXISFORWARD', code: 'MOTORHORIZONTALAXISFORWARD' },
          { name: 'MOTORROTATECLOCKWISE', code: 'MOTORROTATECLOCKWISE' },
          { name: 'MOTORROTATECOUNTERCLOCKWISE', code: 'MOTORROTATECOUNTERCLOCKWISE' },
          { name: 'COMPRESSOR', code: 'COMPRESSOR' },
          { name: 'VALVEVACUUM', code: 'VALVEVACUUM' }
        ];
      }
      default: {
        return [];
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

  private requestInitialData(): void {
    this.myRxStompService.publish({ destination: '/app/factory/placeholder' });
    this.myRxStompService.publish({ destination: '/app/factory/configuration' });
    this.myRxStompService.publish({ destination: '/app/factory/instance' });
    this.myRxStompService.publish({ destination: '/app/factoryMission/mission-configuration'});
  }


}
