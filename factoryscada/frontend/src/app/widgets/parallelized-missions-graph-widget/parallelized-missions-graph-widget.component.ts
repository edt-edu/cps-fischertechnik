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
    if (!this.diagramSource || !this.containerRef) return;

    try {
        const { svg } = await mermaid.render('graphDiv', this.diagramSource);

        // Inject the SVG
        this.containerRef.nativeElement.innerHTML = svg;

        // Wait for next frame so browser paints the SVG
        requestAnimationFrame(() => {
            const svgElement = this.containerRef.nativeElement.querySelector('svg');
            if (!svgElement) return;

            // ❗ Get the actual bounding box of the SVG content
            const bbox = svgElement.getBBox();
            const viewBox = `${bbox.x} ${bbox.y} ${bbox.width} ${bbox.height}`;

            // Apply responsive attributes
            svgElement.removeAttribute('height');
            svgElement.setAttribute('width', '100%');
            svgElement.setAttribute('viewBox', viewBox);
            svgElement.setAttribute('preserveAspectRatio', 'xMinYMin meet');

            svgElement.style.width = '100%';
            svgElement.style.minHeight = '300px';
            svgElement.style.height = 'auto';

            // Optional but good for modern browsers
            svgElement.style.aspectRatio = `${bbox.width} / ${bbox.height}`;

            const container = this.containerRef.nativeElement;
            container.style.width = '100vw';
            container.style.height = 'auto';

            // ✅ Now init svg-pan-zoom and force proper layout
            const panZoom = svgPanZoom(svgElement, {
                zoomEnabled: true,
                controlIconsEnabled: true,
                center: true,
            });

            // Call these AFTER svg-pan-zoom is initialized
            panZoom.resize();
            panZoom.fit();
            panZoom.center();
        });

    } catch (error) {
        console.error('Error rendering Mermaid diagram:', error);
    }
}



}
