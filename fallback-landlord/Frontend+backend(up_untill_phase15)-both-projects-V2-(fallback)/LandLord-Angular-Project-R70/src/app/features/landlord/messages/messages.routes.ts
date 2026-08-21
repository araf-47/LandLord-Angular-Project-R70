import { Routes } from '@angular/router';

export const LANDLORD_MESSAGES_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./message-list.component').then((m) => m.LandlordMessageListComponent) },
  { path: ':conversationId', loadComponent: () => import('./message-thread.component').then((m) => m.LandlordMessageThreadComponent) },
];
