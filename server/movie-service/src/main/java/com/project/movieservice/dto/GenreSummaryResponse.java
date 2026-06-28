package com.project.movieservice.dto;

public class GenreSummaryResponse {
    private Integer id;
    private String genreName;

    public GenreSummaryResponse() {
    }

    public GenreSummaryResponse(Integer id, String genreName) {
        this.id = id;
        this.genreName = genreName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }
}
