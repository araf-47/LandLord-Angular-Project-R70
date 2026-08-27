import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Attaches the access token (and refresh token, for silent renewal) to every
 * request. Parts/auth rotates an expired-but-refreshable access token and
 * echoes the new one back in the x-access-token response header - captured
 * here so the next request already carries it, per Parts/auth/AUTH.md.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const accessToken = auth.getAccessToken();
  const refreshToken = auth.getRefreshToken();

  const authedReq = accessToken
    ? req.clone({
        setHeaders: {
          Authorization: `Bearer ${accessToken}`,
          ...(refreshToken ? { 'x-refresh-token': refreshToken } : {}),
        },
      })
    : req;

  return next(authedReq).pipe(
    tap((event: any) => {
      const rotated = event?.headers?.get?.('x-access-token');
      if (rotated) {
        auth.setAccessToken(rotated);
      }
    })
  );
};
