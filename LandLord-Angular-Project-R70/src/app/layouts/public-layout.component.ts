import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { LogoComponent } from '../shared/logo.component';
import { BARIVARA_DEV_URL } from '../core/cross-app.config';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, LogoComponent],
  template: `
    <header class="public-topbar">
      <a routerLink="/" class="public-brand"><app-logo theme="light" /></a>
      <nav class="public-nav">
        @if (auth.isAuthenticated()) {
          <a [routerLink]="dashboardLink()">My Dashboard</a>
        } @else {
          <a routerLink="/auth/login">Log in</a>
          <a class="btn btn-primary btn-sm" routerLink="/auth/signup">Get Started</a>
        }
      </nav>
    </header>
    <router-outlet />
    <footer class="site-footer">
      <div class="site-footer-inner">
        <div class="site-footer-brand">
          <app-logo theme="light" [height]="44" />
          <p>Property management, simplified.</p>
        </div>
        <nav class="site-footer-links">
          <a routerLink="/auth/login">Log in</a>
          <a routerLink="/auth/signup">Sign up</a>
          <a [href]="bariVaraUrl" target="_blank" rel="noopener">BariVara.com — our rental marketplace</a>
        </nav>
        <p class="site-footer-copy">&copy; {{ year }} LandLord. Built by Araf.</p>
      </div>
    </footer>
  `,
})
export class PublicLayoutComponent {
  protected readonly auth = inject(AuthService);
  protected readonly bariVaraUrl = BARIVARA_DEV_URL;
  protected readonly year = new Date().getFullYear();

  dashboardLink(): string {
    return this.auth.role() === 'landlord' ? '/landlord/dashboard' : '/tenant/dashboard';
  }
}
