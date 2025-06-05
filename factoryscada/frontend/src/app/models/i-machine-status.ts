// Interface for MachineFeedback messages
export interface IMachineStatus {
  machineName: string;
  machineFeedbackRawJSON: string;
  machineFeedbackStatus: string;
  machineFeedbackTimestamp: number;
  machineFeedbackInfo: string;
}
