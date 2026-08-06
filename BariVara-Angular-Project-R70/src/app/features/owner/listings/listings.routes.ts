import { Routes } from '@angular/router';

export const OWNER_LISTINGS_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./listing-list.component').then((m) => m.OwnerListingListComponent) },
  { path: 'new', loadComponent: () => import('./listing-form.component').then((m) => m.OwnerListingFormComponent) },
  { path: ':listingId/edit', loadComponent: () => import('./listing-edit.component').then((m) => m.OwnerListingEditComponent) },
];
