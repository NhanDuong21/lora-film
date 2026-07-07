package com.project.movieservice.dto;

public class GenreResponse implements java.io.Serializable {
    private Integer id;
    private String genreName;

    private String status;

    public GenreResponse() {
    }

    public GenreResponse(Integer id, String genreName, String status) {
        this.id = id;
        this.genreName = genreName;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
