export interface IMissionExtensionConfiguration {
    missions: IMissionExtension[];
    machines: IMissionExtensionMachine[];
    globalMissions: IGlobalMission[];
    activeGlobalMissionName?: string | null;
    activeMissionNames: string[];
    configurationModified?: boolean;
    missionChanges?: IMissionChangeEvent[];
}

export interface IMissionChangeEvent {
    time: string;
    machine: string;
    previousMission?: string | null;
    previousState?: string | null;
    mission?: string | null;
    state?: string | null;
    origin: 'OPERATOR' | 'REPLANNING';
}

export interface IMissionExtension {
    name: string;
    description: string;
    activeState?: string | null;
    awaitingCommandResult?: boolean;
    involvedMachines: string[];
    dotGraph?: string | null;
}

export interface IMissionExtensionMachine {
    name: string;
    type: string;
    missions: IMissionExtensionOption[];
    activeMissionName?: string | null;
    activeState?: string | null;
    nominalMissionName?: string | null;
    lastChangeTime?: string | null;
}

export interface IMissionExtensionOption {
    name: string;
    description: string;
}

export interface IGlobalMission {
    name: string;
    description: string;
    missionNames: string[];
}

export interface IMissionCommandResponse {
    message: string;
    machineName?: string | null;
    missionName?: string | null;
    activeMissionName?: string | null;
}

export interface IMissionExecutionCommand {
    machineName: string;
    missionName: string;
}

export interface IGlobalMissionStartCommand {
    globalMissionName: string;
    machineOverrides: { [key: string]: string };
}

export interface IGlobalMissionMachineOverrideCommand {
    machineName: string;
    missionName: string;
}

export interface IMissionLogs {
    logsByMission: { [missionName: string]: string[] };
}
