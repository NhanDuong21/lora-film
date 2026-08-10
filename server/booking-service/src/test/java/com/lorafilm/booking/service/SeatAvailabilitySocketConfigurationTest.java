package com.lorafilm.booking.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.lorafilm.booking.config.BookingRealtimeProperties;
import com.lorafilm.booking.realtime.SeatAvailabilityChangedEvent;
import com.lorafilm.booking.realtime.SeatAvailabilitySocketConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SeatAvailabilitySocketConfigurationTest {

    @Test
    void socketJsonSupportSerializesJavaTimeAsIso8601() throws Exception {
        BookingRealtimeProperties properties = new BookingRealtimeProperties();
        properties.setPort(19093);
        SocketIOServer server = new SeatAvailabilitySocketConfiguration()
                .seatAvailabilitySocketIoServer(properties);
        Instant timestamp = Instant.parse("2026-07-26T11:33:36.076658900Z");
        SeatAvailabilityChangedEvent event = new SeatAvailabilityChangedEvent(
                "showtime-public-id",
                timestamp,
                List.of(new SeatAvailabilityChangedEvent.SeatUpdate(
                        "seat-public-id", "HELD", timestamp.plusSeconds(900))));

        ByteBuf buffer = Unpooled.buffer();
        try {
            try (ByteBufOutputStream output = new ByteBufOutputStream(buffer)) {
                server.getConfiguration().getJsonSupport()
                        .writeValue(output, List.of("seat:availability-changed", event));
            }

            String json = buffer.toString(StandardCharsets.UTF_8);
            assertTrue(json.contains("\"occurredAt\":\"2026-07-26T11:33:36.076658900Z\""));
            assertTrue(json.contains("\"expiresAt\":\"2026-07-26T11:48:36.076658900Z\""));
        } finally {
            buffer.release();
        }
    }
}
