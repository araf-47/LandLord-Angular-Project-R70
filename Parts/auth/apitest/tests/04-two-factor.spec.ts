import { test, expect } from '@playwright/test';
import {
  ADMIN, ADMIN_PW, auth, body, clearMailSink, createUser, latestOtp, login, loginToken, requestOtp,
} from './helpers';

test.describe('two-factor login and OTP', () => {
  test('an OTP request delivers a six-digit code to the user', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');

    const otp = await requestOtp(request, user.username);

    expect(otp).toMatch(/^\d{6}$/);
  });

  test('enabling 2FA makes the next login a challenge with no token', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    expect((await body(await request.post('/api/v3/user/toggle-2fa', {
      headers: auth(userToken), data: { id: true },
    }))).status).toBe('SUCCESS');

    clearMailSink();
    const challenge = await body(await login(request, user.username, user.password));
    expect(challenge.status).toBe('OTP_REQUIRED');
    expect(challenge.message).toBe('OTP required');
    expect(challenge.data).toBeFalsy();

    // The challenge is only useful if the code was actually delivered.
    const otp = latestOtp();
    const completed = await body(await login(request, user.username, user.password, otp));
    expect(completed.status).toBe('SUCCESS');
    expect(completed.data.accessToken).toBeTruthy();
  });

  test('a wrong OTP is INVALID_OTP even with the right password', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);
    await request.post('/api/v3/user/toggle-2fa', { headers: auth(userToken), data: { id: true } });

    clearMailSink();
    await login(request, user.username, user.password);
    const real = latestOtp();
    const wrong = real === '000000' ? '111111' : '000000';

    const res = await login(request, user.username, user.password, wrong);
    expect(res.status()).toBe(401);
    expect((await body(res)).status).toBe('INVALID_OTP');
  });

  test('an OTP is single-use', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);
    await request.post('/api/v3/user/toggle-2fa', { headers: auth(userToken), data: { id: true } });

    clearMailSink();
    await login(request, user.username, user.password);
    const otp = latestOtp();

    expect((await body(await login(request, user.username, user.password, otp))).status).toBe('SUCCESS');
    expect((await body(await login(request, user.username, user.password, otp))).status).toBe('INVALID_OTP');
  });

  test('an OTP issued for one account cannot complete another login', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const owner = await createUser(request, adminToken, 'USER');
    const other = await createUser(request, adminToken, 'USER');
    const otherToken = await loginToken(request, other.username, other.password);
    await request.post('/api/v3/user/toggle-2fa', { headers: auth(otherToken), data: { id: true } });

    const ownerOtp = await requestOtp(request, owner.username);

    expect((await body(await login(request, other.username, other.password, ownerOtp))).status)
      .toBe('INVALID_OTP');
  });

  test('OTP generation is capped at three per window', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');

    for (let i = 0; i < 3; i++) {
      const res = await request.post('/api/v3/auth/otp', { data: { id: user.username } });
      expect((await body(res)).status).toBe('SUCCESS');
    }

    const fourth = await body(await request.post('/api/v3/auth/otp', { data: { id: user.username } }));
    expect(fourth.status).toBe('ERROR');
    expect(fourth.message).toContain('Too many OTP requests');
  });

  test('clearing the OTP cache restores the generation budget', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');

    for (let i = 0; i < 3; i++) {
      await request.post('/api/v3/auth/otp', { data: { id: user.username } });
    }
    expect((await body(await request.post('/api/v3/auth/otp', { data: { id: user.username } }))).message)
      .toContain('Too many OTP requests');

    expect((await body(await request.post('/api/v3/user/clear-otp-cache', {
      headers: auth(adminToken), data: { id: user.username },
    }))).status).toBe('SUCCESS');

    expect((await body(await request.post('/api/v3/auth/otp', { data: { id: user.username } }))).status)
      .toBe('SUCCESS');
  });

  test('an OTP for an unknown user reports not-found and delivers nothing', async ({ request }) => {
    clearMailSink();
    const res = await body(await request.post('/api/v3/auth/otp', { data: { id: 'pwNoSuchUserAtAll' } }));

    expect(res.status).toBe('ERROR');
    expect(res.message).toBe('User not found');
    expect(() => latestOtp()).toThrow();
  });

  test('a null id is a validation error', async ({ request }) => {
    const res = await body(await request.post('/api/v3/auth/otp', { data: {} }));

    expect(res.status).toBe('VALIDATION_ERROR');
    expect(res.data.id).toBe('ID is required');
  });
});
