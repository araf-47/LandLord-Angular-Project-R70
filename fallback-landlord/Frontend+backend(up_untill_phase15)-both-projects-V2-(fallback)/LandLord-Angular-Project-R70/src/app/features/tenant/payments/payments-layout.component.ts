import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-tenant-payments-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <nav class="tabs">
      <a routerLink="/tenant/payments/pay" routerLinkActive="active">Pay</a>
      <a routerLink="/tenant/payments/history" routerLinkActive="active">History</a>
      <a routerLink="/tenant/payments/pending" routerLinkActive="active">Pending</a>
    </nav>
    <router-outlet />
  `,
})
export class TenantPaymentsLayoutComponent {}
