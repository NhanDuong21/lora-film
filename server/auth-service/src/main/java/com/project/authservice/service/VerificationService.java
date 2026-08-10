package com.project.authservice.service;

import com.project.authservice.dto.request.SendOtpRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.dto.response.SendOtpResponse;

public interface VerificationService {
    SendOtpResponse sendOtp(SendOtpRequest request);
    void verify(VerifyRequest request);
    void sendForgotPasswordEmail(Long accountId, String email, String otp);
    void sendChangeEmailOtp(Long accountId, String currentEmail, String newEmail, String otp);
}
