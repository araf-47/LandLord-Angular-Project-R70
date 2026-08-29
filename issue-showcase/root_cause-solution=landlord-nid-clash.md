Confirmed root cause. Not a NID clash at all — the frontend error message is wrong.

Real cause: password 12345678 fails Parts/auth's strength policy — needs 8-16 chars with at least one lowercase, one uppercase, one digit, one special char. Yours has none of the letters/special char, so registration hits Invalid password (409 from the user-creation step, before the tenant row is even saved).

Why the UI lied to you: tenant-register.component.ts:136-140 catches any 409 and blindly shows "This National ID is already registered to an active tenant" — it never reads the actual message field the backend sent back. Real message was "Invalid password".

Fix: use a password like Tenant@2026 (has upper/lower/digit/special, 8-16 chars), and I should fix the frontend to surface the real backend message instead of guessing. Want me to fix that error-handling bug now?
