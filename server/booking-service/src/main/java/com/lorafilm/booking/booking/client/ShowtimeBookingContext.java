package com.lorafilm.booking.booking.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ShowtimeBookingContext(
        Long showtimeId,
        String showtimePublicId,
        Long movieId,
        String moviePublicId,
        Long cinemaId,
        String cinemaPublicId,
        Long auditoriumId,
        String status,
        Instant startsAt,
        Instant endsAt,
        Instant paymentExpiresAt,
        BigDecimal ticketAmount,
        BigDecimal serviceFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currency,
        String format,
        String roomType,
        String movieTitle,
        String moviePosterUrl,
        String cinemaName,
        String auditoriumName,
        List<SeatContext> seats,
        String auditoriumPublicId,
        Integer auditoriumCapacity) {

    /** Compatibility constructor for callers before auditorium analytics facts. */
    public ShowtimeBookingContext(
            Long showtimeId, String showtimePublicId, Long movieId, String moviePublicId,
            Long cinemaId, String cinemaPublicId, Long auditoriumId, String status,
            Instant startsAt, Instant endsAt, Instant paymentExpiresAt,
            BigDecimal ticketAmount, BigDecimal serviceFee, BigDecimal discountAmount,
            BigDecimal totalAmount, String currency, String format, String roomType,
            String movieTitle, String moviePosterUrl, String cinemaName, String auditoriumName,
            List<SeatContext> seats) {
        this(showtimeId, showtimePublicId, movieId, moviePublicId, cinemaId, cinemaPublicId,
                auditoriumId, status, startsAt, endsAt, paymentExpiresAt, ticketAmount,
                serviceFee, discountAmount, totalAmount, currency, format, roomType,
                movieTitle, moviePosterUrl, cinemaName, auditoriumName, seats, null, null);
    }

    /** Compatibility constructor for contexts created before format snapshots. */
    public ShowtimeBookingContext(
            Long showtimeId,
            String showtimePublicId,
            Long movieId,
            String moviePublicId,
            Long cinemaId,
            String cinemaPublicId,
            Long auditoriumId,
            String status,
            Instant startsAt,
            Instant endsAt,
            Instant paymentExpiresAt,
            BigDecimal ticketAmount,
            BigDecimal serviceFee,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String currency,
            String movieTitle,
            String moviePosterUrl,
            String cinemaName,
            String auditoriumName,
            List<SeatContext> seats) {
        this(showtimeId, showtimePublicId, movieId, moviePublicId,
                cinemaId, cinemaPublicId, auditoriumId, status, startsAt, endsAt,
                paymentExpiresAt, ticketAmount, serviceFee, discountAmount,
                totalAmount, currency, null, null, movieTitle, moviePosterUrl,
                cinemaName, auditoriumName, seats, null, null);
    }

    /** Compatibility constructor for historical numeric-only Movie contexts. */
    public ShowtimeBookingContext(
            Long showtimeId,
            String showtimePublicId,
            Long movieId,
            Long cinemaId,
            Long auditoriumId,
            String status,
            Instant startsAt,
            Instant endsAt,
            Instant paymentExpiresAt,
            BigDecimal ticketAmount,
            BigDecimal serviceFee,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String currency,
            String movieTitle,
            String moviePosterUrl,
            String cinemaName,
            String auditoriumName,
            List<SeatContext> seats) {
        this(showtimeId, showtimePublicId, movieId, null, cinemaId, null,
                auditoriumId, status, startsAt, endsAt, paymentExpiresAt,
                ticketAmount, serviceFee, discountAmount, totalAmount, currency, null, null,
                movieTitle, moviePosterUrl, cinemaName, auditoriumName, seats, null, null);
    }

    public record SeatContext(Long seatId, String seatPublicId, String seatLabel, String seatType,
                               BigDecimal price, String currency, String pairGroup) {
        public SeatContext(Long seatId, String seatPublicId, String seatLabel, String seatType,
                           BigDecimal price, String currency) {
            this(seatId, seatPublicId, seatLabel, seatType, price, currency, null);
        }

        public SeatContext(Long seatId, String seatLabel, String seatType, BigDecimal price, String currency) {
            this(seatId, null, seatLabel, seatType, price, currency, null);
        }

        public SeatContext(Long seatId, String seatLabel, String seatType, BigDecimal price) {
            this(seatId, null, seatLabel, seatType, price, null, null);
        }
    }
}
