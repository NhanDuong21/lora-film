package com.project.scoreservice.client.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class BookingInternalClientImplConfigurationTest {

    private static final String SHARED_DEVELOPMENT_TOKEN =
            "8f0a00f11a51ad253c9560e55236b464bab6b20e57642c01a9c896a98ff061ff";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(BookingInternalClientImpl.class)
            .withPropertyValues("app.internal-token=secret-internal-token");

    @Test
    void usesTheSharedBookingTokenInsteadOfAnOutdatedServiceLocalToken() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(BookingInternalClientImpl.class);
            BookingInternalClientImpl client = context.getBean(BookingInternalClientImpl.class);

            assertThat(ReflectionTestUtils.getField(client, "internalToken"))
                    .isEqualTo(SHARED_DEVELOPMENT_TOKEN);
        });
    }
}
