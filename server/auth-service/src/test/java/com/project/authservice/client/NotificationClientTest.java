package com.project.authservice.client;

import com.project.authservice.exception.OtpDeliveryFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationClientTest {

    private MockRestServiceServer server;
    private NotificationClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new NotificationClient(
                restTemplate,
                "http://notification-service",
                "test-internal-token",
                1_000L);
    }

    @Test
    void registrationOtpUsesCurrentNotificationContractAndGitTemplate() {
        server.expect(once(), requestTo(
                        "http://notification-service/api/v1/internal/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Token", "test-internal-token"))
                .andExpect(jsonPath("$.sourceService").value("auth-service"))
                .andExpect(jsonPath("$.eventType").value("AUTH_REGISTRATION_OTP"))
                .andExpect(jsonPath("$.templateKey").value("REGISTER_OTP"))
                .andExpect(jsonPath("$.recipient.userPublicId").value("42"))
                .andExpect(jsonPath("$.recipient.email").value("customer@example.com"))
                .andExpect(jsonPath("$.channels[0]").value("EMAIL"))
                .andExpect(jsonPath("$.payload.user_name").value("Nguyen Van A"))
                .andExpect(jsonPath("$.payload.otp_code").value("123456"))
                .andExpect(jsonPath("$.payload.expiry_minutes").value(5))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"publicId\":\"notification-1\"}}",
                        MediaType.APPLICATION_JSON));

        expectDelivery("notification-1", "SENT", null);

        client.sendRegistrationOtp(
                42L, "customer@example.com", "Nguyen Van A", "123456");

        server.verify();
    }

    @Test
    void changeEmailOtpSuppliesNewEmailRequiredByTemplate() {
        server.expect(once(), requestTo(
                        "http://notification-service/api/v1/internal/notifications"))
                .andExpect(jsonPath("$.templateKey").value("CHANGE_EMAIL_OTP"))
                .andExpect(jsonPath("$.payload.new_email").value("new@example.com"))
                .andExpect(jsonPath("$.payload.otp_code").value("654321"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"publicId\":\"notification-2\"}}",
                        MediaType.APPLICATION_JSON));

        expectDelivery("notification-2", "DELIVERED", null);

        client.sendChangeEmailOtp(
                42L, "current@example.com", "new@example.com", "654321");

        server.verify();
    }

    @Test
    void failedSmtpDeliveryIsNotReportedAsOtpSent() {
        server.expect(once(), requestTo(
                        "http://notification-service/api/v1/internal/notifications"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"publicId\":\"notification-failed\"}}",
                        MediaType.APPLICATION_JSON));

        expectDelivery("notification-failed", "FAILED", "SMTP_POLICY_REJECTED");

        assertThatThrownBy(() -> client.sendRegistrationOtp(
                42L, "customer@example.com", "Nguyen Van A", "123456"))
                .isInstanceOf(OtpDeliveryFailedException.class)
                .extracting("providerFailureCode")
                .isEqualTo("SMTP_POLICY_REJECTED");

        server.verify();
    }

    private void expectDelivery(String publicId, String status, String failureCode) {
        String failure = failureCode == null ? "null" : "\"" + failureCode + "\"";
        server.expect(once(), requestTo(
                        "http://notification-service/api/v1/internal/notifications/"
                                + publicId + "/deliveries"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "test-internal-token"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":[{\"channel\":\"EMAIL\",\"status\":\""
                                + status + "\",\"failureCode\":" + failure + "}]}",
                        MediaType.APPLICATION_JSON));
    }
}
