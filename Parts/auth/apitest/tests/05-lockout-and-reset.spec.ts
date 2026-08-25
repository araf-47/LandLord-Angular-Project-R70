import { test, expect } from '@playwright/test';
import { ADMIN, ADMIN_PW, auth, body, createUser, login, loginToken, requestOtp } from './helpers';

test.describe('account lockout and password reset', () => {
  const WRONG = 'Definitely@Wrong9';

  test('five wrong passwords lock the account, and the right password is then refused',
    async ({ request }) => {
      const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
      const user = await createUser(request, adminToken, 'USER');

      for (let i = 0; i < 4; i++) {
        expect((await body(await login(request, user.username, WRONG))).status).toBe('BAD_CREDENTIALS');
      }
      // Still usable one attempt short of the limit.
      expect((await body(await login(request, user.username, user.password))).status).toBe('SUCCESS');

      for (let i = 0; i < 5; i++) {
        await login(request, user.username, WRONG);
      }

      const locked = await login(request, user.username, user.password);
      expect(locked.status()).toBe(401);
      const payload = await body(locked);
      expect(payload.status).toBe('ACCOUNT_LOCKED');
      expect(payload.message).toContain('temporarily locked');
    });

  test('a locked account is listed and can be unblocked by an admin', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');

    for (let i = 0; i < 5; i++) {
      await login(request, user.username, WRONG);
    }

    const list = await body(await request.get('/api/v3/user-block/list', { headers: auth(adminToken) }));
    expect(list.status).toBe('SUCCESS');
    const entry = list.data.find((u: any) => u.username === user.username);
    expect(entry).toBeTruthy();
    expect(entry.failedLoginAttempts).toBeGreaterThanOrEqual(5);
    expect(entry.lockedUntil).toBeTruthy();

    const unblock = await body(await request.post('/api/v3/user-block/unblock', {
      headers: auth(adminToken), data: { id: user.username },
    }));
    expect(unblock.status).toBe('SUCCESS');
    expect(unblock.message).toContain('unblocked successfully');

    expect((await body(await login(request, user.username, user.password))).status).toBe('SUCCESS');
  });

  test('unblocking an account that is not locked reports so rather than failing', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');

    const res = await body(await request.post('/api/v3/user-block/unblock', {
      headers: auth(adminToken), data: { id: user.username },
    }));

    expect(res.status).toBe('SUCCESS');
    expect(res.message).toContain('was not blocked');
  });

  test('forgot-password with a valid OTP resets the password and clears the lock',
    async ({ request }) => {
      const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
      const user = await createUser(request, adminToken, 'USER');

      for (let i = 0; i < 5; i++) {
        await login(request, user.username, WRONG);
      }
      expect((await body(await login(request, user.username, user.password))).status).toBe('ACCOUNT_LOCKED');

      const otp = await requestOtp(request, user.username);
      const newPassword = 'Reset@Pass456';

      const reset = await body(await request.post('/api/v3/auth/forgot-password', {
        data: { username: user.username, password: newPassword, otp },
      }));
      expect(reset.status).toBe('SUCCESS');
      expect(reset.message).toBe('Password reset successful');

      // The reset is the documented self-service unlock path.
      expect((await body(await login(request, user.username, newPassword))).status).toBe('SUCCESS');
      expect((await body(await login(request, user.username, user.password))).status).toBe('BAD_CREDENTIALS');
    });

  test('forgot-password with a wrong OTP changes nothing', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    await requestOtp(request, user.username);

    const res = await body(await request.post('/api/v3/auth/forgot-password', {
      data: { username: user.username, password: 'Reset@Pass456', otp: '000000' },
    }));

    expect(res.status).toBe('ERROR');
    expect(res.message).toBe('Invalid otp');
    expect((await body(await login(request, user.username, user.password))).status).toBe('SUCCESS');
  });

  test('forgot-password enforces the password policy before any OTP work', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');

    const res = await body(await request.post('/api/v3/auth/forgot-password', {
      data: { username: user.username, password: 'weak', otp: '123456' },
    }));

    expect(res.status).toBe('VALIDATION_ERROR');
    expect(res.data.password).toContain('8 and 16 characters');
  });

  test('change-password requires both the OTP and the old password', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    const otp = await requestOtp(request, user.username);
    const wrongOld = await body(await request.post('/api/v3/user/change-password', {
      headers: auth(userToken),
      data: { password: 'Changed@Pass1', oldPassword: 'Definitely@Wrong9', otp },
    }));
    expect(wrongOld.status).toBe('ERROR');
    expect(wrongOld.message).toBe('Old password is incorrect');

    // That attempt consumed the OTP, so a fresh one is needed.
    const freshOtp = await requestOtp(request, user.username);
    const ok = await body(await request.post('/api/v3/user/change-password', {
      headers: auth(userToken),
      data: { password: 'Changed@Pass1', oldPassword: user.password, otp: freshOtp },
    }));
    expect(ok.status).toBe('SUCCESS');

    expect((await body(await login(request, user.username, 'Changed@Pass1'))).status).toBe('SUCCESS');
    expect((await body(await login(request, user.username, user.password))).status).toBe('BAD_CREDENTIALS');
  });

  test('change-password rejects a wrong OTP without touching the password', async ({ request }) => {
    const adminToken = await loginToken(request, ADMIN, ADMIN_PW);
    const user = await createUser(request, adminToken, 'USER');
    const userToken = await loginToken(request, user.username, user.password);

    const res = await body(await request.post('/api/v3/user/change-password', {
      headers: auth(userToken),
      data: { password: 'Changed@Pass1', oldPassword: user.password, otp: '000000' },
    }));

    expect(res.status).toBe('ERROR');
    expect(res.message).toBe('Otp is incorrect');
    expect((await body(await login(request, user.username, user.password))).status).toBe('SUCCESS');
  });
});
