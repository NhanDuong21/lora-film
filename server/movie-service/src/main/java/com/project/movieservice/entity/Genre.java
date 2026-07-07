package com.project.movieservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "genres")
public class Genre {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "genre_name", length = 100, nullable = false, unique = true)
    private String genreName;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private com.project.movieservice.enumtype.GenreStatus status = com.project.movieservice.enumtype.GenreStatus.ACTIVE;

    public Genre() {
    }

    public Genre(Integer id, String genreName) {
        this.id = id;
        this.genreName = genreName;
        this.status = com.project.movieservice.enumtype.GenreStatus.ACTIVE;
    }

    public Genre(Integer id, String genreName, com.project.movieservice.enumtype.GenreStatus status) {
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

    public com.project.movieservice.enumtype.GenreStatus getStatus() {
        return status;
    }

    public void setStatus(com.project.movieservice.enumtype.GenreStatus status) {
        this.status = status;
    }
}
