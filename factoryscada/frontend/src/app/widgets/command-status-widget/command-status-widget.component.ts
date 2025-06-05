import {Component, DestroyRef, inject, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {MyRxStompService} from "../../services/my-rx-stomp.service";
import {Machine} from "../../models/i-factory-configuration";
import {Message} from "@stomp/stompjs";
import {ICommandStatus} from "../../models/i-command-status";
import {Subscription} from "rxjs";
import {Button} from "primeng/button";
import {OverlayPanelModule} from "primeng/overlaypanel";

import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-command-status-widget[machine]',
  standalone: true,
  imports: [
    Button,
    OverlayPanelModule
  ],
  templateUrl: './command-status-widget.component.html',
  styleUrl: './command-status-widget.component.scss'
})
export class CommandStatusWidgetComponent implements OnInit, OnDestroy, OnChanges {

  @Input() machine!: Machine;

  private readonly myRxStompService = inject(MyRxStompService);

  machineCommandStatus ?: ICommandStatus;

  topicSubscription ?: Subscription;

  constructor(private sanitizer: DomSanitizer) {}

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

    this.subscribeToTopic(`/topic/${name}/command-status`, (message: Message) => {
      this.machineCommandStatus = this.parseMessage(message);
    });
  }
  private requestInitialData(): void {
    const {name, type} = this.machine || {};
    this.myRxStompService.publish({ destination: `/app/${type}/${name}/command-status` });
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

  protected getCommandInfoHeader(): string{
    const info = this.machineCommandStatus?.commandFeedbackInfo;
    if (info === undefined || info === null) {
      return "";
    } else {
      const newlineIndex = info.indexOf('\n');
      if (newlineIndex === -1) {
        return info; // No newline found, return the whole string
      } else {
        return info.substring(0, newlineIndex);
      }
    }
  }

  protected getCommandInfo(): SafeHtml{
    const info = this.machineCommandStatus?.commandFeedbackInfo;
    if (info === undefined || info === null) {
      return "";
    } else {
      const replacedHtml = info.replace(/\r\n|\r|\n/g, ' <br> ');
      return this.sanitizer.bypassSecurityTrustHtml(replacedHtml);
    }
  }

  protected getCommandTimeStamp(): string {
    const t = this.machineCommandStatus?.currentCommandTimestamp;
    if (t === undefined || t === null) {
      return "";
    } else {
      return new Date(t * 1000).toLocaleTimeString()
    }
  }
  protected getFeedbackTimeStamp(): string {
    const t = this.machineCommandStatus?.commandFeedbackTimestamp;
    if (t === undefined || t === null) {
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
    if (this.machineCommandStatus?.commandFeedbackStatus == "DONE") {
      return 'secondary';
    } else if (this.machineCommandStatus?.commandFeedbackStatus == "MUST_CONTINE") {
      return 'primary';
    } else if (this.machineCommandStatus?.commandFeedbackStatus == "INTERRUPTED") {
      return 'warning';
    } else if (this.machineCommandStatus?.commandFeedbackStatus == "ABORTED_TIMEOUT") {
      return 'warning';
    } else if (this.machineCommandStatus?.commandFeedbackStatus == "ABORTED_ERROR") {
      return 'warning';
    } else if (this.machineCommandStatus?.commandFeedbackStatus === null) {
      return 'warning';
    } else {
      return 'secondary';
    }
  }

  protected getIcon(): string {
    if (this.machineCommandStatus?.commandFeedbackStatus == "DONE") {
      return 'pi pi-moon';
    } else if (this.machineCommandStatus?.commandFeedbackStatus == "MUST_CONTINE") {
      return 'pi pi-spin pi-refresh';
    } else if (this.machineCommandStatus?.commandFeedbackStatus == "INTERRUPTED") {
      return 'pi pi-stop-circle';
    } else if (this.machineCommandStatus?.commandFeedbackStatus == "ABORTED_TIMEOUT") {
      return 'pi-history';
    } else if (this.machineCommandStatus?.commandFeedbackStatus == "ABORTED_ERROR") {
      return 'pi pi-times-circle';
    } else if (this.machineCommandStatus?.commandFeedbackStatus === null) {
      return 'pi pi-question-circle';
    } else {
      return 'pi pi-question-circle';
    }
  }

}
