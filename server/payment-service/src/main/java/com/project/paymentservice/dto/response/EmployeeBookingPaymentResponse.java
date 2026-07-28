package com.project.paymentservice.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.common.MoneyJsonSerializer;

import java.math.BigDecimal;
import java.time.Instant;

public class EmployeeBookingPaymentResponse {
    private Long bookingId;
    private String bookingPublicId;
    private Long accountId;
    private String bookingStatus;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal amount;
    private String currency;
    private Instant expiresAt;
    private String movieTitle;
    private Integer ticketCount;

    public EmployeeBookingPaymentResponse() {
    }

    public static EmployeeBookingPaymentResponse from(BookingPaymentContext context) {
        EmployeeBookingPaymentResponse response = new EmployeeBookingPaymentResponse();
        response.bookingId = context.getBookingId();
        response.bookingPublicId = context.getBookingPublicId();
        response.accountId = context.getAccountId();
        response.bookingStatus = context.getBookingStatus();
        response.amount = context.getAmount();
        response.currency = context.getCurrency();
        response.expiresAt = context.getExpiresAt();
        if (context.getAnalyticsSnapshot() != null) {
            response.movieTitle = context.getAnalyticsSnapshot().getMovieTitle();
            response.ticketCount = context.getAnalyticsSnapshot().getTicketCount();
        }
        return response;
    }

    public Long getBookingId() { return bookingId; }
    public String getBookingPublicId() { return bookingPublicId; }
    public Long getAccountId() { return accountId; }
    public String getBookingStatus() { return bookingStatus; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getMovieTitle() { return movieTitle; }
    public Integer getTicketCount() { return ticketCount; }
}
