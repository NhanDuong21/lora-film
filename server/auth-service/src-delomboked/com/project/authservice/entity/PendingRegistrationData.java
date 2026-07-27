package com.project.authservice.entity;

import com.project.authservice.client.CccdCheckClient.CccdInfo;
import com.project.authservice.dto.request.RegisterRequest;

public class PendingRegistrationData {
    private RegisterRequest request;
    private CccdInfo cccdInfo;
    private String requestId;

    public PendingRegistrationData() {}

    public PendingRegistrationData(RegisterRequest request, CccdInfo cccdInfo) {
        this.request = request;
        this.cccdInfo = cccdInfo;
    }

    public PendingRegistrationData(RegisterRequest request, CccdInfo cccdInfo, String requestId) {
        this.request = request;
        this.cccdInfo = cccdInfo;
        this.requestId = requestId;
    }

    public RegisterRequest getRequest() { return request; }
    public void setRequest(RegisterRequest request) { this.request = request; }
    public CccdInfo getCccdInfo() { return cccdInfo; }
    public void setCccdInfo(CccdInfo cccdInfo) { this.cccdInfo = cccdInfo; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
