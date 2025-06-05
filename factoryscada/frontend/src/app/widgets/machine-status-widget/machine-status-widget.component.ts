import {Component, DestroyRef, inject, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {MyRxStompService} from "../../services/my-rx-stomp.service";
import {Machine} from "../../models/i-factory-configuration";
import {Message} from "@stomp/stompjs";
import {IMachineStatus} from "../../models/i-machine-status";
import {Subscription} from "rxjs";
import {Button} from "primeng/button";
import {OverlayPanelModule} from "primeng/overlaypanel";

@Component({
  selector: 'app-machine-status-widget[machine]',
  standalone: true,
  imports: [
    Button,
    OverlayPanelModule
  ],
  templateUrl: './machine-status-widget.component.html',
  styleUrl: './machine-status-widget.component.scss'
})
export class MachineStatusWidgetComponent implements OnInit, OnDestroy, OnChanges {

  @Input() machine!: Machine;

  private readonly myRxStompService = inject(MyRxStompService);

  machineCommandStatus ?: IMachineStatus;

  topicSubscription ?: Subscription;

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {

    this.topicSubscription?.unsubscribe();
    this.subscribeToTopics();
    this.requestInitialData();
  }

  ngOnDestroy(): void {
    this.topicSubscription?.unsubscribe();
  }

  private subscribeToTopics(): void {
    const {name, type} = this.machine || {};

    this.subscribeToTopic(`/topic/${name}/machine-status`, (message: Message) => {
      this.machineCommandStatus = this.parseMessage(message);
    });
  }
  private requestInitialData(): void {
    const {name, type} = this.machine || {};
    this.myRxStompService.publish({ destination: `/app/${type}/${name}/machine-status` });
  }

  private subscribeToTopic(destination: string, callback: (message: Message) => void): void {
    this.topicSubscription = this.myRxStompService.watch(destination)
      //.pipe(takeUntilDestroyed(this.destroyRef))
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

  protected getFeedbackTimeStamp(): string {
    const t = this.machineCommandStatus?.machineFeedbackTimestamp;
    if (t === undefined) {
      return "";
    } else {
      return new Date(t * 1000).toLocaleTimeString()
    }
  }

  /**
   * severity is used to change the color of the button depending on the running status
   * @protected
   */
  protected getButtonSeverity(): "success" | "info" | "warning" | "danger" | "help" | "primary" | "secondary" | "contrast" | null | undefined {
    if (this.machineCommandStatus?.machineFeedbackStatus == "INITIALIZED_IDLE" || this.machineCommandStatus?.machineFeedbackStatus == "UNINITIALIZED_IDLE") {
      return 'secondary';
    } else if (this.machineCommandStatus?.machineFeedbackStatus === null) {
      return 'warning';
    } else {
      return 'primary';
    }
  }

  protected getIcon(): string {
    if (this.machineCommandStatus?.machineFeedbackStatus === null) {
      return 'pi pi-question-circle';
    } else if (this.machineCommandStatus?.machineFeedbackStatus == "UNINITIALIZED_IDLE") {
      return 'pi pi-ban';   //'pi pi-moon';
    } else if( this.machineCommandStatus?.machineFeedbackStatus == "UNINITIALIZED_ACTIVE") {
      return 'pi pi-spin pi-cog';
    } else if (this.machineCommandStatus?.machineFeedbackStatus == "INITIALIZED_IDLE") {
      return 'pi pi-moon';
    } else if( this.machineCommandStatus?.machineFeedbackStatus == "INITIALIZED_ACTIVE") {
      return 'pi pi-spin pi-spinner';
    } else {
      return 'pi pi-exclamation-triangle';
    }
  }

}
