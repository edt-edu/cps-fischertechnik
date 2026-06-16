import { Routes } from "@angular/router";

export const dashboardLayoutRoutes: Routes = [
  {
    path: 'home',
    loadComponent: () => import('../../pages/home/home.component').then(c => c.HomeComponent)
  },
  {
    path: 'debug-command',
    loadComponent: () => import('../../pages/debug-command/debug-command.component').then(c => c.DebugCommandComponent)
  }, {
    path: 'direct-command',
    loadComponent: () => import('../../pages/direct-command/direct-command.component').then(c => c.DirectCommandComponent)
  }, {
    path: 'mission',
    loadComponent: () => import('../../pages/mission/mission.component').then(c => c.MissionComponent)
  }, {
    path: 'mission-extension',
    loadComponent: () => import('../../pages/mission-extension/mission-extension.component').then(c => c.MissionExtensionComponent)
  }, {
    path: 'dynamic-mission',
    loadComponent: () => import('../../pages/dynamic-mission/dynamic-mission.component').then(c => c.DynamicMissionComponent)
  }
]
