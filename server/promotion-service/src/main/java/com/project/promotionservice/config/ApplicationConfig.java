package com.project.promotionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.time.Clock;

@Configuration
public class ApplicationConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${booking-service.connect-timeout:3000}") int connectTimeout,
            @Value("${booking-service.read-timeout:5000}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

