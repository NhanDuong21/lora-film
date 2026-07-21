package com.lorafilm.booking.infrastructure.entity;

import com.lorafilm.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "booking_sequence_numbers")
public class BookingSequenceNumber extends BaseEntity {

    @Column(name = "sequence_name", length = 100, nullable = false)
    private String sequenceName;

    @Column(name = "sequence_date", nullable = false)
    private LocalDate sequenceDate;

    @Column(name = "current_value", nullable = false)
    private Long currentValue = 0L;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public BookingSequenceNumber() {
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
