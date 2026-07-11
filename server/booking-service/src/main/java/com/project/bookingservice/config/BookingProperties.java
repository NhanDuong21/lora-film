package com.project.bookingservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "booking")
public class BookingProperties {
    
    private Reservation reservation = new Reservation();
    private Redis redis = new Redis();
    private Idempotency idempotency = new Idempotency();
    private ExpirationWorker expirationWorker = new ExpirationWorker();
    private Internal internal = new Internal();

    private long seatReservationTtlMinutes = 5;
    private long paymentTimeoutMinutes = 15;

    public Internal getInternal() {
        return internal;
    }

    public void setInternal(Internal internal) {
        this.internal = internal;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public void setIdempotency(Idempotency idempotency) {
        this.idempotency = idempotency;
    }

    public ExpirationWorker getExpirationWorker() {
        return expirationWorker;
    }

    public void setExpirationWorker(ExpirationWorker expirationWorker) {
        this.expirationWorker = expirationWorker;
    }

    public long getSeatReservationTtlMinutes() {
        return seatReservationTtlMinutes;
    }

    public void setSeatReservationTtlMinutes(long seatReservationTtlMinutes) {
        this.seatReservationTtlMinutes = seatReservationTtlMinutes;
    }

    public long getPaymentTimeoutMinutes() {
        return paymentTimeoutMinutes;
    }

    public void setPaymentTimeoutMinutes(long paymentTimeoutMinutes) {
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    public static class Reservation {
        private long ttlSeconds = 300;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Redis {
        private Lock lock = new Lock();

        public Lock getLock() {
            return lock;
        }

        public void setLock(Lock lock) {
            this.lock = lock;
        }

        public static class Lock {
            private long ttlSeconds = 300;

            public long getTtlSeconds() {
                return ttlSeconds;
            }

            public void setTtlSeconds(long ttlSeconds) {
                this.ttlSeconds = ttlSeconds;
            }
        }
    }

    public static class Idempotency {
        private long ttlHours = 24;

        public long getTtlHours() {
            return ttlHours;
        }

        public void setTtlHours(long ttlHours) {
            this.ttlHours = ttlHours;
        }
    }

    public static class ExpirationWorker {
        private boolean enabled = true;
        private long fixedDelayMs = 30000;
        private int batchSize = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class Internal {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
