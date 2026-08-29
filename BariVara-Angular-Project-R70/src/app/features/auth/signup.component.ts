import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserRole } from '../../core/auth.service';
import { ProfileApiService } from '../../core/profile-api.service';

/**
 * Same shape as the LandLord app's signup wizard, reused per the diagram note
 * "Login / Signup (same pattern as LandLord core Login)". Role choice is limited to
 * tenant / owner — a "LandLord (core-linked)" account originates in the LandLord
 * app, not here.
 */
@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="card">
      <h2>Create account</h2>

      <div class="steps">
        @for (s of [1, 2, 3, 4]; track s) {
          <div class="step-dot" [class.active]="step() === s">{{ s }}</div>
        }
      </div>

      @if (step() === 1) {
        <p>Select account type</p>
        <div class="form-row">
          <button class="btn" [class.btn-primary]="accountType === 'tenant'" (click)="accountType = 'tenant'; next()">
            Tenant
          </button>
          <button class="btn" [class.btn-primary]="accountType === 'owner'" (click)="accountType = 'owner'; next()">
            Apartment Owner
          </button>
        </div>
      }

      @if (step() === 2) {
        <p>Enter your details</p>
        <div class="field">
          <label for="name">Full name</label>
          <input id="name" name="name" [(ngModel)]="name" required />
        </div>
        <div class="field">
          <label for="email">Email</label>
          <input id="email" type="email" name="email" [(ngModel)]="email" required />
        </div>
        <div class="field">
          <label for="phone">Phone</label>
          <input id="phone" name="phone" [(ngModel)]="phone" required />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" type="password" name="password" [(ngModel)]="password" required />
        </div>
        <div class="actions-row">
          <button class="btn" (click)="back()">Back</button>
          <button class="btn btn-primary" (click)="next()">Continue</button>
        </div>
      }

      @if (step() === 3) {
        <p>Accept Terms &amp; Privacy Policy</p>
        <label class="field field-row">
          <input type="checkbox" name="terms" [(ngModel)]="acceptedTerms" />
          I agree to the Terms of Service and Privacy Policy
        </label>
        <div class="actions-row">
          <button class="btn" (click)="back()">Back</button>
          <button class="btn btn-primary" [disabled]="!acceptedTerms" (click)="sendOtp()">Continue</button>
        </div>
      }

      @if (step() === 4) {
        <p>Enter the OTP code sent to {{ email }}</p>
        <div class="field">
          <label for="otp">OTP code</label>
          <input id="otp" name="otp" [(ngModel)]="otp" maxlength="6" />
          <span class="hint-text">Demo code: 123456</span>
        </div>
        @if (otpError()) {
          <p class="error-text">{{ otpError() }} ({{ attemptsLeft() }} attempts left)</p>
        }
        <div class="actions-row">
          <button class="btn" (click)="sendOtp()">Resend code</button>
          <button class="btn btn-primary" [disabled]="submitting()" (click)="verifyOtp()">
            {{ submitting() ? 'Creating…' : 'Verify & create account' }}
          </button>
        </div>
      }

      <p class="mt-md">Already have an account? <a routerLink="/auth/login">Log in</a></p>
    </div>
  `,
})
export class SignupComponent {
  private readonly auth = inject(AuthService);
  private readonly profileApi = inject(ProfileApiService);
  private readonly router = inject(Router);

  readonly step = signal(1);
  accountType: Extract<UserRole, 'tenant' | 'owner'> = 'tenant';
  name = '';
  email = '';
  phone = '';
  password = '';
  acceptedTerms = false;
  otp = '';
  readonly otpError = signal('');
  readonly submitting = signal(false);
  private otpAttempts = 0;
  readonly attemptsLeft = () => 3 - this.otpAttempts;

  next(): void {
    this.step.update((s) => Math.min(4, s + 1));
  }

  back(): void {
    this.step.update((s) => Math.max(1, s - 1));
  }

  sendOtp(): void {
    this.otpError.set('');
    this.step.set(4);
  }

  async verifyOtp(): Promise<void> {
    if (this.otp !== '123456') {
      this.otpAttempts++;
      this.otpError.set('Invalid code.');
      return;
    }

    this.otpError.set('');
    this.submitting.set(true);
    try {
      const request = { name: this.name, email: this.email, phone: this.phone, password: this.password };
      if (this.accountType === 'owner') {
        await this.profileApi.registerOwner(request);
      } else {
        await this.profileApi.registerTenant(request);
      }
      // Username is the phone number, sanitized the same way the backend does
      // (Parts/auth's USERNAME_PATTERN rejects non-alphanumeric characters).
      await this.auth.login(this.phone.replace(/[^a-zA-Z0-9]/g, ''), this.password);
      this.router.navigateByUrl(this.accountType === 'owner' ? '/owner/dashboard' : '/tenant/dashboard');
    } catch (err: any) {
      this.otpError.set(err?.error?.message || err?.message || 'Could not create account.');
    } finally {
      this.submitting.set(false);
    }
  }
}
