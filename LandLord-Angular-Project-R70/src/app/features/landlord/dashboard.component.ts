import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MaintenanceApiService } from '../../core/maintenance-api.service';
import { BillingApiService } from '../../core/billing-api.service';
import { UnitApiService } from '../../core/unit-api.service';
import { periodKey, periodLabel } from '../../core/mock-data.service';
import { StatTileComponent } from '../../shared/stat-tile.component';
import { NavIconComponent, NavIconName } from '../../shared/nav-icon.component';

@Component({
  selector: 'app-landlord-dashboard',
  standalone: true,
  imports: [RouterLink, StatTileComponent, NavIconComponent],
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
        <div class="module-grid mb-lg">
          <app-stat-tile label="Occupancy" [value]="occupancy().occupied + '/' + occupancy().total" color="primary" icon="home" />
          <app-stat-tile label="Collected this month" [value]="collected()" color="success" icon="coins" />
          <app-stat-tile label="Outstanding this month" [value]="outstanding()" color="danger" icon="alert" />
          <app-stat-tile label="Net this month" [value]="net()" color="primary" icon="trend" />
          <app-stat-tile label="Pending maintenance" [value]="pendingMaintenance()" color="warning" icon="alert" />
        </div>

        <h1>Manage your property</h1>
        <div class="module-grid">
          @for (m of modules; track m.link) {
            <a class="module-tile" [routerLink]="m.link">
              <div class="module-title"><app-nav-icon [name]="m.icon" />{{ m.title }}</div>
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

  readonly modules: { title: string; desc: string; link: string; icon: NavIconName }[] = [
    { title: 'Property & Units', desc: 'Manage properties and unit status.', link: '/landlord/properties', icon: 'property' },
    { title: 'Tenant Management', desc: 'Register, view, and move out tenants.', link: '/landlord/tenants', icon: 'tenant' },
    { title: 'Marketplace & Leads', desc: 'Ads and booking requests.', link: '/landlord/marketplace', icon: 'marketplace' },
    { title: 'Rental Agreements', desc: 'View and edit lease terms.', link: '/landlord/rentals', icon: 'rentals' },
    { title: 'Payments', desc: 'Generate bills, receive payments.', link: '/landlord/payments', icon: 'payments' },
    { title: 'Expenses', desc: 'Track property and tenant expenses.', link: '/landlord/expenses', icon: 'expenses' },
    { title: 'Ledger', desc: 'All money in and out, one cash book.', link: '/landlord/ledger', icon: 'ledger' },
    { title: 'Reports', desc: 'Income, expenses, and collection trends.', link: '/landlord/reports', icon: 'reports' },
    { title: 'Maintenance', desc: 'Log and resolve issues.', link: '/landlord/maintenance', icon: 'maintenance' },
    { title: 'Messages', desc: 'Chat with tenants and applicants.', link: '/landlord/messages', icon: 'messages' },
  ];
}
