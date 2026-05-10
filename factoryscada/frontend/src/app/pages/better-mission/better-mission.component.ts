import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';
import { GraphMissionsView } from '../../widgets/parallelized-missions-graph-widget/parallelized-missions-graph-widget.component';
import { LogTableWidgetComponent } from '../../widgets/log-table-widget/log-table-widget.component';
import {
    IBetterMission,
    IBetterMissionMachine,
    IBetterMissionNode,
    IBetterMissionOption,
    IBetterMissionsConfiguration,
    IGlobalMission,
    IGlobalMissionMachineOverrideCommand,
    IGlobalMissionStartCommand,
    IMissionExecutionCommand,
} from '../../models/i-better-missions';
import { MyRxStompService } from '../../services/my-rx-stomp.service';

@Component({
    selector: 'app-better-mission',
    standalone: true,
    imports: [
        AccordionModule,
        ButtonModule,
        DividerModule,
        DropdownModule,
        FormsModule,
        GraphMissionsView,
        LogTableWidgetComponent,
        OverlayPanelModule,
        PanelModule,
    ],
    templateUrl: './better-mission.component.html',
    styleUrl: './better-mission.component.scss',
})
export class BetterMissionComponent implements OnInit {
    // Individual mission properties
    selectedMachineName?: string;
    selectedMissionName?: string;

    // Global mission properties
    selectedGlobalMissionName?: string;
    selectedGlobalMission?: IGlobalMission;
    globalMissionMachineOverrides: { [key: string]: string } = {};

    missionConfiguration?: IBetterMissionsConfiguration;
    selectedMachine?: IBetterMissionMachine;
    selectedMission?: IBetterMission;
    missionDescription = '';
    missionsGraph = 'flowchart LR\n  Empty[No mission selected]';
    infoMessage = '';

    private readonly rxStompService = inject(MyRxStompService);
    private readonly destroyRef = inject(DestroyRef);

    ngOnInit(): void {
        // Subscribe to configuration updates from STOMP topic
        this.rxStompService
            .watch('/topic/better-missions/configuration')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((message) => {
                const body = message.body ? JSON.parse(message.body) : null;
                if (body) {
                    this.missionConfiguration = body;
                    // Update global mission state from server
                    this.selectedGlobalMissionName = body.activeGlobalMissionName;
                    this.globalMissionMachineOverrides = body.globalMissionMachineOverrides || {};
                    this.ensureMachineSelection();
                    this.syncSelection();
                }
            });

        // Subscribe to command responses
        this.rxStompService
            .watch('/topic/better-missions/command-response')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((message) => {
                const body = message.body ? JSON.parse(message.body) : null;
                if (body?.message) {
                    this.infoMessage = body.message;
                }
            });

        // Request initial configuration
        this.requestConfiguration();
    }

    get missions(): IBetterMission[] {
        return this.missionConfiguration?.missions ?? [];
    }

    get globalMissions(): IGlobalMission[] {
        return this.missionConfiguration?.globalMissions ?? [];
    }

    get machines(): IBetterMissionMachine[] {
        return this.missionConfiguration?.machines ?? [];
    }

    get missionOptions(): IBetterMissionOption[] {
        return this.selectedMachine?.missions ?? [];
    }

    get isGlobalMissionActive(): boolean {
        return !!this.selectedGlobalMissionName && this.selectedGlobalMissionName === this.missionConfiguration?.activeGlobalMissionName;
    }

    get canStartMission(): boolean {
        return !!this.selectedMachineName && !!this.selectedMissionName;
    }

    get canStopMission(): boolean {
        return !!this.selectedMachine?.activeMissionName;
    }

    onGlobalMissionSelected(): void {
        this.selectedGlobalMission = this.globalMissions.find(
            (gm) => gm.name === this.selectedGlobalMissionName
        );
        // Initialize overrides with defaults
        if (this.selectedGlobalMission) {
            this.globalMissionMachineOverrides = { ...this.selectedGlobalMission.machineDefaultMissions };
        }
    }

