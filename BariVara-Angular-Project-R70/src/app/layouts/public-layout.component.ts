import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { LogoComponent } from '../shared/logo.component';

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
  `,
})
export class PublicLayoutComponent {
  protected readonly auth = inject(AuthService);

  dashboardLink(): string {
    const role = this.auth.role();
    if (role === 'owner') return '/owner/dashboard';
    if (role === 'landlord-linked') return '/landlord-linked/dashboard';
    return '/tenant/dashboard';
  }
}
