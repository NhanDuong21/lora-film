package com.lorafilm.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCreditsDto {
    private List<TmdbPersonDto> directors;
    private List<TmdbPersonDto> mainCast;

    private List<TmdbPersonDto> writers;
    private List<TmdbPersonDto> producers;
    private List<TmdbPersonDto> supportingCast;
    private List<TmdbPersonDto> crew;

    public List<TmdbPersonDto> getDirectors() {
        return directors;
    }

    public void setDirectors(List<TmdbPersonDto> directors) {
        this.directors = directors;
    }

    public List<TmdbPersonDto> getMainCast() {
        return mainCast;
    }

    public void setMainCast(List<TmdbPersonDto> mainCast) {
        this.mainCast = mainCast;
    }

    public List<TmdbPersonDto> getWriters() {
        return writers;
    }

    public void setWriters(List<TmdbPersonDto> writers) {
        this.writers = writers;
    }

    public List<TmdbPersonDto> getProducers() {
        return producers;
    }

    public void setProducers(List<TmdbPersonDto> producers) {
        this.producers = producers;
    }

    public List<TmdbPersonDto> getSupportingCast() {
        return supportingCast;
    }

    public void setSupportingCast(List<TmdbPersonDto> supportingCast) {
        this.supportingCast = supportingCast;
    }

    public List<TmdbPersonDto> getCrew() {
        return crew;
    }

    public void setCrew(List<TmdbPersonDto> crew) {
        this.crew = crew;
    }
}
