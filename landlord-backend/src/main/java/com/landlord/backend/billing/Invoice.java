package com.landlord.backend.billing;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;

@Entity
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    private Long unitId;

    /** Billing month, e.g. "2026-08". Immutable once created. */
    private String period;

    private Double rent;

    private Double utilitiesTotal = 0.0;

    private Double prevUnpaidRolled = 0.0;

    private Double amount;

    private Double balance;

    private String status = "unpaid";

    private LocalDate dueDate;

    private Instant createdAt = Instant.now();

    /** Phase 16.2: one-shot guard so the rent-due reminder job fires once per
     *  invoice, not every day it stays unpaid. */
    private Instant reminderSentAt;

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Double getRent() {
        return rent;
    }

    public void setRent(Double rent) {
        this.rent = rent;
    }

    public Double getUtilitiesTotal() {
        return utilitiesTotal;
    }

    public void setUtilitiesTotal(Double utilitiesTotal) {
        this.utilitiesTotal = utilitiesTotal;
    }

    public Double getPrevUnpaidRolled() {
        return prevUnpaidRolled;
    }

    public void setPrevUnpaidRolled(Double prevUnpaidRolled) {
        this.prevUnpaidRolled = prevUnpaidRolled;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(Instant reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }
}
