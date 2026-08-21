import { Routes } from '@angular/router';

export const OWNER_PROPERTIES_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./property-list.component').then((m) => m.OwnerPropertyListComponent) },
  { path: 'new', loadComponent: () => import('./property-form.component').then((m) => m.OwnerPropertyFormComponent) },
  { path: ':propertyId/units', loadComponent: () => import('./unit-list.component').then((m) => m.OwnerUnitListComponent) },
  { path: ':propertyId/units/new', loadComponent: () => import('./unit-form.component').then((m) => m.OwnerUnitFormComponent) },
  { path: ':propertyId/units/:unitId/edit', loadComponent: () => import('./unit-form.component').then((m) => m.OwnerUnitFormComponent) },
];
