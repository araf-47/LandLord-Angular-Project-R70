import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MockDataService, periodKey } from '../../core/mock-data.service';

function firstOfMonth(): string {
  const [year, month] = periodKey().split('-');
  return `${year}-${month}-01`;
}

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

@Component({
  selector: 'app-ledger',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Ledger</h1>

    <div class="card">
      <div class="form-row">
        <div class="field">
          <label for="property">Property</label>
          <select id="property" name="property" [ngModel]="propertyFilter()" (ngModelChange)="propertyFilter.set($event)">
            <option value="">All properties</option>
            @for (p of data.properties(); track p.id) {
              <option [value]="p.id">{{ p.name }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="from">From</label>
          <input id="from" type="date" name="from" [ngModel]="fromDate()" (ngModelChange)="fromDate.set($event)" />
        </div>
        <div class="field">
          <label for="to">To</label>
          <input id="to" type="date" name="to" [ngModel]="toDate()" (ngModelChange)="toDate.set($event)" />
        </div>
      </div>
    </div>

    <div class="module-grid" style="margin-bottom:1rem;">
      <div class="card">
        <p class="hint-text">Total in</p>
        <h2 style="color:var(--success);">{{ totalIn() }}</h2>
      </div>
      <div class="card">
        <p class="hint-text">Total out</p>
        <h2 style="color:var(--danger);">{{ totalOut() }}</h2>
      </div>
      <div class="card">
        <p class="hint-text">Net</p>
        <h2>{{ net() }}</h2>
      </div>
    </div>

    <div class="card">
      <table>
        <thead><tr><th>Date</th><th>Type</th><th>Description</th><th>Amount</th><th>Balance</th></tr></thead>
        <tbody>
          @for (row of visibleRows(); track row.id) {
            <tr>
              <td>{{ row.date }}</td>
              <td><span class="badge" [class.badge-paid]="row.type === 'income'" [class.badge-unpaid]="row.type === 'expense'">{{ row.type }}</span></td>
              <td>{{ row.description }}</td>
              <td [style.color]="row.type === 'income' ? 'var(--success)' : 'var(--danger)'">
                {{ row.type === 'income' ? '+' : '-' }}{{ row.amount }}
              </td>
              <td>{{ row.balance }}</td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="hint-text">No transactions in this range.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class LedgerComponent {
  protected readonly data = inject(MockDataService);

  readonly propertyFilter = signal('');
  readonly fromDate = signal(firstOfMonth());
  readonly toDate = signal(today());

  /** Cumulative balance computed over the property-filtered ledger, oldest first,
   *  before the date range narrows what's displayed — so narrowing the date filter
   *  doesn't make the running balance reset to zero. */
  private readonly withBalance = computed(() => {
    let balance = 0;
    return this.data.ledgerEntries(this.propertyFilter() || undefined).map((entry) => {
      balance += entry.type === 'income' ? entry.amount : -entry.amount;
      return { ...entry, balance };
    });
  });

  readonly visibleRows = computed(() =>
    this.withBalance().filter((row) => row.date >= this.fromDate() && row.date <= this.toDate())
  );

  readonly totalIn = computed(() =>
    this.visibleRows()
      .filter((r) => r.type === 'income')
      .reduce((sum, r) => sum + r.amount, 0)
  );

  readonly totalOut = computed(() =>
    this.visibleRows()
      .filter((r) => r.type === 'expense')
      .reduce((sum, r) => sum + r.amount, 0)
  );

  readonly net = computed(() => this.totalIn() - this.totalOut());
}
