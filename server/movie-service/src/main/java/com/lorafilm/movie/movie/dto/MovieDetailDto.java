package com.lorafilm.movie.movie.dto;

import java.util.List;

public class MovieDetailDto extends MovieDto {

    private List<PersonDto> directors;
    private List<PersonDto> actors;
    private List<PersonDto> writers;
    private List<PersonDto> producers;
    private List<ProductionCompanyDto> productionCompanies;
    private List<ProductionCompanyDto> distributors;
    private List<ProductionCompanyDto> studios;
    private List<MovieVersionDto> versions;
    private List<MovieMediaDto> media;

    public MovieDetailDto() {}

    public List<PersonDto> getDirectors() { return directors; }
    public void setDirectors(List<PersonDto> directors) { this.directors = directors; }
    public List<PersonDto> getActors() { return actors; }
    public void setActors(List<PersonDto> actors) { this.actors = actors; }
    public List<PersonDto> getWriters() { return writers; }
    public void setWriters(List<PersonDto> writers) { this.writers = writers; }
    public List<PersonDto> getProducers() { return producers; }
    public void setProducers(List<PersonDto> producers) { this.producers = producers; }
    
    public List<ProductionCompanyDto> getProductionCompanies() { return productionCompanies; }
    public void setProductionCompanies(List<ProductionCompanyDto> productionCompanies) { this.productionCompanies = productionCompanies; }
    public List<ProductionCompanyDto> getDistributors() { return distributors; }
    public void setDistributors(List<ProductionCompanyDto> distributors) { this.distributors = distributors; }
    public List<ProductionCompanyDto> getStudios() { return studios; }
    public void setStudios(List<ProductionCompanyDto> studios) { this.studios = studios; }
    
    public List<MovieVersionDto> getVersions() { return versions; }
    public void setVersions(List<MovieVersionDto> versions) { this.versions = versions; }
    public List<MovieMediaDto> getMedia() { return media; }
    public void setMedia(List<MovieMediaDto> media) { this.media = media; }

    public static class PersonDto {
        private String publicId;
        private String fullName;
        private String roleType;
        private String characterName;
        private String profileImageUrl;

        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getRoleType() { return roleType; }
        public void setRoleType(String roleType) { this.roleType = roleType; }
        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    }

    public static class ProductionCompanyDto {
        private String publicId;
        private String name;
        private String role;
        private String logoUrl;

        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    }

    public static class MovieVersionDto {
        private String publicId;
        private String versionName;
        private String format;
        private String audioLanguage;
        private String subtitleLanguage;
        private String dubLanguage;
        private String status;

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
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class MovieMediaDto {
        private String publicId;
        private String mediaType;
        private String url;
        private String title;
        private Boolean isPrimary;
        private Integer displayOrder;
        private String status;

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
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
