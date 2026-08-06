import { Routes } from '@angular/router';

export const RENTALS_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./rental-list.component').then((m) => m.RentalListComponent) },
  { path: ':agreementId', loadComponent: () => import('./rental-detail.component').then((m) => m.RentalDetailComponent) },
];
