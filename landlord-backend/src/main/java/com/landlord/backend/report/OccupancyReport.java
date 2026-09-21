package com.landlord.backend.report;

import java.util.List;

public record OccupancyReport(
    List<OccupancyReportRow> rows,
    int totalUnits,
    int totalOccupied,
    double occupancyRatePercent
) {}