    onStartGlobalMission(): void {
        if (!this.selectedGlobalMissionName) {
            return;
        }

        const command: IGlobalMissionStartCommand = {
            globalMissionName: this.selectedGlobalMissionName,
            machineOverrides: this.globalMissionMachineOverrides,
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

    onGlobalMissionMachineOverride(machineName: string): void {
        if (!this.isGlobalMissionActive) {
            return;
        }

        const missionName = this.globalMissionMachineOverrides[machineName];
        if (!missionName) {
            return;
        }

        const command: IGlobalMissionMachineOverrideCommand = {
            machineName,
            missionName,
        };

        this.rxStompService.publish({
            destination: '/app/better-missions/global/override',
            body: JSON.stringify(command),
        });
    }

    getMachineCurrentMission(machineName: string): IBetterMission | undefined {
        const missionName = this.globalMissionMachineOverrides[machineName];
        if (!missionName) {
            return undefined;
        }
        return this.missionConfiguration?.missions.find((mission) => mission.name === missionName);
    }

    onMachineSelected(): void {
        this.ensureMachineSelection();
        this.syncSelection();
    }

    onMissionSelected(): void {
        this.syncSelection();
    }

    onStartMission(): void {
        if (!this.selectedMachineName || !this.selectedMissionName) {
            return;
        }

        const command: IMissionExecutionCommand = {
            machineName: this.selectedMachineName,
            missionName: this.selectedMissionName,
        };

        this.rxStompService.publish({
            destination: '/app/better-missions/start',
            body: JSON.stringify(command),
        });
    }

    onStopMission(): void {
        if (!this.selectedMachineName || !this.selectedMachine?.activeMissionName) {
            return;
        }

        this.rxStompService.publish({
            destination: '/app/better-missions/stop',
            body: JSON.stringify({ machineName: this.selectedMachineName, missionName: this.selectedMachine.activeMissionName }),
        });
    }

    private requestConfiguration(): void {
        this.rxStompService.publish({
            destination: '/app/better-missions/configuration',
            body: '',
        });
    }

    private ensureMachineSelection(): void {
        const availableMachines = this.machines;

        if (!availableMachines.length) {
            this.selectedMachineName = undefined;
            this.selectedMachine = undefined;
            this.selectedMissionName = undefined;
            return;
        }

        const selectedMachine = availableMachines.find((machine) => machine.name === this.selectedMachineName);
        if (!selectedMachine) {
            this.selectedMachineName = availableMachines[0].name;
            this.selectedMachine = availableMachines[0];
        } else {
            this.selectedMachine = selectedMachine;
        }

        const currentMission = this.selectedMachine?.missions.find(
            (mission) => mission.name === this.selectedMissionName
        );

        if (!currentMission) {
            this.selectedMissionName = this.selectedMachine?.missions[0]?.name;
        }
    }

    private syncSelection(): void {
        this.selectedMachine = this.missionConfiguration?.machines.find(
            (machine) => machine.name === this.selectedMachineName
        );

        this.selectedMission = this.missionConfiguration?.missions.find(
            (mission) => mission.name === this.selectedMissionName
        );

        if (!this.selectedMission) {
            this.missionDescription = '';
            this.missionsGraph = 'flowchart LR\n  Empty[No mission selected]';
            return;
        }

        this.missionDescription = this.selectedMission.description;
        this.missionsGraph = this.buildMermaidDiagramFromMission(this.selectedMission);
    }

    buildMermaidDiagramFromMission(mission: IBetterMission): string {
        const lines: string[] = ['flowchart LR'];
        const sanitize = (id: string) => id.replace(/\s+/g, '_');

        for (const node of mission.nodes) {
            const nodeId = sanitize(node.id);
            const label = (node.description || node.id).trim().replace(/"/g, '#quot;');
            const hasOutputs = (node.outputs?.length ?? 0) > 0;
            const type = (node.type || '').toLowerCase();

            if (type === 'entrynode') {
                lines.push(`${nodeId}(["<span title='${label}'>${node.id}</span>"])`);
            } else if (!hasOutputs) {
                lines.push(`${nodeId}((("<span title='${label}'>${node.id}</span>")))`);
            } else {
                lines.push(`${nodeId}["<span title='${label}'>${label}</span>"]`);
            }
        }

        for (const node of mission.nodes) {
            const source = sanitize(node.id);
            for (const output of this.getOutputNodeIds(node)) {
                lines.push(`${source} --> ${sanitize(output)}`);
            }
        }

        lines.push('classDef surveillance fill:#ffcccc,stroke:#ff0000,stroke-width:2px;');
        for (const node of mission.nodes) {
            if (node.active) {
                lines.push(`class ${sanitize(node.id)} surveillance`);
            }
        }

        return lines.join('\n');
    }

    private getOutputNodeIds(node: IBetterMissionNode): string[] {
        if ((node.outputs?.length ?? 0) > 0) {
            return node.outputs;
        }

        if ((node.outputNodes?.length ?? 0) > 0) {
            return node.outputNodes.map((output) => output.id);
        }

        return [];
    }
}
