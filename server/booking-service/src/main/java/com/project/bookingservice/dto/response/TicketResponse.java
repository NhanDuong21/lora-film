package com.project.bookingservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TicketResponse {
    private Long ticketId;
    private Long bookingId;
    private Long seatId;
    private BigDecimal price;
    private LocalDateTime createdAt;

    public TicketResponse() {
    }

    public TicketResponse(Long ticketId, Long bookingId, Long seatId, BigDecimal price, LocalDateTime createdAt) {
        this.ticketId = ticketId;
        this.bookingId = bookingId;
        this.seatId = seatId;
        this.price = price;
        this.createdAt = createdAt;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
