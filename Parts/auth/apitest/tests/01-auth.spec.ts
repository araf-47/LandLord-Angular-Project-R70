import { test, expect } from '@playwright/test';
import { ADMIN, ADMIN_PW, auth, body, createUser, login, loginToken, uniqueName, uniquePhone } from './helpers';

test.describe('login and bearer authentication', () => {
  test('the seeded administrator can authenticate', async ({ request }) => {
    const res = await login(request, ADMIN, ADMIN_PW);
    const payload = await body(res);

    expect(res.status()).toBe(200);
    expect(payload.status).toBe('SUCCESS');
    expect(payload.message).toBe('Login successful');
    expect(payload.data.accessToken).toBeTruthy();
    expect(payload.data.refreshToken).toBeTruthy();
    expect(payload.data.tokenType).toBe('Bearer');
    expect(payload.data.expiresInSeconds).toBeGreaterThan(0);
  });

  test('the access token is a three-segment JWT whose subject is the username', async ({ request }) => {
    const token = await loginToken(request, ADMIN, ADMIN_PW);
    const parts = token.split('.');

    expect(parts).toHaveLength(3);
    const claims = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
    expect(claims.sub).toBe(ADMIN);
    expect(claims.exp).toBeGreaterThan(claims.iat);
  });

  test('the token authenticates a protected endpoint and reports the authorities', async ({ request }) => {
    const token = await loginToken(request, ADMIN, ADMIN_PW);
    const res = await request.get('/api/v3/test/any', { headers: auth(token) });
    const payload = await body(res);

    expect(res.status()).toBe(200);
    expect(payload.status).toBe('SUCCESS');
    expect(payload.data.username).toBe(ADMIN);
    expect(payload.data.authorities).toContain('ADMIN');
  });

  test('a protected endpoint with no credentials is 401 ACCESS_DENIED', async ({ request }) => {
    const res = await request.get('/api/v3/test/any');

    expect(res.status()).toBe(401);
    expect((await body(res)).status).toBe('ACCESS_DENIED');
    expect(res.headers()['content-type']).toContain('application/json');
  });

  test('a malformed bearer token is refused and the token headers are blanked', async ({ request }) => {
    const res = await request.get('/api/v3/test/any', { headers: auth('not-a-jwt') });

    expect(res.status()).toBe(401);
    expect((await body(res)).status).toBe('ACCESS_DENIED');
    // The service blanks these so a client stops replaying a bad token.
    expect(res.headers()['x-access-token']).toBe('');
    expect(res.headers()['x-refresh-token']).toBe('');
  });

  test('a token signed with another user key does not authenticate', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const victim = await createUser(request, adminToken, 'ADMIN', 'pwVictim');
    const attacker = await createUser(request, adminToken, 'ADMIN', 'pwAttacker');

    // Take the attacker's own valid token and swap only the subject claim. The
    // signature no longer matches, because tokens are keyed per user.
    const attackerToken = await loginToken(request, attacker.username, attacker.password);
    const [header, payloadPart, signature] = attackerToken.split('.');
    const claims = JSON.parse(Buffer.from(payloadPart, 'base64url').toString('utf8'));
    claims.sub = victim.username;
    const forgedPayload = Buffer.from(JSON.stringify(claims)).toString('base64url');
    const forged = `${header}.${forgedPayload}.${signature}`;

    const res = await request.get('/api/v3/test/any', { headers: auth(forged) });
    expect(res.status()).toBe(401);
    expect((await body(res)).status).not.toBe('SUCCESS');
  });

  test('a wrong password is BAD_CREDENTIALS', async ({ request }) => {
    const res = await login(request, ADMIN, 'Wrong@Pass999');

    expect(res.status()).toBe(401);
    expect((await body(res)).status).toBe('BAD_CREDENTIALS');
  });

  test('an unknown username is indistinguishable from a wrong password', async ({ request }) => {
    const unknown = await body(await login(request, uniqueName('pwGhost'), ADMIN_PW));
    const wrongPassword = await body(await login(request, ADMIN, 'Wrong@Pass999'));

    // No username-enumeration oracle: same status, same message.
    expect(unknown.status).toBe('BAD_CREDENTIALS');
    expect(unknown.status).toBe(wrongPassword.status);
    expect(unknown.message).toBe(wrongPassword.message);
  });

  test('a login body missing a field is a validation error, not an auth failure', async ({ request }) => {
    const res = await request.post('/api/v3/auth/login', { data: { username: ADMIN } });
    const payload = await body(res);

    expect(res.status()).toBe(200);
    expect(payload.status).toBe('VALIDATION_ERROR');
    expect(payload.data.password).toBe('Password is required');
  });

  test('the response never carries a password field', async ({ request }) => {
    const res = await login(request, ADMIN, ADMIN_PW);
    expect(await res.text()).not.toContain('password');
  });

  test('no session cookie is issued - the service is stateless', async ({ request }) => {
    const res = await login(request, ADMIN, ADMIN_PW);
    const cookies = res.headersArray().filter((h) => h.name.toLowerCase() === 'set-cookie');
    expect(cookies.map((c) => c.value).join(';')).not.toContain('JSESSIONID');
  });

  test('the health endpoint is reachable without credentials', async ({ request }) => {
    const res = await request.get('/actuator/health');

    expect(res.status()).toBe(200);
    expect(await res.text()).toContain('UP');
  });

  test('an unmapped path is protected rather than 404-open', async ({ request }) => {
    const res = await request.get('/api/v3/no/such/endpoint');
    expect(res.status()).toBe(401);
  });
});
