import { Injectable, signal } from '@angular/core';

export interface Property {
  id: string;
  name: string;
  address: string;
}

export interface Unit {
  id: string;
  propertyId: string;
  unitNumber: string;
  rent: number;
  status: 'vacant' | 'occupied';
}

export interface TenantRecord {
  id: string;
  name: string;
  phone: string;
  email: string;
  unitId?: string;
  status: 'active' | 'inactive';
}

export interface RentalAgreement {
  id: string;
  tenantId: string;
  unitId: string;
  startDate: string;
  terms: string;
  deposit: number;
}

export interface Invoice {
  id: string;
  tenantId: string;
  amount: number;
  balance: number;
  status: 'unpaid' | 'partial' | 'paid';
  dueDate: string;
}

export interface PaymentRecord {
  id: string;
  tenantId: string;
  invoiceId: string;
  amount: number;
  method: 'cash' | 'bank' | 'mobile' | 'online';
  status: 'confirmed' | 'pending' | 'rejected';
  date: string;
}

export interface ExpenseRecord {
  id: string;
  category: string;
  description: string;
  amount: number;
  tag: 'property' | 'tenant';
}

export interface MaintenanceTicket {
  id: string;
  unitId: string;
  tenantId: string;
  description: string;
  status: 'pending' | 'resolved';
}

export interface Conversation {
  id: string;
  withName: string;
  messages: { from: string; text: string; date: string }[];
}

export interface MarketplaceRequest {
  id: string;
  unitId: string;
  applicantName: string;
  status: 'pending' | 'approved' | 'rejected';
}

export interface AppNotification {
  id: string;
  title: string;
  body: string;
  read: boolean;
}

let idCounter = 1000;
export function nextId(prefix: string): string {
  return `${prefix}-${idCounter++}`;
}

/** Frontend-only stand-in: the tenant area acts as if this tenant is signed in. */
export const CURRENT_TENANT_ID = 't-1';

/**
 * In-memory data store standing in for a backend so the scaffolded pages
 * can list/add/edit records and demonstrate the flows from the diagrams.
 */
@Injectable({ providedIn: 'root' })
export class MockDataService {
  readonly properties = signal<Property[]>([
    { id: 'p-1', name: 'Green View Apartments', address: 'Road 12, Dhanmondi, Dhaka' },
  ]);

  readonly units = signal<Unit[]>([
    { id: 'u-1', propertyId: 'p-1', unitNumber: 'A-101', rent: 15000, status: 'occupied' },
    { id: 'u-2', propertyId: 'p-1', unitNumber: 'A-102', rent: 14000, status: 'vacant' },
  ]);

  readonly tenants = signal<TenantRecord[]>([
    { id: 't-1', name: 'Rahim Uddin', phone: '01710000000', email: 'rahim@example.com', unitId: 'u-1', status: 'active' },
  ]);

  readonly agreements = signal<RentalAgreement[]>([
    { id: 'a-1', tenantId: 't-1', unitId: 'u-1', startDate: '2026-01-01', terms: '12-month lease', deposit: 30000 },
  ]);

  readonly invoices = signal<Invoice[]>([
    { id: 'inv-1', tenantId: 't-1', amount: 15000, balance: 15000, status: 'unpaid', dueDate: '2026-08-05' },
  ]);

  readonly payments = signal<PaymentRecord[]>([]);

  readonly expenses = signal<ExpenseRecord[]>([]);

  readonly tickets = signal<MaintenanceTicket[]>([
    { id: 'tk-1', unitId: 'u-1', tenantId: 't-1', description: 'Kitchen faucet leaking', status: 'pending' },
  ]);

  readonly conversations = signal<Conversation[]>([
    {
      id: 'c-1',
      withName: 'Rahim Uddin',
      messages: [{ from: 'Rahim Uddin', text: 'When is the plumber coming?', date: '2026-08-04' }],
    },
  ]);

  readonly marketplaceRequests = signal<MarketplaceRequest[]>([
    { id: 'r-1', unitId: 'u-2', applicantName: 'Karim Hossain', status: 'pending' },
  ]);

  readonly notifications = signal<AppNotification[]>([
    { id: 'n-1', title: 'Rent due reminder', body: 'Your rent for August is due on the 5th.', read: false },
  ]);

  unitsByProperty(propertyId: string): Unit[] {
    return this.units().filter((u) => u.propertyId === propertyId);
  }

  tenantByUnit(unitId: string): TenantRecord | undefined {
    return this.tenants().find((t) => t.unitId === unitId);
  }
}
