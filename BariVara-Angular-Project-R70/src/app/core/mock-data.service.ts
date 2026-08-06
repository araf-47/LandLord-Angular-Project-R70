import { Injectable, signal } from '@angular/core';

export interface OwnerProperty {
  id: string;
  ownerId: string;
  name: string;
  address: string;
  district: string;
  area: string;
}

export interface OwnerUnit {
  id: string;
  propertyId: string;
  unitNumber: string;
  rent: number;
}

/** A marketplace ad. Mirrors the LandLord app's Unit shape where concepts overlap
 *  (unitNumber, rent, status) — see project-plan.md Phase 2.9. */
export type PropertyType = 'apartment' | 'room' | 'office';

export interface Listing {
  id: string;
  ownerId: string;
  unitId?: string;
  /** Who posted this — an independent apartment owner, or a unit synced (read-only,
   *  from BariVara's side) from an existing LandLord core landlord account. */
  source: 'owner' | 'landlord-linked';
  title: string;
  address: string;
  district: string;
  area: string;
  propertyType: PropertyType;
  rent: number;
  status: 'active' | 'paused' | 'taken';
}

export const DISTRICTS = ['Dhaka', 'Chattogram', 'Sylhet', 'Khulna', 'Rajshahi'];

export const AREAS_BY_DISTRICT: Record<string, string[]> = {
  Dhaka: ['Dhanmondi', 'Banani', 'Gulshan', 'Mirpur', 'Bashundhara R/A', 'Mogbazar', 'Uttara'],
  Chattogram: ['Agrabad', 'Nasirabad', 'Khulshi'],
  Sylhet: ['Zindabazar', 'Shahjalal Upashahar'],
  Khulna: ['Sonadanga', 'Khalishpur'],
  Rajshahi: ['Shaheb Bazar', 'Uposhohor'],
};

export const PROPERTY_TYPES: { value: PropertyType; label: string }[] = [
  { value: 'apartment', label: 'Flat / Apartment' },
  { value: 'room', label: 'Room / Sublet' },
  { value: 'office', label: 'Office Space' },
];

export interface Favorite {
  id: string;
  tenantId: string;
  listingId: string;
}

/** Mirrors the LandLord app's MarketplaceRequest shape. */
export interface BookingRequest {
  id: string;
  listingId: string;
  tenantId: string;
  applicantName: string;
  status: 'pending' | 'approved' | 'rejected';
}

export interface Conversation {
  id: string;
  withName: string;
  messages: { from: string; text: string; date: string }[];
}

export interface AppNotification {
  id: string;
  title: string;
  body: string;
  read: boolean;
}

export interface TenantProfile {
  id: string;
  name: string;
  email: string;
  phone: string;
}

export interface OwnerProfile {
  id: string;
  name: string;
  email: string;
  phone: string;
}

let idCounter = 1000;
export function nextId(prefix: string): string {
  return `${prefix}-${idCounter++}`;
}

/** Frontend-only stand-ins: the tenant/owner areas act as if these are signed in. */
export const CURRENT_TENANT_ID = 'tenant-1';
export const CURRENT_OWNER_ID = 'owner-1';

/**
 * In-memory data store standing in for a backend, same pattern as the LandLord
 * app's MockDataService. Seeded so a manual demo walkthrough tells one coherent
 * story alongside the LandLord app: the landlord-linked listing below uses the same
 * unit number/rent as the vacant unit in the LandLord app's seed data (A-102,
 * 14,000) — not runtime-linked, just narratively consistent for a demo.
 */
@Injectable({ providedIn: 'root' })
export class MockDataService {
  readonly tenantProfile = signal<TenantProfile>({
    id: CURRENT_TENANT_ID,
    name: 'Karim Hossain',
    email: 'karim@example.com',
    phone: '01720000000',
  });

  readonly ownerProfile = signal<OwnerProfile>({
    id: CURRENT_OWNER_ID,
    name: 'Nasrin Akhter',
    email: 'nasrin@example.com',
    phone: '01730000000',
  });

  readonly ownerProperties = signal<OwnerProperty[]>([
    { id: 'op-1', ownerId: CURRENT_OWNER_ID, name: 'Lakeview Residence', address: 'Road 5, Banani, Dhaka', district: 'Dhaka', area: 'Banani' },
  ]);

  readonly ownerUnits = signal<OwnerUnit[]>([
    { id: 'ou-1', propertyId: 'op-1', unitNumber: 'B-201', rent: 18000 },
  ]);

  readonly listings = signal<Listing[]>([
    {
      id: 'listing-1',
      ownerId: CURRENT_OWNER_ID,
      unitId: 'ou-1',
      source: 'owner',
      title: 'Lakeview Residence — B-201',
      address: 'Road 5, Banani, Dhaka',
      district: 'Dhaka',
      area: 'Banani',
      propertyType: 'apartment',
      rent: 18000,
      status: 'active',
    },
    {
      id: 'listing-2',
      ownerId: 'landlord-core',
      source: 'landlord-linked',
      title: 'Green View Apartments — A-102',
      address: 'Road 12, Dhanmondi, Dhaka',
      district: 'Dhaka',
      area: 'Dhanmondi',
      propertyType: 'apartment',
      rent: 14000,
      status: 'active',
    },
  ]);

  readonly favorites = signal<Favorite[]>([]);

  readonly bookingRequests = signal<BookingRequest[]>([
    { id: 'br-1', listingId: 'listing-1', tenantId: CURRENT_TENANT_ID, applicantName: 'Karim Hossain', status: 'pending' },
  ]);

  readonly conversations = signal<Conversation[]>([
    {
      id: 'conv-1',
      withName: 'Nasrin Akhter',
      messages: [{ from: 'Nasrin Akhter', text: 'Is the unit still available for viewing this weekend?', date: '2026-08-05' }],
    },
  ]);

  readonly notifications = signal<AppNotification[]>([
    { id: 'note-1', title: 'Booking request submitted', body: 'Your request for Lakeview Residence — B-201 was sent to the owner.', read: false },
  ]);

  listingById(id: string): Listing | undefined {
    return this.listings().find((l) => l.id === id);
  }

  unitsByProperty(propertyId: string): OwnerUnit[] {
    return this.ownerUnits().filter((u) => u.propertyId === propertyId);
  }

  listingsByOwner(ownerId: string): Listing[] {
    return this.listings().filter((l) => l.ownerId === ownerId);
  }

  favoritesForTenant(tenantId: string): Favorite[] {
    return this.favorites().filter((f) => f.tenantId === tenantId);
  }

  requestsForListing(listingId: string): BookingRequest[] {
    return this.bookingRequests().filter((r) => r.listingId === listingId);
  }

  requestsForOwner(ownerId: string): BookingRequest[] {
    const ownerListingIds = new Set(this.listingsByOwner(ownerId).map((l) => l.id));
    return this.bookingRequests().filter((r) => ownerListingIds.has(r.listingId));
  }
}
