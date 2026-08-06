import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card">
      <h2>Reset your password</h2>

      @if (!sent()) {
        <p>Enter the email on your account and we'll send a password-reset link.</p>
        <div class="field">
          <label for="email">Account email</label>
          <input id="email" type="email" name="email" [(ngModel)]="email" required />
        </div>
        <div class="actions-row">
          <button class="btn btn-primary" (click)="send()">Send password-reset link</button>
        </div>
      } @else {
        <p>A reset link was sent to {{ email }}.</p>
        <div class="actions-row">
          <a class="btn btn-primary" routerLink="/auth/reset-password">Open reset link (demo)</a>
        </div>
      }

      <p style="margin-top:1rem;"><a routerLink="/auth/login">Back to login</a></p>
    </div>
  `,
})
export class ForgotPasswordComponent {
  email = '';
  readonly sent = signal(false);

  send(): void {
    if (!this.email) return;
    this.sent.set(true);
  }
}
