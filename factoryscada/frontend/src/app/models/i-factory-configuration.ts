// Interface pour la configuration principale
export interface IConfiguration {
  name: string;
  controllers: Controller[];
}

// Interface pour une machine
export interface Machine {
  name: string;
  type: string;
}

// Interface pour un contrôleur
export interface Controller {
  name: string;
  host: string;
  commandPort: number;
  notificationPort: number;
  machines: Machine[];
}
