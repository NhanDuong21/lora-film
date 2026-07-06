package com.project.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "payment.mock.enabled=false"
})
public class MockProductionIsolationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mockProviderShouldNotExistWhenDisabled() {
        assertFalse(applicationContext.containsBean("mockPaymentProvider"), 
                "MockPaymentProvider bean should not exist when payment.mock.enabled is false");
    }

    @Test
    void mockCallbackShouldNotExistWhenDisabled() {
        assertFalse(applicationContext.containsBean("mockCallbackController"), 
                "MockCallbackController bean should not exist when payment.mock.enabled is false");
    }

    @Test
    void mockCallbackEndpointShouldNotBeExposedWhenDisabled() throws Exception {
        String req = "{\"paymentId\":1,\"simulatedStatus\":\"SUCCESS\"}";
        
        // When MOCK is disabled, the controller is not registered.
        // It should return 404 Not Found (or 403 Forbidden due to security, but no public mock endpoint).
        mockMvc.perform(post("/api/payments/callback/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    System.out.println("MOCK ENDPOINT STATUS: " + status);
                    System.out.println("MOCK ENDPOINT RESPONSE: " + result.getResponse().getContentAsString());
                    org.junit.jupiter.api.Assertions.assertTrue(status == 404 || status == 401 || status == 403 || status == 400 || status == 405, 
                        "Status should be a client error, but was " + status);
                });
    }
}
