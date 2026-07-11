package com.project.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
    "payment.mock.enabled=true"
})
public class MockProductionEnabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void mockProviderShouldExistWhenEnabled() {
        assertTrue(applicationContext.containsBean("mockPaymentProvider"), 
                "MockPaymentProvider bean should exist when payment.mock.enabled is true");
    }

    @Test
    void mockCallbackShouldExistWhenEnabled() {
        assertTrue(applicationContext.containsBean("mockCallbackController"), 
                "MockCallbackController bean should exist when payment.mock.enabled is true");
    }
}
