import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiInvoice, BillingApiService } from '../../../core/billing-api.service';
import { periodKey, periodLabel } from '../../../core/mock-data.service';
import { ApiTenant, TenantApiService } from '../../../core/tenant-api.service';

@Component({
  selector: 'app-generate-bills',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Monthly Bills</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading bills…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card">
          <p class="hint-text">{{ label(currentPeriod) }} — bills generate against the current month only.</p>
          <button class="btn btn-primary" (click)="generate()">Generate bills for tenants missing one this month</button>
        </div>

        <div class="card">
          <div class="table-scroll">
          <table>
            <thead>
              <tr><th>Tenant</th><th>Rent</th><th>Utilities</th><th>Rolled over</th><th>Total due</th><th>Status</th></tr>
            </thead>
            <tbody>
              @for (i of rows(); track i.id) {
                <tr>
                  <td>{{ tenantName(i.tenantId) }}</td>
                  <td>{{ i.rent }}</td>
                  <td>{{ i.utilitiesTotal }}</td>
                  <td>{{ i.prevUnpaidRolled }}</td>
                  <td>
                    @if (i.status === 'partial') {
                      {{ i.balance }}/{{ i.amount }}
                    } @else {
                      {{ i.balance }}
                    }
                  </td>
                  <td><span class="badge" [class.badge-unpaid]="i.status === 'unpaid'" [class.badge-partial]="i.status === 'partial'" [class.badge-paid]="i.status === 'paid'">{{ i.status }}</span></td>
                </tr>
              } @empty {
                <tr><td colspan="6" class="hint-text">No bills for this month yet.</td></tr>
              }
            </tbody>
          </table>
          </div>
        </div>
      }
    }
  `,
})
export class GenerateBillsComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly tenantApi = inject(TenantApiService);

  readonly currentPeriod = periodKey();
  readonly invoices = signal<ApiInvoice[]>([]);
  readonly tenants = signal<ApiTenant[]>([]);

  readonly rows = computed(() =>
    this.invoices()
      .filter((i) => i.period === this.currentPeriod)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id)
  );

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await this.tenantApi.load();
      this.tenants.set(this.tenantApi.tenants());
      await this.refresh();
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  private async refresh(): Promise<void> {
    this.invoices.set(await this.billingApi.invoicesForPeriod(this.currentPeriod));
  }

  label(period: string): string {
    return periodLabel(period);
  }

  tenantName(tenantId: number): string {
    return this.tenants().find((t) => t.id === tenantId)?.name ?? '—';
  }

  async generate(): Promise<void> {
    const billed = new Set(this.invoices().map((i) => i.tenantId));
    const due = this.tenants().filter((t) => t.status === 'active' && t.unitId != null && !billed.has(t.id));
    for (const tenant of due) {
      await this.billingApi.generateInvoice(tenant.id);
    }
    await this.refresh();
  }
}
