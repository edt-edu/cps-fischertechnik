// Interface pout l'instance de la factory
export interface IFactoryInstance {
  controllers: Controllers;
  machines: Machines;
}

// Interface pour le protocole
export interface Protocol {
  hostname: string;
  sendPort: number;
  receivePort: number;
}

// Interface pour un contrôleur
export interface Controller {
  hostname: string;
  sendPort: number;
  receivePort: number;
}

// Interface pour une machine
export interface Machine {
  protocol: Protocol;
  commandNames: string[];
}

// Interface pour l'ensemble des contrôleurs
export interface Controllers {
  [key: string]: Controller;
}

// Interface pour l'ensemble des machines
export interface Machines {
  [key: string]: Machine;
}
