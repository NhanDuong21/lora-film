package com.project.bookingservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.bookingservice.enumtype.BookingStatus;
import com.project.bookingservice.dto.reservation.ReservationSeatResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {

    private Long bookingId;
    private String bookingCode;
    private Long showtimeId;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private List<ReservationSeatResponse> seats;
    private List<Object> tickets; // Since ticket generation is out of scope

    public BookingResponse() {
    }

    public BookingResponse(Long bookingId, String bookingCode, Long showtimeId, BigDecimal totalAmount,
                           BookingStatus status, LocalDateTime expiresAt, LocalDateTime createdAt,
                           List<ReservationSeatResponse> seats, List<Object> tickets) {
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.showtimeId = showtimeId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.seats = seats;
        this.tickets = tickets;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ReservationSeatResponse> getSeats() {
        return seats;
    }

    public void setSeats(List<ReservationSeatResponse> seats) {
        this.seats = seats;
    }

    public List<Object> getTickets() {
        return tickets;
    }

    public void setTickets(List<Object> tickets) {
        this.tickets = tickets;
    }
}
