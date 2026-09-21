package com.landlord.backend.report;

public record OccupancyReportRow(
    Long propertyId,
    String propertyName,
    int totalUnits,
    int occupiedUnits,
    int vacantUnits,
    double occupancyRatePercent
) {}
