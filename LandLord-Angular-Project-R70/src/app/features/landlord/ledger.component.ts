import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { periodKey } from '../../core/mock-data.service';
import { BillingApiService } from '../../core/billing-api.service';
import { MaintenanceApiService } from '../../core/maintenance-api.service';
import { TenantApiService } from '../../core/tenant-api.service';
import { UnitApiService } from '../../core/unit-api.service';
import { PropertyApiService, ApiProperty } from '../../core/property-api.service';
import { StatTileComponent } from '../../shared/stat-tile.component';

interface LedgerEntry {
  id: string;
  date: string;
  type: 'income' | 'expense';
  description: string;
  propertyId: number | undefined;
  amount: number;
}

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
  imports: [FormsModule, StatTileComponent],
  template: `
    <h1>Ledger</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading ledger…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load the ledger. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card">
          <div class="form-row">
            <div class="field">
              <label for="property">Property</label>
              <select id="property" name="property" [ngModel]="propertyFilter()" (ngModelChange)="propertyFilter.set($event)">
                <option value="">All properties</option>
                @for (p of properties(); track p.id) {
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

        <div class="module-grid mb-md">
          <app-stat-tile label="Total in" [value]="totalIn()" color="success" />
          <app-stat-tile label="Total out" [value]="totalOut()" color="danger" />
          <app-stat-tile label="Net" [value]="net()" />
        </div>

        <div class="card">
          <div class="table-scroll">
          <table>
            <thead><tr><th>Date</th><th>Type</th><th>Description</th><th>Amount</th><th>Balance</th></tr></thead>
            <tbody>
              @for (row of visibleRows(); track row.id) {
                <tr [class.row-income]="row.type === 'income'" [class.row-expense]="row.type === 'expense'">
                  <td>{{ row.date }}</td>
                  <td><span class="badge" [class.badge-paid]="row.type === 'income'" [class.badge-unpaid]="row.type === 'expense'">{{ row.type }}</span></td>
                  <td>{{ row.description }}</td>
                  <td [class.text-success]="row.type === 'income'" [class.text-danger]="row.type === 'expense'">
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
        </div>
      }
    }
  `,
})
export class LedgerComponent implements OnInit {
  private readonly billing = inject(BillingApiService);
  private readonly maintenance = inject(MaintenanceApiService);
  private readonly tenantApi = inject(TenantApiService);
  private readonly unitApi = inject(UnitApiService);
  private readonly propertyApi = inject(PropertyApiService);

  readonly properties = signal<ApiProperty[]>([]);
  readonly entries = signal<LedgerEntry[]>([]);

  readonly propertyFilter = signal('');
  readonly fromDate = signal(firstOfMonth());
  readonly toDate = signal(today());

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await Promise.all([this.propertyApi.load(), this.unitApi.load(), this.tenantApi.load()]);
      this.properties.set(this.propertyApi.properties());

      const units = this.unitApi.units();
      const unitPropertyId = new Map(units.map((u) => [u.id, u.propertyId]));

      const tenants = this.tenantApi.tenants();
      const tenantName = new Map(tenants.map((t) => [t.id, t.name]));

      const tenantUnitId = new Map<number, number>();
      for (const t of tenants) {
        if (t.unitId != null) tenantUnitId.set(t.id, t.unitId);
      }
      const orphanTenants = tenants.filter((t) => t.unitId == null);
      const agreements = await Promise.all(orphanTenants.map((t) => this.tenantApi.agreementFor(t.id)));
      agreements.forEach((agreement, i) => {
        if (agreement) tenantUnitId.set(orphanTenants[i].id, agreement.unitId);
      });

      const propertyIdForTenant = (tenantId: number) => {
        const unitId = tenantUnitId.get(tenantId);
        return unitId != null ? unitPropertyId.get(unitId) : undefined;
      };

      const [payments, expenses] = await Promise.all([this.billing.allPayments(), this.maintenance.allExpenses()]);

      const income: LedgerEntry[] = payments
        .filter((p) => p.status === 'confirmed')
        .map((p) => ({
          id: `p-${p.id}`,
          date: p.date.slice(0, 10),
          type: 'income' as const,
          description: `Payment — ${tenantName.get(p.tenantId) ?? 'Tenant'}`,
          propertyId: propertyIdForTenant(p.tenantId),
          amount: p.amount,
        }));

      const outflow: LedgerEntry[] = expenses.map((e) => ({
        id: `e-${e.id}`,
        date: e.date,
        type: 'expense' as const,
        description: e.description ? `${e.category} — ${e.description}` : e.category,
        propertyId: e.propertyId ?? undefined,
        amount: e.amount,
      }));

      this.entries.set([...income, ...outflow].sort((a, b) => a.date.localeCompare(b.date)));
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  /** Cumulative balance computed oldest-first (so each row's balance is correct
   *  running-total-to-date), then reversed so the table displays newest first —
   *  top row's balance always matches the Net card above. */
  readonly visibleRows = computed(() => {
    let balance = 0;
    const propertyFilter = this.propertyFilter();
    return this.entries()
      .filter((entry) => !propertyFilter || String(entry.propertyId) === propertyFilter)
      .filter((entry) => entry.date >= this.fromDate() && entry.date <= this.toDate())
      .map((entry) => {
        balance += entry.type === 'income' ? entry.amount : -entry.amount;
        return { ...entry, balance };
      })
      .reverse();
  });

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
