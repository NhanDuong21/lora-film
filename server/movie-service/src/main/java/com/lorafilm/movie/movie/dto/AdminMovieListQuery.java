package com.lorafilm.movie.movie.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class AdminMovieListQuery {

    private String status;

    @Min(1)
    private Long genreId;

    private String keyword;
    private String city;

    @Min(1)
    private Long cinemaId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;

    private String sort = "releaseDate,desc";
    private String source;
    private String healthStatus;
    private String hasPrimaryPoster;
    private String hasActiveVersion;
    private String hasShowtime;
    private String genrePublicId;
    private String country;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate releaseDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate tmdbUpdatedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate tmdbUpdatedTo;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Long getCinemaId() { return cinemaId; }
    public void setCinemaId(Long cinemaId) { this.cinemaId = cinemaId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public String getHasPrimaryPoster() { return hasPrimaryPoster; }
    public void setHasPrimaryPoster(String hasPrimaryPoster) { this.hasPrimaryPoster = hasPrimaryPoster; }
    public String getHasActiveVersion() { return hasActiveVersion; }
    public void setHasActiveVersion(String hasActiveVersion) { this.hasActiveVersion = hasActiveVersion; }
    public String getHasShowtime() { return hasShowtime; }
    public void setHasShowtime(String hasShowtime) { this.hasShowtime = hasShowtime; }
    public String getGenrePublicId() { return genrePublicId; }
    public void setGenrePublicId(String genrePublicId) { this.genrePublicId = genrePublicId; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public LocalDate getReleaseDateFrom() { return releaseDateFrom; }
    public void setReleaseDateFrom(LocalDate releaseDateFrom) { this.releaseDateFrom = releaseDateFrom; }
    public LocalDate getReleaseDateTo() { return releaseDateTo; }
    public void setReleaseDateTo(LocalDate releaseDateTo) { this.releaseDateTo = releaseDateTo; }
    public LocalDate getTmdbUpdatedFrom() { return tmdbUpdatedFrom; }
    public void setTmdbUpdatedFrom(LocalDate tmdbUpdatedFrom) { this.tmdbUpdatedFrom = tmdbUpdatedFrom; }
    public LocalDate getTmdbUpdatedTo() { return tmdbUpdatedTo; }
    public void setTmdbUpdatedTo(LocalDate tmdbUpdatedTo) { this.tmdbUpdatedTo = tmdbUpdatedTo; }
}
