import { Component, OnInit, inject, signal } from '@angular/core';
import { MessagingApiService } from '../../core/messaging-api.service';

@Component({
  selector: 'app-tenant-notifications',
  standalone: true,
  template: `
    <h1>Notifications</h1>
    <div class="card stack">
      @for (n of api.notifications(); track n.id) {
        <div class="card" (click)="open(n.id)" style="cursor:pointer;">
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
  `,
})
export class TenantNotificationsComponent implements OnInit {
  protected readonly api = inject(MessagingApiService);
  readonly opened = signal(-1);

  async ngOnInit(): Promise<void> {
    // No id needed: the backend derives "which tenant" from the auth token
    // for a TENANT-role caller and ignores any client-supplied id anyway.
    await this.api.loadNotifications();
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
