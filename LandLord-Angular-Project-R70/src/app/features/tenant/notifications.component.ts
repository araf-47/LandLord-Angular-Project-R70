import { Component, OnInit, inject, signal } from '@angular/core';
import { MessagingApiService } from '../../core/messaging-api.service';

@Component({
  selector: 'app-tenant-notifications',
  standalone: true,
  template: `
    <h1>Notifications</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading notifications…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card stack">
          @for (n of api.notifications(); track n.id) {
            <div class="card cursor-pointer" (click)="open(n.id)">
              <strong>{{ n.title }}</strong>
              @if (opened() === n.id) {
                <p>{{ n.body }}</p>
                <button class="btn btn-sm btn-danger" (click)="remove(n.id); $event.stopPropagation()">Delete</button>
              }
            </div>
          } @empty {
            <p class="hint-text">No notifications.</p>
          }
        </div>
      }
    }
  `,
})
export class TenantNotificationsComponent implements OnInit {
  protected readonly api = inject(MessagingApiService);
  readonly opened = signal(-1);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    // No id needed: the backend derives "which tenant" from the auth token
    // for a TENANT-role caller and ignores any client-supplied id anyway.
    this.status.set('loading');
    try {
      await this.api.loadNotifications();
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  async open(id: number): Promise<void> {
    const wasOpen = this.opened() === id;
    this.opened.set(wasOpen ? -1 : id);
    if (!wasOpen) {
      await this.api.markNotificationRead(id);
    }
  }

  async remove(id: number): Promise<void> {
    await this.api.deleteNotification(id);
  }
}
