package com.project.authservice.event.dto;

public class RegistrationValidationRequestedEventData {
    private final String requestId;
    private final String email;
    private final String phoneNumber;
    private final String cccd;

    public RegistrationValidationRequestedEventData(String requestId, String email, String phoneNumber, String cccd) {
        this.requestId = requestId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.cccd = cccd;
    }

    public String getRequestId() { return requestId; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getCccd() { return cccd; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String requestId;
        private String email;
        private String phoneNumber;
        private String cccd;

        private Builder() {}

        public Builder requestId(String requestId) { this.requestId = requestId; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public Builder cccd(String cccd) { this.cccd = cccd; return this; }

        public RegistrationValidationRequestedEventData build() {
            return new RegistrationValidationRequestedEventData(requestId, email, phoneNumber, cccd);
        }
    }
}
