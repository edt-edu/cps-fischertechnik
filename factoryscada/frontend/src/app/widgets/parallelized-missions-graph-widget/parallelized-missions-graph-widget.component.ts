import { Component, AfterViewInit, Input, OnChanges, SimpleChanges, ViewChild, ElementRef } from '@angular/core';
import mermaid from 'mermaid';

@Component({
    selector: 'app-parralelized-missions-graph',
    standalone: true,
    templateUrl: './parallelized-missions-graph-widget.component.html',
    styleUrls: ['./parallelized-missions-graph-widget.component.scss']
})
export class GraphMissionsView implements AfterViewInit, OnChanges {

    @Input() diagramSource: string = '';
    @ViewChild('container', { static: true }) containerRef!: ElementRef<HTMLDivElement>;

    private initialized = false;

    ngAfterViewInit(): void {
        mermaid.initialize({ startOnLoad: false });
        this.initialized = true;
        this.renderMermaid();
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['diagramSource'] && this.initialized) {
            this.renderMermaid();
        }
    }

    private async renderMermaid(): Promise<void> {
        if (!this.diagramSource || !this.containerRef) return;

        try {
            const { svg } = await mermaid.render('graphDiv', this.diagramSource);
            this.containerRef.nativeElement.innerHTML = svg;
        } catch (error) {
            console.error('Error rendering Mermaid diagram:', error);
        }
    }

}
