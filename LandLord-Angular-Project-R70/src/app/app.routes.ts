import { Routes } from '@angular/router';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'auth' },
  {
    path: 'auth',
    loadComponent: () => import('./layouts/auth-layout.component').then((m) => m.AuthLayoutComponent),
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'landlord',
    loadComponent: () => import('./layouts/landlord-layout.component').then((m) => m.LandlordLayoutComponent),
    loadChildren: () => import('./features/landlord/landlord.routes').then((m) => m.LANDLORD_ROUTES),
    canActivate: [roleGuard('landlord')],
  },
  {
    path: 'tenant',
    loadComponent: () => import('./layouts/tenant-layout.component').then((m) => m.TenantLayoutComponent),
    loadChildren: () => import('./features/tenant/tenant.routes').then((m) => m.TENANT_ROUTES),
    canActivate: [roleGuard('tenant')],
  },
  { path: '**', redirectTo: 'auth' },
];
