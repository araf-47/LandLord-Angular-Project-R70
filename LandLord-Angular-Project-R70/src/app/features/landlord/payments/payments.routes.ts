import { Routes } from '@angular/router';

export const PAYMENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./payments-layout.component').then((m) => m.PaymentsLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'bills' },
      { path: 'bills', loadComponent: () => import('./generate-bills.component').then((m) => m.GenerateBillsComponent) },
      { path: 'receive', loadComponent: () => import('./receive-payment.component').then((m) => m.ReceivePaymentComponent) },
    ],
  },
];
