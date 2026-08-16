import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, UserRole } from './auth.service';

export function roleGuard(role: UserRole): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/auth/login']);
    }
    if (auth.role() !== role) {
      return router.createUrlTree([auth.role() === 'landlord' ? '/landlord' : '/tenant']);
    }
    return true;
  };
}
