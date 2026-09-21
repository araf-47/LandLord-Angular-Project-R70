package com.landlord.backend.report;

public record ExpenseReportRow(String category, double landlordAmount, double tenantAmount, double total, int count) {}
