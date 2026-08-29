import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Real accounts (Phase 7) aren't self-service in this app: the landlord's
 * account is the single seeded LANDLORD login, and tenant accounts are
 * created by the landlord at walk-in registration time (a temp password
 * handed over in person), not through a public signup form. This page is
 * informational only.
 */
@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="card">
      <h2>Create account</h2>
      <p>
        Accounts here aren't self-service. If you're a tenant, your landlord
        creates your login when they register you — ask them for your
        username (your phone number) and temporary password.
      </p>
      <p>
        If you're the landlord, use the account you already have.
      </p>
      <p class="mt-md">Already have an account? <a routerLink="/auth/login">Log in</a></p>
    </div>
  `,
})
export class SignupComponent {}
