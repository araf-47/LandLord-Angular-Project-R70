import { Routes } from '@angular/router';

export const PROPERTIES_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./property-list.component').then((m) => m.PropertyListComponent) },
  { path: 'new', loadComponent: () => import('./property-form.component').then((m) => m.PropertyFormComponent) },
  { path: ':propertyId/edit', loadComponent: () => import('./property-form.component').then((m) => m.PropertyFormComponent) },
  { path: ':propertyId/units', loadComponent: () => import('./unit-list.component').then((m) => m.UnitListComponent) },
  { path: ':propertyId/units/new', loadComponent: () => import('./unit-form.component').then((m) => m.UnitFormComponent) },
  { path: ':propertyId/units/:unitId/edit', loadComponent: () => import('./unit-form.component').then((m) => m.UnitFormComponent) },
];
