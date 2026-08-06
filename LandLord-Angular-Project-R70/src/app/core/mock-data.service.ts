import { Injectable, signal } from '@angular/core';
import { AppNotification, Conversation } from './shared-contracts';

export type { AppNotification, Conversation };

export interface Property {
  id: string;
  name: string;
  address: string;
}

/** Matches BariVara's PropertyType — needed here so a vacant unit's VacancyAdSync
 *  payload (shared-contracts.ts) has somewhere to actually get this value from. */
export type PropertyType = 'apartment' | 'room' | 'office';

export interface Unit {
  id: string;
  propertyId: string;
  unitNumber: string;
  rent: number;
  status: 'vacant' | 'occupied';
  propertyType: PropertyType;
  /** Vacant units auto-post to BariVara; the landlord can pause that listing without changing occupancy. */
  adPaused?: boolean;
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
  unitId: string;
  /** Billing month this invoice belongs to, e.g. '2026-08'. Immutable once created. */
  period: string;
  rent: number;
  utilities: number;
  /** Unpaid balance carried in from prior periods, snapshotted at generation time. */
  prevUnpaidRolled: number;
  amount: number;
  balance: number;
  status: 'unpaid' | 'partial' | 'paid';
  dueDate: string;
  createdAt: string;
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
  propertyId: string;
  category: string;
  description: string;
  amount: number;
  /** Who bears this cost — the landlord absorbs it as a business expense, or it's tracked against a specific tenant. */
  bearer: 'landlord' | 'tenant';
  /** Set whenever bearer is 'tenant' — which tenant this cost is attributed to. */
  tenantId?: string;
  date: string;
}

/** One line of the cash book: a confirmed payment (income) or a logged expense (outflow). */
export interface LedgerEntry {
  id: string;
  date: string;
  type: 'income' | 'expense';
  description: string;
  propertyId?: string;
  amount: number;
}

export interface MaintenanceTicket {
  id: string;
  unitId: string;
  tenantId: string;
  description: string;
  status: 'pending' | 'resolved';
}

export interface MarketplaceRequest {
  id: string;
  unitId: string;
  applicantName: string;
  /** Set when the applicant is already a LandLord tenant (internal transfer request).
   *  Left unset for external BariVara marketplace applicants, who don't have a
   *  LandLord TenantRecord unless/until approved. */
  tenantId?: string;
  status: 'pending' | 'approved' | 'rejected';
}

let idCounter = 1000;
export function nextId(prefix: string): string {
  return `${prefix}-${idCounter++}`;
}

/** Frontend-only stand-in: the tenant area acts as if this tenant is signed in. */
export const CURRENT_TENANT_ID = 't-1';

/** 'YYYY-MM' key for the given date's calendar month. Sorts correctly as a string. */
export function periodKey(date: Date = new Date()): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

