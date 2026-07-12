package com.lorafilm.movie.movie.dto;

import java.util.List;

public class MovieDetailDto extends MovieDto {

    private List<PersonDto> directors;
    private List<PersonDto> actors;
    private List<ProductionCompanyDto> productionCompanies;
    private List<MovieVersionDto> versions;
    private List<MovieMediaDto> media;

    public MovieDetailDto() {}

    public List<PersonDto> getDirectors() { return directors; }
    public void setDirectors(List<PersonDto> directors) { this.directors = directors; }
    public List<PersonDto> getActors() { return actors; }
    public void setActors(List<PersonDto> actors) { this.actors = actors; }
    public List<ProductionCompanyDto> getProductionCompanies() { return productionCompanies; }
    public void setProductionCompanies(List<ProductionCompanyDto> productionCompanies) { this.productionCompanies = productionCompanies; }
    public List<MovieVersionDto> getVersions() { return versions; }
    public void setVersions(List<MovieVersionDto> versions) { this.versions = versions; }
    public List<MovieMediaDto> getMedia() { return media; }
    public void setMedia(List<MovieMediaDto> media) { this.media = media; }

    public static class PersonDto {
        private String publicId;
        private String fullName;
        private String roleType;
        private String characterName;

        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getRoleType() { return roleType; }
        public void setRoleType(String roleType) { this.roleType = roleType; }
        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }
    }

    public static class ProductionCompanyDto {
        private String publicId;
        private String name;
        private String role;

        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class MovieVersionDto {
        private String publicId;
        private String versionName;
        private String format;
        private String audioLanguage;
        private String subtitleLanguage;
        private String dubLanguage;

        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getVersionName() { return versionName; }
        public void setVersionName(String versionName) { this.versionName = versionName; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getAudioLanguage() { return audioLanguage; }
        public void setAudioLanguage(String audioLanguage) { this.audioLanguage = audioLanguage; }
        public String getSubtitleLanguage() { return subtitleLanguage; }
        public void setSubtitleLanguage(String subtitleLanguage) { this.subtitleLanguage = subtitleLanguage; }
        public String getDubLanguage() { return dubLanguage; }
        public void setDubLanguage(String dubLanguage) { this.dubLanguage = dubLanguage; }
    }

    public static class MovieMediaDto {
        private String publicId;
        private String mediaType;
        private String url;
        private String title;
        private Boolean isPrimary;
        private Integer displayOrder;

        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getMediaType() { return mediaType; }
        public void setMediaType(String mediaType) { this.mediaType = mediaType; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Boolean getIsPrimary() { return isPrimary; }
        public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    }
}
