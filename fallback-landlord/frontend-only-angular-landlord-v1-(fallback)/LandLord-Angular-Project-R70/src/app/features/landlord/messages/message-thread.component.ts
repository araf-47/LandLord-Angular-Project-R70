import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-landlord-message-thread',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (conversation()) {
      <h1>{{ conversation()!.withName }}</h1>
      <div class="card stack">
        @for (m of conversation()!.messages; track $index) {
          <div><strong>{{ m.from }}:</strong> {{ m.text }} <span class="hint-text">({{ m.date }})</span></div>
        }
      </div>
      <div class="card">
        <div class="field">
          <textarea rows="2" name="reply" [(ngModel)]="reply" placeholder="Send / reply message"></textarea>
        </div>
        <button class="btn btn-primary" (click)="send()">Send</button>
      </div>
    }
  `,
})
export class LandlordMessageThreadComponent {
  private readonly data = inject(MockDataService);
  private readonly conversationId = inject(ActivatedRoute).snapshot.paramMap.get('conversationId')!;

  reply = '';
  readonly conversation = computed(() => this.data.conversations().find((c) => c.id === this.conversationId));

  send(): void {
    if (!this.reply) return;
    this.data.conversations.update((list) =>
      list.map((c) =>
        c.id === this.conversationId
          ? { ...c, messages: [...c.messages, { from: 'You', text: this.reply, date: new Date().toISOString().slice(0, 10) }] }
          : c
      )
    );
    this.reply = '';
  }
}
