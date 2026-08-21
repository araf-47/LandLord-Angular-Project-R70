/**
 * Stopgap for "who is logged in" on real-backend pages, ahead of real auth
 * (Phase 7 — same hold applies to BariVara). Mirrors LandLord's
 * CURRENT_TENANT_ID_REAL. Replace every usage once login issues a real session.
 */
export const CURRENT_TENANT_ID_REAL = 1;
