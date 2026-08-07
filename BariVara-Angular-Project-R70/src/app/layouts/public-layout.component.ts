import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { LogoComponent } from '../shared/logo.component';
import { LANDLORD_CORE_DEV_URL } from '../core/cross-app.config';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, LogoComponent],
  template: `
    <header class="public-topbar">
      <a routerLink="/" class="public-brand"><app-logo theme="light" /></a>
      <nav class="public-nav">
        <a routerLink="/browse">Browse</a>
        @if (auth.isAuthenticated()) {
          <a [routerLink]="dashboardLink()">My Dashboard</a>
        } @else {
          <a routerLink="/auth/login">Log in</a>
          <a class="btn btn-primary btn-sm" routerLink="/auth/signup">Sign up</a>
        }
      </nav>
    </header>
    <router-outlet />
    <footer class="site-footer">
      <div class="site-footer-inner">
        <div class="site-footer-brand">
          <app-logo theme="light" [height]="44" />
          <p>Find your next home.</p>
        </div>
        <nav class="site-footer-links">
          <a routerLink="/browse">Browse</a>
          <a routerLink="/auth/login">Log in</a>
          <a routerLink="/auth/signup">Sign up</a>
          <a [href]="landlordCoreUrl" target="_blank" rel="noopener">Property owner? Manage listings on LandLord</a>
        </nav>
        <p class="site-footer-copy">&copy; {{ year }} BariVara.com. Built by Araf.</p>
      </div>
    </footer>
  `,
})
export class PublicLayoutComponent {
  protected readonly auth = inject(AuthService);
  protected readonly landlordCoreUrl = LANDLORD_CORE_DEV_URL;
  protected readonly year = new Date().getFullYear();

  dashboardLink(): string {
    const role = this.auth.role();
    if (role === 'owner') return '/owner/dashboard';
    if (role === 'landlord-linked') return '/landlord-linked/dashboard';
    return '/tenant/dashboard';
  }
}
