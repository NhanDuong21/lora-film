package com.lorafilm.booking.booking.entity;

import com.lorafilm.booking.booking.enums.TicketStatus;
import com.lorafilm.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "booking_tickets")
public class BookingTicket extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "ticket_code", length = 50, nullable = false, unique = true)
    private String ticketCode;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "seat_label", length = 20, nullable = false)
    private String seatLabel;

    @Column(name = "seat_row", length = 5)
    private String seatRow;

    @Column(name = "seat_column")
    private Integer seatColumn;

    @Column(name = "seat_type", length = 30)
    private String seatType;

    @Column(name = "ticket_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal ticketPrice;

    @Column(name = "movie_title", length = 255)
    private String movieTitle;

    @Column(name = "cinema_name", length = 255)
    private String cinemaName;

    @Column(name = "auditorium_name", length = 255)
    private String auditoriumName;

    @Column(name = "showtime_start")
    private Instant showtimeStart;

    @Column(name = "showtime_end")
    private Instant showtimeEnd;

    @Column(name = "movie_format", length = 30)
    private String movieFormat;

    @Column(name = "audio_language", length = 30)
    private String audioLanguage;

    @Column(name = "subtitle_language", length = 30)
    private String subtitleLanguage;

    @Column(name = "qr_code", length = 255)
    private String qrCode;

    @Column(name = "barcode", length = 255)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status = TicketStatus.ACTIVE;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_by_account_id")
    private Long usedByAccountId;

    @Column(name = "used_cinema_public_id", length = 36)
    private String usedCinemaPublicId;

    @Column(name = "used_gate_label", length = 80)
    private String usedGateLabel;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public BookingTicket() {
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public Integer getSeatColumn() {
        return seatColumn;
    }

    public void setSeatColumn(Integer seatColumn) {
        this.seatColumn = seatColumn;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public String getAuditoriumName() {
        return auditoriumName;
    }

    public void setAuditoriumName(String auditoriumName) {
        this.auditoriumName = auditoriumName;
    }

    public Instant getShowtimeStart() {
        return showtimeStart;
    }

    public void setShowtimeStart(Instant showtimeStart) {
        this.showtimeStart = showtimeStart;
    }

    public Instant getShowtimeEnd() {
        return showtimeEnd;
    }

    public void setShowtimeEnd(Instant showtimeEnd) {
        this.showtimeEnd = showtimeEnd;
    }

    public String getMovieFormat() {
        return movieFormat;
    }

    public void setMovieFormat(String movieFormat) {
        this.movieFormat = movieFormat;
    }

    public String getAudioLanguage() {
        return audioLanguage;
    }

    public void setAudioLanguage(String audioLanguage) {
        this.audioLanguage = audioLanguage;
    }

    public String getSubtitleLanguage() {
        return subtitleLanguage;
    }

    public void setSubtitleLanguage(String subtitleLanguage) {
        this.subtitleLanguage = subtitleLanguage;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Long getUsedByAccountId() {
        return usedByAccountId;
    }

    public void setUsedByAccountId(Long usedByAccountId) {
        this.usedByAccountId = usedByAccountId;
    }

    public String getUsedCinemaPublicId() {
        return usedCinemaPublicId;
    }

    public void setUsedCinemaPublicId(String usedCinemaPublicId) {
        this.usedCinemaPublicId = usedCinemaPublicId;
    }

    public String getUsedGateLabel() {
        return usedGateLabel;
    }

    public void setUsedGateLabel(String usedGateLabel) {
        this.usedGateLabel = usedGateLabel;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
