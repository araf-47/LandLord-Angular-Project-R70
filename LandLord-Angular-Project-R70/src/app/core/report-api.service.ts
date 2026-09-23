import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface IncomeStatementRow {
  propertyId: number;
  propertyName: string;
  billed: number;
  collected: number;
  outstanding: number;
  collectionRatePercent: number;
}

export interface IncomeStatementReport {
  startDate: string | null;
  endDate: string | null;
  rows: IncomeStatementRow[];
  totalBilled: number;
  totalCollected: number;
  totalOutstanding: number;
  collectionRatePercent: number;
}

export interface ExpenseReportRow {
  category: string;
  landlordAmount: number;
  tenantAmount: number;
  total: number;
  count: number;
}

export interface ExpenseReport {
  startDate: string | null;
  endDate: string | null;
  rows: ExpenseReportRow[];
  totalAmount: number;
}

export interface OccupancyReportRow {
  propertyId: number;
  propertyName: string;
  totalUnits: number;
  occupiedUnits: number;
  vacantUnits: number;
  occupancyRatePercent: number;
}

export interface OccupancyReport {
  rows: OccupancyReportRow[];
  totalUnits: number;
  totalOccupied: number;
  occupancyRatePercent: number;
}

export interface TenantLedgerRow {
  period: string;
  amount: number;
  paid: number;
  balance: number;
  status: string;
  dueDate: string | null;
}

export interface TenantLedgerReport {
  tenantId: number;
  tenantName: string;
  rows: TenantLedgerRow[];
  totalInvoiced: number;
  totalPaid: number;
  totalOutstanding: number;
}

export interface ReportFilters {
  startDate?: string;
  endDate?: string;
  propertyId?: number;
  category?: string;
  tenantId?: number;
}

const BASE = 'http://localhost:8080/api/reports';

@Injectable({ providedIn: 'root' })
export class ReportApiService {
  private readonly http = inject(HttpClient);

  async incomeStatement(filters: ReportFilters = {}): Promise<IncomeStatementReport> {
    return firstValueFrom(
      this.http.get<IncomeStatementReport>(`${BASE}/income-statement`, { params: this.toParams(filters) })
    );
  }

  downloadIncomeStatementPdf(filters: ReportFilters = {}): Promise<void> {
    return this.download('income-statement.pdf', filters);
  }

  downloadIncomeStatementExcel(filters: ReportFilters = {}): Promise<void> {
    return this.download('income-statement.xlsx', filters);
  }

  async expenseReport(filters: ReportFilters = {}): Promise<ExpenseReport> {
    return firstValueFrom(
      this.http.get<ExpenseReport>(`${BASE}/expense-report`, { params: this.toParams(filters) })
    );
  }

  downloadExpenseReportPdf(filters: ReportFilters = {}): Promise<void> {
    return this.download('expense-report.pdf', filters);
  }

  downloadExpenseReportExcel(filters: ReportFilters = {}): Promise<void> {
    return this.download('expense-report.xlsx', filters);
  }

  async occupancyReport(filters: ReportFilters = {}): Promise<OccupancyReport> {
    return firstValueFrom(
      this.http.get<OccupancyReport>(`${BASE}/occupancy-report`, { params: this.toParams(filters) })
    );
  }

  downloadOccupancyReportPdf(filters: ReportFilters = {}): Promise<void> {
    return this.download('occupancy-report.pdf', filters);
  }

  downloadOccupancyReportExcel(filters: ReportFilters = {}): Promise<void> {
    return this.download('occupancy-report.xlsx', filters);
  }

  async tenantLedger(filters: ReportFilters): Promise<TenantLedgerReport> {
    return firstValueFrom(
      this.http.get<TenantLedgerReport>(`${BASE}/tenant-ledger`, { params: this.toParams(filters) })
    );
  }

  downloadTenantLedgerPdf(filters: ReportFilters): Promise<void> {
    return this.download('tenant-ledger.pdf', filters);
  }

  downloadTenantLedgerExcel(filters: ReportFilters): Promise<void> {
    return this.download('tenant-ledger.xlsx', filters);
  }

  downloadFullReportPdf(filters: ReportFilters = {}): Promise<void> {
    return this.download('full-report.pdf', filters);
  }

  downloadFullReportExcel(filters: ReportFilters = {}): Promise<void> {
    return this.download('full-report.xlsx', filters);
  }

  private async download(path: string, filters: ReportFilters): Promise<void> {
    const blob = await firstValueFrom(
      this.http.get(`${BASE}/${path}`, { params: this.toParams(filters), responseType: 'blob' })
    );
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = path;
    link.click();
    URL.revokeObjectURL(url);
  }

  private toParams(filters: ReportFilters): Record<string, string> {
    const params: Record<string, string> = {};
    if (filters.startDate) params['startDate'] = filters.startDate;
    if (filters.endDate) params['endDate'] = filters.endDate;
    if (filters.propertyId) params['propertyId'] = String(filters.propertyId);
    if (filters.category) params['category'] = filters.category;
    if (filters.tenantId) params['tenantId'] = String(filters.tenantId);
    return params;
  }
}
