package com.lorafilm.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbPersonDetailsDto {
    private Long tmdbPersonId;
    private String name;
    private List<String> alsoKnownAs;
    private String biography;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deathday;
    
    private String placeOfBirth;
    private String knownForDepartment;
    private String homepage;
    private String imdbId;
    private TmdbGenderDto gender;
    private TmdbProfileDto profile;

    public Long getTmdbPersonId() { return tmdbPersonId; }
    public void setTmdbPersonId(Long tmdbPersonId) { this.tmdbPersonId = tmdbPersonId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getAlsoKnownAs() { return alsoKnownAs; }
    public void setAlsoKnownAs(List<String> alsoKnownAs) { this.alsoKnownAs = alsoKnownAs; }

    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public LocalDate getDeathday() { return deathday; }
    public void setDeathday(LocalDate deathday) { this.deathday = deathday; }

    public String getPlaceOfBirth() { return placeOfBirth; }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }

    public String getKnownForDepartment() { return knownForDepartment; }
    public void setKnownForDepartment(String knownForDepartment) { this.knownForDepartment = knownForDepartment; }

    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public TmdbGenderDto getGender() { return gender; }
    public void setGender(TmdbGenderDto gender) { this.gender = gender; }

    public TmdbProfileDto getProfile() { return profile; }
    public void setProfile(TmdbProfileDto profile) { this.profile = profile; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbGenderDto {
        private Integer code;
        private String name;
        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbProfileDto {
        private String path;
        private String url;
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
