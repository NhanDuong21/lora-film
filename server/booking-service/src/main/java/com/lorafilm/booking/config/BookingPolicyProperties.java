package com.lorafilm.booking.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Booking-owned policy values.  These values are deliberately separate from
 * the legacy standalone reservation timeout.
 */
@Validated
@ConfigurationProperties(prefix = "booking")
public class BookingPolicyProperties {

    @Min(1)
    @Max(100)
    private int maxSeatsPerBooking = 8;

    @Min(1)
    private long holdDurationSeconds = 900;

    @Min(1)
    private long creationLockTtlSeconds = 30;

    public int getMaxSeatsPerBooking() {
        return maxSeatsPerBooking;
    }

    public void setMaxSeatsPerBooking(int maxSeatsPerBooking) {
        this.maxSeatsPerBooking = maxSeatsPerBooking;
    }

    public long getHoldDurationSeconds() {
        return holdDurationSeconds;
    }

    public void setHoldDurationSeconds(long holdDurationSeconds) {
        this.holdDurationSeconds = holdDurationSeconds;
    }

    public long getCreationLockTtlSeconds() {
        return creationLockTtlSeconds;
    }

    public void setCreationLockTtlSeconds(long creationLockTtlSeconds) {
        this.creationLockTtlSeconds = creationLockTtlSeconds;
    }
}
