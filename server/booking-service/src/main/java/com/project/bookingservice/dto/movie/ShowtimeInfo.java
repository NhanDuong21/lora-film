package com.project.bookingservice.dto.movie;

public class ShowtimeInfo {
    private Long id;
    private Long roomId;
    private boolean available;
    private java.math.BigDecimal price;
    private Long movieId;
    private String movieTitle;

    public ShowtimeInfo() {
    }

    public ShowtimeInfo(Long id, Long roomId, boolean available, java.math.BigDecimal price, Long movieId, String movieTitle) {
        this.id = id;
        this.roomId = roomId;
        this.available = available;
        this.price = price;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
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
}
