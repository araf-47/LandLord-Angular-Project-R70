package com.idb.auth.service;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.dto.request.ForgotPasswordRequest;
import com.idb.auth.dto.request.LoginRequest;
import com.idb.auth.dto.response.AuthResponse;

public interface AuthService {

    ApiResponse<AuthResponse> login(LoginRequest loginRequest) throws TraceableException;

    ApiResponse<String> forgotPassword(ForgotPasswordRequest request) throws TraceableException;
}
