package com.lorafilm.booking.booking.entity;

import com.lorafilm.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "booking_snapshots")
public class BookingSnapshot extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "movie_title", length = 255)
    private String movieTitle;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @Column(name = "movie_poster", length = 500)
    private String moviePoster;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "age_rating", length = 20)
    private String ageRating;

    @Column(name = "showtime_id")
    private Long showtimeId;

    @Column(name = "showtime_start")
    private Instant showtimeStart;

    @Column(name = "showtime_end")
    private Instant showtimeEnd;

    @Column(name = "cinema_id")
    private Long cinemaId;

    @Column(name = "cinema_name", length = 255)
    private String cinemaName;

    @Column(name = "auditorium_id")
    private Long auditoriumId;

    @Column(name = "auditorium_name", length = 255)
    private String auditoriumName;

    @Column(name = "seat_count")
    private Integer seatCount;

    @Column(name = "promotion_code", length = 100)
    private String promotionCode;

    @Column(name = "promotion_name", length = 255)
    private String promotionName;

    @Column(name = "snapshot_json", columnDefinition = "JSON")
    private String snapshotJson;

    public BookingSnapshot() {
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

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getMoviePoster() {
        return moviePoster;
    }

    public void setMoviePoster(String moviePoster) {
        this.moviePoster = moviePoster;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
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

    public Long getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(Long cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public Long getAuditoriumId() {
        return auditoriumId;
    }

    public void setAuditoriumId(Long auditoriumId) {
        this.auditoriumId = auditoriumId;
    }

    public String getAuditoriumName() {
        return auditoriumName;
    }

    public void setAuditoriumName(String auditoriumName) {
        this.auditoriumName = auditoriumName;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }
}
