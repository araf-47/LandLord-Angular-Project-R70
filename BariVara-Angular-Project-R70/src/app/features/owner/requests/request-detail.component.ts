import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MockDataService } from '../../../core/mock-data.service';

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
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);
  private readonly requestId = inject(ActivatedRoute).snapshot.paramMap.get('requestId')!;

  chatMessage = '';
  readonly request = computed(() => this.data.bookingRequests().find((r) => r.id === this.requestId));

  listingTitle(): string {
    return this.data.listingById(this.request()?.listingId ?? '')?.title ?? '—';
  }

  decide(status: 'approved' | 'rejected'): void {
    const req = this.request();
    this.data.bookingRequests.update((list) => list.map((r) => (r.id === this.requestId ? { ...r, status } : r)));

    if (status === 'approved' && req) {
      this.data.listings.update((list) => list.map((l) => (l.id === req.listingId ? { ...l, status: 'taken' } : l)));
    }
    this.router.navigateByUrl('/owner/requests');
  }
}
