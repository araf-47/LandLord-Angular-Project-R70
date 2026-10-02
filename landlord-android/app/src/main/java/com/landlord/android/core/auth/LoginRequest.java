package com.landlord.android.core.auth;

public class LoginRequest {
    public String username;
    public String password;
    public String otp;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
