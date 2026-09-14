import { Injectable, inject } from '@angular/core';
import { BillingApiService } from './billing-api.service';
import { MaintenanceApiService } from './maintenance-api.service';
import { PropertyApiService } from './property-api.service';
import { UnitApiService } from './unit-api.service';
import { periodLabel } from './mock-data.service';

export interface MonthlySummaryPoint {
  period: string;
  label: string;
  collected: number;
  expenses: number;
  net: number;
}

export interface CollectionRatePoint {
  period: string;
  label: string;
  rate: number;
}

export interface PropertyOccupancy {
  propertyId: number;
  name: string;
  occupied: number;
  total: number;
}

const MONTHS_BACK = 6;

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly billingApi = inject(BillingApiService);
  private readonly maintenanceApi = inject(MaintenanceApiService);
  private readonly propertyApi = inject(PropertyApiService);
  private readonly unitApi = inject(UnitApiService);

  private recentPeriods(months = MONTHS_BACK): string[] {
    const now = new Date();
    const periods: string[] = [];
    for (let i = months - 1; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      periods.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
    }
    return periods;
  }

  async monthlySummary(months = MONTHS_BACK): Promise<MonthlySummaryPoint[]> {
    const [payments, expenses] = await Promise.all([
      this.billingApi.allPayments(),
      this.maintenanceApi.allExpenses(),
    ]);

    return this.recentPeriods(months).map((period) => {
      const collected = payments
        .filter((p) => p.status === 'confirmed' && p.date.startsWith(period))
        .reduce((sum, p) => sum + p.amount, 0);
      const expensesTotal = expenses
        .filter((e) => e.date.startsWith(period))
        .reduce((sum, e) => sum + e.amount, 0);
      return { period, label: periodLabel(period), collected, expenses: expensesTotal, net: collected - expensesTotal };
    });
  }

  async collectionRate(months = MONTHS_BACK): Promise<CollectionRatePoint[]> {
    const periods = this.recentPeriods(months);
    const invoicesByPeriod = await Promise.all(periods.map((period) => this.billingApi.invoicesForPeriod(period)));

    return periods.map((period, i) => {
      const invoices = invoicesByPeriod[i];
      const billed = invoices.reduce((sum, inv) => sum + inv.amount, 0);
      const outstanding = invoices.reduce((sum, inv) => sum + inv.balance, 0);
      const rate = billed === 0 ? 0 : Math.round(((billed - outstanding) / billed) * 100);
      return { period, label: periodLabel(period), rate };
    });
  }

  /** Current occupancy snapshot per property — not a time series, no history table exists for that. */
  async occupancyByProperty(): Promise<PropertyOccupancy[]> {
    if (this.propertyApi.properties().length === 0) {
      await this.propertyApi.load();
    }
    await this.unitApi.load();
    const units = this.unitApi.units();

    return this.propertyApi.properties().map((p) => {
      const propertyUnits = units.filter((u) => u.propertyId === p.id);
      return {
        propertyId: p.id,
        name: p.name,
        occupied: propertyUnits.filter((u) => u.status === 'occupied').length,
        total: propertyUnits.length,
      };
    });
  }
}
