import { defineConfig } from '@playwright/test';

/**
 * Black-box HTTP tests against a running auth service.
 *
 * No browser is involved: every test uses Playwright's `request` fixture, which
 * is a plain HTTP client. The point of this suite - as distinct from the Java
 * integration tests - is that it talks to a *deployed* jar over the network with
 * no access to the Spring context, the database, or any test hook. If something
 * only works because a test shares the JVM, it fails here.
 *
 * Workers are pinned to 1: the service has global per-IP and per-account state
 * (lockout counters, OTP budgets), so parallel workers coming from the same
 * loopback address would interfere with each other.
 */
const MAIN_URL = process.env.AUTH_BASE_URL ?? 'http://localhost:18080';
const IPBLOCK_URL = process.env.AUTH_IPBLOCK_BASE_URL ?? 'http://localhost:18081';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : [['list']],
  timeout: 30_000,
  expect: { timeout: 10_000 },
  use: {
    extraHTTPHeaders: { Accept: 'application/json' },
    // Error responses are the subject of most assertions here, so a non-2xx must
    // never throw on its own.
    ignoreHTTPSErrors: true,
  },

  /*
   * Two instances, because IP blocking is a different bean graph rather than a
   * runtime flag: IpBlockingServiceImpl, IpBlockingFilter and IpBlockController
   * are each @ConditionalOnProperty, so /api/v3/ip-block/** simply does not exist
   * unless the service was started with blocking on. The blocking instance also
   * gets its own database, since a block is persistent state that would otherwise
   * leak into the main suite - and it runs last, because it deliberately blocks
   * the runner's own address.
   */
  projects: [
    {
      name: 'api',
      use: { baseURL: MAIN_URL },
      testIgnore: /ip-blocking\.spec\.ts/,
    },
    {
      name: 'ip-blocking',
      use: { baseURL: IPBLOCK_URL },
      testMatch: /ip-blocking\.spec\.ts/,
    },
  ],

  webServer: process.env.AUTH_BASE_URL
    ? undefined
    : [
        {
          command: 'bash ./start-service.sh',
          url: `${MAIN_URL}/actuator/health`,
          reuseExistingServer: !process.env.CI,
          timeout: 120_000,
          stdout: 'pipe',
          stderr: 'pipe',
        },
        {
          command: 'bash ./start-service-ipblock.sh',
          url: `${IPBLOCK_URL}/actuator/health`,
          reuseExistingServer: !process.env.CI,
          timeout: 120_000,
          stdout: 'pipe',
          stderr: 'pipe',
        },
      ],
});
