package com.idb.auth.dto.request;

import static com.idb.auth.constant.AuthConstants.MESSAGE_PASSWORD_INVALID;
import static com.idb.auth.constant.AuthConstants.PASSWORD_PATTERN;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @JsonProperty("password")
    @NotBlank(message = "Password is required")
    @Pattern(regexp = PASSWORD_PATTERN, message = MESSAGE_PASSWORD_INVALID)
    private String password;

    @JsonProperty("oldPassword")
    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "Otp is required")
    @JsonProperty("otp")
    private String otp;
}
