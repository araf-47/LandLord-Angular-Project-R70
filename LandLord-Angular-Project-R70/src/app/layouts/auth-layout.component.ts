import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { LogoComponent } from '../shared/logo.component';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, LogoComponent],
  template: `
    <div class="auth-shell">
      <div class="auth-card">
        <a routerLink="/" class="public-brand" style="display:flex; justify-content:center; margin-bottom:1.25rem;">
          <app-logo theme="light" />
        </a>
        <router-outlet />
      </div>
    </div>
  `,
})
export class AuthLayoutComponent {}
