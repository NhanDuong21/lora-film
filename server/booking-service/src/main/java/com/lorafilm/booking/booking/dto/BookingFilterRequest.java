package com.lorafilm.booking.booking.dto;

import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.BookingAttentionFilter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;

public class BookingFilterRequest {

    private String bookingCode;
    private Long userId;
    private List<Long> userIds;
    private BookingStatus status;
    private BookingAttentionFilter attention;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant toDate;

    private int page = 0;
    private int size = 20;

    public BookingFilterRequest() {
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BookingAttentionFilter getAttention() {
        return attention;
    }

    public void setAttention(BookingAttentionFilter attention) {
        this.attention = attention;
    }

    public Instant getFromDate() {
        return fromDate;
    }

    public void setFromDate(Instant fromDate) {
        this.fromDate = fromDate;
    }

    public Instant getToDate() {
        return toDate;
    }

    public void setToDate(Instant toDate) {
        this.toDate = toDate;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
