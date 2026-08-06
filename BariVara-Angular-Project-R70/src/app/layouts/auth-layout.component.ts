import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <div class="auth-shell">
      <div class="auth-card">
        <a routerLink="/" class="public-brand" style="display:block; text-align:center; margin-bottom:1.25rem;">
          BariVara.com
        </a>
        <router-outlet />
      </div>
    </div>
  `,
})
export class AuthLayoutComponent {}
