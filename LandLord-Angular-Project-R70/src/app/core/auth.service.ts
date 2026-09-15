import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type UserRole = 'landlord' | 'tenant';

export interface AuthUser {
  id: number;
  username: string;
  role: UserRole;
  email: string | null;
  phone: string | null;
  twoFactorEnabled: boolean;
  notifyRentDueEmail: boolean;
  notifyRentDueSms: boolean;
  notifyPaymentReceivedEmail: boolean;
  notifyMaintenanceEmail: boolean;
}

const AUTH_BASE = 'http://localhost:8080/api/v3/auth';
const USER_BASE = 'http://localhost:8080/api/v3/user';
const ME_URL = 'http://localhost:8080/api/auth/me';

const ACCESS_TOKEN_KEY = 'landlord_access_token';
const REFRESH_TOKEN_KEY = 'landlord_refresh_token';
const USER_KEY = 'landlord_auth_user';

interface LoginResponseData {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
}

interface ApiEnvelope<T> {
  data?: T;
  message: string;
  status: string;
}

/**
 * Field-validation failures come back as {"message":"Validation failed",
 * "data":{"password":"..."}} - the top-level message alone is useless, so
 * pull the per-field detail out when present.
 */
function envelopeErrorMessage(res: ApiEnvelope<unknown>): string {
  if (res.data && typeof res.data === 'object') {
    const details = Object.values(res.data as Record<string, string>).join(' ');
    if (details) {
      return details;
    }
  }
  return res.message;
}

interface MeResponse {
  id: number;
  username: string;
  roles: string[];
  email: string | null;
  phone: string | null;
  twoFactorEnabled: boolean;
  notifyRentDueEmail: boolean;
  notifyRentDueSms: boolean;
  notifyPaymentReceivedEmail: boolean;
  notifyMaintenanceEmail: boolean;
}

function toAuthUser(me: MeResponse): AuthUser {
  return {
    id: me.id,
    username: me.username,
    role: me.roles.includes('LANDLORD') ? 'landlord' : 'tenant',
    email: me.email,
    phone: me.phone,
    twoFactorEnabled: me.twoFactorEnabled,
    notifyRentDueEmail: me.notifyRentDueEmail,
    notifyRentDueSms: me.notifyRentDueSms,
    notifyPaymentReceivedEmail: me.notifyPaymentReceivedEmail,
    notifyMaintenanceEmail: me.notifyMaintenanceEmail,
  };
}

/**
 * Talks to the real backend (Parts/auth, embedded in landlord-backend).
 * Tokens live in localStorage so a refresh keeps the session; the actual
 * Authorization/refresh headers are attached by auth.interceptor.ts, not here.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly userSignal = signal<AuthUser | null>(this.restoreUser());

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.userSignal() !== null);
  readonly role = computed(() => this.userSignal()?.role ?? null);

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  /** Called by the interceptor when the server silently rotates the token. */
  setAccessToken(token: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, token);
  }

  async login(username: string, password: string): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<ApiEnvelope<LoginResponseData>>(`${AUTH_BASE}/login`, { username, password })
    );
    if (res.status !== 'SUCCESS' || !res.data) {
      throw new Error(res.message || 'Login failed');
    }
    localStorage.setItem(ACCESS_TOKEN_KEY, res.data.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, res.data.refreshToken);

    const me = await firstValueFrom(this.http.get<MeResponse>(ME_URL));
    const user = toAuthUser(me);
    this.userSignal.set(user);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  /** Re-fetches the caller's profile and refreshes the cached user signal. */
  async refreshUser(): Promise<void> {
    const me = await firstValueFrom(this.http.get<MeResponse>(ME_URL));
    const user = toAuthUser(me);
    this.userSignal.set(user);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  async updateProfile(email: string, phone: string): Promise<void> {
    const current = this.userSignal();
    if (!current) throw new Error('Not authenticated');
    const res = await firstValueFrom(
      this.http.post<ApiEnvelope<string>>(`${USER_BASE}/update`, { id: current.id, email, phone })
    );
    if (res.status !== 'SUCCESS') {
      throw new Error(envelopeErrorMessage(res) || 'Could not update your profile.');
    }
    await this.refreshUser();
  }

  async changePassword(oldPassword: string, newPassword: string, otp: string): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<ApiEnvelope<unknown>>(`${USER_BASE}/change-password`, {
        oldPassword,
        password: newPassword,
        otp,
      })
    );
    if (res.status !== 'SUCCESS') {
      throw new Error(envelopeErrorMessage(res) || 'Could not change your password.');
    }
  }

  async toggleTwoFactor(enabled: boolean): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<ApiEnvelope<string>>(`${USER_BASE}/toggle-2fa`, { id: enabled })
    );
    if (res.status !== 'SUCCESS') {
      throw new Error(envelopeErrorMessage(res) || 'Could not update two-factor authentication.');
    }
    await this.refreshUser();
  }

  async logoutEverywhere(): Promise<void> {
    const res = await firstValueFrom(this.http.post<ApiEnvelope<string>>(`${USER_BASE}/logout-all`, {}));
    if (res.status !== 'SUCCESS') {
      throw new Error(envelopeErrorMessage(res) || 'Could not log out other sessions.');
    }
    this.logout();
  }

  async updateNotificationPrefs(prefs: {
    notifyRentDueEmail: boolean;
    notifyRentDueSms: boolean;
    notifyPaymentReceivedEmail: boolean;
    notifyMaintenanceEmail: boolean;
  }): Promise<void> {
    const res = await firstValueFrom(
      this.http.post<ApiEnvelope<string>>(`${USER_BASE}/notification-prefs`, prefs)
    );
    if (res.status !== 'SUCCESS') {
      throw new Error(envelopeErrorMessage(res) || 'Could not update notification preferences.');
    }
    await this.refreshUser();
  }

  async requestPasswordResetOtp(username: string): Promise<string> {
    const res = await firstValueFrom(
      this.http.post<ApiEnvelope<unknown>>(`${AUTH_BASE}/otp`, { id: username })
    );
    if (res.status !== 'SUCCESS') {
      throw new Error(envelopeErrorMessage(res) || 'Could not send the reset code.');
    }
    return res.message;
  }

  async resetPassword(username: string, otp: string, password: string): Promise<string> {
    const res = await firstValueFrom(
      this.http.post<ApiEnvelope<unknown>>(`${AUTH_BASE}/forgot-password`, { username, otp, password })
    );
    if (res.status !== 'SUCCESS') {
      throw new Error(envelopeErrorMessage(res) || 'Could not reset the password.');
    }
    return res.message;
  }

  logout(): void {
    this.userSignal.set(null);
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  private restoreUser(): AuthUser | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }
}
