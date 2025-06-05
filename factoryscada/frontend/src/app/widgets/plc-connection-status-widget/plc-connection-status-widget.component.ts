import {Component, inject, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {Controller, Machine} from "../../models/i-factory-configuration";
import {Button} from "primeng/button";
import {OverlayPanelModule} from "primeng/overlaypanel";
import {MyRxStompService} from "../../services/my-rx-stomp.service";
import {IMachineStatus} from "../../models/i-machine-status";
import {Subscription} from "rxjs";
import {IPlcConnectionStatus} from "../../models/i-plc-connection-status";
import {Message} from "@stomp/stompjs";

@Component({
  selector: 'app-plc-connection-status-widget',
  standalone: true,
  imports: [
    Button,
    OverlayPanelModule
  ],
  templateUrl: './plc-connection-status-widget.component.html',
  styleUrl: './plc-connection-status-widget.component.scss'
})
export class PlcConnectionStatusWidgetComponent implements OnDestroy, OnChanges {
  @Input() plc!: Controller;

  private readonly myRxStompService = inject(MyRxStompService);

  plcConnectionStatus ?: IPlcConnectionStatus;

  connectionStatusTopicSubscription ?: Subscription;

  ngOnChanges(changes: SimpleChanges): void {

    this.connectionStatusTopicSubscription?.unsubscribe();
    this.subscribeToTopics();
    this.requestInitialData();
  }

  ngOnDestroy(): void {
    this.connectionStatusTopicSubscription?.unsubscribe();
  }

  private subscribeToTopics(): void {

    this.connectionStatusTopicSubscription = this.myRxStompService.watch(`/topic/${this.plc?.name}/plc-connection-status`).subscribe( (message: Message) => {
      try {
        this.plcConnectionStatus =  JSON.parse(message.body);
      } catch (error) {
        console.error('Failed to parse message', error);
      }
    });
  }
  private requestInitialData(): void {
    this.myRxStompService.publish({ destination: `/app/plc/${this.plc?.name}/plc-connection` });
  }

  /**
   * severity is used to change the color of the button depending of the connection status
   * @protected
   */
  protected getButtonSeverity(): "success" | "info" | "warning" | "danger" | "help" | "primary" | "secondary" | "contrast" | null | undefined {
    if(this.plcConnectionStatus?.commandPortConnected && this.plcConnectionStatus?.notificationPortConnected) {
      return 'success';
    } else {
      return 'warning';
    }
  }

  protected getIcon(): string {
    if(this.plcConnectionStatus?.commandPortConnected && this.plcConnectionStatus?.notificationPortConnected) {
      return 'pi pi-arrow-right-arrow-left';
    } else if(this.plcConnectionStatus?.commandPortConnected) {
      return 'pi pi-arrow-left';
    } else if(this.plcConnectionStatus?.notificationPortConnected) {
      return 'pi pi-arrow-right';
    } else {
      return 'pi pi-times'
    }
  }
}
