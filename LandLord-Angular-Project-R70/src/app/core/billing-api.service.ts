import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiInvoice {
  id: number;
  tenantId: number;
  unitId: number;
  period: string;
  rent: number;
  utilitiesTotal: number;
  prevUnpaidRolled: number;
  amount: number;
  balance: number;
  status: 'unpaid' | 'partial' | 'paid';
  dueDate: string;
  createdAt: string;
}

export interface ApiPayment {
  id: number;
  tenantId: number;
  invoiceId: number;
  amount: number;
  method: 'cash' | 'bank' | 'mobile' | 'online';
  status: 'confirmed' | 'pending' | 'rejected';
  date: string;
}

const BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class BillingApiService {
  private readonly http = inject(HttpClient);

  async invoicesForTenant(tenantId: number): Promise<ApiInvoice[]> {
    return firstValueFrom(this.http.get<ApiInvoice[]>(`${BASE}/invoices`, { params: { tenantId } }));
  }

  async paymentsForTenant(tenantId: number): Promise<ApiPayment[]> {
    return firstValueFrom(this.http.get<ApiPayment[]>(`${BASE}/payments`, { params: { tenantId } }));
  }

  async generateInvoice(tenantId: number, utilitiesTotal = 0): Promise<ApiInvoice> {
    return firstValueFrom(this.http.post<ApiInvoice>(`${BASE}/invoices/generate`, { tenantId, utilitiesTotal }));
  }

  async recordPayment(tenantId: number, invoiceId: number, amount: number, method: ApiPayment['method'] = 'cash'): Promise<ApiPayment> {
    return firstValueFrom(this.http.post<ApiPayment>(`${BASE}/payments`, { tenantId, invoiceId, amount, method }));
  }

  async outstandingBalance(tenantId: number): Promise<number> {
    return firstValueFrom(this.http.get<number>(`${BASE}/tenants/${tenantId}/outstanding-balance`));
  }
}
