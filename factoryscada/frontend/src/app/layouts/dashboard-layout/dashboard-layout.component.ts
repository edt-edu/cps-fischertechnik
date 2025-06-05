import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from "@angular/router";
import { TabMenuModule } from 'primeng/tabmenu';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    TabMenuModule
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
        routerLink: 'home',
      },
      {
        label: 'Machine Debug Command',
        routerLink: 'debug-command',
      },
      {
        label: 'Machine Command',
        routerLink: 'direct-command'
      },
      {
        label: 'Mission',
        routerLink: 'mission'
      }
    ];
  }
}
