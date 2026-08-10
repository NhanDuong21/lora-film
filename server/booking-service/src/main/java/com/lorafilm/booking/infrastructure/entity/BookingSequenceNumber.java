package com.lorafilm.booking.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "booking_sequence_numbers")
@EntityListeners(AuditingEntityListener.class)
public class BookingSequenceNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sequence_name", length = 100, nullable = false, updatable = false)
    private String sequenceName;

    @Column(name = "sequence_date", nullable = false, updatable = false)
    private LocalDate sequenceDate;

    @Column(name = "current_value", nullable = false)
    private Long currentValue = 0L;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public BookingSequenceNumber() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public LocalDate getSequenceDate() {
        return sequenceDate;
    }

    public void setSequenceDate(LocalDate sequenceDate) {
        this.sequenceDate = sequenceDate;
    }

    public Long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Long currentValue) {
        this.currentValue = currentValue;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
