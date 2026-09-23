package com.landlord.backend.report;

public record FullReport(
    IncomeStatementReport incomeStatement,
    ExpenseReport expenseReport,
    OccupancyReport occupancyReport
) {}
