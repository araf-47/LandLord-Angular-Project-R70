package com.idb.auth.dto.request;

import static com.idb.auth.constant.AuthConstants.MESSAGE_PASSWORD_INVALID;
import static com.idb.auth.constant.AuthConstants.PASSWORD_PATTERN;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Username is required")
    @JsonProperty("username")
    private String username;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = PASSWORD_PATTERN, message = MESSAGE_PASSWORD_INVALID)
    @JsonProperty("password")
    private String password;

    @NotBlank(message = "OTP is required")
    @JsonProperty("otp")
    private String otp;
}
