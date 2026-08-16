import { Routes } from '@angular/router';

export const PUBLIC_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./homepage.component').then((m) => m.HomepageComponent) },
  { path: 'browse', loadComponent: () => import('./browse.component').then((m) => m.BrowseComponent) },
  { path: 'listings/:id', loadComponent: () => import('./listing-detail.component').then((m) => m.ListingDetailComponent) },
];
