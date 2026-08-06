import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserRole } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card">
      <h2>Log in</h2>
      <p>Enter your account email and password.</p>

      <form (ngSubmit)="submit()">
        <div class="field">
          <label for="email">Email</label>
          <input id="email" type="email" name="email" [(ngModel)]="email" required />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" type="password" name="password" [(ngModel)]="password" required />
        </div>
        <div class="field">
          <label for="role">Sign in as</label>
          <select id="role" name="role" [(ngModel)]="role">
            <option value="tenant">Tenant</option>
            <option value="owner">Apartment Owner</option>
            <option value="landlord-linked">LandLord (core account)</option>
          </select>
          <span class="hint-text">Demo stub — role normally comes from the account record.</span>
        </div>

        @if (error()) {
          <p class="error-text">{{ error() }}</p>
        }

        <div class="actions-row">
          <button type="submit" class="btn btn-primary">Log in</button>
        </div>
      </form>

      <p style="margin-top:1rem;">
        <a routerLink="/auth/forgot-password">Forgot password?</a>
      </p>
      <p>
        Don't have an account? <a routerLink="/auth/signup">Sign up</a>
      </p>
    </div>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  email = '';
  password = '';
  role: UserRole = 'tenant';
  readonly error = signal('');

  submit(): void {
    if (!this.email || !this.password) {
      this.error.set('Enter both email and password.');
      return;
    }
    this.error.set('');
    this.auth.login(this.email, this.password, this.role);

    const home: Record<UserRole, string> = {
      tenant: '/tenant/dashboard',
      owner: '/owner/dashboard',
      'landlord-linked': '/landlord-linked/dashboard',
    };
    this.router.navigateByUrl(home[this.role]);
  }
}
