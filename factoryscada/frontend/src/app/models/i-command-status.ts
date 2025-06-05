// Interface for CommandFeedback messages
export interface ICommandStatus {
  machineName: string;

  currentCommandRawJSON: string;

  currentCommandId: string;
  currentCommandName: string;
  currentCommandTimestamp: number;

  commandFeedbackRawJSON: string;
  commandFeedbackStatus: string;
  commandFeedbackTimestamp: number;
  commandFeedbackInfo: string;
}
