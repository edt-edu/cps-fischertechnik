import { Component, DestroyRef, ElementRef, OnInit, QueryList, ViewChildren, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { PanelModule } from 'primeng/panel';
import { TabViewModule } from 'primeng/tabview';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import {
  IMissionChangeEvent,
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
import { MissionStateGraphWidgetComponent } from '../../widgets/mission-state-graph-widget/mission-state-graph-widget.component';

/** A mission of the current state, and the machine it is bound to (none if no listed machine runs it). */
interface MissionEntry {
  mission: string;
  machine?: IMissionExtensionMachine;
}

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
    MissionStateGraphWidgetComponent,
    PanelModule,
    TabViewModule,
    ToastModule,
  ],
  providers: [MessageService],
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

  // Missions whose detail panel is open: only those render their state machine graph
  expandedMissions = new Set<string>();

  // Backend-persisted logs keyed by mission name (survive page refresh)
  backendLogs: { [missionName: string]: string[] } = {};

  @ViewChildren('logList') logLists!: QueryList<ElementRef<HTMLElement>>;

  private readonly rxStompService = inject(MyRxStompService);
  private readonly httpService = inject(MissionExtensionHttpService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly messageService = inject(MessageService);
  private static readonly REPLANNING_GROUP_GAP_MS = 120_000;
  private dismissedReplanningTime: string | null = null;
  private lastNotifiedChangeTime: number | null = null;

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

  get isGlobalMissionActive(): boolean {
    return !!this.selectedGlobalMissionName &&
      this.selectedGlobalMissionName === this.missionConfiguration?.activeGlobalMissionName;
  }

  get shouldShowMissionsDetail(): boolean {
    return this.isGlobalMissionActive ||
      (this.missionConfiguration?.activeMissionNames?.length ?? 0) > 0 ||
      Object.values(this.backendLogs).some(logs => logs.length > 0);
  }

  /**
   * The current state, one entry per machine: the mission it runs, else the mission a change stopped (else its
   * mission in the selected global mission, not started). Then the running missions bound to no listed machine.
   */
  get missionDisplayEntries(): MissionEntry[] {
    const entries: MissionEntry[] = [];
    const shown = new Set<string>();
    const globalMissionNames = this.selectedGlobalMission?.missionNames ?? [];
    for (const machine of this.machines) {
      const stopped = this.latestChange(machine.name);
      const mission = machine.activeMissionName
        ?? (stopped && !stopped.mission ? stopped.previousMission : null)
        ?? machine.missions.map(option => option.name).find(name => globalMissionNames.includes(name));
      if (mission && !shown.has(mission)) {
        entries.push({ mission, machine });
        shown.add(mission);
      }
    }
    (this.missionConfiguration?.activeMissionNames ?? [])
      .filter(name => !shown.has(name))
      .sort()
      .forEach(mission => entries.push({ mission }));
    if (entries.length > 0) return entries;
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

  // --- Running configuration ---

  get missionChanges(): IMissionChangeEvent[] {
    return this.missionConfiguration?.missionChanges ?? [];
  }

  get lastChangeWasReplanning(): boolean {
    return this.missionChanges[0]?.origin === 'REPLANNING';
  }

  /**
   * The mission changes of the latest replanning, oldest first: the most recent changes, as long as they come from the
   * replanning (an operator change after it ends it).
   */
  get lastReplanning(): IMissionChangeEvent[] {
    const changes = this.missionChanges;
    if (changes[0]?.origin !== 'REPLANNING') return [];
    const group = [changes[0]];
    for (const change of changes.slice(1)) {
      const gap = Date.parse(group[group.length - 1].time) - Date.parse(change.time);
      if (change.origin !== 'REPLANNING' || gap > MissionExtensionComponent.REPLANNING_GROUP_GAP_MS) break;
      group.push(change);
    }
    return group.reverse();
  }

  get showReplanningBanner(): boolean {
    const replanning = this.lastReplanning;
    return replanning.length > 0 && replanning[replanning.length - 1].time !== this.dismissedReplanningTime;
  }

  dismissReplanningBanner(): void {
    const replanning = this.lastReplanning;
    this.dismissedReplanningTime = replanning[replanning.length - 1]?.time ?? null;
  }

  /** The latest mission change of the machine, if any. */
  latestChange(machine: string): IMissionChangeEvent | undefined {
    return this.missionChanges.find(change => change.machine === machine);
  }

  /** What the entry's machine runs: its nominal mission, an alternative, or nothing (stopped, not started). */
  entryStatus(entry: MissionEntry): 'nominal' | 'alternative' | 'stopped' | 'not-running' {
    const machine = entry.machine;
    if (!machine || machine.activeMissionName !== entry.mission) {
      if (!machine && this.missionConfiguration?.activeMissionNames?.includes(entry.mission)) return 'nominal';
      return machine && this.latestChange(machine.name)?.previousMission === entry.mission ? 'stopped' : 'not-running';
    }
    return this.isAlternativeMission(machine) ? 'alternative' : 'nominal';
  }

  /** The latest change of the entry's machine, if it concerns the entry's mission (started or stopped it). */
  entryChange(entry: MissionEntry): IMissionChangeEvent | undefined {
    if (!entry.machine) return undefined;
    const change = this.latestChange(entry.machine.name);
    return change && (change.mission === entry.mission || change.previousMission === entry.mission) ? change : undefined;
  }

  /** The mission an alternative replaced on its machine (its last change, else the nominal mission). */
  replacedMission(entry: MissionEntry): string | null {
    if (this.entryStatus(entry) !== 'alternative') return null;
    const change = this.entryChange(entry);
    return (change?.mission === entry.mission ? change.previousMission : null) ?? entry.machine?.nominalMissionName ?? null;
  }

  /** The other machines of the mission than the one running it. */
  otherMachines(entry: MissionEntry): string[] {
    return this.getMissionMachines(entry.mission).filter(name => name !== entry.machine?.name);
  }

  missionPanelClass(entry: MissionEntry): string {
    return `mission-panel mission-panel-${this.entryStatus(entry)}`;
  }

  isAlternativeMission(machine: IMissionExtensionMachine): boolean {
    return !!machine.activeMissionName && machine.activeMissionName !== machine.nominalMissionName;
  }

  describeMission(mission?: string | null, state?: string | null): string {
    if (!mission) return 'no mission';
    return state ? `${mission} :: ${state}` : mission;
  }

  // --- Mission detail helpers ---

  getMissionActiveState(missionName: string): string | null {
    const mission = this.missionConfiguration?.missions.find(m => m.name === missionName);
    return mission?.activeState ?? null;
  }

  getMissionDotGraph(missionName: string): string | null {
    const mission = this.missionConfiguration?.missions.find(m => m.name === missionName);
    return mission?.dotGraph ?? null;
  }

  onMissionPanelToggle(missionName: string, collapsed: boolean): void {
    if (collapsed) {
      this.expandedMissions.delete(missionName);
    } else {
      this.expandedMissions.add(missionName);
    }
  }

  getMissionMachines(missionName: string): string[] {
    const mission = this.missionConfiguration?.missions.find(m => m.name === missionName);
    return mission?.involvedMachines ?? [];
  }

  /** Whether the mission waits for the result of a command it sent (from its model, not its state's name). */
  isMissionCommandRunning(missionName: string): boolean {
    const mission = this.missionConfiguration?.missions.find(m => m.name === missionName);
    return !!mission?.awaitingCommandResult;
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
    this.notifyReplanningChanges(config.missionChanges ?? []);
    this.missionConfiguration = config;
    this.selectedGlobalMissionName = config.activeGlobalMissionName || this.selectedGlobalMissionName;
    this.onGlobalMissionSelected();
  }

  /** Shows a toast for each mission change made by the replanning since the last configuration. */
  private notifyReplanningChanges(changes: IMissionChangeEvent[]): void {
    const lastNotified = this.lastNotifiedChangeTime;
    this.lastNotifiedChangeTime = changes.length > 0 ? Date.parse(changes[0].time) : (lastNotified ?? 0);
    if (lastNotified === null) {
      // first configuration (page load): the past changes are listed, not notified
      return;
    }
    changes
      .filter(change => Date.parse(change.time) > lastNotified && change.origin === 'REPLANNING')
      .reverse()
      .forEach(change => this.messageService.add({
        severity: 'warn',
        summary: `Replanning: ${change.machine}`,
        detail: `${this.describeMission(change.previousMission, change.previousState)} → `
          + this.describeMission(change.mission, change.state),
        life: 8000,
      }));
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
