/**
 * Known dev URL for the LandLord core app, per Part 1 Phase 3.4. Both apps default
 * to fixed ports (this app's `npm start` → 4201, LandLord's `npm start` → 4200) so
 * they can run side by side without a flag. Once real domains exist (Phase 19), swap
 * this for an environment-based config — for now it's a plain constant since
 * there's only ever one target: local dev.
 */
export const LANDLORD_CORE_DEV_URL = 'http://localhost:4200';
