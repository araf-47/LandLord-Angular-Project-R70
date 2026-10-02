package com.landlord.android.core.auth;

import java.util.List;

public class MeResponse {
    public long id;
    public String username;
    public List<String> roles;
    public String email;
    public String phone;
    public boolean twoFactorEnabled;
    public boolean notifyRentDueEmail;
    public boolean notifyRentDueSms;
    public boolean notifyPaymentReceivedEmail;
    public boolean notifyMaintenanceEmail;
}
