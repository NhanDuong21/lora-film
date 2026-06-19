package com.project.authservice.service;

import com.project.authservice.dto.request.ResendOtpRequest;
import com.project.authservice.dto.request.SendOtpRequest;
import com.project.authservice.dto.request.VerifyRequest;
import com.project.authservice.dto.response.ResendOtpResponse;

public interface VerificationService {
    void sendOtp(SendOtpRequest request);
    ResendOtpResponse resendOtp(ResendOtpRequest request);
    void verify(VerifyRequest request);
}
