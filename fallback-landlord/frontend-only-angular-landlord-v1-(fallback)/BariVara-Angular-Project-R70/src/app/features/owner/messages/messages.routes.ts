import { Routes } from '@angular/router';

export const OWNER_MESSAGES_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./message-list.component').then((m) => m.OwnerMessageListComponent) },
  { path: ':conversationId', loadComponent: () => import('./message-thread.component').then((m) => m.OwnerMessageThreadComponent) },
];
