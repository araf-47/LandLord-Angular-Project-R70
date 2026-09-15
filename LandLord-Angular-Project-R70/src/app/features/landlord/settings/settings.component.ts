import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { ConfirmService } from '../../../shared/confirm.service';
import { ThemeService } from '../../../shared/theme.service';
import { ToastService } from '../../../shared/toast.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h1>Settings</h1>

    @switch (status()) {
      @case ('loading') {
        <div class="card"><p class="hint-text">Loading settings…</p></div>
      }
      @case ('error') {
        <div class="card">
          <p class="text-danger mb-sm">Couldn't load your settings. {{ error() }}</p>
          <button type="button" class="btn btn-sm" (click)="ngOnInit()">Retry</button>
        </div>
      }
      @case ('ready') {
        <div class="card max-w-md">
          <h2>Profile</h2>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" [(ngModel)]="email" />
          </div>
          <div class="field">
            <label for="phone">Phone</label>
            <input id="phone" name="phone" [(ngModel)]="phone" required />
          </div>
          <div class="actions-row">
            <button class="btn btn-primary" (click)="saveProfile()">Save profile</button>
          </div>
        </div>

        <div class="card max-w-md">
          <h2>Security</h2>

          <p class="hint-text">Change password</p>
          <div class="field">
            <label for="oldPassword">Current password</label>
            <input id="oldPassword" name="oldPassword" type="password" [(ngModel)]="oldPassword" />
          </div>
          <div class="field">
            <label for="newPassword">New password</label>
            <input id="newPassword" name="newPassword" type="password" [(ngModel)]="newPassword" />
          </div>
          <div class="form-row">
            <div class="field">
              <label for="otp">Verification code</label>
              <input id="otp" name="otp" [(ngModel)]="otp" />
            </div>
            <div class="actions-row">
              <button type="button" class="btn btn-sm" (click)="sendOtp()">Send code</button>
            </div>
          </div>
          <div class="actions-row">
            <button class="btn btn-primary" (click)="savePassword()">Change password</button>
          </div>

          <hr />

          <div class="field">
            <label>
              <input type="checkbox" [(ngModel)]="twoFactorEnabled" name="twoFactorEnabled" (ngModelChange)="saveTwoFactor($event)" />
              Two-factor authentication
            </label>
          </div>

          <div class="actions-row">
            <button type="button" class="btn btn-danger" (click)="logoutEverywhere()">Log out of all devices</button>
          </div>
        </div>

        <div class="card max-w-md">
          <h2>Appearance</h2>
          <div class="field">
            <label>
              <input
                type="checkbox"
                name="darkMode"
                [ngModel]="themeService.theme() === 'dark'"
                (ngModelChange)="themeService.toggle()"
              />
              Dark mode
            </label>
          </div>
        </div>

        <div class="card max-w-md">
          <h2>Notification preferences</h2>
          <div class="field">
            <label>
              <input type="checkbox" name="notifyRentDueEmail" [(ngModel)]="notifyRentDueEmail" />
              Rent due reminders (email)
            </label>
          </div>
          <div class="field">
            <label>
              <input type="checkbox" name="notifyRentDueSms" [(ngModel)]="notifyRentDueSms" />
              Rent due reminders (SMS)
            </label>
          </div>
          <div class="field">
            <label>
              <input type="checkbox" name="notifyPaymentReceivedEmail" [(ngModel)]="notifyPaymentReceivedEmail" />
              Payment received alerts (email)
            </label>
          </div>
          <div class="field">
            <label>
              <input type="checkbox" name="notifyMaintenanceEmail" [(ngModel)]="notifyMaintenanceEmail" />
              Maintenance updates (email)
            </label>
          </div>
          <div class="actions-row">
            <button class="btn btn-primary" (click)="saveNotificationPrefs()">Save preferences</button>
          </div>
        </div>
      }
    }
  `,
})
export class SettingsComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly confirmService = inject(ConfirmService);
  private readonly router = inject(Router);
  protected readonly themeService = inject(ThemeService);

  readonly status = signal<'loading' | 'error' | 'ready'>('loading');
  readonly error = signal<string | undefined>(undefined);

  email = '';
  phone = '';
  oldPassword = '';
  newPassword = '';
  otp = '';
  twoFactorEnabled = false;
  notifyRentDueEmail = true;
  notifyRentDueSms = false;
  notifyPaymentReceivedEmail = true;
  notifyMaintenanceEmail = true;

  async ngOnInit(): Promise<void> {
    this.status.set('loading');
    try {
      await this.auth.refreshUser();
      const user = this.auth.user();
      if (user) {
        this.email = user.email ?? '';
        this.phone = user.phone ?? '';
        this.twoFactorEnabled = user.twoFactorEnabled;
        this.notifyRentDueEmail = user.notifyRentDueEmail;
        this.notifyRentDueSms = user.notifyRentDueSms;
        this.notifyPaymentReceivedEmail = user.notifyPaymentReceivedEmail;
        this.notifyMaintenanceEmail = user.notifyMaintenanceEmail;
      }
      this.status.set('ready');
    } catch {
      this.error.set('Check your connection and try again.');
      this.status.set('error');
    }
  }

  async saveProfile(): Promise<void> {
    try {
      await this.auth.updateProfile(this.email, this.phone);
      this.toast.success('Profile updated.');
    } catch (e) {
      this.toast.error(e instanceof Error ? e.message : 'Could not update your profile.');
    }
  }

  async sendOtp(): Promise<void> {
    const username = this.auth.user()?.username;
    if (!username) return;
    try {
      await this.auth.requestPasswordResetOtp(username);
      this.toast.success('Verification code sent.');
    } catch (e) {
      this.toast.error(e instanceof Error ? e.message : 'Could not send the code.');
    }
  }

  async savePassword(): Promise<void> {
    if (!this.oldPassword || !this.newPassword || !this.otp) return;
    try {
      await this.auth.changePassword(this.oldPassword, this.newPassword, this.otp);
      this.oldPassword = '';
      this.newPassword = '';
      this.otp = '';
      this.toast.success('Password changed.');
    } catch (e) {
      this.toast.error(e instanceof Error ? e.message : 'Could not change your password.');
    }
  }

  async saveTwoFactor(enabled: boolean): Promise<void> {
    try {
      await this.auth.toggleTwoFactor(enabled);
      this.toast.success(enabled ? 'Two-factor authentication enabled.' : 'Two-factor authentication disabled.');
    } catch (e) {
      this.twoFactorEnabled = !enabled;
      this.toast.error(e instanceof Error ? e.message : 'Could not update two-factor authentication.');
    }
  }

  async logoutEverywhere(): Promise<void> {
    const ok = await this.confirmService.confirm(
      'Log out of all devices?',
      'This will end every active session, including this one. You will need to log in again.'
    );
    if (!ok) return;
    try {
      await this.auth.logoutEverywhere();
      this.router.navigateByUrl('/auth/login');
    } catch (e) {
      this.toast.error(e instanceof Error ? e.message : 'Could not log out other sessions.');
    }
  }

  async saveNotificationPrefs(): Promise<void> {
    try {
      await this.auth.updateNotificationPrefs({
        notifyRentDueEmail: this.notifyRentDueEmail,
        notifyRentDueSms: this.notifyRentDueSms,
        notifyPaymentReceivedEmail: this.notifyPaymentReceivedEmail,
        notifyMaintenanceEmail: this.notifyMaintenanceEmail,
      });
      this.toast.success('Notification preferences saved.');
    } catch (e) {
      this.toast.error(e instanceof Error ? e.message : 'Could not update notification preferences.');
    }
  }
}
