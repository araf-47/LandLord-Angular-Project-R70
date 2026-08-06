import { Routes } from '@angular/router';

export const TENANT_PAYMENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./payments-layout.component').then((m) => m.TenantPaymentsLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'pay' },
      { path: 'pay', loadComponent: () => import('./pay.component').then((m) => m.TenantPayComponent) },
      { path: 'history', loadComponent: () => import('./history.component').then((m) => m.TenantPaymentHistoryComponent) },
      { path: 'pending', loadComponent: () => import('./pending.component').then((m) => m.TenantPaymentPendingComponent) },
    ],
  },
];
