// Interface of the factory missions 
export interface IFactoryMissionsConfiguration {
  name: string;
  missions: Mission[];
}

// Interface of a mission
export interface Mission {
  name: string;
  description: string;
  commands: Command[];
}

// Interface of a command
export interface Command {
  name: string;
  description: string;
  placeholder: string;
}
