package com.landlord.backend.report;

import java.time.LocalDate;
import java.util.List;

public record IncomeStatementReport(
    LocalDate startDate,
    LocalDate endDate,
    List<IncomeStatementRow> rows,
    double totalBilled,
    double totalCollected,
    double totalOutstanding,
    double collectionRatePercent
) {}
