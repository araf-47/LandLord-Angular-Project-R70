import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiBookingRequest, BookingApiService } from '../../../core/booking-api.service';
import { ListingApiService } from '../../../core/listing-api.service';

@Component({
  selector: 'app-owner-request-detail',
  standalone: true,
  imports: [FormsModule],
  template: `
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
        <button class="btn btn-sm">Send</button>
      </div>

      @if (request()!.status === 'pending') {
        <div class="actions-row">
          <button class="btn btn-primary" (click)="decide('approved')">Approve, mark unit filled, take down ad</button>
          <button class="btn btn-danger" (click)="decide('rejected')">Reject, notify applicant</button>
        </div>
      }
    }
  `,
})
export class OwnerRequestDetailComponent {
  private readonly bookingApi = inject(BookingApiService);
  private readonly listingApi = inject(ListingApiService);
  private readonly router = inject(Router);
  private readonly requestId = Number(inject(ActivatedRoute).snapshot.paramMap.get('requestId'));

  chatMessage = '';
  readonly request = signal<ApiBookingRequest | undefined>(undefined);
  private listingTitleValue = '—';

  constructor() {
    this.bookingApi.get(this.requestId).then(async (r) => {
      this.request.set(r);
      const listing = await this.listingApi.get(r.listingId);
      this.listingTitleValue = listing.title;
    });
  }

  listingTitle(): string {
    return this.listingTitleValue;
  }

  async decide(status: 'approved' | 'rejected'): Promise<void> {
    await this.bookingApi.decide(this.requestId, status);
    this.router.navigateByUrl('/owner/requests');
  }
}
