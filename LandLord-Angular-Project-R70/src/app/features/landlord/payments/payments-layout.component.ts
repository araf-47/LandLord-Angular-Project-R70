import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-payments-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <nav class="tabs">
      <a routerLink="/landlord/payments/bills" routerLinkActive="active">Bills</a>
      <a routerLink="/landlord/payments/receive" routerLinkActive="active">Receive Payment</a>
      <a routerLink="/landlord/payments/pending" routerLinkActive="active">Pending Cash</a>
    </nav>
    <router-outlet />
  `,
})
export class PaymentsLayoutComponent {}
