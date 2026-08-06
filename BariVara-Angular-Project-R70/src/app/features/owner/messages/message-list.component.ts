import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-owner-message-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Messages</h1>
    <div class="card stack">
      @for (c of data.conversations(); track c.id) {
        <a class="module-tile" [routerLink]="['/owner/messages', c.id]">
          <div class="module-title">{{ c.withName }}</div>
          <p>{{ c.messages[c.messages.length - 1]?.text }}</p>
        </a>
      }
    </div>
  `,
})
export class OwnerMessageListComponent {
  protected readonly data = inject(MockDataService);
}
