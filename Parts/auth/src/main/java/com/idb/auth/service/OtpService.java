package com.idb.auth.service;

import com.idb.auth.common.dto.response.ApiResponse;
import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.model.User;

public interface OtpService {

    ApiResponse<String> generateOtp(String username) throws TraceableException, LogOnlyException;

    boolean validateOtp(String username, String otp) throws TraceableException;

    String generateSecureRandomOtp();

    void clearCache(String username) throws TraceableException;

    void sendOtpEmail(User user, String otp) throws LogOnlyException, TraceableException;
}
