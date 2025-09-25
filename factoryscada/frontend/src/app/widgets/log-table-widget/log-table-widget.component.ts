import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, Pipe, PipeTransform, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Message } from "@stomp/stompjs";
import { Table, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { MyRxStompService } from "../../services/my-rx-stomp.service";
import { JsonPrettyHtmlPipe } from '../../utilities/json-pretty.pipe'; 


export interface LogMessage {
  timestamp: Date;
  messageType: 'MACHINE_FEEDBACk' | 'COMMAND_FEEDBACK';
  machine: string;
  command?: string;
  status?: string;
  info?: string;
  rawJson?: string;
}


@Component({
  selector: 'app-log-table-widget',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    TooltipModule,
    JsonPrettyHtmlPipe
  ],
  templateUrl: './log-table-widget.component.html',
  styleUrls: ['./log-table-widget.component.scss']
})
export class LogTableWidgetComponent implements AfterViewInit {
  logs: LogMessage[] = [];

  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;
  @ViewChild('dt') private table!: Table;

  private readonly myRxStompService = inject(MyRxStompService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.subscribeToTopics();
    this.requestInitialData();
  }

  ngAfterViewInit() {
    this.scrollToBottom();
  }

  addLog(log: LogMessage) {
    this.logs = [...this.logs, log]; // PrimeNG change detection works best with immutability
    setTimeout(() => this.scrollToBottom());
  }

  private scrollToBottom() {
    if (this.scrollContainer) {
      const nativeEl = this.scrollContainer.nativeElement;
      nativeEl.scrollTop = nativeEl.scrollHeight;
    }
  }

  private parseLogsMessage(message: Message): LogMessage[]  {
    const messages: LogMessage[] = [];

    try {
      const rawBody = message.body;
      const lines = rawBody.trim().split('\n');

      for (const line of lines) {
        if (line.trim()) {
          const regex = /^((\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9})))\s*:\s*(\{.*\})$/;
          const match = line.match(regex);
          if (match) {
            const timestampStr = match[1]; // "2025-09-24T10:07:22.034777757"
            const date = new Date(timestampStr);
            const jsonStr = match[4];      // '{"topicName": ... }'

            const parsed = JSON.parse(jsonStr);
            const log: LogMessage = {
                  timestamp: date,
                  messageType: parsed.message.jsonType,
                  machine: parsed.topicName,
                  command: parsed.message.commandId ?? parsed.message.outputId ?? '',
                  status: parsed.message.status,
                  info: parsed.message.info ?? parsed.message.name + ' - ' + (parsed.message.parameters ? JSON.stringify(parsed.message.parameters) : ''),
                  rawJson: jsonStr
                };
             messages.push(log);
          } else {
            console.warn('Unrecognized log line format:', line);
          }
        }
      }
      return messages;
    } catch (e) {
      console.error('Failed to parse message', e);
      return messages;
    }
  }
  private subscribeToTopic(destination: string, callback: (message: Message) => void): void {
    this.myRxStompService.watch(destination)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(callback);
  }
  private subscribeToTopics(): void {

    this.subscribeToTopic('/topic/frontend-logs', (message: Message) => {
      this.logs = this.parseLogsMessage(message);
      setTimeout(() => this.scrollToBottom());
    });
  }
  private requestInitialData(): void {
    this.myRxStompService.publish({ destination: '/app/logs/request' });
  }

  messageTypeIcons: Record<string, { icon: string; color: string }> = {
    MACHINE_FEEDBACK: { icon: 'pi pi-cog', color: 'text-blue-500' },
    COMMAND_FEEDBACK: { icon: 'pi pi-reply', color: 'text-green-500' },
    COMMAND: { icon: 'pi pi-send', color: 'text-purple-500' },
  };

  getMessageTypeIcon(type: string): { icon: string; color: string } {
    return this.messageTypeIcons[type] || { icon: 'pi pi-question-circle', color: 'text-gray-500' };
  }
}

