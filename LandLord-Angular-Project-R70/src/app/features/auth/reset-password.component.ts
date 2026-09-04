import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="card">
      <h2>Set a new password</h2>

      @if (!done()) {
        <div class="field">
          <label for="username">Username</label>
          <input id="username" name="username" [(ngModel)]="username" required />
        </div>
        <div class="field">
          <label for="otp">Verification code</label>
          <input id="otp" name="otp" [(ngModel)]="otp" required />
        </div>
        <div class="field">
          <label for="password">New password</label>
          <input id="password" type="password" name="password" [(ngModel)]="password" required />
          <small>8-16 characters, with an uppercase letter, a lowercase letter, a number, and a special character.</small>
        </div>
        <div class="field">
          <label for="confirm">Confirm password</label>
          <input id="confirm" type="password" name="confirm" [(ngModel)]="confirm" required />
        </div>
        @if (error()) {
          <p class="error-text">{{ error() }}</p>
        }
        <div class="actions-row">
          <button class="btn btn-primary" (click)="save()" [disabled]="loading()">
            {{ loading() ? 'Updating…' : 'Update password' }}
          </button>
        </div>
      } @else {
        <p>Password updated. You can now log in.</p>
        <div class="actions-row">
          <button class="btn btn-primary" (click)="toLogin()">Return to login</button>
        </div>
      }
    </div>
  `,
})
export class ResetPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  username = this.route.snapshot.queryParamMap.get('username') ?? '';
  otp = '';
  password = '';
  confirm = '';
  readonly error = signal('');
  readonly loading = signal(false);
  readonly done = signal(false);

  async save(): Promise<void> {
    if (!this.username || !this.otp) {
      this.error.set('Enter your username and the verification code.');
      return;
    }
    if (!this.password || this.password !== this.confirm) {
      this.error.set('Passwords do not match.');
      return;
    }
    this.error.set('');
    this.loading.set(true);
    try {
      await this.auth.resetPassword(this.username, this.otp, this.password);
      this.done.set(true);
    } catch (err: any) {
      this.error.set(err?.error?.message || err?.message || 'Could not reset the password.');
    } finally {
      this.loading.set(false);
    }
  }

  toLogin(): void {
    this.router.navigateByUrl('/auth/login');
  }
}
