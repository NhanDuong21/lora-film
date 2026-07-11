package com.lorafilm.movie.showtime.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.lorafilm.movie.showtime.dto.ShowtimeAuditoriumDto;
import com.lorafilm.movie.showtime.dto.ShowtimeCinemaDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieVersionDto;

public class BookingContextResponse {
    private BookingContextShowtimeDto showtime;
    private ShowtimeMovieDto movie;
    private ShowtimeMovieVersionDto movieVersion;
    private ShowtimeCinemaDto cinema;
    private ShowtimeAuditoriumDto auditorium;
    private List<BookingContextSeatDto> selectedSeats;
    private BookingContextPricingDto pricing;
    private OffsetDateTime bookingExpiredAt;

    public BookingContextResponse() {}

    public BookingContextShowtimeDto getShowtime() {
        return showtime;
    }

    public void setShowtime(BookingContextShowtimeDto showtime) {
        this.showtime = showtime;
    }

    public ShowtimeMovieDto getMovie() {
        return movie;
    }

    public void setMovie(ShowtimeMovieDto movie) {
        this.movie = movie;
    }

    public ShowtimeMovieVersionDto getMovieVersion() {
        return movieVersion;
    }

    public void setMovieVersion(ShowtimeMovieVersionDto movieVersion) {
        this.movieVersion = movieVersion;
    }

    public ShowtimeCinemaDto getCinema() {
        return cinema;
    }

    public void setCinema(ShowtimeCinemaDto cinema) {
        this.cinema = cinema;
    }

    public ShowtimeAuditoriumDto getAuditorium() {
        return auditorium;
    }

    public void setAuditorium(ShowtimeAuditoriumDto auditorium) {
        this.auditorium = auditorium;
    }

    public List<BookingContextSeatDto> getSelectedSeats() {
        return selectedSeats;
    }

    public void setSelectedSeats(List<BookingContextSeatDto> selectedSeats) {
        this.selectedSeats = selectedSeats;
    }

    public BookingContextPricingDto getPricing() {
        return pricing;
    }

    public void setPricing(BookingContextPricingDto pricing) {
        this.pricing = pricing;
    }

    public OffsetDateTime getBookingExpiredAt() {
        return bookingExpiredAt;
    }

    public void setBookingExpiredAt(OffsetDateTime bookingExpiredAt) {
        this.bookingExpiredAt = bookingExpiredAt;
    }
}
