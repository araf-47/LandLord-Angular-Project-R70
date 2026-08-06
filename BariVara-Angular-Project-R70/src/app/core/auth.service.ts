import { Injectable, computed, signal } from '@angular/core';

export type UserRole = 'tenant' | 'owner' | 'landlord-linked';

export interface AuthUser {
  name: string;
  email: string;
  role: UserRole;
}

const STORAGE_KEY = 'barivara_auth_user';

/**
 * Frontend-only stub, same shape as the LandLord app's AuthService: no real
 * backend, persists to localStorage so refresh/route-guard checks behave like a
 * logged-in session. Deliberately not shared code with the LandLord app — they're
 * two separate deployed sites with no shared runtime (see project-plan.md Phase 3).
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
