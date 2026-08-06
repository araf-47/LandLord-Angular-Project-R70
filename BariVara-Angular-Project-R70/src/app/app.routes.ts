import { Routes } from '@angular/router';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layouts/public-layout.component').then((m) => m.PublicLayoutComponent),
    loadChildren: () => import('./features/public/public.routes').then((m) => m.PUBLIC_ROUTES),
  },
  {
    path: 'auth',
    loadComponent: () => import('./layouts/auth-layout.component').then((m) => m.AuthLayoutComponent),
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'tenant',
    loadComponent: () => import('./layouts/tenant-layout.component').then((m) => m.TenantLayoutComponent),
    loadChildren: () => import('./features/tenant/tenant.routes').then((m) => m.TENANT_ROUTES),
    canActivate: [roleGuard('tenant')],
  },
  {
    path: 'owner',
    loadComponent: () => import('./layouts/owner-layout.component').then((m) => m.OwnerLayoutComponent),
    loadChildren: () => import('./features/owner/owner.routes').then((m) => m.OWNER_ROUTES),
    canActivate: [roleGuard('owner')],
  },
  {
    path: 'landlord-linked',
    loadComponent: () => import('./layouts/landlord-linked-layout.component').then((m) => m.LandlordLinkedLayoutComponent),
    loadChildren: () => import('./features/landlord-linked/landlord-linked.routes').then((m) => m.LANDLORD_LINKED_ROUTES),
    canActivate: [roleGuard('landlord-linked')],
  },
  { path: '**', redirectTo: '' },
];
