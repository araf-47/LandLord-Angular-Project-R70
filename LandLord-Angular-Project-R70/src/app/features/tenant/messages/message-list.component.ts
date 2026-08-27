import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MessagingApiService } from '../../../core/messaging-api.service';

@Component({
  selector: 'app-tenant-message-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Messages</h1>
    <div class="card stack">
      @for (c of api.conversations(); track c.id) {
        <a class="module-tile" [routerLink]="['/tenant/messages', c.id]">
          <div class="module-title">{{ c.withName }}</div>
        </a>
      } @empty {
        <p class="hint-text">No conversations yet.</p>
      }
    </div>
  `,
})
export class TenantMessageListComponent implements OnInit {
  protected readonly api = inject(MessagingApiService);

  async ngOnInit(): Promise<void> {
    // No id needed: the backend derives "which tenant" from the auth token
    // for a TENANT-role caller and ignores any client-supplied id anyway.
    await this.api.loadConversations();
  }
}
