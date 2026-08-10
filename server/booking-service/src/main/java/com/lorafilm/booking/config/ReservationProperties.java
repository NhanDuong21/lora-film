package com.lorafilm.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReservationProperties {

    @Value("${booking.hold-duration-seconds:900}")
    private long reservationTimeout;

    public ReservationProperties() {
    }

    public ReservationProperties(long reservationTimeout) {
        this.reservationTimeout = reservationTimeout;
    }

    public long getReservationTimeout() {
        return reservationTimeout;
    }

    public void setReservationTimeout(long reservationTimeout) {
        this.reservationTimeout = reservationTimeout;
    }
}
