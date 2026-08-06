import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landlord-dashboard',
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Choose a module</h1>
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
export class LandlordDashboardComponent {
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
