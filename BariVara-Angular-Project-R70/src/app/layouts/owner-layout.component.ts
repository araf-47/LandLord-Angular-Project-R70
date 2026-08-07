import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { LogoComponent } from '../shared/logo.component';

@Component({
  selector: 'app-owner-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, LogoComponent],
  template: `
    <div class="app-shell">
      <aside class="sidebar">
        <div class="sidebar-brand"><app-logo theme="dark" /></div>
        <nav class="sidebar-nav">
          <a routerLink="/owner/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/owner/properties" routerLinkActive="active">Properties</a>
          <a routerLink="/owner/listings" routerLinkActive="active">My Listings</a>
          <a routerLink="/owner/requests" routerLinkActive="active">Requests</a>
          <a routerLink="/owner/messages" routerLinkActive="active">Messages</a>
        </nav>
        <div class="sidebar-footer">
          <button class="btn btn-ghost" style="color:#cbd5e1; width:100%; justify-content:flex-start;" (click)="logout()">
            Logout
          </button>
        </div>
      </aside>
      <div class="main-area">
        <header class="topbar">
          <strong>Apartment Owner Dashboard</strong>
          <span class="hint-text">{{ auth.user()?.name }} ({{ auth.user()?.email }})</span>
        </header>
        <div class="page-content">
          <router-outlet />
        </div>
      </div>
    </div>
  `,
})
export class OwnerLayoutComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/auth/login');
  }
}
