import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card">
      <h2>Reset your password</h2>

      @if (!sent()) {
        <p>Enter your username and we'll email a verification code to reset your password.</p>
        <div class="field">
          <label for="username">Username</label>
          <input id="username" name="username" [(ngModel)]="username" required />
        </div>

        @if (error()) {
          <p class="error-text">{{ error() }}</p>
        }

        <div class="actions-row">
          <button class="btn btn-primary" (click)="send()" [disabled]="loading()">
            {{ loading() ? 'Sending…' : 'Send code' }}
          </button>
        </div>
      } @else {
        <p>A verification code was sent to {{ username }}'s registered email.</p>
        <div class="actions-row">
          <button class="btn btn-primary" (click)="toResetPassword()">Enter code</button>
        </div>
      }

      <p class="mt-md"><a routerLink="/auth/login">Back to login</a></p>
    </div>
  `,
})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  readonly error = signal('');
  readonly loading = signal(false);
  readonly sent = signal(false);

  async send(): Promise<void> {
    if (!this.username) return;
    this.error.set('');
    this.loading.set(true);
    try {
      await this.auth.requestPasswordResetOtp(this.username);
      this.sent.set(true);
    } catch (err: any) {
      this.error.set(err?.error?.message || err?.message || 'Could not send the reset code.');
    } finally {
      this.loading.set(false);
    }
  }

  toResetPassword(): void {
    this.router.navigate(['/auth/reset-password'], { queryParams: { username: this.username } });
  }
}
