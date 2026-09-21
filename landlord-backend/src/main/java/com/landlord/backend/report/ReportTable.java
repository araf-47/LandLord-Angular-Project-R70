package com.landlord.backend.report;

import java.util.List;

/** Generic tabular shape all four fixed reports render down to for PDF/Excel export. */
public record ReportTable(
    String title,
    String subtitle,
    List<String> headers,
    List<List<String>> rows,
    List<String> totalsRow
) {}
