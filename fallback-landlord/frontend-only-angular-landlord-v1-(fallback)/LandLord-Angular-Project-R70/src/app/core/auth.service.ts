import { Injectable, computed, signal } from '@angular/core';

export type UserRole = 'landlord' | 'tenant';

export interface AuthUser {
  name: string;
  email: string;
  role: UserRole;
}

const STORAGE_KEY = 'landlord_auth_user';

/**
 * Frontend-only stub: no real backend. Persists to localStorage so
 * refresh/route-guard checks behave like a logged-in session.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSignal = signal<AuthUser | null>(this.restore());

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.userSignal() !== null);
  readonly role = computed(() => this.userSignal()?.role ?? null);

  login(email: string, _password: string, role: UserRole): void {
    const user: AuthUser = { name: email.split('@')[0], email, role };
    this.persist(user);
  }

  signup(name: string, email: string, role: UserRole): void {
    const user: AuthUser = { name, email, role };
    this.persist(user);
  }

  logout(): void {
    this.userSignal.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  private persist(user: AuthUser): void {
    this.userSignal.set(user);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  }

  private restore(): AuthUser | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }
}
