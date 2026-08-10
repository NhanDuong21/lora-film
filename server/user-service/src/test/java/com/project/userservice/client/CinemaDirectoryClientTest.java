package com.project.userservice.client;

import com.project.userservice.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CinemaDirectoryClientTest {

    private static final String CINEMA_ID = "b1575c2d-9081-11f1-bf65-0ebab02bf6f5";

    @Test
    void acceptsExistingCinema() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://movie-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CinemaDirectoryClient client = new CinemaDirectoryClient(builder.build());
        server.expect(once(), requestTo("http://movie-service/internal/cinemas/" + CINEMA_ID + "/exists"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"exists\":true}}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> client.requireExisting(CINEMA_ID)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void rejectsUnknownCinemaWithVietnameseMessage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://movie-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CinemaDirectoryClient client = new CinemaDirectoryClient(builder.build());
        server.expect(once(), requestTo("http://movie-service/internal/cinemas/" + CINEMA_ID + "/exists"))
                .andRespond(withSuccess("{\"success\":true,\"data\":{\"exists\":false}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.requireExisting(CINEMA_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không tồn tại");
        server.verify();
    }
}
