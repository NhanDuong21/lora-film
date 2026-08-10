package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.showtime.domain.entity.ShowtimeRefundOutboxEvent;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeRefundOutboxStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRefundOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class ShowtimeRefundOutboxService {
    private final ShowtimeRefundOutboxRepository repository;
    private final int batchSize;
    private final int leaseSeconds;
    private final int maxAttempts;

    public ShowtimeRefundOutboxService(
            ShowtimeRefundOutboxRepository repository,
            @Value("${payment.refund-outbox.batch-size:20}") int batchSize,
            @Value("${payment.refund-outbox.lease-seconds:30}") int leaseSeconds,
            @Value("${payment.refund-outbox.max-attempts:8}") int maxAttempts) {
        this.repository = repository;
        this.batchSize = batchSize;
        this.leaseSeconds = leaseSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public void enqueueCancellation(
            String showtimePublicId,
            String cancellationReason) {
        String eventId = UUID.nameUUIDFromBytes(
                ("showtime-cancelled:" + showtimePublicId)
                        .getBytes(StandardCharsets.UTF_8)).toString();
        if (repository.findByEventId(eventId).isPresent()) {
            return;
        }
        ShowtimeRefundOutboxEvent event = new ShowtimeRefundOutboxEvent();
        event.setEventId(eventId);
        event.setShowtimePublicId(showtimePublicId);
        event.setCancellationReason(sanitize(cancellationReason));
        event.setStatus(ShowtimeRefundOutboxStatus.PENDING);
        event.setNextAttemptAt(Instant.now());
        repository.save(event);
    }

    @Transactional
    public List<Long> claim(String ownerToken) {
        Instant now = Instant.now();
        Instant leaseUntil = now.plusSeconds(leaseSeconds);
        return repository.findReady(
                        EnumSet.of(
                                ShowtimeRefundOutboxStatus.PENDING,
                                ShowtimeRefundOutboxStatus.FAILED),
                        now,
                        PageRequest.of(0, batchSize))
                .stream()
                .map(candidate -> {
                    ShowtimeRefundOutboxEvent event = repository
                            .findByIdForUpdate(candidate.getId()).orElseThrow();
                    if (event.getLockedUntil() != null
                            && event.getLockedUntil().isAfter(now)) {
                        return null;
                    }
                    event.setStatus(ShowtimeRefundOutboxStatus.PROCESSING);
                    event.setLockedBy(ownerToken);
                    event.setLockedUntil(leaseUntil);
                    repository.save(event);
                    return event.getId();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkItem work(Long id, String ownerToken) {
        ShowtimeRefundOutboxEvent event = repository.findById(id).orElseThrow();
        if (event.getStatus() != ShowtimeRefundOutboxStatus.PROCESSING
                || !ownerToken.equals(event.getLockedBy())) {
            throw new IllegalStateException("Showtime refund outbox lease mismatch");
        }
        return new WorkItem(
                event.getEventId(),
                event.getShowtimePublicId(),
                event.getCancellationReason());
    }

    @Transactional
    public void published(Long id, String ownerToken) {
        ShowtimeRefundOutboxEvent event = owned(id, ownerToken);
        event.setStatus(ShowtimeRefundOutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        clearLease(event);
        event.setLastError(null);
        event.setNextAttemptAt(null);
        repository.save(event);
    }

    @Transactional
    public void failed(Long id, String ownerToken, String error) {
        ShowtimeRefundOutboxEvent event = owned(id, ownerToken);
        int attempts = event.getAttemptCount() + 1;
        event.setAttemptCount(attempts);
        event.setLastError(sanitize(error));
        clearLease(event);
        if (attempts >= maxAttempts) {
            event.setStatus(ShowtimeRefundOutboxStatus.DEAD_LETTER);
            event.setNextAttemptAt(null);
        } else {
            event.setStatus(ShowtimeRefundOutboxStatus.FAILED);
            long delay = Math.min(300, 1L << Math.min(attempts, 8));
            event.setNextAttemptAt(Instant.now().plusSeconds(delay));
        }
        repository.save(event);
    }

    private ShowtimeRefundOutboxEvent owned(Long id, String ownerToken) {
        ShowtimeRefundOutboxEvent event = repository.findByIdForUpdate(id).orElseThrow();
        if (event.getStatus() != ShowtimeRefundOutboxStatus.PROCESSING
                || !ownerToken.equals(event.getLockedBy())) {
            throw new IllegalStateException("Showtime refund outbox lease mismatch");
        }
        return event;
    }

    private void clearLease(ShowtimeRefundOutboxEvent event) {
        event.setLockedBy(null);
        event.setLockedUntil(null);
    }

    private String sanitize(String value) {
        String normalized = value == null || value.isBlank()
                ? "Suất chiếu đã bị hủy"
                : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= 1000
                ? normalized : normalized.substring(0, 1000);
    }

    public record WorkItem(
            String eventId,
            String showtimePublicId,
            String cancellationReason) {
    }
}
