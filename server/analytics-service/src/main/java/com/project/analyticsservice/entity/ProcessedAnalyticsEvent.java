package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "processed_analytics_events", indexes = {
        @Index(name = "idx_processed_event_type", columnList = "event_type"),
        @Index(name = "idx_processed_event_at", columnList = "processed_at")
})
@Getter
@Setter
@NoArgsConstructor
public class ProcessedAnalyticsEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false, unique = true, length = 150)
    private String eventId;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;
    @Column(name = "aggregate_key", nullable = false, length = 150)
    private String aggregateKey;
    @Column(name = "schema_version", nullable = false, length = 20)
    private String schemaVersion;
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;
    @Column(name = "source_topic", length = 249)
    private String sourceTopic;
    @Column(name = "source_partition")
    private Integer sourcePartition;
    @Column(name = "source_offset")
    private Long sourceOffset;
    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    @Column(name = "trace_id", length = 100)
    private String traceId;
    @Column(name = "event_occurred_at")
    private Instant eventOccurredAt;
    @Column(name = "received_at")
    private Instant receivedAt;
    @CreationTimestamp @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}
