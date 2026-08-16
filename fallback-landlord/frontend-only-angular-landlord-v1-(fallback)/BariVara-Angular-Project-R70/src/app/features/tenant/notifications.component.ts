import { Component, inject, signal } from '@angular/core';
import { MockDataService } from '../../core/mock-data.service';

@Component({
  selector: 'app-tenant-notifications',
  standalone: true,
  template: `
    <h1>Notifications</h1>
    <div class="card stack">
      @for (n of data.notifications(); track n.id) {
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
export class TenantNotificationsComponent {
  protected readonly data = inject(MockDataService);
  readonly opened = signal('');

  open(id: string): void {
    this.opened.set(this.opened() === id ? '' : id);
    this.data.notifications.update((list) => list.map((n) => (n.id === id ? { ...n, read: true } : n)));
  }

  remove(id: string): void {
    this.data.notifications.update((list) => list.filter((n) => n.id !== id));
  }
}
