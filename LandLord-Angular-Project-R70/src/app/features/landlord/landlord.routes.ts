import { Routes } from '@angular/router';

export const LANDLORD_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', loadComponent: () => import('./dashboard.component').then((m) => m.LandlordDashboardComponent) },
  { path: 'properties', loadChildren: () => import('./properties/properties.routes').then((m) => m.PROPERTIES_ROUTES) },
  { path: 'tenants', loadChildren: () => import('./tenants/tenants.routes').then((m) => m.TENANTS_ROUTES) },
  { path: 'marketplace', loadChildren: () => import('./marketplace/marketplace.routes').then((m) => m.MARKETPLACE_ROUTES) },
  { path: 'rentals', loadChildren: () => import('./rentals/rentals.routes').then((m) => m.RENTALS_ROUTES) },
  { path: 'payments', loadChildren: () => import('./payments/payments.routes').then((m) => m.PAYMENTS_ROUTES) },
  { path: 'expenses', loadComponent: () => import('./expenses/expense-management.component').then((m) => m.ExpenseManagementComponent) },
  { path: 'maintenance', loadChildren: () => import('./maintenance/maintenance.routes').then((m) => m.LANDLORD_MAINTENANCE_ROUTES) },
  { path: 'messages', loadChildren: () => import('./messages/messages.routes').then((m) => m.LANDLORD_MESSAGES_ROUTES) },
];
