import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-landlord-linked-layout',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="app-shell">
      <aside class="sidebar">
        <div class="sidebar-brand">BariVara.com</div>
        <nav class="sidebar-nav">
          <a class="active">Dashboard</a>
        </nav>
        <div class="sidebar-footer">
          <button class="btn btn-ghost" style="color:#cbd5e1; width:100%; justify-content:flex-start;" (click)="logout()">
            Logout
          </button>
        </div>
      </aside>
      <div class="main-area">
        <header class="topbar">
          <strong>LandLord (core-linked) Dashboard</strong>
          <span class="hint-text">{{ auth.user()?.name }} ({{ auth.user()?.email }})</span>
        </header>
        <div class="page-content">
          <router-outlet />
        </div>
      </div>
    </div>
  `,
})
export class LandlordLinkedLayoutComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/auth/login');
  }
}
