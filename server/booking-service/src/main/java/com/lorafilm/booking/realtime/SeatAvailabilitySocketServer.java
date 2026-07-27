package com.lorafilm.booking.realtime;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import jakarta.annotation.PostConstruct;

/**
 * Public, read-only projection of committed seat availability.
 */
@Component
@ConditionalOnProperty(name = "booking.realtime.enabled", havingValue = "true", matchIfMissing = true)
public class SeatAvailabilitySocketServer {

    private static final Logger log = LoggerFactory.getLogger(SeatAvailabilitySocketServer.class);
    private static final String SUBSCRIBE_EVENT = "seat:subscribe";
    private static final String UNSUBSCRIBE_EVENT = "seat:unsubscribe";
    private static final String AVAILABILITY_EVENT = "seat:availability-changed";
    private static final String ROOM_PREFIX = "showtime:";

    private final SocketIOServer server;

    public SeatAvailabilitySocketServer(SocketIOServer server) {
        this.server = server;
    }

    @PostConstruct
    public void start() {
        server.addEventListener(SUBSCRIBE_EVENT, String.class,
                (client, showtimePublicId, ackRequest) -> subscribe(client, showtimePublicId, ackRequest));
        server.addEventListener(UNSUBSCRIBE_EVENT, String.class,
                (client, showtimePublicId, ackRequest) -> unsubscribe(client, showtimePublicId, ackRequest));
        server.start();
        log.info("Seat availability Socket.IO server started");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommittedSeatAvailabilityChange(SeatAvailabilityChangedEvent event) {
        if (event == null || event.showtimePublicId() == null || event.seats().isEmpty()) {
            return;
        }
        log.debug("Broadcasting seat availability change: showtime={}, seats={}, connectedClients={}",
                event.showtimePublicId(), event.seats().size(), server.getAllClients().size());
        server.getRoomOperations(roomFor(event.showtimePublicId()))
                .sendEvent(AVAILABILITY_EVENT, event);
    }

    private void subscribe(SocketIOClient client, String showtimePublicId, AckRequest ackRequest) {
        if (!isValidShowtimePublicId(showtimePublicId)) {
            acknowledge(ackRequest, false);
            return;
        }
        client.joinRoom(roomFor(showtimePublicId));
        log.debug("Socket.IO client {} subscribed to {}", client.getSessionId(), showtimePublicId);
        acknowledge(ackRequest, true);
    }

    private void unsubscribe(SocketIOClient client, String showtimePublicId, AckRequest ackRequest) {
        if (isValidShowtimePublicId(showtimePublicId)) {
            client.leaveRoom(roomFor(showtimePublicId));
        }
        acknowledge(ackRequest, true);
    }

    private void acknowledge(AckRequest ackRequest, boolean ok) {
        if (ackRequest != null && ackRequest.isAckRequested()) {
            ackRequest.sendAckData(java.util.Map.of("ok", ok));
        }
    }

    private boolean isValidShowtimePublicId(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            return false;
        }
        try {
            java.util.UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String roomFor(String showtimePublicId) {
        return ROOM_PREFIX + showtimePublicId;
    }
}
