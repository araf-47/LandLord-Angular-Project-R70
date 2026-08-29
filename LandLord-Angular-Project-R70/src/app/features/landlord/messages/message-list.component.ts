import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MessagingApiService } from '../../../core/messaging-api.service';

@Component({
  selector: 'app-landlord-message-list',
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
            <a class="module-tile" [routerLink]="['/landlord/messages', c.id]">
              <div class="module-title">{{ c.withName }}</div>
            </a>
          } @empty {
            <p class="hint-text">No conversations yet. Start one from a tenant's profile.</p>
          }
        </div>
      }
    }
  `,
})
export class LandlordMessageListComponent implements OnInit {
  protected readonly api = inject(MessagingApiService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
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
