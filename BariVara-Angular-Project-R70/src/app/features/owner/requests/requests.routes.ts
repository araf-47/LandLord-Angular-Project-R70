import { Routes } from '@angular/router';

export const OWNER_REQUESTS_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./request-list.component').then((m) => m.OwnerRequestListComponent) },
  { path: ':requestId', loadComponent: () => import('./request-detail.component').then((m) => m.OwnerRequestDetailComponent) },
];
