import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MockDataService, periodLabel } from '../../../core/mock-data.service';

@Component({
  selector: 'app-tenant-detail',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (tenant()) {
      <h1>{{ tenant()!.name }}</h1>
      <div class="card">
        <p><strong>Phone:</strong> {{ tenant()!.phone }}</p>
        <p><strong>Email:</strong> {{ tenant()!.email }}</p>
        <p><strong>Unit:</strong> {{ unitLabel() }}</p>
        <p><strong>Status:</strong> {{ tenant()!.status }}</p>
      </div>

      @if (agreement()) {
        <div class="card">
          <h3>Rental agreement</h3>
          @if (!editing()) {
            <p><strong>Terms:</strong> {{ agreement()!.terms }}</p>
            <p><strong>Deposit:</strong> {{ agreement()!.deposit }}</p>
            <p><strong>Start date:</strong> {{ agreement()!.startDate }}</p>
            <button class="btn" (click)="editing.set(true)">Edit lease agreement</button>
          } @else {
            <div class="field">
              <label for="terms">Terms</label>
              <input id="terms" name="terms" [(ngModel)]="termsDraft" />
            </div>
            <div class="actions-row">
              <button class="btn btn-primary" (click)="saveTerms()">Save changes</button>
              <button class="btn" (click)="editing.set(false)">Cancel</button>
            </div>
          }
        </div>
      }

      <div class="card">
        <h3>Billing history</h3>
        <table>
          <thead><tr><th>Month</th><th>Rent</th><th>Utilities</th><th>Rolled over</th><th>Total</th><th>Status</th></tr></thead>
          <tbody>
            @for (i of billingHistory(); track i.id) {
              <tr>
                <td>{{ monthLabel(i.period) }}</td>
                <td>{{ i.rent }}</td>
                <td>{{ i.utilities }}</td>
                <td>{{ i.prevUnpaidRolled }}</td>
                <td>{{ i.amount }}</td>
                <td><span class="badge" [class.badge-unpaid]="i.status !== 'paid'" [class.badge-paid]="i.status === 'paid'">{{ i.status }}</span></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="hint-text">No bills yet.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class TenantDetailComponent {
  private readonly data = inject(MockDataService);
  private readonly tenantId = inject(ActivatedRoute).snapshot.paramMap.get('tenantId')!;

  readonly editing = signal(false);
  termsDraft = '';

  readonly tenant = computed(() => this.data.tenants().find((t) => t.id === this.tenantId));
  readonly agreement = computed(() => this.data.agreements().find((a) => a.tenantId === this.tenantId));
  readonly billingHistory = computed(() => this.data.invoicesForTenant(this.tenantId));

  constructor() {
    const a = this.agreement();
    if (a) this.termsDraft = a.terms;
  }

  monthLabel(period: string): string {
    return periodLabel(period);
  }

  unitLabel(): string {
    return this.data.units().find((u) => u.id === this.tenant()?.unitId)?.unitNumber ?? '—';
  }

  saveTerms(): void {
    const a = this.agreement();
    if (!a) return;
    this.data.agreements.update((list) => list.map((x) => (x.id === a.id ? { ...x, terms: this.termsDraft } : x)));
    this.editing.set(false);
  }
}
