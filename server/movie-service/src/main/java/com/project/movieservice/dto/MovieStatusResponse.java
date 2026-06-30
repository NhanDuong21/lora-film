package com.project.movieservice.dto;

public class MovieStatusResponse {
    private Long id;
    private String status;

    public MovieStatusResponse() {
    }

    public MovieStatusResponse(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
