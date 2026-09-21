package com.landlord.backend.report;

public record IncomeStatementRow(
    Long propertyId,
    String propertyName,
    double billed,
    double collected,
    double outstanding,
    double collectionRatePercent
) {}
