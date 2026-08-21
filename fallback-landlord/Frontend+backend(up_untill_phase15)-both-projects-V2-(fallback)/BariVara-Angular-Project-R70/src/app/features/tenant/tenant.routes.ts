import { Routes } from '@angular/router';

export const TENANT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', loadComponent: () => import('./dashboard.component').then((m) => m.TenantDashboardComponent) },
  { path: 'search', loadComponent: () => import('../public/browse.component').then((m) => m.BrowseComponent) },
  { path: 'listings/:id', loadComponent: () => import('../public/listing-detail.component').then((m) => m.ListingDetailComponent) },
  { path: 'favorites', loadComponent: () => import('./favorites.component').then((m) => m.TenantFavoritesComponent) },
  { path: 'notifications', loadComponent: () => import('./notifications.component').then((m) => m.TenantNotificationsComponent) },
  { path: 'messages', loadChildren: () => import('./messages/messages.routes').then((m) => m.TENANT_MESSAGES_ROUTES) },
  { path: 'profile', loadComponent: () => import('./profile.component').then((m) => m.TenantProfileComponent) },
];
