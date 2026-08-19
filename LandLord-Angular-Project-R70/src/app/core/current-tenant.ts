/**
 * Stopgap for "who is logged in" on tenant-side pages migrated to the real backend,
 * ahead of real auth (Phase 7). Mirrors the mock's CURRENT_TENANT_ID but as a real
 * numeric tenant id. Replace every usage once login issues a real session/token.
 */
export const CURRENT_TENANT_ID_REAL = 3;
