import { IConfiguration, Machine } from "../models/i-factory-configuration";
import { ICommandPlaceholder } from "../models/i-command-placeholder";
import { IFactoryInstance } from "../models/i-factory-instance";
import { IRxStompPublishParams } from "@stomp/rx-stomp";
import { IFactoryParallelizedMissionsConfiguration, MissionParallelized, Nodes } from "../models/i-factory-paralelized_missions";

/**
 * Retrieves the list of machines from the given configuration.
 * @param config - The configuration object.
 * @returns An array of machines.
 */
export function getMachines(config: IConfiguration | undefined): Machine[] {
  if (config === undefined) return [];
  var machines: Machine[] = [];
  for (const controller of config.controllers) {
    if (controller.machines != null) {
      machines.push(...controller.machines);
    }
  }
  return machines;
}

/**
 * Retrieves the list of missions from the given mission configuration.
 * @param config - The mission configuration object.
 * @returns An array of missions.
 */
export function getMissions(config: IFactoryParallelizedMissionsConfiguration | undefined): MissionParallelized[] {
  if (config === undefined) return [];
  var missions: MissionParallelized[] = [];
  missions.push(...config.missions);
  return missions;
}

/**
 * Retrieves the list of machines from the given configuration that are used in the given mission.
 * @param config - The configuration object.
 * @param missionConfig - The mission configuration object.
 * @returns An array of machines.
 */
export function getMachinesInMission( config: IConfiguration | undefined,
                                      mission: MissionParallelized | undefined): Machine[] {
  console.log('CALLING getMachinesInMission '+config+mission);
  if (config === undefined || mission === undefined) return [];
  var machines: Machine[] = [];

  var missionMachineNames = new Set<string>();
  for(const node of mission.nodes) {
    if (node.placeholder != undefined){
      missionMachineNames.add(JSON.parse(node.placeholder).topicName);
    }
  }
  console.log(missionMachineNames);
  for (const controller of config.controllers) {
    for (const machine of controller.machines) {
      if(missionMachineNames.has(machine.name)) {
        machines.push(machine)
      }
    }
  }
  return machines;
}

/**
 * Retreives the list of missions that involves the given machine
 * @param machine - The machine we are looking for in the missions
 * @param config - The missions configuration object.
 * @returns An array of missions.
 */
export function getMissionInvolvingMachine(machine: Machine | undefined, config: IFactoryParallelizedMissionsConfiguration | undefined): MissionParallelized[] {
  if (config === undefined || machine === undefined) return [];
  var missions: MissionParallelized[] = [];
  for ( const mission of config.missions) {
    for ( const command of mission.nodes) {
      const commandPlaceHolder = JSON.parse(command.placeholder)
      if (commandPlaceHolder.topicName == machine.name) {
        if(!missions.includes(mission)) {
          missions.push(mission)
        }
      }
    }
  }
  return missions;
}

/**
 * Gets the command placeholder for a specific machine and command.
 * @param commands - The command placeholders object.
 * @param machineType - The type of the machine.
 * @param commandName - The name of the command.
 * @returns The command placeholder as a string.
 */
export function getCommandPlaceholder(commands: ICommandPlaceholder | undefined,
                                      machineType: string | undefined,
                                      commandName: string | undefined): string |undefined{
  // Check if parameters are defined
  if (commands === undefined || machineType === undefined || commandName === undefined) {
    return undefined;
  }
  // Access the commands for the specified machine
  const machineCommands = commands[machineType];
  // Return the command placeholder if the machine and command exist
  return machineCommands ? machineCommands[commandName] : undefined;
}

/**
 * Gets the mission command placeholder for a mission command qualified name.
 * @param missionsConfiguration - The missions configuration object.
 * @param commandName - The name of the command.
 * @returns The command placeholder as a string.
 */
export function getMissionCommandPlaceholder(missionsConfiguration: IFactoryParallelizedMissionsConfiguration | undefined,
  commandQualifierName: string | undefined): string | undefined{
  // Check if parameters are defined
  if (missionsConfiguration === undefined || commandQualifierName === undefined) {
    return undefined;
  }
  const qname = commandQualifierName.split('::');
  const mission = missionsConfiguration.missions.find(mission => mission.name === qname[0])
  const command = mission?.nodes.find(command => command.placeholder === qname[1] )
  // Return the command placeholder if the machine and command exist
  return command ? command.placeholder : undefined;
}

/**
 * Gets the mission command description for a mission command qualified name.
 * @param missionsConfiguration - The missions configuration object.
 * @param commandName - The name of the command.
 * @returns The command placeholder as a string.
 */
export function getMissionCommandDescription(missionsConfiguration: IFactoryParallelizedMissionsConfiguration | undefined,
  commandQualifierName: string | undefined): string {
  // Check if parameters are defined
  if (missionsConfiguration === undefined || commandQualifierName === undefined) {
    return '';
  }
  const qname = commandQualifierName.split('::');
  const mission = missionsConfiguration.missions.find(mission => mission.name === qname[0])
  const command = mission?.nodes.find(command => command.description === qname[1] )
  // Return the command placeholder if the machine and command exist
  return command ? command.description : '';
}

/**
 * Retrieves the command names for a specific machine instance.
 * @param instance - The factory instance.
 * @param machineName - The name of the machine.
 * @returns An array of command names.
 */
