import { Routes } from '@angular/router';

export const TENANT_MESSAGES_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./message-list.component').then((m) => m.TenantMessageListComponent) },
  { path: ':conversationId', loadComponent: () => import('./message-thread.component').then((m) => m.TenantMessageThreadComponent) },
];
