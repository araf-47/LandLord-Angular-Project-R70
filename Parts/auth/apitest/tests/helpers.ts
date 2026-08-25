import { APIRequestContext, expect } from '@playwright/test';
import { readdirSync, readFileSync, rmSync, existsSync } from 'fs';
import { join } from 'path';

export const ADMIN = process.env.ADMIN_USERNAME ?? 'admin';
export const ADMIN_PW = process.env.ADMIN_PASSWORD ?? 'Admin@12345';

const MAIL_SINK = process.env.MAIL_SINK_DIR ?? join(__dirname, '..', '.mail-sink');

/**
 * Every test seeds its own uniquely-named users. There is no database access from
 * here, so nothing can be truncated between tests - unique names are what keeps
 * the suite rerunnable and order-independent.
 */
let seq = 0;
export function uniqueName(prefix: string): string {
  seq += 1;
  return `${prefix}${Date.now().toString(36)}${seq}`;
}

export function uniquePhone(): string {
  // users.phone is unique; 13 digits keeps it inside the E.164 pattern.
  const n = (Date.now() % 1_000_000_000) * 10 + (seq % 10);
  return `+8801${String(n).slice(0, 9)}`;
}

export type ApiBody = { status?: string; message?: string; data?: any };

/** Reads the ApiResponse envelope, asserting it really is one. */
export async function body(res: { json: () => Promise<any>; text: () => Promise<string> }): Promise<ApiBody> {
  const text = await res.text();
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(`Response was not JSON: ${text}`);
  }
}

export async function statusOf(res: any): Promise<string | undefined> {
  return (await body(res)).status;
}

export async function login(request: APIRequestContext, username: string, password: string, otp?: string) {
  return request.post('/api/v3/auth/login', {
    data: otp === undefined ? { username, password } : { username, password, otp },
  });
}

export async function loginToken(request: APIRequestContext, username: string, password: string): Promise<string> {
  const res = await login(request, username, password);
  const payload = await body(res);
  expect(payload.status, `login failed for ${username}: ${JSON.stringify(payload)}`).toBe('SUCCESS');
  return payload.data.accessToken;
}

export function auth(token: string) {
  return { Authorization: `Bearer ${token}` };
}

/** Registers a user through the admin API and returns its credentials. */
export async function createUser(
  request: APIRequestContext,
  adminToken: string,
  role: string,
  prefix = 'pwUser',
): Promise<{ username: string; password: string }> {
  const username = uniqueName(prefix);
  const password = 'Api@Pass123';
  const res = await request.post('/api/v3/user/register', {
    headers: auth(adminToken),
    data: { username, password, email: `${username}@apitest.local`, phone: uniquePhone(), roles: [role] },
  });
  const payload = await body(res);
  expect(payload.status, `register failed: ${JSON.stringify(payload)}`).toBe('SUCCESS');
  return { username, password };
}

/**
 * Reads the newest OTP out of the mail sink.
 *
 * The service stores OTPs BCrypt-hashed and never returns one, so a black-box
 * test has no other way to obtain it. FileMailSink is the local stand-in for a
 * mail catcher and is enabled only by start-service.sh.
 */
export function latestOtp(): string {
  if (!existsSync(MAIL_SINK)) {
    throw new Error(`Mail sink ${MAIL_SINK} does not exist - is mail.sink.enabled=true?`);
  }
  const files = readdirSync(MAIL_SINK).filter((f) => f.endsWith('.json')).sort();
  if (files.length === 0) {
    throw new Error(`No mail captured in ${MAIL_SINK}`);
  }
  const mail = JSON.parse(readFileSync(join(MAIL_SINK, files[files.length - 1]), 'utf8'));
  const otp = mail?.templateModel?.otp;
  if (!otp) {
    throw new Error(`Captured mail had no otp: ${JSON.stringify(mail)}`);
  }
  return String(otp);
}

export function clearMailSink(): void {
  if (existsSync(MAIL_SINK)) {
    rmSync(MAIL_SINK, { recursive: true, force: true });
  }
}

/** Requests an OTP for a username and returns the delivered code. */
export async function requestOtp(request: APIRequestContext, username: string): Promise<string> {
  clearMailSink();
  const res = await request.post('/api/v3/auth/otp', { data: { id: username } });
  expect(await statusOf(res), `otp request failed: ${await res.text()}`).toBe('SUCCESS');
  return latestOtp();
}
