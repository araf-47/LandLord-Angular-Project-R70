import { SlicePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiMessage, MessagingApiService } from '../../../core/messaging-api.service';

const POLL_MS = 10000;

@Component({
  selector: 'app-tenant-message-thread',
  standalone: true,
  imports: [FormsModule, SlicePipe],
  template: `
    @if (conversation()) {
      <h1>{{ conversation()!.withName }}</h1>
      <div class="card stack">
        @for (m of messages(); track m.id) {
          <div><strong>{{ m.senderRole === 'tenant' ? 'You' : conversation()!.withName }}:</strong> {{ m.text }} <span class="hint-text">({{ m.sentAt | slice: 0 : 10 }})</span></div>
        } @empty {
          <p class="hint-text">No messages yet.</p>
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
export class TenantMessageThreadComponent implements OnInit, OnDestroy {
  private readonly api = inject(MessagingApiService);
  private readonly conversationId = Number(inject(ActivatedRoute).snapshot.paramMap.get('conversationId'));
  private pollHandle?: ReturnType<typeof setInterval>;

  reply = '';
  readonly conversation = computed(() => this.api.conversations().find((c) => c.id === this.conversationId));
  readonly messages = signal<ApiMessage[]>([]);

  async ngOnInit(): Promise<void> {
    if (!this.api.conversations().length) {
      await this.api.loadConversations();
    }
    await this.refresh();
    this.pollHandle = setInterval(() => this.refresh(), POLL_MS);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) clearInterval(this.pollHandle);
  }

  async refresh(): Promise<void> {
    this.messages.set(await this.api.messagesFor(this.conversationId));
  }

  async send(): Promise<void> {
    if (!this.reply) return;
    await this.api.sendMessage(this.conversationId, 'tenant', this.reply);
    this.reply = '';
    await this.refresh();
  }
}
