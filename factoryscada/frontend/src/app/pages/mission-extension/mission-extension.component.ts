import { Component, DestroyRef, ElementRef, OnInit, QueryList, ViewChildren, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { PanelModule } from 'primeng/panel';
import { TabViewModule } from 'primeng/tabview';
import {
    IMissionExtensionMachine,
    IMissionExtensionConfiguration,
    IGlobalMission,
    IGlobalMissionStartCommand,
    IMissionExecutionCommand,
    IMissionLogs,
} from '../../models/i-mission-extension';
import { MyRxStompService } from '../../services/my-rx-stomp.service';
import { MissionExtensionHttpService } from '../../services/mission-extension-http.service';
import { LogTableWidgetComponent } from '../../widgets/log-table-widget/log-table-widget.component';

@Component({
    selector: 'app-mission-extension',
    standalone: true,
    imports: [
        ButtonModule,
        CommonModule,
        DividerModule,
        DropdownModule,
        FormsModule,
        LogTableWidgetComponent,
        PanelModule,
        TabViewModule,
    ],
    templateUrl: './mission-extension.component.html',
    styleUrl: './mission-extension.component.scss',
})
export class MissionExtensionComponent implements OnInit {
    // Global mission
    selectedGlobalMissionName?: string;
    selectedGlobalMission?: IGlobalMission;

    // Individual mission
    selectedIndividualMissionName?: string;
    selectedIndividualMachineName?: string;

    missionConfiguration?: IMissionExtensionConfiguration;

    // Backend-persisted logs keyed by mission name (survive page refresh)
    backendLogs: { [missionName: string]: string[] } = {};

    @ViewChildren('logList') logLists!: QueryList<ElementRef<HTMLElement>>;

    private readonly rxStompService = inject(MyRxStompService);
    private readonly httpService = inject(MissionExtensionHttpService);
    private readonly destroyRef = inject(DestroyRef);

    ngOnInit(): void {
        // STOMP subscriptions first so they are ready before any push arrives
        this.rxStompService
            .watch('/topic/mission-extension/configuration')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(message => {
                const body = message.body ? JSON.parse(message.body) : null;
                if (body) this.applyConfiguration(body);
            });

        this.rxStompService
            .watch('/topic/mission-extension/logs')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(message => {
                const body = message.body ? JSON.parse(message.body) : null;
                if (body) this.applyLogs(body);
            });

        // HTTP initial load: restores running state and logs immediately on refresh
        this.httpService.getConfiguration()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: config => this.applyConfiguration(config),
                error: () => this.requestConfigurationViaStomp(),
            });

        this.httpService.getLogs()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({ next: logs => this.applyLogs(logs) });
    }

    // --- Getters ---

    get globalMissions(): IGlobalMission[] {
        return this.missionConfiguration?.globalMissions ?? [];
    }

    get machines(): IMissionExtensionMachine[] {
        return this.missionConfiguration?.machines ?? [];
    }

    get allMissionOptions(): { name: string; description: string }[] {
        const fromActive = (this.missionConfiguration?.missions ?? []).map(m => ({
            name: m.name, description: m.description,
        }));
        if (fromActive.length > 0) return fromActive;
        const seen = new Set<string>();
        return (this.missionConfiguration?.globalMissions ?? [])
            .flatMap(gm => gm.missionNames.map(n => ({ name: n, description: '' })))
            .filter(opt => !seen.has(opt.name) && seen.add(opt.name));
    }

    get globalMissionEntries(): { mission: string }[] {
        if (!this.selectedGlobalMission?.missionNames) return [];
        return this.selectedGlobalMission.missionNames.map(mission => ({ mission }));
    }

    get isGlobalMissionActive(): boolean {
        return !!this.selectedGlobalMissionName &&
            this.selectedGlobalMissionName === this.missionConfiguration?.activeGlobalMissionName;
    }

    get shouldShowMissionsDetail(): boolean {
        return this.isGlobalMissionActive ||
            Object.values(this.backendLogs).some(logs => logs.length > 0);
    }

    get missionDisplayEntries(): { mission: string }[] {
        const fromGlobal = this.globalMissionEntries;
        if (fromGlobal.length > 0) return fromGlobal;
        return Object.keys(this.backendLogs)
            .filter(name => this.backendLogs[name].length > 0)
            .map(name => ({ mission: name }));
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
        if (!this.selectedIndividualMissionName) return false;
        return this.missionConfiguration?.activeMissionNames?.includes(this.selectedIndividualMissionName) ?? false;
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
        const state = this.getMissionActiveState(missionName);
        return !!state && !state.toLowerCase().includes('idle');
    }

    getMissionLogs(missionName: string): string[] {
        return this.backendLogs[missionName] ?? [];
    }

    // --- Actions ---

    onGlobalMissionSelected(): void {
        this.selectedGlobalMission = this.globalMissions.find(
            gm => gm.name === this.selectedGlobalMissionName
        );
    }

    onStartGlobalMission(): void {
        if (!this.selectedGlobalMissionName) return;

        const command: IGlobalMissionStartCommand = {
            globalMissionName: this.selectedGlobalMissionName,
            machineOverrides: {},
        };

        this.rxStompService.publish({
            destination: '/app/mission-extension/global/start',
            body: JSON.stringify(command),
        });
    }

    onStopGlobalMission(): void {
        this.rxStompService.publish({
            destination: '/app/mission-extension/global/stop',
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
            destination: '/app/mission-extension/start',
            body: JSON.stringify(command),
        });
    }

    onStopIndividualMission(): void {
        if (!this.selectedIndividualMissionName) return;

        this.rxStompService.publish({
            destination: '/app/mission-extension/stop',
            body: JSON.stringify({
                machineName: '',
                missionName: this.selectedIndividualMissionName,
            }),
        });
    }

    // --- Private ---

    private requestConfigurationViaStomp(): void {
        this.rxStompService.publish({
            destination: '/app/mission-extension/configuration',
            body: '',
        });
    }

    private applyConfiguration(config: IMissionExtensionConfiguration): void {
        this.missionConfiguration = config;
        this.selectedGlobalMissionName = config.activeGlobalMissionName || this.selectedGlobalMissionName;
        this.onGlobalMissionSelected();
    }

    private applyLogs(logs: IMissionLogs): void {
        this.backendLogs = logs.logsByMission ?? {};
        setTimeout(() => this.scrollLogsToBottom());
    }

    private scrollLogsToBottom(): void {
        this.logLists?.forEach(ref => {
            ref.nativeElement.scrollTop = ref.nativeElement.scrollHeight;
        });
    }
}
