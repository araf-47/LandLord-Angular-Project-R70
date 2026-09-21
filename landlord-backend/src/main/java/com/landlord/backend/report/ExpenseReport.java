package com.landlord.backend.report;

import java.time.LocalDate;
import java.util.List;

public record ExpenseReport(
    LocalDate startDate,
    LocalDate endDate,
    List<ExpenseReportRow> rows,
    double totalAmount
) {}
