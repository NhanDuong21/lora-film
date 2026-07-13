package com.lorafilm.movie.showtime.dto.response;

import java.time.Instant;

public class AdminShowtimeResponse {

    private String showtimePublicId;
    
    // Movie summary
    private MovieSummary movie;
    
    // MovieVersion summary
    private MovieVersionSummary movieVersion;
    
    // Cinema summary
    private CinemaSummary cinema;
    
    // Auditorium summary
    private AuditoriumSummary auditorium;

    private Instant startTime;
    private Instant endTime;
    private String status;
    private Instant bookingOpenTime;
    private Instant bookingCloseTime;
    private String cancellationReason;
    private Instant createdAt;
    private Instant updatedAt;

    public AdminShowtimeResponse() {}

    public String getShowtimePublicId() { return showtimePublicId; }
    public void setShowtimePublicId(String showtimePublicId) { this.showtimePublicId = showtimePublicId; }

    public MovieSummary getMovie() { return movie; }
    public void setMovie(MovieSummary movie) { this.movie = movie; }

    public MovieVersionSummary getMovieVersion() { return movieVersion; }
    public void setMovieVersion(MovieVersionSummary movieVersion) { this.movieVersion = movieVersion; }

    public CinemaSummary getCinema() { return cinema; }
    public void setCinema(CinemaSummary cinema) { this.cinema = cinema; }

    public AuditoriumSummary getAuditorium() { return auditorium; }
    public void setAuditorium(AuditoriumSummary auditorium) { this.auditorium = auditorium; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getBookingOpenTime() { return bookingOpenTime; }
    public void setBookingOpenTime(Instant bookingOpenTime) { this.bookingOpenTime = bookingOpenTime; }

    public Instant getBookingCloseTime() { return bookingCloseTime; }
    public void setBookingCloseTime(Instant bookingCloseTime) { this.bookingCloseTime = bookingCloseTime; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Nested summary classes
    public static class MovieSummary {
        private String publicId;
        private String slug;
        private String title;
        // getters & setters
        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    public static class MovieVersionSummary {
        private String publicId;
        private String versionName;
        private String format;
        private String audioLanguage;
        private String subtitleLanguage;
        // getters & setters
        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getVersionName() { return versionName; }
        public void setVersionName(String versionName) { this.versionName = versionName; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getAudioLanguage() { return audioLanguage; }
        public void setAudioLanguage(String audioLanguage) { this.audioLanguage = audioLanguage; }
        public String getSubtitleLanguage() { return subtitleLanguage; }
        public void setSubtitleLanguage(String subtitleLanguage) { this.subtitleLanguage = subtitleLanguage; }
    }

    public static class CinemaSummary {
        private String publicId;
        private String slug;
        private String name;
        private String timezone;
        // getters & setters
        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }

    public static class AuditoriumSummary {
        private String publicId;
        private String name;
        private String screenType;
        private String soundType;
        private Integer cleaningBufferMinutes;
        // getters & setters
        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getScreenType() { return screenType; }
        public void setScreenType(String screenType) { this.screenType = screenType; }
        public String getSoundType() { return soundType; }
        public void setSoundType(String soundType) { this.soundType = soundType; }
        public Integer getCleaningBufferMinutes() { return cleaningBufferMinutes; }
        public void setCleaningBufferMinutes(Integer cleaningBufferMinutes) { this.cleaningBufferMinutes = cleaningBufferMinutes; }
    }
}
