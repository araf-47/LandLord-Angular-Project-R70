import { test, expect } from '@playwright/test';
import { ADMIN, ADMIN_PW, auth, body, createUser, loginToken } from './helpers';

test.describe('role and permission enforcement', () => {
  test('an ADMIN-only URL admits ADMIN and refuses USER with 403', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    const asAdmin = await request.get('/api/v3/test/admin-only', { headers: auth(adminToken) });
    expect(asAdmin.status()).toBe(200);
    expect((await body(asAdmin)).status).toBe('SUCCESS');

    const asUser = await request.get('/api/v3/test/admin-only', { headers: auth(userToken) });
    expect(asUser.status()).toBe(403);
    expect((await body(asUser)).status).toBe('ACCESS_DENIED');
  });

  test('roles are not hierarchical - a USER-only URL refuses ADMIN', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    expect((await request.get('/api/v3/test/user-only', { headers: auth(userToken) })).status()).toBe(200);
    // Authorities are matched literally; ADMIN is not a superset of USER.
    expect((await request.get('/api/v3/test/user-only', { headers: auth(adminToken) })).status()).toBe(403);
  });

  test('@PreAuthorize enforces on top of the URL rule', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    // permissions.json grants /method-secured to both roles, so the URL layer lets
    // the USER through - the method annotation is what stops it.
    expect((await request.get('/api/v3/test/method-secured', { headers: auth(adminToken) })).status()).toBe(200);
    expect((await request.get('/api/v3/test/method-secured', { headers: auth(userToken) })).status()).toBe(403);
  });

  test('a wildcard rule covers every path beneath it', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    // /api/v3/user-block/** is ADMIN-only.
    expect((await request.get('/api/v3/user-block/list', { headers: auth(adminToken) })).status()).toBe(200);
    expect((await request.get('/api/v3/user-block/list', { headers: auth(userToken) })).status()).toBe(403);
  });

  test('a user sees exactly their own role permissions', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    const res = await request.get('/api/v3/permission/get-user-permissions', { headers: auth(userToken) });
    const names = (await body(res)).data.map((p: any) => p.name);

    expect(names).toEqual(expect.arrayContaining(['TEST_USER_ONLY', 'MANAGE_PASSWORD', 'TOGGLE_2FA']));
    expect(names).not.toContain('REGISTER_USER');
    expect(names).not.toContain('LIST_ROLES');
  });

  test('permission entries carry the url and route the UI needs', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const res = await request.get('/api/v3/permission/get-user-permissions', { headers: auth(adminToken) });
    const listRoles = (await body(res)).data.find((p: any) => p.name === 'LIST_ROLES');

    expect(listRoles).toBeTruthy();
    expect(listRoles.url).toBe('/api/v3/role/list');
    expect(listRoles.route).toBe('/dashboard/roles');
  });

  test('role listing is ADMIN-only and hides the default admin role', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    const res = await request.get('/api/v3/role/list', { headers: auth(adminToken) });
    const roles = (await body(res)).data;
    expect(roles).toContain('USER');
    expect(roles).not.toContain('ADMIN');

    expect((await request.get('/api/v3/role/list', { headers: auth(userToken) })).status()).toBe(403);
  });

  test('a non-admin cannot register users', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    const res = await request.post('/api/v3/user/register', {
      headers: auth(userToken),
      data: { username: 'pwSneaky', password: 'Api@Pass123', email: 'x@apitest.local',
              phone: '+8801799999999', roles: ['ADMIN'] },
    });

    expect(res.status()).toBe(403);
    expect((await body(res)).status).toBe('ACCESS_DENIED');
  });
});
