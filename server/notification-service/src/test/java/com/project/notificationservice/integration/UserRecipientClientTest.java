package com.project.notificationservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserRecipientClientTest {

    @Test
    void resolvesMinimalNotificationRecipientByAccountId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserRecipientClient client = new UserRecipientClient(
                builder, "http://user-service", "test-internal-token");
        server.expect(requestTo(
                        "http://user-service/api/v1/internal/users/42/notification-recipient"))
                .andExpect(header("X-Internal-Token", "test-internal-token"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "data": {
                            "accountId": 42,
                            "email": "customer@example.com",
                            "fullName": "Nguyen Van A"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        UserRecipientClient.ResolvedRecipient recipient =
                client.findByUserPublicId("42").orElseThrow();

        assertThat(recipient.email()).isEqualTo("customer@example.com");
        assertThat(recipient.fullName()).isEqualTo("Nguyen Van A");
        server.verify();
    }

    @Test
    void ignoresNonNumericUserPublicIdWithoutCallingUserService() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserRecipientClient client = new UserRecipientClient(
                builder, "http://user-service", "test-internal-token");

        assertThat(client.findByUserPublicId("not-an-account-id")).isEmpty();

        server.verify();
    }
}
