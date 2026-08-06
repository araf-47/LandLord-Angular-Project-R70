import { Routes } from '@angular/router';

export const TENANTS_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./tenant-list.component').then((m) => m.TenantListComponent) },
  { path: 'register', loadComponent: () => import('./tenant-register.component').then((m) => m.TenantRegisterComponent) },
  { path: ':tenantId', loadComponent: () => import('./tenant-detail.component').then((m) => m.TenantDetailComponent) },
  { path: ':tenantId/move-out', loadComponent: () => import('./tenant-moveout.component').then((m) => m.TenantMoveoutComponent) },
];
