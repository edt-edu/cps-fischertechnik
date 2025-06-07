// Interface  the instance of the factory
export interface IFactoryInstance {
  controllers: Controllers;
  machines: Machines;
}

// Interface for Protocol
export interface Protocol {
  hostname: string;
  sendPort: number;
  receivePort: number;
}

// Interface for Controller
export interface Controller {
  hostname: string;
  sendPort: number;
  receivePort: number;
}

// Interface for machine
export interface Machine {
  protocol: Protocol;
  commandNames: string[];     // list of command names defined by reflexivity in the backend
  rawCommandNames: string[];  // list of command names defined in the command-placeholder.yml

// Interface for group of Controllers 
export interface Controllers {
  [key: string]: Controller;
}

// Interface for group of machines
export interface Machines {
  [key: string]: Machine;
}
