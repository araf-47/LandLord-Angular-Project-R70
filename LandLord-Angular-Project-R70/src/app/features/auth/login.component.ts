import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card">
      <h2>Log in</h2>
      <p>Landlord: your seeded username. Tenant: the phone number your landlord registered you with.</p>

      <form (ngSubmit)="submit()">
        <div class="field">
          <label for="username">Username</label>
          <input id="username" name="username" [(ngModel)]="username" required />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" type="password" name="password" [(ngModel)]="password" required />
        </div>

        @if (error()) {
          <p class="error-text">{{ error() }}</p>
        }

        <div class="actions-row">
          <button type="submit" class="btn btn-primary" [disabled]="loading()">
            {{ loading() ? 'Logging in…' : 'Log in' }}
          </button>
        </div>
      </form>

      <p class="mt-md">
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

  username = '';
  password = '';
  readonly error = signal('');
  readonly loading = signal(false);

  async submit(): Promise<void> {
    if (!this.username || !this.password) {
      this.error.set('Enter both username and password.');
      return;
    }
    this.error.set('');
    this.loading.set(true);
    try {
      await this.auth.login(this.username, this.password);
      const role = this.auth.role();
      this.router.navigateByUrl(role === 'landlord' ? '/landlord/dashboard' : '/tenant/dashboard');
    } catch (err: any) {
      this.error.set(err?.error?.message || err?.message || 'Login failed. Check your username and password.');
    } finally {
      this.loading.set(false);
    }
  }
}
