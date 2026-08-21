/**
 * Known dev URL for the BariVara app, per Part 1 Phase 3.4. Both apps default to
 * fixed ports (`npm start` here → 4200, BariVara's `npm start` → 4201) so they can
 * run side by side without a flag. Once real domains exist (Phase 19), swap this
 * for an environment-based config — for now it's a plain constant since there's
 * only ever one target: local dev.
 */
export const BARIVARA_DEV_URL = 'http://localhost:4201';
