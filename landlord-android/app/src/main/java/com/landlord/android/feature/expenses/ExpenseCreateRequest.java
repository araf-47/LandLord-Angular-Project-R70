package com.landlord.android.feature.expenses;

/** Wire shape for POST /api/expenses - MaintenanceController.NewExpenseRequest. */
public class ExpenseCreateRequest {
    public Long propertyId;
    public String category;
    public String description;
    public Double amount;
    public String bearer;
    public Long tenantId;
}
