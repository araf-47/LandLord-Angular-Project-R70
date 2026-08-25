import { test, expect } from '@playwright/test';
import { ADMIN, ADMIN_PW, auth, body, createUser, login, loginToken } from './helpers';

test.describe('token lifecycle', () => {
  test('a valid token is not rotated - no x-access-token header appears', async ({ request }) => {
    const token = await loginToken(request, ADMIN, ADMIN_PW);
    const res = await request.get('/api/v3/test/any', { headers: auth(token) });

    expect(res.status()).toBe(200);
    expect(res.headers()['x-access-token']).toBeUndefined();
  });

  test('logout-all revokes the access and refresh tokens, and a fresh login still works',
    async ({ request }) => {
      const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
      const user = await createUser(request, adminToken, 'USER');

      const loginRes = await body(await login(request, user.username, user.password));
      const accessToken = loginRes.data.accessToken;
      const refreshToken = loginRes.data.refreshToken;

      expect((await body(await request.get('/api/v3/test/any', { headers: auth(accessToken) }))).status)
        .toBe('SUCCESS');

      // Cross the second boundary: JWT iat has whole-second resolution, so the
      // service must refuse tokens issued in the same second as the revoke.
      await new Promise((r) => setTimeout(r, 1100));

      const logout = await request.post('/api/v3/user/logout-all', {
        headers: auth(accessToken), data: {},
      });
      expect((await body(logout)).status).toBe('SUCCESS');

      // The token is still unexpired and correctly signed; the watermark refuses it.
      const afterLogout = await request.get('/api/v3/test/any', { headers: auth(accessToken) });
      expect(afterLogout.status()).toBe(401);
      const afterBody = await body(afterLogout);
      expect(afterBody.status).toBe('SESSION_EXPIRED');
      expect(afterBody.message).toContain('revoked');

      // The refresh token predates the watermark too, so it cannot mint a replacement.
      const withRefresh = await request.get('/api/v3/test/any', {
        headers: { ...auth(accessToken), 'x-refresh-token': refreshToken },
      });
      expect((await body(withRefresh)).status).toBe('SESSION_EXPIRED');

      await new Promise((r) => setTimeout(r, 1100));
      const reLogin = await loginToken(request, user.username, user.password);
      expect((await body(await request.get('/api/v3/test/any', { headers: auth(reLogin) }))).status)
        .toBe('SUCCESS');
    });

  test('logout-all affects only the revoking user', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const revoking = await createUser(request, adminToken, 'USER');
    const bystander = await createUser(request, adminToken, 'USER');

    const revokingToken = await loginToken(request, revoking.username, revoking.password);
    const bystanderToken = await loginToken(request, bystander.username, bystander.password);

    await new Promise((r) => setTimeout(r, 1100));
    await request.post('/api/v3/user/logout-all', { headers: auth(revokingToken), data: {} });

    expect((await body(await request.get('/api/v3/test/any', { headers: auth(revokingToken) }))).status)
      .toBe('SESSION_EXPIRED');
    expect((await body(await request.get('/api/v3/test/any', { headers: auth(bystanderToken) }))).status)
      .toBe('SUCCESS');
  });

  test('a token with a tampered payload fails signature verification', async ({ request }) => {
    const token = await loginToken(request, ADMIN, ADMIN_PW);
    const [header, payloadPart, signature] = token.split('.');
    const claims = JSON.parse(Buffer.from(payloadPart, 'base64url').toString('utf8'));
    // Push the expiry far into the future.
    claims.exp = claims.exp + 10_000_000;
    const tampered = `${header}.${Buffer.from(JSON.stringify(claims)).toString('base64url')}.${signature}`;

    const res = await request.get('/api/v3/test/any', { headers: auth(tampered) });
    expect(res.status()).toBe(401);
    expect((await body(res)).status).not.toBe('SUCCESS');
  });

  test('a token dropped to an unsigned alg=none is refused', async ({ request }) => {
    const token = await loginToken(request, ADMIN, ADMIN_PW);
    const payloadPart = token.split('.')[1];
    const noneHeader = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64url');

    const res = await request.get('/api/v3/test/any', { headers: auth(`${noneHeader}.${payloadPart}.`) });
    expect(res.status()).toBe(401);
    expect((await body(res)).status).not.toBe('SUCCESS');
  });

  test('an Authorization header with the wrong scheme is treated as no credentials', async ({ request }) => {
    const res = await request.get('/api/v3/test/any', {
      headers: { Authorization: 'Basic YWRtaW46YWRtaW4=' },
    });
    expect(res.status()).toBe(401);
  });

  test('a password change invalidates tokens signed with the old hash', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    // Reset the password through the admin update endpoint.
    expect((await body(await request.post('/api/v3/user/update', {
      headers: auth(adminToken),
      data: { id: await userIdOf(request, adminToken, user.username), password: 'Rotated@Pass1' },
    }))).status).toBe('SUCCESS');

    // The old token's HMAC key was the previous hash, so it no longer verifies.
    const res = await request.get('/api/v3/test/any', { headers: auth(userToken) });
    expect(res.status()).toBe(401);

    expect((await body(await login(request, user.username, 'Rotated@Pass1'))).status).toBe('SUCCESS');
    expect((await body(await login(request, user.username, user.password))).status).toBe('BAD_CREDENTIALS');
  });
});

/**
 * There is no "get user by name" endpoint, so the id is recovered from the
 * blocked-user listing after deliberately locking the account. Keeping this in
 * one place documents the gap rather than scattering it.
 */
async function userIdOf(request: any, adminToken: string, username: string): Promise<number> {
  for (let i = 0; i < 5; i++) {
    await login(request, username, 'Definitely@Wrong9');
  }
  const list = await body(await request.get('/api/v3/user-block/list', { headers: auth(adminToken) }));
  const entry = list.data.find((u: any) => u.username === username);
  expect(entry, `user ${username} not found in blocked list`).toBeTruthy();
  // Unlock again so the caller gets a usable account back.
  await request.post('/api/v3/user-block/unblock', { headers: auth(adminToken), data: { id: username } });
  return entry.id;
}
