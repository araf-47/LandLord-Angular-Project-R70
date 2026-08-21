/**
 * Cross-app contract, Part 1 Phase 3.
 *
 * This file's content is identical in both projects — LandLord's copy lives here,
 * BariVara's at `BariVara-Angular-Project-R70/src/app/core/shared-contracts.ts`.
 * There's no monorepo/package link between the two (they're genuinely separate
 * deployed sites), so this is kept in sync by hand. If you change one, change the
 * other the same way.
 *
 * Two kinds of shapes live here:
 *
 * 1. Concepts that are identical in both apps today (Conversation, AppNotification)
 *    — moved here instead of staying duplicated inline in each MockDataService.
 *
 * 2. The boundary-crossing DTOs — what actually flows between the two systems once
 *    Phase 15 makes the sync real. Defining the wire shape now means Phase 15 maps
 *    each app's internal model onto/from these instead of inventing the contract
 *    from scratch under backend time pressure. Until Phase 15, nothing calls these;
 *    each app still manages its own mock data independently.
 */

export interface Conversation {
  id: string;
  /** Booking request (or other originating context) id this conversation was
   *  started from, so a repeat "start conversation" click finds the existing
   *  thread instead of creating a duplicate. Absent on older seed conversations. */
  contextId?: string;
  withName: string;
  messages: { from: string; text: string; date: string }[];
}

export interface AppNotification {
  id: string;
  title: string;
  body: string;
  read: boolean;
}

/**
 * What LandLord sends when a unit goes vacant (c3040 "Auto-post ad to
 * BariVara.com"). BariVara turns this into a Listing with source: 'landlord-linked'.
 */
export interface VacancyAdSync {
  unitId: number;
  propertyName: string;
  address: string;
  district: string;
  area: string;
  propertyType: 'apartment' | 'room' | 'office';
  rent: number;
}

/**
 * What BariVara sends when someone requests a landlord-linked listing (e5045 "Sync
 * to LandLord core Marketplace & Leads"). LandLord turns this into a
 * MarketplaceRequest. `tenantId` is BariVara's own tenant id, not a LandLord one —
 * LandLord has no record of this person unless/until it approves them.
 */
export interface BookingRequestSync {
  unitId: number;
  applicantName: string;
  barivaraTenantId: number;
  message?: string;
}

/**
 * What LandLord sends back once a unit is filled or an ad is paused (e5125
 * "Approve, mark unit filled, take down ad"), so BariVara can update or remove the
 * corresponding listing.
 */
export interface UnitStatusSync {
  unitId: number;
  status: 'vacant' | 'occupied';
  adPaused: boolean;
}
