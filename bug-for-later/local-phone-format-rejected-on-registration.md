# Local-format phone (no +880) rejected at account creation — both apps

**Found:** 2026-08-29/30, during UI/UX plan v1 browser click-through (tenant booking-request flow test + plan verification pass).

**Apps: confirmed in both.**
- BariVara frontend (`localhost:4201`), signup flow (`/auth/signup`).
- LandLord frontend (`localhost:4200`), landlord's own "Register tenant (walk-in)" form (`/landlord/tenants/register`) — **despite that form's own hint text reading "This becomes their login username — with or without +880, both work."** The hint is currently false.

## Repro (BariVara signup)

1. Go to `/auth/signup`, pick Tenant (or Apartment Owner — same code path).
2. Fill details, phone entered as local format: `01911223344`.
3. Reach step 4 (OTP), enter demo code `123456`, click "Verify & create account".
4. Fails with **"Invalid phone number (3 attempts left)"** even though the OTP code itself is correct.
5. No account row created (verified via DB: `select * from users where email = ...` returned 0 rows).

## Repro (LandLord walk-in tenant registration)

1. Log in as landlord, go to Tenant Management → "Register tenant (walk-in)".
2. Fill name/email/national ID/temp password, phone entered as local format: `01933445566`.
3. Pick a vacant unit, fill lease terms + deposit, click "Confirm agreement & assign unit".
4. Fails with **"Invalid phone number"** shown inline under the phone-adjacent fields — even though the form's own hint text says both formats work.
5. Retried the identical form with `+8801933445566` — succeeded immediately, tenant created and unit assigned.

## Workaround that works (both apps)

Enter the phone with the `+880` country code prefix instead of the local `0`-prefixed form.

## Root cause (likely)

`TenantController.java` (landlord-backend), around line 121-123, has this comment on the login/registration path:

```java
// signup step for LandLord-side tenants). Username is the phone number,
// normalized to local 0-prefixed form so "+8801..." and "01..." both land
// on the same username regardless of which format was typed at registration.
```

So `normalizeBdPhone` exists and is meant to make both formats land on the same username. But since the *same* "Invalid phone number" rejection happens on LandLord's own walk-in registration form too (a completely separate UI path that presumably hits the same backend validation), the actual validation check on the phone field appears to run **before** normalization, or normalization isn't applied at all on the account-creation/registration path — only later, e.g. at login. Whatever the real code path, both frontends currently only accept the `+880` form here, not the local `0` form.

## Impact

- Any tenant/owner registration in either app — LandLord's own walk-in tenant form and BariVara's public signup — fails if the person (or the landlord filling in on their behalf) types the phone number in the everyday local format everyone in Bangladesh actually uses.
- LandLord's form actively tells the user "with or without +880, both work," which is currently false — makes the error more confusing, not less.
- Not a regression from the UI/UX plan v1 work — pre-existing gap, found incidentally while testing the tenant booking flow and then re-checked during the plan's verification pass.

## Suggested fix

Find wherever phone validation runs ahead of/instead of `normalizeBdPhone` on the registration/account-creation path (likely shared between LandLord's walk-in tenant registration and BariVara's signup, since both fail identically) and apply the same local-vs-+880 normalization there before validating, so both formats are accepted consistently everywhere. Until fixed, either fix the LandLord form's hint text to stop claiming both formats work, or make the error message say the country code is required.
