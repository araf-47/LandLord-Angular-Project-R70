import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { LogoComponent } from '../shared/logo.component';

@Component({
  selector: 'app-tenant-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LogoComponent],
  template: `
    <div class="app-shell">
      <aside class="sidebar">
        <div class="sidebar-brand"><app-logo theme="dark" /></div>
        <nav class="sidebar-nav">
          <a routerLink="/tenant/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/tenant/profile" routerLinkActive="active">My Profile</a>
          <a routerLink="/tenant/notifications" routerLinkActive="active">Notifications</a>
          <a routerLink="/tenant/payments" routerLinkActive="active">Payments</a>
          <a routerLink="/tenant/maintenance" routerLinkActive="active">Maintenance</a>
          <a routerLink="/tenant/documents" routerLinkActive="active">Documents</a>
          <a routerLink="/tenant/browse-transfer" routerLinkActive="active">Browse &amp; Transfer</a>
          <a routerLink="/tenant/messages" routerLinkActive="active">Messages</a>
        </nav>
        <div class="sidebar-footer">
          <button class="btn btn-ghost" style="color:#cbd5e1; width:100%; justify-content:flex-start;" (click)="logout()">
            Logout
          </button>
        </div>
      </aside>
      <div class="main-area">
        <header class="topbar">
          <strong>Tenant Dashboard</strong>
          <span class="hint-text">{{ auth.user()?.name }} ({{ auth.user()?.email }})</span>
        </header>
        <div class="page-content">
          <router-outlet />
        </div>
      </div>
    </div>
  `,
})
export class TenantLayoutComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/auth/login');
  }
}
