import { Routes } from '@angular/router';
import { DashboardLayoutComponent } from "./layouts/dashboard-layout/dashboard-layout.component";
import { DirectCommandComponent } from './pages/direct-command/direct-command.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard/direct-command'
  }, 
  {
    path: 'dashboard',
    component: DashboardLayoutComponent,
    loadChildren: () => import('./layouts/dashboard-layout/dashboard-layout.routes')
      .then(m => m.dashboardLayoutRoutes)
  }, 
  {
    path: '**',
    redirectTo: ''
  }
];
