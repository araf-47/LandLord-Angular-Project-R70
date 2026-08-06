import { Routes } from '@angular/router';

export const OWNER_PROPERTIES_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./property-list.component').then((m) => m.OwnerPropertyListComponent) },
  { path: 'new', loadComponent: () => import('./property-form.component').then((m) => m.OwnerPropertyFormComponent) },
];
