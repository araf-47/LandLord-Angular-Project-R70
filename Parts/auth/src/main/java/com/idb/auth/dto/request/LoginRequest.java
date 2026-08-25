package com.idb.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @JsonProperty("username")
    @NotBlank(message = "Username is required")
    private String username;

    @JsonProperty("password")
    @NotBlank(message = "Password is required")
    private String password;

    /** Only required when the account has 2FA enabled. */
    @JsonProperty("otp")
    private String otp;
}
