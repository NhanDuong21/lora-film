package com.lorafilm.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.lorafilm.booking.config.BookingPolicyProperties;
import com.lorafilm.booking.config.BookingRealtimeProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({BookingPolicyProperties.class, BookingRealtimeProperties.class})
public class BookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}
