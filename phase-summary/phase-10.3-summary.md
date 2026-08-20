Phase 10.3 done — auto bill generation.

Backend: extracted bill-generation math into BillingService (shared by the manual "Generate bills" button and the new job), added MonthlyBillingScheduler with @Scheduled(cron = "0 0 0 1 * *") — fires midnight on the 1st of each month, bills every active tenant with a unit assigned who doesn't already have an invoice this period (same rule as the manual button). Auto-generated bills carry rent + rollover; utilities can still be added per-bill after.

Verified live: temporarily dropped the cron to every 10s, registered a throwaway test tenant, watched the job bill it on the next tick and correctly skip it on the one after — then reverted to the real monthly schedule, cleaned up test data, restarted clean.

project-plan.md updated (10.3 marked done, 10.8 flagged held). Both backends (landlord-backend :8080, barivara-backend :8081) and both frontends (:4200, :4201) are running now if you want to click through.

10.8 (payment gateway) parked per your call — noted in memory so it won't get pushed as next-step. Remaining open items: 10.5 (receipt PDF), Phase 15 (real cross-system sync), or whatever else you'd rather tackle.
