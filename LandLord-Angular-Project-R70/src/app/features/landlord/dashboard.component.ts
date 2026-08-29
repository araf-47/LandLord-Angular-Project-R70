import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MaintenanceApiService } from '../../core/maintenance-api.service';
import { BillingApiService } from '../../core/billing-api.service';
import { UnitApiService } from '../../core/unit-api.service';
import { periodKey, periodLabel } from '../../core/mock-data.service';

@Component({
  selector: 'app-landlord-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading overview…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load this page. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <h1>{{ currentPeriodLabel() }} overview</h1>
        <div class="module-grid" style="margin-bottom:2rem;">
          <div class="card">
            <p class="hint-text">Occupancy</p>
            <h2>{{ occupancy().occupied }}/{{ occupancy().total }}</h2>
          </div>
          <div class="card">
            <p class="hint-text">Collected this month</p>
            <h2 style="color:var(--success);">{{ collected() }}</h2>
          </div>
          <div class="card">
            <p class="hint-text">Outstanding this month</p>
            <h2 style="color:var(--danger);">{{ outstanding() }}</h2>
          </div>
          <div class="card">
            <p class="hint-text">Net this month</p>
            <h2>{{ net() }}</h2>
          </div>
          <div class="card">
            <p class="hint-text">Pending maintenance</p>
            <h2>{{ pendingMaintenance() }}</h2>
          </div>
        </div>

        <h1>Manage your property</h1>
        <div class="module-grid">
          @for (m of modules; track m.link) {
            <a class="module-tile" [routerLink]="m.link">
              <div class="module-title">{{ m.title }}</div>
              <p>{{ m.desc }}</p>
            </a>
          }
        </div>
      }
    }
  `,
})
export class LandlordDashboardComponent implements OnInit {
  private readonly maintenanceApi = inject(MaintenanceApiService);
  private readonly billingApi = inject(BillingApiService);
  private readonly unitApi = inject(UnitApiService);

  private readonly period = periodKey();

  readonly occupancy = signal({ occupied: 0, total: 0 });
  readonly collected = signal(0);
  readonly outstanding = signal(0);
  private readonly expensesThisPeriod = signal(0);

  readonly pendingMaintenance = () => this.maintenanceApi.tickets().filter((t) => t.status === 'pending').length;
  readonly net = () => this.collected() - this.expensesThisPeriod();

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      const [, expenses, payments, invoices] = await Promise.all([
        this.maintenanceApi.load(),
        this.maintenanceApi.allExpenses(),
        this.billingApi.allPayments(),
        this.billingApi.invoicesForPeriod(this.period),
      ]);

      this.expensesThisPeriod.set(expenses.filter((e) => e.date.startsWith(this.period)).reduce((sum, e) => sum + e.amount, 0));

      this.collected.set(
        payments
          .filter((p) => p.status === 'confirmed' && p.date.startsWith(this.period))
          .reduce((sum, p) => sum + p.amount, 0)
      );

      this.outstanding.set(invoices.filter((i) => i.status !== 'paid').reduce((sum, i) => sum + i.balance, 0));

      await this.unitApi.load();
      const units = this.unitApi.units();
      this.occupancy.set({ occupied: units.filter((u) => u.status === 'occupied').length, total: units.length });
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  currentPeriodLabel(): string {
    return periodLabel(this.period);
  }

  readonly modules = [
    { title: 'Property & Units', desc: 'Manage properties and unit status.', link: '/landlord/properties' },
    { title: 'Tenant Management', desc: 'Register, view, and move out tenants.', link: '/landlord/tenants' },
    { title: 'Marketplace & Leads', desc: 'Ads and booking requests.', link: '/landlord/marketplace' },
    { title: 'Rental Agreements', desc: 'View and edit lease terms.', link: '/landlord/rentals' },
    { title: 'Payments', desc: 'Generate bills, receive payments.', link: '/landlord/payments' },
    { title: 'Expenses', desc: 'Track property and tenant expenses.', link: '/landlord/expenses' },
    { title: 'Ledger', desc: 'All money in and out, one cash book.', link: '/landlord/ledger' },
    { title: 'Maintenance', desc: 'Log and resolve issues.', link: '/landlord/maintenance' },
    { title: 'Messages', desc: 'Chat with tenants and applicants.', link: '/landlord/messages' },
  ];
}
