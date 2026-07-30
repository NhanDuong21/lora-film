package com.project.authservice.client;

import com.project.authservice.exception.CccdException.InvalidCccdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CccdCheckClientTest {
    private static final String API_URL = "https://cccd.example.test/check";

    private MockRestServiceServer server;
    private CccdCheckClient client;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        CircuitBreakerFactory circuitBreakerFactory = mock(CircuitBreakerFactory.class);
        when(circuitBreakerFactory.create("cccdVerification"))
                .thenReturn(new PassThroughCircuitBreaker());
        client = new CccdCheckClient(restTemplate, circuitBreakerFactory);
        ReflectionTestUtils.setField(client, "cccdApiUrl", API_URL);
        ReflectionTestUtils.setField(client, "cccdApiKey", "test-key");
    }

    @Test
    void returnsVerifiedCccdInformation() {
        server.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "valid": true,
                          "cccdMasked": "092******789",
                          "provinceCode": "092",
                          "provinceName": "Test Province",
                          "gender": "MALE",
                          "birthYear": 2005,
                          "note": "verified"
                        }
                        """, MediaType.APPLICATION_JSON));

        CccdCheckClient.CccdInfo result = client.checkCccd("092205006789");

        assertThat(result.getCccdMasked()).isEqualTo("092******789");
        assertThat(result.getBirthYear()).isEqualTo(2005);
        server.verify();
    }

    @Test
    void keepsInvalidCccdAsBusinessValidationFailure() {
        server.expect(requestTo(API_URL))
                .andRespond(withSuccess("{\"valid\":false}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.checkCccd("092205006789"))
                .isInstanceOf(InvalidCccdException.class);
        server.verify();
    }

    @Test
    void preservesRegistrationValidationFlowWhenCccdProviderIsUnavailable() {
        server.expect(requestTo(API_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.checkCccd("092205006789"))
                .isInstanceOf(InvalidCccdException.class);
        server.verify();
    }

    private static class PassThroughCircuitBreaker implements CircuitBreaker {
        @Override
        public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
            try {
                return toRun.get();
            } catch (Throwable throwable) {
                return fallback.apply(new CompletionException(throwable));
            }
        }
    }
}
