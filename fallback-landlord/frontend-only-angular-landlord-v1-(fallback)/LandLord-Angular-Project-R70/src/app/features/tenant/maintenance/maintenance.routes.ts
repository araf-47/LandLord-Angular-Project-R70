import { Routes } from '@angular/router';

export const TENANT_MAINTENANCE_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./ticket-list.component').then((m) => m.TenantTicketListComponent) },
  { path: 'new', loadComponent: () => import('./ticket-new.component').then((m) => m.TenantTicketNewComponent) },
];
