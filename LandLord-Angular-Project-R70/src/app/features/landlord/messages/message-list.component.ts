import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MessagingApiService } from '../../../core/messaging-api.service';

@Component({
  selector: 'app-landlord-message-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Messages</h1>
    <div class="card stack">
      @for (c of api.conversations(); track c.id) {
        <a class="module-tile" [routerLink]="['/landlord/messages', c.id]">
          <div class="module-title">{{ c.withName }}</div>
        </a>
      } @empty {
        <p class="hint-text">No conversations yet. Start one from a tenant's profile.</p>
      }
    </div>
  `,
})
export class LandlordMessageListComponent implements OnInit {
  protected readonly api = inject(MessagingApiService);

  async ngOnInit(): Promise<void> {
    await this.api.loadConversations();
  }
}
