package com.landlord.backend.report;

import java.util.List;

public record TenantLedgerReport(
    Long tenantId,
    String tenantName,
    List<TenantLedgerRow> rows,
    double totalInvoiced,
    double totalPaid,
    double totalOutstanding
) {}
