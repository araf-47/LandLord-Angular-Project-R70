import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MockDataService, nextId } from '../../../core/mock-data.service';

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

          @if (asking()) {
            <div class="field">
              <label>Did repair cost money?</label>
              <div class="form-row">
                <button class="btn btn-sm" (click)="resolve(true)">Yes</button>
                <button class="btn btn-sm" (click)="resolve(false)">No</button>
              </div>
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
  readonly ticket = computed(() => this.data.tickets().find((t) => t.id === this.ticketId));

  askCost(): void {
    this.asking.set(true);
  }

  resolve(costMoney: boolean): void {
    this.data.tickets.update((list) => list.map((t) => (t.id === this.ticketId ? { ...t, status: 'resolved' } : t)));

    if (costMoney) {
      const t = this.ticket();
      this.data.expenses.update((list) => [
        ...list,
        { id: nextId('exp'), category: 'Maintenance', description: t?.description ?? '', amount: 0, tag: 'property' },
      ]);
    }
    this.router.navigateByUrl('/landlord/maintenance');
  }
}
