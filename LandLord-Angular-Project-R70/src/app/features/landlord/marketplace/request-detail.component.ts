import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MarketplaceApiService } from '../../../core/marketplace-api.service';
import { MessagingApiService } from '../../../core/messaging-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading request…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        @if (request()) {
          <h1>Request — {{ request()!.applicantName }}</h1>
          <div class="card">
            <p><strong>Unit requested:</strong> {{ unitLabel() }}</p>
            <p><strong>Status:</strong> {{ request()!.status }}</p>
            @if (request()!.barivaraTenantId) {
              <p class="hint-text">Synced from a BariVara.com booking request.</p>
            }
            @if (request()!.message) {
              <p><strong>Message:</strong> {{ request()!.message }}</p>
            }
            @if (request()!.tenantId) {
              <a [routerLink]="['/landlord/tenants', request()!.tenantId]">View tenant profile</a>
            }
          </div>

          <div class="card">
            <h3>Chat with applicant (optional)</h3>
            <div class="field">
              <textarea rows="3" name="chat" [(ngModel)]="chatMessage" placeholder="Write a message..."></textarea>
            </div>
            @if (chatError) {
              <p class="hint-text" style="color:var(--color-danger, #c0392b);">{{ chatError }}</p>
            }
            <button class="btn btn-sm" (click)="sendChat()">Send</button>
          </div>

          @if (request()!.status === 'pending') {
            <div class="actions-row">
              <button class="btn btn-primary" (click)="decide('approved')">Approve, notify applicant</button>
              <button class="btn btn-danger" (click)="decide('rejected')">Reject, notify applicant</button>
            </div>
          }
        }
      }
    }
  `,
})
export class RequestDetailComponent implements OnInit {
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly unitApi = inject(UnitApiService);
  private readonly messagingApi = inject(MessagingApiService);
  private readonly router = inject(Router);
  private readonly requestId = Number(inject(ActivatedRoute).snapshot.paramMap.get('requestId'));

  chatMessage = '';
  chatError = '';
  readonly request = computed(() => this.marketplaceApi.requests().find((r) => r.id === this.requestId));

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await Promise.all([this.marketplaceApi.load(), this.unitApi.load()]);
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  unitLabel(): string {
    return this.unitApi.units().find((u) => u.id === this.request()?.unitId)?.unitNumber ?? '—';
  }

  async decide(status: 'approved' | 'rejected'): Promise<void> {
    await this.marketplaceApi.decide(this.requestId, status);
    this.router.navigateByUrl('/landlord/marketplace/requests');
  }

  async sendChat(): Promise<void> {
    const text = this.chatMessage.trim();
    if (!text) {
      this.chatError = 'Write a message first.';
      return;
    }
    const req = this.request()!;

    let conversation;
    if (req.tenantId) {
      await this.messagingApi.loadConversationsForTenant(req.tenantId);
      conversation = this.messagingApi.conversations()[0];
    }
    if (!conversation) {
      conversation = await this.messagingApi.createConversation(req.tenantId, req.applicantName);
    }

    await this.messagingApi.sendMessage(conversation.id, 'landlord', text);
    this.router.navigate(['/landlord/messages', conversation.id]);
  }
}
