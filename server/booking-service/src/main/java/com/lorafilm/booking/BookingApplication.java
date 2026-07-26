package com.lorafilm.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.lorafilm.booking.config.BookingPolicyProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BookingPolicyProperties.class)
public class BookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}
