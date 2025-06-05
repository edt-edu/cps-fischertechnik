// Interface globale pour contenir toutes les commandes des machines
export interface ICommandPlaceholder {
  [machineType: string]: Commands;
}

// Interface pour les commandes de chaque type de machine
interface Commands {
  [commandName: string]: string;
}
