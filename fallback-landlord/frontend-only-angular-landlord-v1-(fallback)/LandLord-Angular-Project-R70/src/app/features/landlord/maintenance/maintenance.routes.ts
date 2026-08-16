import { Routes } from '@angular/router';

export const LANDLORD_MAINTENANCE_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./ticket-list.component').then((m) => m.LandlordTicketListComponent) },
  { path: 'new', loadComponent: () => import('./ticket-new.component').then((m) => m.LandlordTicketNewComponent) },
  { path: ':ticketId', loadComponent: () => import('./ticket-detail.component').then((m) => m.LandlordTicketDetailComponent) },
];
