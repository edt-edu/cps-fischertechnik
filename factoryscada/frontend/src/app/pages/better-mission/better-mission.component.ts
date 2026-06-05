import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { PanelModule } from 'primeng/panel';
import { TabViewModule } from 'primeng/tabview';
import {
    IBetterMission,
    IBetterMissionMachine,
    IBetterMissionsConfiguration,
    IGlobalMission,
    IGlobalMissionStartCommand,
    IMissionExecutionCommand,
} from '../../models/i-better-missions';
import { MyRxStompService } from '../../services/my-rx-stomp.service';

export interface MissionLogEntry {
    timestamp: Date;
    type: 'state' | 'event' | 'action' | 'info';
    message: string;
}

@Component({
    selector: 'app-better-mission',
    standalone: true,
    imports: [
        ButtonModule,
        CommonModule,
        DividerModule,
        DropdownModule,
        FormsModule,
        PanelModule,
        TabViewModule,
    ],
    templateUrl: './better-mission.component.html',
    styleUrl: './better-mission.component.scss',
})
export class BetterMissionComponent implements OnInit {
    // Global mission
    selectedGlobalMissionName?: string;
    selectedGlobalMission?: IGlobalMission;

    // Individual mission
    selectedIndividualMissionName?: string;
    selectedIndividualMachineName?: string;

    missionConfiguration?: IBetterMissionsConfiguration;

    // Logs per mission (keyed by mission name)
    private missionLogs: Map<string, MissionLogEntry[]> = new Map();
    private previousStates: Map<string, string | null> = new Map();
    private readonly MAX_LOGS = 50;

    private readonly rxStompService = inject(MyRxStompService);
    private readonly destroyRef = inject(DestroyRef);

    ngOnInit(): void {
        this.rxStompService
            .watch('/topic/better-missions/configuration')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((message) => {
                const body = message.body ? JSON.parse(message.body) : null;
                if (body) {
                    this.onConfigurationUpdate(body);
                }
            });

        this.rxStompService
            .watch('/topic/better-missions/command-response')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((message) => {
                const body = message.body ? JSON.parse(message.body) : null;
                if (body?.message) {
                    this.addLogFromCommandResponse(body);
                }
            });

        this.requestConfiguration();
    }

    // --- Getters ---

    get globalMissions(): IGlobalMission[] {
        return this.missionConfiguration?.globalMissions ?? [];
    }

    get machines(): IBetterMissionMachine[] {
        return this.missionConfiguration?.machines ?? [];
    }

    get allMissionOptions(): { name: string; description: string }[] {
        return (this.missionConfiguration?.missions ?? []).map(m => ({
            name: m.name,
            description: m.description,
        }));
    }

    get globalMissionEntries(): { mission: string }[] {
        if (!this.selectedGlobalMission?.missionNames) return [];
        return this.selectedGlobalMission.missionNames.map(mission => ({ mission }));
    }

    get isGlobalMissionActive(): boolean {
        return !!this.selectedGlobalMissionName &&
            this.selectedGlobalMissionName === this.missionConfiguration?.activeGlobalMissionName;
    }

    get selectedIndividualMissionDescription(): string {
        if (!this.selectedIndividualMissionName) return '';
        const mission = this.missionConfiguration?.missions.find(
            m => m.name === this.selectedIndividualMissionName
        );
        return mission?.description ?? '';
    }

    get compatibleMachines(): { name: string }[] {
        if (!this.selectedIndividualMissionName) return [];
        return (this.missionConfiguration?.machines ?? [])
            .filter(m => m.missions.some(opt => opt.name === this.selectedIndividualMissionName))
            .map(m => ({ name: m.name }));
    }

    get canStartIndividual(): boolean {
        return !!this.selectedIndividualMissionName && !!this.selectedIndividualMachineName;
    }

    get canStopIndividual(): boolean {
        if (!this.selectedIndividualMachineName) return false;
        const machine = this.machines.find(m => m.name === this.selectedIndividualMachineName);
        return !!machine?.activeMissionName;
    }

    get hasAnyRunningMission(): boolean {
        return this.machines.some(m => !!m.activeMissionName);
    }

    // --- Mission detail helpers ---

    getMissionActiveState(missionName: string): string | null {
        const mission = this.missionConfiguration?.missions.find(m => m.name === missionName);
        return mission?.activeState ?? null;
    }

