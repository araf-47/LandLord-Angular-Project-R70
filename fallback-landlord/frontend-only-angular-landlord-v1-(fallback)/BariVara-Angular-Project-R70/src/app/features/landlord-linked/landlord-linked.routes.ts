import { Routes } from '@angular/router';

export const LANDLORD_LINKED_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', loadComponent: () => import('./dashboard.component').then((m) => m.LandlordLinkedDashboardComponent) },
];
