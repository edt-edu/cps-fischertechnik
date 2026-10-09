import { Component, ElementRef, Input, OnChanges, ViewChild } from '@angular/core';

type VizInstance = Awaited<ReturnType<typeof import('@viz-js/viz')['instance']>>;

/** Graphviz (WebAssembly) is only loaded the first time a graph is shown, and shared by all graphs. */
let vizInstance: Promise<VizInstance> | null = null;
function viz(): Promise<VizInstance> {
    vizInstance ??= import('@viz-js/viz').then(module => module.instance());
    return vizInstance;
}

const ACTIVE_STATE_STYLE = 'style=filled, fillcolor="#facc15", color="#b45309", penwidth=2';

/**
 * Renders the Graphviz state machine of a mission (the DOT_SCHEMA generated from its SysML model), with its
 * active state highlighted.
 */
@Component({
    selector: 'app-mission-state-graph-widget',
    standalone: true,
    template: `
        <div class="state-graph" #graph></div>
        @if (error) {
            <div class="state-graph-error">{{ error }}</div>
        }
    `,
    styles: [`
        .state-graph { overflow-x: auto; padding: 0.5rem 0; }
        .state-graph ::ng-deep svg { max-width: 100%; height: auto; }
        .state-graph-error { color: #dc2626; font-size: 0.85rem; }
    `],
})
export class MissionStateGraphWidgetComponent implements OnChanges {
    @Input() dotGraph?: string | null;
    @Input() activeState?: string | null;

    @ViewChild('graph', { static: true }) graph!: ElementRef<HTMLElement>;

    error: string | null = null;
    private renderedKey: string | null = null;
    private renderRequest = 0;

    ngOnChanges(): void {
        const key = `${this.activeState ?? ''}\u0000${this.dotGraph ?? ''}`;
        if (key === this.renderedKey) return;
        this.renderedKey = key;
        this.render();
    }

    private async render(): Promise<void> {
        const request = ++this.renderRequest;
        const container = this.graph.nativeElement;
        if (!this.dotGraph) {
            container.replaceChildren();
            this.error = 'No state machine graph for this mission.';
            return;
        }
        try {
            const svg = (await viz()).renderSVGElement(highlightState(this.dotGraph, this.activeState));
            if (request !== this.renderRequest) return; // a newer state arrived while rendering
            container.replaceChildren(svg);
            this.error = null;
        } catch (e) {
            if (request !== this.renderRequest) return;
            container.replaceChildren();
            this.error = `Cannot render the state machine: ${e instanceof Error ? e.message : e}`;
        }
    }
}

/** Adds the highlight attributes to the active state node, as a last statement so they override the defaults. */
export function highlightState(dot: string, activeState?: string | null): string {
    const end = dot.lastIndexOf('}');
    if (!activeState || end < 0) return dot;
    const name = activeState.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
    return `${dot.slice(0, end)}    "${name}" [${ACTIVE_STATE_STYLE}];\n${dot.slice(end)}`;
}
