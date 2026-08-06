import { Routes } from '@angular/router';

export const PAYMENTS_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'bills' },
  { path: 'bills', loadComponent: () => import('./generate-bills.component').then((m) => m.GenerateBillsComponent) },
  { path: 'receive', loadComponent: () => import('./receive-payment.component').then((m) => m.ReceivePaymentComponent) },
  { path: 'pending', loadComponent: () => import('./pending-cash.component').then((m) => m.PendingCashComponent) },
];
