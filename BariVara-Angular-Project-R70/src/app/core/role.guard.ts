import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, UserRole } from './auth.service';

const HOME_BY_ROLE: Record<UserRole, string> = {
  tenant: '/tenant',
  owner: '/owner',
  'landlord-linked': '/landlord-linked',
};

export function roleGuard(role: UserRole): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/auth/login']);
    }
    const currentRole = auth.role();
    if (currentRole !== role) {
      return router.createUrlTree([currentRole ? HOME_BY_ROLE[currentRole] : '/']);
    }
    return true;
  };
}
