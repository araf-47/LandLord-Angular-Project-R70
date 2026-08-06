import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-landlord-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-shell">
      <aside class="sidebar">
        <div class="sidebar-brand">LandLord</div>
        <nav class="sidebar-nav">
          <a routerLink="/landlord/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/landlord/properties" routerLinkActive="active">Property &amp; Units</a>
          <a routerLink="/landlord/tenants" routerLinkActive="active">Tenant Management</a>
          <a routerLink="/landlord/marketplace" routerLinkActive="active">Marketplace &amp; Leads</a>
          <a routerLink="/landlord/rentals" routerLinkActive="active">Rental Agreements</a>
          <a routerLink="/landlord/payments" routerLinkActive="active">Payments</a>
          <a routerLink="/landlord/expenses" routerLinkActive="active">Expenses</a>
          <a routerLink="/landlord/ledger" routerLinkActive="active">Ledger</a>
          <a routerLink="/landlord/maintenance" routerLinkActive="active">Maintenance</a>
          <a routerLink="/landlord/messages" routerLinkActive="active">Messages</a>
        </nav>
        <div class="sidebar-footer">
          <button class="btn btn-ghost" style="color:#cbd5e1; width:100%; justify-content:flex-start;" (click)="logout()">
            Logout
          </button>
        </div>
      </aside>
      <div class="main-area">
        <header class="topbar">
          <strong>Landlord Dashboard</strong>
          <span class="hint-text">{{ auth.user()?.name }} ({{ auth.user()?.email }})</span>
        </header>
        <div class="page-content">
          <router-outlet />
        </div>
      </div>
    </div>
  `,
})
export class LandlordLayoutComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/auth/login');
  }
}
