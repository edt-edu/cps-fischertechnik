import { Component, DestroyRef, ElementRef, inject, OnInit, Renderer2, ViewChild } from '@angular/core';
import { MyRxStompService } from "../../services/my-rx-stomp.service";
import { Message } from "@stomp/stompjs";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { IFactoryInstance } from "../../models/i-factory-instance";
import { IConfiguration, Machine } from "../../models/i-factory-configuration";
import { ICommandPlaceholder } from "../../models/i-command-placeholder";
import { LogTableWidgetComponent } from "../../widgets/log-table-widget/log-table-widget.component";
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';
import { FieldsetModule } from 'primeng/fieldset';
import { AccordionModule } from 'primeng/accordion';
import { DropdownModule } from 'primeng/dropdown';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import {
  beautifyJson,
  getRawCommandNames,
  getCommandPlaceholder,
  getMachines,
  humanizeCommandName,
  isValidJson,
  publishCommand
} from "../../utilities/utils";

declare var $: any;

@Component({
  selector: 'app-debug-command',
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
    ScrollPanelModule
  ],
  templateUrl: './debug-command.component.html',
  styleUrls: ['./debug-command.component.scss']
})
export class DebugCommandComponent implements OnInit {
  @ViewChild('commandExecuteLog', { static: true }) commandExecuteLog!: ElementRef<HTMLDivElement>;

  selectedMachine?: Machine;
  selectedCommand?: string;
  commandToSend?: string;

  placeholder?: ICommandPlaceholder;
  configuration?: IConfiguration;
  instance?: IFactoryInstance;

  protected readonly getMachines = getMachines;
  protected readonly getCommandNames = getRawCommandNames;  // command names comes from the command-placeholder.yml
  protected readonly humanizeCommandName = humanizeCommandName;
  protected readonly getCommandPlaceholder = getCommandPlaceholder;
  protected readonly isValidJson = isValidJson;

  private readonly myRxStompService = inject(MyRxStompService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly renderer = inject(Renderer2);

  ngOnInit(): void {
    //this.initializeSemanticJS();
    this.subscribeToTopics();
    this.requestInitialData();
  }

  onMachineSelected(): void {
    this.selectedCommand = undefined;
  }

  onCommandSelected(): void {
    console.info("onCommandSelected()" + this.selectedCommand)
    try {
      const command = getCommandPlaceholder(
        this.placeholder,
        this.selectedMachine?.type,
        this.selectedCommand
      );
      if (command) {
        const { name, type } = this.selectedMachine || {};
        // fill the command with updated values (timestamp, machine name)
        const updatedcommand = {
          ...JSON.parse(command),
          topicName: name,
          timestamp: Date.now()
        };
        this.commandToSend = beautifyJson(JSON.stringify(updatedcommand), 4);
        return
      }
    } catch (e) {
      console.error('Error processing command', e);
    }
    this.commandToSend = undefined;
  }

  onCancel(): void {
    this.commandToSend = undefined;
    this.selectedCommand = undefined;
  }

  onExecute(): void {
    // <-- Implementation of command execution logic -->
    // we ignore the name of the command used as template and use the "debug" route
    let publishParams = publishCommand(this.selectedMachine, "debug", this.commandToSend);
    if (publishParams) {
      this.myRxStompService.publish(publishParams);
    } else {
      console.error('Error processing on execute command', publishParams);
    }
    // <-- Implementation of command execution logic -->
  }


  private requestInitialData(): void {
    this.myRxStompService.publish({ destination: '/app/factory/placeholder' });
    this.myRxStompService.publish({ destination: '/app/factory/configuration' });
    this.myRxStompService.publish({ destination: '/app/factory/instance' });
    this.myRxStompService.publish({ destination: '/app/logs/request' });
  }

  private subscribeToTopics(): void {
    this.subscribeToTopic('/topic/command-placeholder', (message: Message) => {
      this.placeholder = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/factory-configuration', (message: Message) => {
      this.configuration = this.parseMessage(message);
    });

    this.subscribeToTopic('/topic/factory-instance', (message: Message) => {
      this.instance = this.parseMessage(message);
    });
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
