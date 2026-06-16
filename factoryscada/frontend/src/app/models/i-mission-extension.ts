export interface IMissionExtensionConfiguration {
    missions: IMissionExtension[];
    machines: IMissionExtensionMachine[];
    globalMissions: IGlobalMission[];
    activeGlobalMissionName?: string | null;
    activeMissionNames: string[];
}

export interface IMissionExtension {
    name: string;
    description: string;
    activeState?: string | null;
    involvedMachines: string[];
    dotGraph?: string | null;
}

export interface IMissionExtensionMachine {
    name: string;
    type: string;
    missions: IMissionExtensionOption[];
    activeMissionName?: string | null;
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
