import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="public-topbar">
      <a routerLink="/" class="public-brand">LandLord</a>
      <nav class="public-nav">
        <a routerLink="/auth/login">Log in</a>
        <a class="btn btn-primary btn-sm" routerLink="/auth/signup">Get Started</a>
      </nav>
    </header>
    <router-outlet />
  `,
})
export class PublicLayoutComponent {}