export function periodLabel(period: string): string {
  const [year, month] = period.split('-').map(Number);
  return new Date(year, month - 1, 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
}

function shiftPeriod(period: string, deltaMonths: number): string {
  const [year, month] = period.split('-').map(Number);
  return periodKey(new Date(year, month - 1 + deltaMonths, 1));
}

function periodDueDate(period: string, day = 5): string {
  const [year, month] = period.split('-').map(Number);
  return new Date(year, month - 1, day).toISOString().slice(0, 10);
}

/** Two months of billing history for the seed tenant, ending at the current month. */
function seedInvoices(): Invoice[] {
  const current = periodKey();
  const twoAgo = shiftPeriod(current, -2);
  const oneAgo = shiftPeriod(current, -1);
  const now = new Date().toISOString();

  return [
    {
      id: 'inv-1',
      tenantId: 't-1',
      unitId: 'u-1',
      period: twoAgo,
      rent: 15000,
      utilities: 0,
      prevUnpaidRolled: 0,
      amount: 15000,
      balance: 0,
      status: 'paid',
      dueDate: periodDueDate(twoAgo),
      createdAt: now,
    },
    {
      id: 'inv-2',
      tenantId: 't-1',
      unitId: 'u-1',
      period: oneAgo,
      rent: 15000,
      utilities: 1000,
      prevUnpaidRolled: 0,
      amount: 16000,
      balance: 6000,
      status: 'partial',
      dueDate: periodDueDate(oneAgo),
      createdAt: now,
    },
    {
      id: 'inv-3',
      tenantId: 't-1',
      unitId: 'u-1',
      period: current,
      rent: 15000,
      utilities: 0,
      prevUnpaidRolled: 6000,
      amount: 21000,
      balance: 21000,
      status: 'unpaid',
      dueDate: periodDueDate(current),
      createdAt: now,
    },
  ];
}

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
    { id: 'u-1', propertyId: 'p-1', unitNumber: 'A-101', rent: 15000, status: 'occupied', propertyType: 'apartment' },
    { id: 'u-2', propertyId: 'p-1', unitNumber: 'A-102', rent: 14000, status: 'vacant', propertyType: 'apartment' },
  ]);

  readonly tenants = signal<TenantRecord[]>([
    { id: 't-1', name: 'Rahim Uddin', phone: '01710000000', email: 'rahim@example.com', unitId: 'u-1', status: 'active' },
  ]);

  readonly agreements = signal<RentalAgreement[]>([
    { id: 'a-1', tenantId: 't-1', unitId: 'u-1', startDate: '2026-01-01', terms: '12-month lease', deposit: 30000 },
  ]);

  readonly invoices = signal<Invoice[]>(seedInvoices());

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

  currentPeriod(): string {
    return periodKey();
  }

  invoicesForPeriod(period: string): Invoice[] {
    return this.invoices().filter((i) => i.period === period);
  }

  /** A tenant's bills across all months, most recent first — the billing ledger. */
  invoicesForTenant(tenantId: string): Invoice[] {
    return this.invoices()
      .filter((i) => i.tenantId === tenantId)
      .sort((a, b) => b.period.localeCompare(a.period));
  }

  /** A tenant's payment attempts (confirmed, pending, rejected), most recent first. */
  paymentsForTenant(tenantId: string): PaymentRecord[] {
    return this.payments()
      .filter((p) => p.tenantId === tenantId)
      .sort((a, b) => b.date.localeCompare(a.date));
  }

  /** What a tenant currently owes, across every unpaid/partial month. */
  totalDueForTenant(tenantId: string): number {
    return this.invoicesForTenant(tenantId)
      .filter((i) => i.status !== 'paid')
      .reduce((sum, i) => sum + i.balance, 0);
  }

  /** What a tenant has actually paid, lifetime — confirmed payments only. */
  totalPaidForTenant(tenantId: string): number {
    return this.paymentsForTenant(tenantId)
      .filter((p) => p.status === 'confirmed')
      .reduce((sum, p) => sum + p.amount, 0);
  }

  /** Maintenance cost this tenant is responsible for (bearer: 'tenant'), not what the landlord absorbed. */
  maintenanceCostForTenant(tenantId: string): number {
    return this.maintenanceHistoryForTenant(tenantId)
      .filter((e) => e.bearer === 'tenant')
      .reduce((sum, e) => sum + e.amount, 0);
  }

  /** Every resolved maintenance cost tied to this tenant, whoever bore it, most recent first. */
  maintenanceHistoryForTenant(tenantId: string): ExpenseRecord[] {
    return this.expenses()
      .filter((e) => e.tenantId === tenantId && e.category === 'Maintenance')
      .sort((a, b) => b.date.localeCompare(a.date));
  }

  /** Every period that has bills, plus the current one, newest first — drives the month picker. */
  knownPeriods(): string[] {
    const periods = new Set(this.invoices().map((i) => i.period));
    periods.add(this.currentPeriod());
    return Array.from(periods).sort((a, b) => b.localeCompare(a));
  }

  /**
   * Creates this period's invoice for any active tenant that doesn't already have
   * one, rolling forward their unpaid balance from prior periods. Safe to call
   * repeatedly — never touches a period/tenant pair that already exists. This is
   * the one place monthly bill generation happens; a real cron job (Part 2) only
   * needs to call the backend equivalent of this method, not reimplement it.
   */
  ensureBillsGenerated(period: string): void {
    const alreadyBilled = new Set(this.invoicesForPeriod(period).map((i) => i.tenantId));
    const activeTenants = this.tenants().filter((t) => t.status === 'active' && t.unitId && !alreadyBilled.has(t.id));
    if (!activeTenants.length) return;

    const now = new Date().toISOString();
    const newInvoices: Invoice[] = activeTenants.map((t) => {
      const rent = this.units().find((u) => u.id === t.unitId)?.rent ?? 0;
      const prevUnpaidRolled = this.invoices()
        .filter((i) => i.tenantId === t.id && i.period < period)
        .reduce((sum, i) => sum + i.balance, 0);
      const amount = rent + prevUnpaidRolled;

      return {
        id: nextId('inv'),
        tenantId: t.id,
        unitId: t.unitId!,
        period,
        rent,
        utilities: 0,
        prevUnpaidRolled,
        amount,
        balance: amount,
        status: 'unpaid',
        dueDate: periodDueDate(period),
        createdAt: now,
      };
    });

    this.invoices.update((list) => [...list, ...newInvoices]);
  }

  /**
   * Applies a payment against a tenant's oldest unpaid period first, so the ledger
   * always clears debt in the order it was incurred. Shared by every payment entry
   * point (landlord receive-payment, tenant online pay, confirmed cash) so there is
   * one place to get this right.
   */
  applyPaymentToTenant(tenantId: string, amount: number): void {
    let remaining = amount;
    const oldestFirst = this.invoices()
      .filter((i) => i.tenantId === tenantId && i.status !== 'paid')
      .sort((a, b) => a.period.localeCompare(b.period));

    const updates = new Map<string, Invoice>();
    for (const invoice of oldestFirst) {
      if (remaining <= 0) break;
      const applied = Math.min(remaining, invoice.balance);
      remaining -= applied;
      const newBalance = invoice.balance - applied;
      updates.set(invoice.id, { ...invoice, balance: newBalance, status: newBalance === 0 ? 'paid' : 'partial' });
    }

    if (updates.size) {
      this.invoices.update((list) => list.map((i) => updates.get(i.id) ?? i));
    }
  }

  /**
   * Resolves which property a tenant belongs to for ledger attribution. Checks the
   * tenant's current unit first, then falls back to their rental agreement — that
   * way payments made before a move-out still attribute correctly even after
   * `unitId` gets cleared.
   */
  propertyIdForTenant(tenantId: string): string | undefined {
    const tenant = this.tenants().find((t) => t.id === tenantId);
    const unitId = tenant?.unitId ?? this.agreements().find((a) => a.tenantId === tenantId)?.unitId;
    return this.units().find((u) => u.id === unitId)?.propertyId;
  }

  /**
   * The cash book: every confirmed payment (income) and every logged expense
   * (outflow), merged and sorted oldest first. Pass a propertyId to scope it to
   * one property; omit for the landlord-wide view.
   */
  ledgerEntries(propertyId?: string): LedgerEntry[] {
    const income: LedgerEntry[] = this.payments()
      .filter((p) => p.status === 'confirmed')
      .map((p) => ({
        id: p.id,
        date: p.date,
        type: 'income' as const,
        description: `Payment — ${this.tenants().find((t) => t.id === p.tenantId)?.name ?? 'Tenant'}`,
        propertyId: this.propertyIdForTenant(p.tenantId),
        amount: p.amount,
      }));

    const outflow: LedgerEntry[] = this.expenses().map((e) => ({
      id: e.id,
      date: e.date,
      type: 'expense' as const,
      description: e.description ? `${e.category} — ${e.description}` : e.category,
      propertyId: e.propertyId,
      amount: e.amount,
    }));

    return [...income, ...outflow]
      .filter((entry) => !propertyId || entry.propertyId === propertyId)
      .sort((a, b) => a.date.localeCompare(b.date));
  }
}
