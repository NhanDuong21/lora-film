package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import java.time.Instant;
import java.util.Optional;

public class ShowtimeValidationContext {
    private final Movie movie;
    private final MovieVersion movieVersion;
    private final Cinema cinema;
    private final Auditorium auditorium;
    private final Instant startTime;
    private final Instant endTime;
    
    // Original showtime ID, populated if this is an update validation
    private final Long excludeShowtimeId;

    public ShowtimeValidationContext(
            Movie movie, 
            MovieVersion movieVersion, 
            Cinema cinema, 
            Auditorium auditorium, 
            Instant startTime, 
            Instant endTime,
            Long excludeShowtimeId) {
        this.movie = movie;
        this.movieVersion = movieVersion;
        this.cinema = cinema;
        this.auditorium = auditorium;
        this.startTime = startTime;
        this.endTime = endTime;
        this.excludeShowtimeId = excludeShowtimeId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Movie getMovie() {
        return movie;
    }

    public MovieVersion getMovieVersion() {
        return movieVersion;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public Auditorium getAuditorium() {
        return auditorium;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Optional<Long> getExcludeShowtimeId() {
        return Optional.ofNullable(excludeShowtimeId);
    }

    public static class Builder {
        private Movie movie;
        private MovieVersion movieVersion;
        private Cinema cinema;
        private Auditorium auditorium;
        private Instant startTime;
        private Instant endTime;
        private Long excludeShowtimeId;

        public Builder movie(Movie movie) {
            this.movie = movie;
            return this;
        }

        public Builder movieVersion(MovieVersion movieVersion) {
            this.movieVersion = movieVersion;
            return this;
        }

        public Builder cinema(Cinema cinema) {
            this.cinema = cinema;
            return this;
        }

        public Builder auditorium(Auditorium auditorium) {
            this.auditorium = auditorium;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder excludeShowtimeId(Long excludeShowtimeId) {
            this.excludeShowtimeId = excludeShowtimeId;
            return this;
        }

        public ShowtimeValidationContext build() {
            return new ShowtimeValidationContext(
                    movie, movieVersion, cinema, auditorium, startTime, endTime, excludeShowtimeId);
        }
    }
}
