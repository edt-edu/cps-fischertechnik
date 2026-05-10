import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from "@angular/router";
import { MenubarModule } from 'primeng/menubar';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    MenubarModule
    //TabMenuModule
  ],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.scss'
})
export class DashboardLayoutComponent {
  menuItems: MenuItem[] = [];


  ngOnInit() {
    this.fillMenu();
  }

  fillMenu() {
    this.menuItems = [
      {
        label: 'Home',
        icon: 'pi pi-home',
        routerLink: 'home',
      },
      {
        label: 'Machine Command',
        icon: 'pi pi-cog',
        routerLink: 'direct-command',
        items: [
          {
            label: 'Machine Command',
            icon: 'pi pi-cog',
            //  icon: 'pi pi-sliders-h',
            routerLink: 'direct-command'
          },
          {
            label: 'Machine Debug Command',
            icon: 'pi pi-code',
            routerLink: 'debug-command',
          }
        ]
      },
      {
        label: 'Mission',
        icon: 'pi pi-sitemap',
        items: [
          {
            label: 'Legacy Mission',
            icon: 'pi pi-sitemap',
            routerLink: 'mission'
          },
          {
            label: 'Better Missions',
            icon: 'pi pi-share-alt',
            routerLink: 'better-mission'
          }
        ]
      }
    ];
  }
}
