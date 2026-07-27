package com.lorafilm.booking.realtime;

import com.lorafilm.booking.config.BookingRealtimeProperties;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "booking.realtime.enabled", havingValue = "true", matchIfMissing = true)
public class SeatAvailabilitySocketConfiguration {

    @Bean(name = "seatAvailabilitySocketIoServer", destroyMethod = "stop")
    public SocketIOServer seatAvailabilitySocketIoServer(BookingRealtimeProperties properties) {
        com.corundumstudio.socketio.Configuration configuration =
                new com.corundumstudio.socketio.Configuration();
        configuration.setHostname(properties.getHost());
        configuration.setPort(properties.getPort());
        if (properties.getAllowedOrigin() != null
                && !properties.getAllowedOrigin().isBlank()) {
            configuration.setOrigin(properties.getAllowedOrigin());
        }
        configuration.setJsonSupport(new BookingSocketJsonSupport());
        return new SocketIOServer(configuration);
    }

    /**
     * netty-socketio creates its own ObjectMapper, so Spring Boot's Java time
     * modules are not inherited automatically. Keep realtime timestamps as
     * ISO-8601 strings for browser clients.
     */
    private static final class BookingSocketJsonSupport extends JacksonJsonSupport {

        private BookingSocketJsonSupport() {
            super(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }
}
