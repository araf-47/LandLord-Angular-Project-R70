import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MockDataService, periodLabel } from '../../../core/mock-data.service';

@Component({
  selector: 'app-generate-bills',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Monthly Bills</h1>
    <div class="card">
      <div class="field" style="max-width:280px;">
        <label for="period">Month</label>
        <select id="period" name="period" [ngModel]="selectedPeriod()" (ngModelChange)="selectedPeriod.set($event)">
          @for (p of data.knownPeriods(); track p) {
            <option [value]="p">{{ label(p) }}</option>
          }
        </select>
      </div>

      @if (selectedPeriod() === data.currentPeriod()) {
        <p class="hint-text">
          Bills for the current month are generated automatically. Use this to pick up any tenant who
          became active after the month started.
        </p>
        <button class="btn btn-primary" (click)="generate()">Generate bills for this month</button>
      } @else {
        <p class="hint-text">Past months are read-only history.</p>
      }
    </div>

    <div class="card">
      <table>
        <thead>
          <tr><th>Tenant</th><th>Rent</th><th>Utilities</th><th>Rolled over</th><th>Total due</th><th>Status</th></tr>
        </thead>
        <tbody>
          @for (i of rows(); track i.id) {
            <tr>
              <td>{{ tenantName(i.tenantId) }}</td>
              <td>{{ i.rent }}</td>
              <td>{{ i.utilities }}</td>
              <td>{{ i.prevUnpaidRolled }}</td>
              <td>{{ i.amount }}</td>
              <td><span class="badge" [class.badge-unpaid]="i.status !== 'paid'" [class.badge-paid]="i.status === 'paid'">{{ i.status }}</span></td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="hint-text">No bills for this month yet.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class GenerateBillsComponent {
  protected readonly data = inject(MockDataService);

  readonly selectedPeriod = signal(this.data.currentPeriod());
  readonly rows = computed(() => this.data.invoicesForPeriod(this.selectedPeriod()));

  constructor() {
    // Frontend stand-in for the monthly cron job (Part 2): make sure the
    // current month is never empty just because nobody clicked "generate".
    this.data.ensureBillsGenerated(this.data.currentPeriod());
  }

  label(period: string): string {
    return periodLabel(period);
  }

  tenantName(tenantId: string): string {
    return this.data.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }

  generate(): void {
    this.data.ensureBillsGenerated(this.selectedPeriod());
  }
}
