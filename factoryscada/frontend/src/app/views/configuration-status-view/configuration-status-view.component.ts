
import {Component, DestroyRef, inject, OnInit} from '@angular/core';
import {TreeNode} from "primeng/api";
import {TreeModule} from "primeng/tree";
import {MessageService} from 'primeng/api';
import {IConfiguration} from "../../models/i-factory-configuration";
import {Message} from "@stomp/stompjs";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import { MyRxStompService } from "../../services/my-rx-stomp.service";
import {IFactoryInstance, Controller, Controllers, Machine, Machines} from "../../models/i-factory-instance";

import {
  MachineStatusWidgetComponent
} from "../../widgets/machine-status-widget/machine-status-widget.component";
import {
  CommandStatusWidgetComponent
} from "../../widgets/command-status-widget/command-status-widget.component";
import {OrganizationChartModule} from "primeng/organizationchart";
import {PrimeTemplate} from "primeng/api";
import {FieldsetModule} from "primeng/fieldset";
import {
  PlcConnectionStatusWidgetComponent
} from "../../widgets/plc-connection-status-widget/plc-connection-status-widget.component";
import {Button} from "primeng/button";
import {OverlayPanelModule} from "primeng/overlaypanel";

@Component({
  selector: 'app-configuration-status-view',
  standalone: true,
  imports: [
    CommandStatusWidgetComponent,
    MachineStatusWidgetComponent,
    OrganizationChartModule,
    PrimeTemplate,
    TreeModule,
    FieldsetModule,
    PlcConnectionStatusWidgetComponent,
    Button,
    OverlayPanelModule
  ],
  templateUrl: './configuration-status-view.component.html',
  styleUrl: './configuration-status-view.component.scss'
})


// cf. https://stackblitz.com/edit/primeng-organizationchart-demo?file=src%2Fapp%2Fapp.component.ts

export class ConfigurationStatusViewComponent implements OnInit {


  private readonly myRxStompService = inject(MyRxStompService);
  private readonly destroyRef = inject(DestroyRef);

  dataTree: TreeNode[] = [{
    label: "Unknown configuration",
    expanded: true,
    type: 'configuration',
    styleClass: 'p-configuration'
  }];
  selectedNode?: TreeNode;

  configuration?: IConfiguration;// = JSON.parse("");
  instance?: IFactoryInstance;

  constructor(private messageService: MessageService) {
  }

  ngOnInit() {
    this.subscribeToTopics();
    this.requestInitialData();
  }

  private subscribeToTopics(): void {
    this.subscribeToTopic('/topic/factory-configuration', (message: Message) => {
      this.configuration = this.parseMessage(message);
      if (this.configuration) {
        this.dataTree = [this.convertToTreeNode(this.configuration)]
        console.log('data2='+JSON.stringify(this.dataTree))
      }
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
  private requestInitialData(): void {
    this.myRxStompService.publish({ destination: '/app/factory/configuration' });
    this.myRxStompService.publish({ destination: '/app/factory/instance' });
  }

  public isWSActive(): boolean {
    return this.myRxStompService.connected();
  }
  // Utility function to convert to TreeNode
  public convertToTreeNode(config: IConfiguration): TreeNode {
    return {
      label: config.name,
      expanded: true,
      type: 'configuration',
      styleClass: 'p-configuration',
      children: config.controllers.map(controller => ({
        label: controller.name,
        expanded: true,
        styleClass: 'p-plc',
        type: 'plc',
        data: controller,

      }))
    };
  }

  private parseMessage(message: Message): any {
    try {
      return JSON.parse(message.body);
    } catch (error) {
      console.error('Failed to parse message', error);
      return undefined;
    }
  }

  protected getConfigurationIcon(): string {
    if (this.myRxStompService.connected()) {
      return 'pi pi-arrow-right-arrow-left';
    } else {
      return 'pi pi-times';
    }
  }
  protected getConfigurationSeverity(): "success" | "info" | "warning" | "danger" | "help" | "primary" | "secondary" | "contrast" | null | undefined {
    if (this.myRxStompService.connected()) {
      return 'success';
    } else {
      return 'warning';
    }
  }
  protected getConfigurationInfo(): string {
    return ''+this.myRxStompService.myRxStompConfig.brokerURL
  }
}