export function getCommandNames(instance: IFactoryInstance | undefined, machineName: string | undefined): string[] {
  // Check if instance and machineName are defined
  if (instance === undefined || machineName === undefined) {
    return [];
  }
  // Check if the machine exists in the instance
  const machine = instance.machines[machineName];
  return machine ? machine.commandNames : [];
}

/**
 * Retrieves the command names defined in the command-placeholder.yml for a specific machine instance.
 * @param instance - The factory instance.
 * @param machineName - The name of the machine.
 * @returns An array of command names.
 */
export function getRawCommandNames(instance: IFactoryInstance | undefined, machineName: string | undefined): string[] {
  // Check if instance and machineName are defined
  if (instance === undefined || machineName === undefined) {
    return [];
  }
  // Check if the machine exists in the instance
  const machine = instance.machines[machineName];
  return machine ? machine.rawCommandNames : [];
}


/**
 * Retrieves the mission command qualified names for a specific machine instance.
 * The qualified name for a is `${mission.name}::${command.name}`
 * @param instance - The factory instance.
 * @param machineName - The name of the machine.
 * @returns An array of command names.
 */
export function getMissionCommandQualifiedNames(missionsConfig: IFactoryParallelizedMissionsConfiguration | undefined, machineName: string | undefined): string[] {
  if (missionsConfig === undefined || machineName === undefined) return [];
  var commandNames: string[] = [];
  for ( const mission of missionsConfig.missions) {
    for ( const command of mission.nodes) {
      console.log(command.placeholder)
      if (command.placeholder != undefined){
        const commandPlaceHolder = JSON.parse(command.placeholder)
        if (commandPlaceHolder.topicName == machineName) {
          commandNames.push(`${mission.name}::${command.id}`)
        }
      }
    }
  }
  return commandNames;
}


export function getCommandNameFromJSONPlaceholder(placeholder : string ) : string {
  return JSON.parse(placeholder).message.name;
}
export function getDestinationSuffixFromJSONPlaceholder(placeholder : string | undefined ) : string {
  if (placeholder === undefined) return '';
  return '/command/'+getCommandNameFromJSONPlaceholder(placeholder).toLowerCase();
}

/**
 * Creates a command message object with the destination and body.
 *
 * This function generates an object containing the `destination` and `body` based on the provided `machine`,
 * `commandName`, and `commandBody`. If any of these parameters are `undefined`, it logs an error and returns `undefined`.
 *
 * @param {Machine | undefined} machine - The machine object containing `type` and `name` properties, or `undefined`.
 * @param {string | undefined} commandName - The name of the command to be included in the destination path, or `undefined`.
 * @param {string | undefined} commandBody - The body of the command, which will be parsed and stringified, or `undefined`.
 *
 * @returns {IRxStompPublishParams | undefined} An object containing `destination` and `body` if all parameters are valid, otherwise `undefined`.
 */
export function publishCommand(
  machine: Machine | undefined,
  commandName: string | undefined,
  commandBody: string | undefined
): IRxStompPublishParams | undefined {
  if (machine && commandName && commandBody) {
    const destination = `/app/${machine.type}/${machine.name}/command/${commandName}`;
    const body = JSON.stringify(JSON.parse(commandBody));

    return {destination, body};
  } else {
    console.error('Machine, commandName, or commandBody is undefined.');
    return undefined;
  }
}


/**
 * Converts a command name into a more readable format.
 * @param commandName - The name of the command.
 * @returns The humanized command name.
 */
export function humanizeCommandName(commandName: string): string {
  // Replace uppercase letters with space followed by the letter, then capitalize the first letter of each word
  return commandName
    .replace(/([A-Z])/g, ' $1') // Replace uppercase letters with spaces before them
    .trim() // Remove leading and trailing spaces
    .toLowerCase() // Convert all characters to lowercase
    .split(' ') // Split the text into words
    .map(word => word.charAt(0).toUpperCase() + word.slice(1)) // Capitalize the first letter of each word
    .join(' '); // Join the words back into a single string
}

/**
 * Beautifies a JSON string.
 * @param {string | any} jsonString - The JSON string to beautify.
 * @param {number} [indentation=2] - The number of spaces to use for indentation.
 * @returns {string} The beautified JSON string.
 */
export function beautifyJson(jsonString: string | any, indentation = 2): string {

  if (jsonString === undefined) return JSON.stringify(JSON.parse("{}"));

  try {
    // If the input is not a string, convert it to a JSON string
    if (typeof jsonString !== 'string') {
      jsonString = JSON.stringify(JSON.parse(jsonString));
    }
    // Parse the JSON string into an object
    const jsonObject = JSON.parse(jsonString);
    // Stringify the object with indentation
    return JSON.stringify(jsonObject, null, indentation);
  } catch (error) {
    // If there is an error in parsing or stringify, log the error and return the original string
    console.error('Invalid JSON string provided:', error);
    return jsonString;
  }
}

/**
 * Checks if a given string is a valid JSON.
 *
 * @param {string} str - The string to be checked.
 * @returns {boolean} Returns `true` if the string is a valid JSON, otherwise `false`.
 */
export function isValidJson(str: string | undefined): boolean {
  if (str === undefined)
    return false;
  try {
    JSON.parse(str);
    return true;
  } catch (e) {
    return false;
  }
}
