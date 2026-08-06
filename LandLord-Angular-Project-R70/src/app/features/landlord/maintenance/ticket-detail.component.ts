import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ExpenseRecord, MockDataService, nextId } from '../../../core/mock-data.service';

@Component({
  selector: 'app-landlord-ticket-detail',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (ticket()) {
      <h1>Ticket — {{ ticket()!.description }}</h1>
      <div class="card stack" style="max-width:520px;">
        <p><strong>Status:</strong> {{ ticket()!.status }}</p>

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
  `,
})
export class LandlordTicketDetailComponent {
  private readonly data = inject(MockDataService);
  private readonly router = inject(Router);
  private readonly ticketId = inject(ActivatedRoute).snapshot.paramMap.get('ticketId')!;

  readonly asking = signal(false);
  readonly costingForm = signal(false);
  readonly ticket = computed(() => this.data.tickets().find((t) => t.id === this.ticketId));

  amount = 0;
  bearer: ExpenseRecord['bearer'] = 'landlord';

  askCost(): void {
    this.asking.set(true);
  }

  resolve(): void {
    this.data.tickets.update((list) => list.map((t) => (t.id === this.ticketId ? { ...t, status: 'resolved' } : t)));

    if (this.costingForm() && this.amount > 0) {
      const t = this.ticket();
      const propertyId = this.data.units().find((u) => u.id === t?.unitId)?.propertyId ?? '';
      this.data.expenses.update((list) => [
        ...list,
        {
          id: nextId('exp'),
          propertyId,
          category: 'Maintenance',
          description: t?.description ?? '',
          amount: this.amount,
          bearer: this.bearer,
          tenantId: t?.tenantId,
          date: new Date().toISOString().slice(0, 10),
        },
      ]);
    }
    this.router.navigateByUrl('/landlord/maintenance');
  }
}
