import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MessagingApiService } from '../../../core/messaging-api.service';

@Component({
  selector: 'app-tenant-message-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Messages</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading conversations…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card stack">
          @for (c of api.conversations(); track c.id) {
            <a class="module-tile" [routerLink]="['/tenant/messages', c.id]">
              <div class="module-title">{{ c.withName }}</div>
            </a>
          } @empty {
            <p class="hint-text">No conversations yet.</p>
          }
        </div>
      }
    }
  `,
})
export class TenantMessageListComponent implements OnInit {
  protected readonly api = inject(MessagingApiService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    // No id needed: the backend derives "which tenant" from the auth token
    // for a TENANT-role caller and ignores any client-supplied id anyway.
    this.status.set('loading');
    try {
      await this.api.loadConversations();
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }
}