    getMissionMachines(missionName: string): string[] {
        const mission = this.missionConfiguration?.missions.find(m => m.name === missionName);
        return mission?.involvedMachines ?? [];
    }

    isMissionCommandRunning(missionName: string): boolean {
        // A mission is "running a command" if it has an active state that isn't Idle
        const state = this.getMissionActiveState(missionName);
        return !!state && !state.toLowerCase().includes('idle');
    }

    getMissionLogs(missionName: string): MissionLogEntry[] {
        return this.missionLogs.get(missionName) ?? [];
    }

    // --- Actions ---

    onGlobalMissionSelected(): void {
        this.selectedGlobalMission = this.globalMissions.find(
            gm => gm.name === this.selectedGlobalMissionName
        );
    }

    onStartGlobalMission(): void {
        if (!this.selectedGlobalMissionName) return;

        // Clear logs on fresh start
        this.missionLogs.clear();
        this.previousStates.clear();

        const command: IGlobalMissionStartCommand = {
            globalMissionName: this.selectedGlobalMissionName,
            machineOverrides: {},
        };

        this.rxStompService.publish({
            destination: '/app/better-missions/global/start',
            body: JSON.stringify(command),
        });
    }

    onStopGlobalMission(): void {
        this.rxStompService.publish({
            destination: '/app/better-missions/global/stop',
            body: '',
        });
    }

    onIndividualMissionSelected(): void {
        this.selectedIndividualMachineName = undefined;
        const compatible = this.compatibleMachines;
        if (compatible.length === 1) {
            this.selectedIndividualMachineName = compatible[0].name;
        }
    }

    onStartIndividualMission(): void {
        if (!this.selectedIndividualMachineName || !this.selectedIndividualMissionName) return;

        const command: IMissionExecutionCommand = {
            machineName: this.selectedIndividualMachineName,
            missionName: this.selectedIndividualMissionName,
        };

        this.rxStompService.publish({
            destination: '/app/better-missions/start',
            body: JSON.stringify(command),
        });
    }

    onStopIndividualMission(): void {
        if (!this.selectedIndividualMachineName) return;

        const machine = this.machines.find(m => m.name === this.selectedIndividualMachineName);
        if (!machine?.activeMissionName) return;

        this.rxStompService.publish({
            destination: '/app/better-missions/stop',
            body: JSON.stringify({
                machineName: this.selectedIndividualMachineName,
                missionName: machine.activeMissionName,
            }),
        });
    }

    // --- Private ---

    private requestConfiguration(): void {
        this.rxStompService.publish({
            destination: '/app/better-missions/configuration',
            body: '',
        });
    }

    private onConfigurationUpdate(config: IBetterMissionsConfiguration): void {
        const previousConfig = this.missionConfiguration;
        this.missionConfiguration = config;
        this.selectedGlobalMissionName = config.activeGlobalMissionName || this.selectedGlobalMissionName;
        this.onGlobalMissionSelected();

        // Detect state changes and generate logs
        if (previousConfig && config.activeGlobalMissionName) {
            for (const mission of config.missions) {
                const prevMission = previousConfig.missions.find(m => m.name === mission.name);
                const prevState = this.previousStates.get(mission.name) ?? prevMission?.activeState ?? null;
                const newState = mission.activeState ?? null;

                if (prevState !== newState && newState) {
                    this.addLog(mission.name, 'state', `State → ${newState}`);
                    if (prevState) {
                        this.addLog(mission.name, 'event', `Transition from ${prevState}`);
                    }
                }

                this.previousStates.set(mission.name, newState);
            }
        }
    }

    private addLogFromCommandResponse(response: { message: string; machineName?: string; missionName?: string }): void {
        // Try to attribute to a specific mission
        const missionName = response.missionName;
        if (missionName) {
            this.addLog(missionName, 'action', response.message);
        } else if (response.message) {
            // Add to all active missions as info
            const activeMissions = this.globalMissionEntries.map(e => e.mission);
            for (const m of activeMissions) {
                this.addLog(m, 'info', response.message);
            }
        }
    }

    private addLog(missionName: string, type: MissionLogEntry['type'], message: string): void {
        if (!this.missionLogs.has(missionName)) {
            this.missionLogs.set(missionName, []);
        }
        const logs = this.missionLogs.get(missionName)!;
        logs.unshift({ timestamp: new Date(), type, message });
        if (logs.length > this.MAX_LOGS) {
            logs.length = this.MAX_LOGS;
        }
    }
}
