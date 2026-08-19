import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ApiMaintenanceTicket {
  id: number;
  unitId: number;
  tenantId: number;
  description: string;
  status: 'pending' | 'resolved';
  cost: number | null;
  createdAt: string;
}

export interface ApiExpense {
  id: number;
  propertyId: number | null;
  ticketId: number | null;
  category: string;
  description: string;
  amount: number;
  bearer: 'landlord' | 'tenant';
  tenantId: number | null;
  date: string;
}

const BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class MaintenanceApiService {
  private readonly http = inject(HttpClient);

  readonly tickets = signal<ApiMaintenanceTicket[]>([]);

  async load(): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiMaintenanceTicket[]>(`${BASE}/maintenance-tickets`));
    this.tickets.set(result);
  }

  async loadForTenant(tenantId: number): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiMaintenanceTicket[]>(`${BASE}/maintenance-tickets`, { params: { tenantId } }));
    this.tickets.set(result);
  }

  async loadForUnit(unitId: number): Promise<void> {
    const result = await firstValueFrom(this.http.get<ApiMaintenanceTicket[]>(`${BASE}/maintenance-tickets`, { params: { unitId } }));
    this.tickets.set(result);
  }

  async ticket(id: number): Promise<ApiMaintenanceTicket> {
    return firstValueFrom(this.http.get<ApiMaintenanceTicket>(`${BASE}/maintenance-tickets/${id}`));
  }

  async createTicket(unitId: number, tenantId: number, description: string): Promise<ApiMaintenanceTicket> {
    const created = await firstValueFrom(this.http.post<ApiMaintenanceTicket>(`${BASE}/maintenance-tickets`, { unitId, tenantId, description }));
    this.tickets.update((list) => [...list, created]);
    return created;
  }

  async updateStatus(id: number, status: 'pending' | 'resolved', cost?: number, bearer?: 'landlord' | 'tenant'): Promise<ApiMaintenanceTicket> {
    const updated = await firstValueFrom(this.http.put<ApiMaintenanceTicket>(`${BASE}/maintenance-tickets/${id}/status`, { status, cost, bearer }));
    this.tickets.update((list) => list.map((t) => (t.id === id ? updated : t)));
    return updated;
  }

  async expensesForProperty(propertyId: number): Promise<ApiExpense[]> {
    return firstValueFrom(this.http.get<ApiExpense[]>(`${BASE}/expenses`, { params: { propertyId } }));
  }

  async expensesForTenant(tenantId: number): Promise<ApiExpense[]> {
    return firstValueFrom(this.http.get<ApiExpense[]>(`${BASE}/expenses`, { params: { tenantId } }));
  }

  async allExpenses(): Promise<ApiExpense[]> {
    return firstValueFrom(this.http.get<ApiExpense[]>(`${BASE}/expenses`));
  }

  async createExpense(expense: { propertyId: number; category: string; description: string; amount: number; bearer: 'landlord' | 'tenant'; tenantId?: number }): Promise<ApiExpense> {
    return firstValueFrom(this.http.post<ApiExpense>(`${BASE}/expenses`, expense));
  }

  async deleteExpense(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE}/expenses/${id}`));
  }
}
