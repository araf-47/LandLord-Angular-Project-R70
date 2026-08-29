import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { API_ORIGIN, MaintenanceApiService } from '../../../core/maintenance-api.service';

@Component({
  selector: 'app-landlord-ticket-detail',
  standalone: true,
  imports: [FormsModule],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading ticket…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        @if (ticket()) {
          <h1>Ticket — {{ ticket()!.description }}</h1>
          <div class="card stack" style="max-width:520px;">
            <p><strong>Status:</strong> {{ ticket()!.status }}</p>

            @if (ticket()!.photoUrl) {
              <img [src]="photoUrl()" alt="Ticket photo" style="max-width:100%;border-radius:8px;" />
            }

            @if (ticket()!.status === 'pending') {
              <button class="btn btn-primary" (click)="askCost()">Update status: Resolved</button>

              @if (asking() && !costingForm()) {
                <div class="field">
                  <label>Did repair cost money?</label>
                  <div class="form-row">
                    <button class="btn btn-sm" (click)="costingForm.set(true)">Yes</button>
                    <button class="btn btn-sm" (click)="resolve()">No</button>
                  </div>
                </div>
              }

              @if (costingForm()) {
                <div class="field">
                  <label for="amount">Amount</label>
                  <input id="amount" type="number" name="amount" [(ngModel)]="amount" />
                </div>
                <div class="field">
                  <label for="bearer">Who bears this?</label>
                  <select id="bearer" name="bearer" [(ngModel)]="bearer">
                    <option value="landlord">Landlord</option>
                    <option value="tenant">Tenant</option>
                  </select>
                </div>
                <div class="actions-row">
                  <button class="btn btn-primary" (click)="resolve()">Save &amp; resolve</button>
                </div>
              }
            }
          </div>
        }
      }
    }
  `,
})
export class LandlordTicketDetailComponent implements OnInit {
  private readonly api = inject(MaintenanceApiService);
  private readonly router = inject(Router);
  private readonly ticketId = Number(inject(ActivatedRoute).snapshot.paramMap.get('ticketId'));

  readonly asking = signal(false);
  readonly costingForm = signal(false);
  readonly ticket = computed(() => this.api.tickets().find((t) => t.id === this.ticketId));
  readonly photoUrl = computed(() => `${API_ORIGIN}${this.ticket()!.photoUrl}`);

  amount = 0;
  bearer: 'landlord' | 'tenant' = 'landlord';

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      if (this.api.tickets().length === 0) {
        await this.api.load();
      }
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  askCost(): void {
    this.asking.set(true);
  }

  async resolve(): Promise<void> {
    const cost = this.costingForm() && this.amount > 0 ? this.amount : undefined;
    await this.api.updateStatus(this.ticketId, 'resolved', cost, cost ? this.bearer : undefined);
    this.router.navigateByUrl('/landlord/maintenance');
  }
}
