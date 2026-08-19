import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MaintenanceApiService } from '../../core/maintenance-api.service';
import { MockDataService, periodLabel } from '../../core/mock-data.service';

@Component({
  selector: 'app-landlord-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
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
  `,
})
export class LandlordDashboardComponent implements OnInit {
  private readonly data = inject(MockDataService);
  private readonly maintenanceApi = inject(MaintenanceApiService);

  private readonly period = this.data.currentPeriod();
  private readonly expensesThisPeriod = signal(0);

  readonly occupancy = () => this.data.occupancyStats();
  readonly collected = () => this.data.collectedInPeriod(this.period);
  readonly outstanding = () => this.data.outstandingInPeriod(this.period);
  readonly pendingMaintenance = () => this.maintenanceApi.tickets().filter((t) => t.status === 'pending').length;
  readonly net = () => this.collected() - this.expensesThisPeriod();

  async ngOnInit(): Promise<void> {
    const [, expenses] = await Promise.all([this.maintenanceApi.load(), this.maintenanceApi.allExpenses()]);
    this.expensesThisPeriod.set(expenses.filter((e) => e.date.startsWith(this.period)).reduce((sum, e) => sum + e.amount, 0));
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
