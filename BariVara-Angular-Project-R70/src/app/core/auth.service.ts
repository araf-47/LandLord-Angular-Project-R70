import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type UserRole = 'tenant' | 'owner' | 'landlord-linked';

export interface AuthUser {
  username: string;
  role: UserRole;
}

const AUTH_BASE = 'http://localhost:8081/api/v3/auth';
const ME_URL = 'http://localhost:8081/api/auth/me';

const ACCESS_TOKEN_KEY = 'barivara_access_token';
const REFRESH_TOKEN_KEY = 'barivara_refresh_token';
const USER_KEY = 'barivara_auth_user';

const ROLE_MAP: Record<string, UserRole> = {
  TENANT: 'tenant',
  OWNER: 'owner',
  LANDLORD: 'landlord-linked',
};

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

interface MeResponse {
  username: string;
  roles: string[];
}

/**
 * Talks to the real backend (Parts/auth, embedded in barivara-backend,
 * separately from landlord-backend's own copy — deliberately no SSO between
 * the two apps, see project-plan.md Phase 7).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly userSignal = signal<AuthUser | null>(this.restore());

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.userSignal() !== null);
  readonly role = computed(() => this.userSignal()?.role ?? null);

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

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
    const role = me.roles.map((r) => ROLE_MAP[r]).find((r): r is UserRole => !!r) ?? 'tenant';
    const user: AuthUser = { username: me.username, role };
    this.userSignal.set(user);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  logout(): void {
    this.userSignal.set(null);
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  private restore(): AuthUser | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }
}
