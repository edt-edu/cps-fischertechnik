import { IFactoryParallelizedMissionsConfiguration, MissionParallelized, Nodes } from './i-factory-paralelized_missions';

export interface IBetterMissionsConfiguration extends IFactoryParallelizedMissionsConfiguration {
    missions: IBetterMission[];
    machines: IBetterMissionMachine[];
    globalMissions: IGlobalMission[];
    activeGlobalMissionName?: string | null;
    globalMissionMachineOverrides: { [key: string]: string };
}

export interface IBetterMission extends MissionParallelized {
    activeState?: string | null;
    nodes: IBetterMissionNode[];
}

export interface IBetterMissionNode extends Nodes {
    active?: boolean;
    placeholder: string;
    outputNodes: IBetterMissionNode[];
}

export interface IBetterMissionMachine {
    name: string;
    type: string;
    missions: IBetterMissionOption[];
    activeMissionName?: string | null;
}

export interface IBetterMissionOption {
    name: string;
    description: string;
}

export interface IGlobalMission {
    name: string;
    description: string;
    machineDefaultMissions: { [key: string]: string };
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
