import { test, expect } from '@playwright/test';
import { ADMIN, ADMIN_PW, auth, body, login, loginToken } from './helpers';

/**
 * Runs against the second service instance, started with
 * {@code auth.ip.block.enabled=true} (threshold 3 for unauthenticated and invalid-JWT
 * attempts) and its own database.
 *
 * IpBlockingServiceImpl, IpBlockingFilter and IpBlockController are each
 * {@code @ConditionalOnProperty}, so {@code /api/v3/ip-block/**} does not exist at all
 * on the default instance - these endpoints need their own service to be reachable.
 *
 * These tests deliberately block the runner's own address, so they are serialised and
 * each one restores reachability through the unblock endpoint. If that exemption
 * regresses, this file cannot finish.
 */
test.describe.configure({ mode: 'serial' });

test.describe('per-IP blocking', () => {
  /**
   * A token captured BEFORE anything is blocked.
   *
   * Not test convenience - it is the real recovery contract. The filter exempts
   * /api/v3/ip-block/unblock** but NOT /api/v3/auth/login, so once an address is
   * blocked no new token can be minted from it. Recovery needs a token that already
   * existed, or an out-of-band route.
   */
  let rescueToken: string;

  /**
   * Lifts any block on the caller's own address.
   *
   * Does not consult /ip-block/list first: only the unblock endpoints are exempt, so
   * a list call while blocked is itself a 429. Both loopback spellings are covered
   * because which one the container reports depends on IPv4 vs IPv6; unblocking an
   * address that was never blocked is a documented no-op.
   */
  async function unblockSelf(request: any, adminToken: string) {
    for (const address of ['127.0.0.1', '0:0:0:0:0:0:0:1', '::1']) {
      await request.post('/api/v3/ip-block/unblock', {
        headers: auth(adminToken), data: { id: address },
      });
    }
  }

  async function listBlocked(request: any, adminToken: string) {
    return body(await request.post('/api/v3/ip-block/list', {
      headers: auth(adminToken), data: { pageNumber: 0, pageSize: 50, filter: {} },
    }));
  }

  test.beforeEach(async ({ request }) => {
    rescueToken = await loginToken(request, ADMIN, ADMIN_PW);
    await unblockSelf(request, rescueToken);
  });

  test.afterEach(async ({ request }) => {
    await unblockSelf(request, rescueToken);
  });

  test('the ip-block endpoints exist on this instance and require an ADMIN token', async ({ request }) => {
    const list = await request.post('/api/v3/ip-block/list', {
      headers: auth(rescueToken), data: { pageNumber: 0, pageSize: 10, filter: {} },
    });

    expect(list.status()).toBe(200);
    const payload = await body(list);
    expect(payload.status).toBe('SUCCESS');
    expect(payload).toHaveProperty('totalElements');
    expect(payload).toHaveProperty('totalPages');

    expect((await request.post('/api/v3/ip-block/list', {
      data: { pageNumber: 0, pageSize: 10, filter: {} },
    })).status()).toBe(401);
  });

  test('attempts accumulate below the threshold and are classified, without blocking',
    async ({ request }) => {
      // Two of three. The row exists and is inspectable precisely because the
      // threshold has not been reached yet - once it trips, unblocking deletes it.
      for (let i = 0; i < 2; i++) {
        expect((await request.get('/api/v3/test/any')).status()).toBe(401);
      }

      const row = (await listBlocked(request, rescueToken)).data[0];
      expect(row).toBeTruthy();
      expect(row.active).toBe(false);
      expect(row.failedUnauthenticatedAttempts).toBe(2);
      expect(row.lastFailureType).toBe('UNAUTHENTICATED');
      expect(row.reason).toBe('Unauthenticated access attempt');
      expect(row.unblockAt).toBeFalsy();
    });

  test('the third unauthenticated attempt trips the block and every path returns 429',
    async ({ request }) => {
      for (let i = 0; i < 3; i++) {
        expect((await request.get('/api/v3/test/any')).status()).toBe(401);
      }

      const blocked = await request.get('/api/v3/test/any');
      expect(blocked.status()).toBe(429);
      const payload = await body(blocked);
      expect(payload.status).toBe('IP_BLOCKED');
      expect(payload.message).toContain('24 hours');

      // Login is a public URL, but the IP filter runs ahead of the whole security
      // chain, so being public does not help.
      const loginWhileBlocked = await login(request, ADMIN, ADMIN_PW);
      expect(loginWhileBlocked.status()).toBe(429);
      expect((await body(loginWhileBlocked)).status).toBe('IP_BLOCKED');

      // Listing is not exempt either - only unblocking is.
      expect((await request.post('/api/v3/ip-block/list', {
        headers: auth(rescueToken), data: { pageNumber: 0, pageSize: 50, filter: {} },
      })).status()).toBe(429);
    });

  test('the health endpoint answers even while the address is blocked', async ({ request }) => {
    for (let i = 0; i < 4; i++) {
      await request.get('/api/v3/test/any');
    }
    expect((await request.get('/api/v3/test/any')).status()).toBe(429);

    // A liveness probe must never be gated by a security counter, or a blocked
    // address makes the instance look dead and it gets pulled from rotation.
    const health = await request.get('/actuator/health');
    expect(health.status()).toBe(200);
    expect(await health.text()).toContain('UP');
  });

  test('the unblock endpoint stays reachable while blocked and restores service',
    async ({ request }) => {
      for (let i = 0; i < 4; i++) {
        await request.get('/api/v3/test/any');
      }
      expect((await request.get('/api/v3/test/any')).status()).toBe(429);

      // The only way back in from this address.
      await unblockSelf(request, rescueToken);

      // Service restored: an unauthenticated call is a plain 401 again, and login works.
      expect((await request.get('/api/v3/test/any')).status()).toBe(401);
      expect((await body(await login(request, ADMIN, ADMIN_PW))).status).toBe('SUCCESS');

      // Unblocking DELETES the record rather than deactivating it, so the counter
      // restarts. The 401 probe above re-created a row - and it reads 1, not 5,
      // which is what proves the old record was really removed.
      const fresh = (await listBlocked(request, rescueToken)).data;
      expect(fresh).toHaveLength(1);
      expect(fresh[0].failedUnauthenticatedAttempts).toBe(1);
      expect(fresh[0].active).toBe(false);
    });

  test('login is NOT exempt - recovery requires a token obtained beforehand', async ({ request }) => {
    for (let i = 0; i < 4; i++) {
      await request.get('/api/v3/test/any');
    }

    // Deliberate: exempting /auth/login would gut the brute-force protection this
    // feature exists for. The consequence is that an operator blocked with no live
    // token must recover out of band.
    expect((await login(request, ADMIN, ADMIN_PW)).status()).toBe(429);

    // The pre-existing token still works against the exempt endpoint.
    await unblockSelf(request, rescueToken);
    expect((await body(await login(request, ADMIN, ADMIN_PW))).status).toBe('SUCCESS');
  });

  test('malformed JWTs are classified as INVALID_JWT and trip their own threshold',
    async ({ request }) => {
      for (let i = 0; i < 2; i++) {
        expect((await request.get('/api/v3/test/any', { headers: auth('not-a-jwt') })).status()).toBe(401);
      }

      const row = (await listBlocked(request, rescueToken)).data[0];
      expect(row.lastFailureType).toBe('INVALID_JWT');
      expect(row.reason).toBe('Invalid JWT token');
      // INVALID_JWT shares the failedLoginAttempts counter but has its own limit.
      expect(row.failedLoginAttempts).toBe(2);
      expect(row.active).toBe(false);

      expect((await request.get('/api/v3/test/any', { headers: auth('not-a-jwt') })).status()).toBe(401);
      expect((await request.get('/api/v3/test/any', { headers: auth('not-a-jwt') })).status()).toBe(429);
    });

  test('a wrong password is recorded as a LOGIN attempt against the username', async ({ request }) => {
    expect((await body(await login(request, ADMIN, 'Definitely@Wrong9'))).status).toBe('BAD_CREDENTIALS');

    const row = (await listBlocked(request, rescueToken)).data[0];
    expect(row.username).toBe(ADMIN);
    expect(row.lastFailureType).toBe('LOGIN');
    expect(row.reason).toBe('Invalid credentials');
    expect(row.failedLoginAttempts).toBe(1);
  });

  test('unblock-user clears every record attributed to that username', async ({ request }) => {
    await login(request, ADMIN, 'Definitely@Wrong9');
    expect((await listBlocked(request, rescueToken)).data.filter((r: any) => r.username === ADMIN))
      .toHaveLength(1);

    const cleared = await body(await request.post('/api/v3/ip-block/unblock-user', {
      headers: auth(rescueToken), data: { id: ADMIN },
    }));
    expect(cleared.status).toBe('SUCCESS');
    expect(cleared.message).toContain(`IP entries for user ${ADMIN}`);

    expect((await listBlocked(request, rescueToken)).data.filter((r: any) => r.username === ADMIN))
      .toHaveLength(0);
  });

  test('unblock-user for a username with no records reports nothing found', async ({ request }) => {
    const res = await body(await request.post('/api/v3/ip-block/unblock-user', {
      headers: auth(rescueToken), data: { id: 'pwNoSuchUserAnywhere' },
    }));

    expect(res.status).toBe('SUCCESS');
    expect(res.message).toBe('No blocked IP entries found for user pwNoSuchUserAnywhere');
  });

  test('a successful login clears that user own accumulated IP records', async ({ request }) => {
    await login(request, ADMIN, 'Definitely@Wrong9');
    expect((await listBlocked(request, rescueToken)).data.length).toBeGreaterThan(0);

    expect((await body(await login(request, ADMIN, ADMIN_PW))).status).toBe('SUCCESS');

    // AuthProvider calls unblockAllForUser on success, so a legitimate user cannot
    // be locked out by their own earlier typos.
    expect((await listBlocked(request, rescueToken)).data.filter((r: any) => r.username === ADMIN))
      .toHaveLength(0);
  });

  test('an unblock for an address that was never blocked is a no-op, not an error',
    async ({ request }) => {
      const res = await body(await request.post('/api/v3/ip-block/unblock', {
        headers: auth(rescueToken), data: { id: '203.0.113.199' },
      }));

      expect(res.status).toBe('SUCCESS');
      expect(res.message).toContain('was not blocked');
    });
});
