import { Component, OnInit, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MarketplaceApiService } from '../../../core/marketplace-api.service';
import { UnitApiService } from '../../../core/unit-api.service';

@Component({
  selector: 'app-request-detail',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    @if (request()) {
      <h1>Request — {{ request()!.applicantName }}</h1>
      <div class="card">
        <p><strong>Unit requested:</strong> {{ unitLabel() }}</p>
        <p><strong>Status:</strong> {{ request()!.status }}</p>
        @if (request()!.tenantId) {
          <a [routerLink]="['/landlord/tenants', request()!.tenantId]">View tenant profile</a>
        }
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
          <button class="btn btn-primary" (click)="decide('approved')">Approve, notify applicant</button>
          <button class="btn btn-danger" (click)="decide('rejected')">Reject, notify applicant</button>
        </div>
      }
    }
  `,
})
export class RequestDetailComponent implements OnInit {
  private readonly marketplaceApi = inject(MarketplaceApiService);
  private readonly unitApi = inject(UnitApiService);
  private readonly router = inject(Router);
  private readonly requestId = Number(inject(ActivatedRoute).snapshot.paramMap.get('requestId'));

  chatMessage = '';
  readonly request = computed(() => this.marketplaceApi.requests().find((r) => r.id === this.requestId));

  async ngOnInit(): Promise<void> {
    await Promise.all([this.marketplaceApi.load(), this.unitApi.load()]);
  }

  unitLabel(): string {
    return this.unitApi.units().find((u) => u.id === this.request()?.unitId)?.unitNumber ?? '—';
  }

  async decide(status: 'approved' | 'rejected'): Promise<void> {
    await this.marketplaceApi.decide(this.requestId, status);
    this.router.navigateByUrl('/landlord/marketplace/requests');
  }
}
