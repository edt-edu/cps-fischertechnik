// Interface of the factory missions paralelized
export interface IFactoryParallelizedMissionsConfiguration {
  name: string;
  missions: MissionParallelized[];
}

// Interface of a mission
export interface MissionParallelized {
  name: string;
  description: string;
  nodes: Nodes[];
}

// Interface of a command
export interface Nodes {
  id: string;
  description: string;
  outputs: string[];
  placeholder: string;
  outputNodes: Nodes[];
}
