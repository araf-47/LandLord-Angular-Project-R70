import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="card">
      <h2>Set a new password</h2>

      @if (!done()) {
        <div class="field">
          <label for="password">New password</label>
          <input id="password" type="password" name="password" [(ngModel)]="password" required />
        </div>
        <div class="field">
          <label for="confirm">Confirm password</label>
          <input id="confirm" type="password" name="confirm" [(ngModel)]="confirm" required />
        </div>
        @if (error()) {
          <p class="error-text">{{ error() }}</p>
        }
        <div class="actions-row">
          <button class="btn btn-primary" (click)="save()">Update password</button>
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
  private readonly router = inject(Router);

  password = '';
  confirm = '';
  readonly error = signal('');
  readonly done = signal(false);

  save(): void {
    if (!this.password || this.password !== this.confirm) {
      this.error.set('Passwords do not match.');
      return;
    }
    this.error.set('');
    this.done.set(true);
  }

  toLogin(): void {
    this.router.navigateByUrl('/auth/login');
  }
}
