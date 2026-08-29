import { SlicePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiMessage, MessagingApiService } from '../../../core/messaging-api.service';
import { ToastService } from '../../../shared/toast.service';

const POLL_MS = 10000;

@Component({
  selector: 'app-tenant-message-thread',
  standalone: true,
  imports: [FormsModule, SlicePipe],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading conversation…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
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
        <button class="btn btn-primary" [disabled]="sending()" (click)="send()">{{ sending() ? 'Sending…' : 'Send' }}</button>
      </div>
    }
      }
    }
  `,
})
export class TenantMessageThreadComponent implements OnInit, OnDestroy {
  private readonly api = inject(MessagingApiService);
  private readonly toast = inject(ToastService);
  private readonly conversationId = Number(inject(ActivatedRoute).snapshot.paramMap.get('conversationId'));
  private pollHandle?: ReturnType<typeof setInterval>;

  reply = '';
  readonly conversation = computed(() => this.api.conversations().find((c) => c.id === this.conversationId));
  readonly messages = signal<ApiMessage[]>([]);
  readonly sending = signal(false);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      if (!this.api.conversations().length) {
        await this.api.loadConversations();
      }
      await this.refresh();
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
      return;
    }
    this.pollHandle = setInterval(() => this.refresh(), POLL_MS);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) clearInterval(this.pollHandle);
  }

  async refresh(): Promise<void> {
    this.messages.set(await this.api.messagesFor(this.conversationId));
  }

  async send(): Promise<void> {
    if (!this.reply || this.sending()) return;
    this.sending.set(true);
    try {
      await this.api.sendMessage(this.conversationId, 'tenant', this.reply);
      this.reply = '';
      await this.refresh();
    } catch {
      this.toast.error("Couldn't send your message. Please try again.");
    } finally {
      this.sending.set(false);
    }
  }
}
