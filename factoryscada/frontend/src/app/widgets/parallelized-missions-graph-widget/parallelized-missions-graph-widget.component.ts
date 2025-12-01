import {
    Component,
    AfterViewInit,
    Input,
    OnChanges,
    SimpleChanges,
    ViewChild,
    ElementRef
} from '@angular/core';
import mermaid from 'mermaid';
import svgPanZoom from 'svg-pan-zoom';

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
    private panZoomInstance: SvgPanZoom.Instance | null = null;

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
        /*
        Renders the Mermaid diagram SVG and sets up zoom/pan functionality.
        */
        if (!this.diagramSource || !this.containerRef) return;

        try {
            // Render Mermaid diagram to SVG markup
            const { svg } = await mermaid.render('graphDiv', this.diagramSource);

            // Insert SVG into container
            this.containerRef.nativeElement.innerHTML = svg;

            // Wait for DOM update before manipulating SVG
            requestAnimationFrame(() => {
                // Wait for DOM updates twice (Mermaid can inject styles late)
                requestAnimationFrame(() => {
                    const svgElement = this.containerRef.nativeElement.querySelector('svg');
                    if (!svgElement) return;

                    // Calculate viewBox based on actual SVG content size
                    // Ensure bbox is valid
                    const bbox = svgElement.getBBox();
                    if (!isFinite(bbox.width) || !isFinite(bbox.height) || bbox.width === 0 || bbox.height === 0) {
                        console.warn('BBox invalid, deferring panZoom init');
                        // Try again a bit later
                        setTimeout(() => this.renderMermaid(), 50);
                        return;
                    }
                    const viewBox = `${bbox.x} ${bbox.y} ${bbox.width} ${bbox.height}`;

                    // Set responsive SVG attributes and styles
                    svgElement.removeAttribute('height');
                    svgElement.setAttribute('width', '100%');
                    svgElement.setAttribute('viewBox', viewBox);
                    svgElement.setAttribute('preserveAspectRatio', 'xMinYMin meet');
                    svgElement.style.width = '100%';
                    svgElement.style.minHeight = '300px';
                    svgElement.style.height = 'auto';
                    svgElement.style.aspectRatio = `${bbox.width} / ${bbox.height}`;

                    // Set container width to viewport width
                    const container = this.containerRef.nativeElement;
                    container.style.width = '100vw';
                    container.style.height = 'auto';

                    // Initialize svg-pan-zoom with zoom and controls enabled
                   // Initialize pan/zoom in a separate microtask
                    setTimeout(() => {
                        const panZoom = svgPanZoom(svgElement, {
                            zoomEnabled: true,
                            controlIconsEnabled: true,
                            center: true,
                        });

                        panZoom.resize();
                        panZoom.fit();
                        panZoom.center();
                    }, 0);
                });
            });
        } catch (error) {
            console.error('Error rendering Mermaid diagram:', error);
        }
        
    }



}
