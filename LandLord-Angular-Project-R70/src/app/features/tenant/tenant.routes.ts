import { Routes } from '@angular/router';

export const TENANT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', loadComponent: () => import('./dashboard.component').then((m) => m.TenantDashboardComponent) },
  { path: 'profile', loadComponent: () => import('./profile.component').then((m) => m.TenantProfileComponent) },
  { path: 'notifications', loadComponent: () => import('./notifications.component').then((m) => m.TenantNotificationsComponent) },
  { path: 'payments', loadChildren: () => import('./payments/payments.routes').then((m) => m.TENANT_PAYMENTS_ROUTES) },
  { path: 'maintenance', loadChildren: () => import('./maintenance/maintenance.routes').then((m) => m.TENANT_MAINTENANCE_ROUTES) },
  { path: 'documents', loadComponent: () => import('./documents/document-dashboard.component').then((m) => m.TenantDocumentDashboardComponent) },
  { path: 'browse-transfer', loadComponent: () => import('./browse-transfer.component').then((m) => m.TenantBrowseTransferComponent) },
  { path: 'messages', loadChildren: () => import('./messages/messages.routes').then((m) => m.TENANT_MESSAGES_ROUTES) },
];
