package com.landlord.backend.report;

import java.time.LocalDate;

public record TenantLedgerRow(String period, double amount, double paid, double balance, String status, LocalDate dueDate) {}
