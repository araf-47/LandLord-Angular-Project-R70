import { Routes } from '@angular/router';

export const MARKETPLACE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./marketplace-layout.component').then((m) => m.MarketplaceLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'ads' },
      { path: 'ads', loadComponent: () => import('./ad-management.component').then((m) => m.AdManagementComponent) },
      { path: 'requests', loadComponent: () => import('./request-list.component').then((m) => m.RequestListComponent) },
      { path: 'requests/:requestId', loadComponent: () => import('./request-detail.component').then((m) => m.RequestDetailComponent) },
    ],
  },
];
