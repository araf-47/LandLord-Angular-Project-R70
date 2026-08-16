import { Routes } from '@angular/router';

export const OWNER_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', loadComponent: () => import('./dashboard.component').then((m) => m.OwnerDashboardComponent) },
  { path: 'properties', loadChildren: () => import('./properties/properties.routes').then((m) => m.OWNER_PROPERTIES_ROUTES) },
  { path: 'listings', loadChildren: () => import('./listings/listings.routes').then((m) => m.OWNER_LISTINGS_ROUTES) },
  { path: 'requests', loadChildren: () => import('./requests/requests.routes').then((m) => m.OWNER_REQUESTS_ROUTES) },
  { path: 'messages', loadChildren: () => import('./messages/messages.routes').then((m) => m.OWNER_MESSAGES_ROUTES) },
];
