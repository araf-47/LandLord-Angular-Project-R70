import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiBookingRequest, BookingApiService } from '../../../core/booking-api.service';
import { ListingApiService } from '../../../core/listing-api.service';
import { MockDataService } from '../../../core/mock-data.service';

@Component({
  selector: 'app-owner-request-detail',
  standalone: true,
  imports: [FormsModule],
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
            <p><strong>Listing:</strong> {{ listingTitle() }}</p>
            <p><strong>Status:</strong> {{ request()!.status }}</p>
          </div>

          <div class="card">
            <h3>Chat with applicant (optional)</h3>
            <div class="field">
              <textarea rows="3" name="chat" [(ngModel)]="chatMessage" placeholder="Write a message..."></textarea>
            </div>
            @if (chatError) {
              <p class="hint-text text-danger">{{ chatError }}</p>
            }
            <button class="btn btn-sm" (click)="sendChat()">Send</button>
          </div>

          @if (request()!.status === 'pending') {
            <div class="actions-row">
              <button class="btn btn-primary" (click)="decide('approved')">Approve, mark unit filled, take down ad</button>
              <button class="btn btn-danger" (click)="decide('rejected')">Reject, notify applicant</button>
            </div>
          }
        }
      }
    }
  `,
})
export class OwnerRequestDetailComponent implements OnInit {
  private readonly bookingApi = inject(BookingApiService);
  private readonly listingApi = inject(ListingApiService);
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);
  private readonly requestId = Number(inject(ActivatedRoute).snapshot.paramMap.get('requestId'));

  chatMessage = '';
  chatError = '';
  readonly request = signal<ApiBookingRequest | undefined>(undefined);
  private listingTitleValue = '—';

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const r = await this.bookingApi.get(this.requestId);
      this.request.set(r);
      const listing = await this.listingApi.get(r.listingId);
      this.listingTitleValue = listing.title;
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  listingTitle(): string {
    return this.listingTitleValue;
  }

  async decide(status: 'approved' | 'rejected'): Promise<void> {
    await this.bookingApi.decide(this.requestId, status);
    this.router.navigateByUrl('/owner/requests');
  }

  sendChat(): void {
    const text = this.chatMessage.trim();
    if (!text) {
      this.chatError = 'Write a message first.';
      return;
    }
    const req = this.request()!;

    const conversation = this.data.findOrCreateConversation(String(req.id), req.applicantName);
    this.data.conversations.update((list) =>
      list.map((c) =>
        c.id === conversation.id
          ? { ...c, messages: [...c.messages, { from: 'You', text, date: new Date().toISOString().slice(0, 10) }] }
          : c
      )
    );
    this.router.navigate(['/owner/messages', conversation.id]);
  }
}
