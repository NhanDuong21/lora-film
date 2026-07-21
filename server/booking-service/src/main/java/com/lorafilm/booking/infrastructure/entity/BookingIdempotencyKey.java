package com.lorafilm.booking.infrastructure.entity;

import com.lorafilm.booking.common.entity.BaseEntity;
import com.lorafilm.booking.infrastructure.enums.IdempotencyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "booking_idempotency_keys")
public class BookingIdempotencyKey extends BaseEntity {

    @Column(name = "idempotency_key", length = 255, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 255, nullable = false)
    private String requestHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "endpoint", length = 255, nullable = false)
    private String endpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private IdempotencyStatus status = IdempotencyStatus.PROCESSING;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "JSON")
    private String responseBody;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public BookingIdempotencyKey() {
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyStatus status) {
        this.status = status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
