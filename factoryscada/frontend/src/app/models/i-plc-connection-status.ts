// Interface pout l'instance de la factory
export interface IPlcConnectionStatus {
  plcName: string;
  commandPortConnected: boolean;
  notificationPortConnected: boolean;
}
