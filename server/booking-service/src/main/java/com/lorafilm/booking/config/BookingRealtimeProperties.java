package com.lorafilm.booking.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime transport settings for the optional seat-availability Socket.IO
 * broadcaster.  These settings do not participate in reservation authority;
 * MySQL remains the source of truth.
 */
@Validated
@ConfigurationProperties(prefix = "booking.realtime")
public class BookingRealtimeProperties {

    private boolean enabled = true;
    private String host = "0.0.0.0";

    @Min(1024)
    @Max(65535)
    private int port = 9093;

    /**
     * Must be supplied by deployment configuration. Keeping this unset avoids
     * baking a development host into the service artifact.
     */
    private String allowedOrigin;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAllowedOrigin() {
        return allowedOrigin;
    }

    public void setAllowedOrigin(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }
}
