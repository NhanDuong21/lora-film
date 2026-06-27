package com.project.movieservice.dto;

public class MovieUpdatedResponse extends MovieCreatedResponse {
    public MovieUpdatedResponse() {
        super();
    }

    public MovieUpdatedResponse(Long id, String title, String status) {
        super(id, title, status);
    }
}
