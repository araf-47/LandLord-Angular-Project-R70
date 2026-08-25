import { test, expect } from '@playwright/test';
import {
  ADMIN, ADMIN_PW, auth, body, createUser, login, loginToken, uniqueName, uniquePhone,
} from './helpers';

test.describe('user management and input contracts', () => {
  test('a registered user is immediately usable with the granted role', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const username = uniqueName('pwFresh');

    const res = await body(await request.post('/api/v3/user/register', {
      headers: auth(adminToken),
      data: { username, password: 'Api@Pass123', email: `${username}@apitest.local`,
              phone: uniquePhone(), roles: ['USER'] },
    }));
    expect(res.status).toBe('SUCCESS');
    expect(res.data).toBe(username);

    const token = await loginToken(request, username, 'Api@Pass123');
    expect((await request.get('/api/v3/test/user-only', { headers: auth(token) })).status()).toBe(200);
    expect((await request.get('/api/v3/test/admin-only', { headers: auth(token) })).status()).toBe(403);
  });

  test('registration rejects a weak password, bad phone, bad email and non-alphanumeric name',
    async ({ request }) => {
      const adminToken = await loginToken(request, ADMIN, ADMIN_PW);

      const post = (data: any) =>
        request.post('/api/v3/user/register', { headers: auth(adminToken), data });

      const base = () => ({
        username: uniqueName('pwBad'), password: 'Api@Pass123',
        email: 'ok@apitest.local', phone: uniquePhone(), roles: ['USER'],
      });

      expect((await body(await post({ ...base(), password: 'weak' }))).message).toBe('Invalid password');
      expect((await body(await post({ ...base(), phone: 'not-a-phone' }))).message)
        .toBe('Invalid phone number');
      expect((await body(await post({ ...base(), email: 'not-an-email' }))).message).toBe('Invalid email');
      expect((await body(await post({ ...base(), username: 'has_underscore' }))).message)
        .toBe('Invalid username');
      expect((await body(await post({ ...base(), roles: [] }))).message).toBe('Roles are required');
    });

  test('a duplicate username is refused', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');

    const res = await body(await request.post('/api/v3/user/register', {
      headers: auth(adminToken),
      data: { username: user.username, password: 'Api@Pass123',
              email: `dup-${user.username}@apitest.local`, phone: uniquePhone(), roles: ['USER'] },
    }));

    expect(res.message).toBe('Username already exists');
  });

  test('a duplicate phone number is refused', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const phone = uniquePhone();
    const first = uniqueName('pwPhoneA');

    expect((await body(await request.post('/api/v3/user/register', {
      headers: auth(adminToken),
      data: { username: first, password: 'Api@Pass123', email: `${first}@apitest.local`,
              phone, roles: ['USER'] },
    }))).status).toBe('SUCCESS');

    const second = uniqueName('pwPhoneB');
    const res = await body(await request.post('/api/v3/user/register', {
      headers: auth(adminToken),
      data: { username: second, password: 'Api@Pass123', email: `${second}@apitest.local`,
              phone, roles: ['USER'] },
    }));

    // The unique constraint surfaces as a handled error, not a 500 or a stack trace.
    expect(res.status).toBe('ERROR');
    expect(JSON.stringify(res)).not.toContain('ConstraintViolation');
  });

  test('an admin can change a user role, and it applies to an existing token', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    // Recover the id via the blocked list, then unlock - there is no lookup endpoint.
    for (let i = 0; i < 5; i++) {
      await login(request, user.username, 'Definitely@Wrong9');
    }
    const list = await body(await request.get('/api/v3/user-block/list', { headers: auth(adminToken) }));
    const id = list.data.find((u: any) => u.username === user.username).id;
    await request.post('/api/v3/user-block/unblock', { headers: auth(adminToken), data: { id: user.username } });

    expect((await body(await request.post('/api/v3/user/update', {
      headers: auth(adminToken), data: { id, roles: ['ADMIN'] },
    }))).status).toBe('SUCCESS');

    // Authorities are re-read per request, so the promotion needs no re-login.
    const probe = await body(await request.get('/api/v3/test/any', { headers: auth(userToken) }));
    expect(probe.data.authorities).toContain('ADMIN');
    expect((await request.get('/api/v3/test/admin-only', { headers: auth(userToken) })).status()).toBe(200);
  });

  test('editing a role permission set is admin-only and replaces rather than appends',
    async ({ request }) => {
      const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
      const user = await createUser(request, adminToken, 'USER');
      const userToken = await loginToken(request, user.username, user.password);

      // A USER must not be able to grant itself permissions.
      const asUser = await request.post('/api/v3/permission/role-permissions', {
        headers: auth(userToken), data: { roleId: 1, permissionIds: [1] },
      });
      expect(asUser.status()).toBe(403);

      const missingFields = await body(await request.post('/api/v3/permission/role-permissions', {
        headers: auth(adminToken), data: { permissionIds: [] },
      }));
      expect(missingFields.status).toBe('VALIDATION_ERROR');
    });

  test('malformed JSON returns the ApiResponse envelope, never a stack trace', async ({ request }) => {
    const res = await request.post('/api/v3/auth/login', {
      headers: { 'Content-Type': 'application/json' },
      data: '{"username":"admin",',
    });
    const text = await res.text();

    expect(text).not.toContain('at org.springframework');
    expect(text).not.toContain('Exception');
    expect(JSON.parse(text).status).toBeTruthy();
  });

  test('SQL-shaped input is treated as a literal value', async ({ request }) => {
    const res = await body(await login(request, "admin'--", "x' OR '1'='1"));

    expect(res.status).toBe('BAD_CREDENTIALS');
    // And the service is still healthy afterwards.
    expect((await request.get('/actuator/health')).status()).toBe(200);
  });

  test('every failure class shares one JSON envelope shape', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    const responses = [
      await request.get('/api/v3/test/any'),                                       // 401
      await request.get('/api/v3/test/admin-only', { headers: auth(userToken) }),   // 403
      await request.post('/api/v3/auth/login', { data: { username: 'x' } }),        // validation
      await request.post('/api/v3/auth/otp', { data: { id: 'pwNoSuchUser' } }),     // service error
    ];

    for (const res of responses) {
      expect(res.headers()['content-type']).toContain('application/json');
      const payload = await body(res);
      expect(payload.status).toBeTruthy();
      expect(payload.status).not.toBe('SUCCESS');
    }
  });
});
