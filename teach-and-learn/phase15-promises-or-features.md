Three things Phase 15 build:

1. Unit goes vacant → ad auto-appears on BariVara. Landlord marks unit vacant, LandLord backend calls BariVara backend, a real listing gets created there automatically. No manual "post ad" step anymore for landlord-linked units.
2. Someone books on BariVara → request lands in LandLord's inbox. Tenant submits booking request on BariVara, that gets pushed over to LandLord's Marketplace & Leads section, landlord sees it and can approve/reject from LandLord side.
3. Landlord approves/fills unit → BariVara ad disappears or updates. Once landlord approves a tenant or unit gets occupied, LandLord tells BariVara to take the ad down or mark it taken. Ad won't sit there stale.

Then a full end-to-end test: vacate unit → ad appears → booking → landlord approves → tenant registered → ad removed. Prove the whole loop actually works live, not just each piece in isolation.

Uses the VacancyAdSync/BookingRequestSync/UnitStatusSync contracts we just fixed the types on — this is literally what those were built for, sitting unused until now.

Net effect: "connected" stops being just visual/UI-level and becomes actually real — data flows between the two apps automatically.
