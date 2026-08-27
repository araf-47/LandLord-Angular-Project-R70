import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { LogoComponent } from '../shared/logo.component';

@Component({
  selector: 'app-landlord-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LogoComponent],
  template: `
    <div class="app-shell">
      @if (sidebarOpen()) {
        <div class="sidebar-backdrop" (click)="sidebarOpen.set(false)"></div>
      }
      <aside class="sidebar" [class.open]="sidebarOpen()">
        <div class="sidebar-brand"><app-logo theme="dark" /></div>
        <nav class="sidebar-nav" (click)="sidebarOpen.set(false)">
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
          <div class="topbar-left">
            <button class="menu-toggle" type="button" (click)="sidebarOpen.set(!sidebarOpen())" aria-label="Toggle menu">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="2" y1="5" x2="18" y2="5" /><line x1="2" y1="10" x2="18" y2="10" /><line x1="2" y1="15" x2="18" y2="15" /></svg>
            </button>
            <strong>Landlord Dashboard</strong>
          </div>
          <span class="hint-text">{{ auth.user()?.username }}</span>
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
  protected readonly sidebarOpen = signal(false);

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/auth/login');
  }
}
