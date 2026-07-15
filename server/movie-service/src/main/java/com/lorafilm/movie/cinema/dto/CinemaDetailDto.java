package com.lorafilm.movie.cinema.dto;

import java.util.List;

public class CinemaDetailDto extends CinemaDto {

    private List<OperatingHourDto> operatingHours;
    private List<CinemaMediaDto> gallery;
    private List<AuditoriumDto> activeAuditoriums;
    private String status;
    private String description;

    public CinemaDetailDto() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<OperatingHourDto> getOperatingHours() { return operatingHours; }
    public void setOperatingHours(List<OperatingHourDto> operatingHours) { this.operatingHours = operatingHours; }
    public List<CinemaMediaDto> getGallery() { return gallery; }
    public void setGallery(List<CinemaMediaDto> gallery) { this.gallery = gallery; }
    public List<AuditoriumDto> getActiveAuditoriums() { return activeAuditoriums; }
    public void setActiveAuditoriums(List<AuditoriumDto> activeAuditoriums) { this.activeAuditoriums = activeAuditoriums; }

    public static class OperatingHourDto {
        private Integer dayOfWeek;
        private String openTime;
        private String closeTime;
        private Boolean isClosed;

        public Integer getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public String getOpenTime() { return openTime; }
        public void setOpenTime(String openTime) { this.openTime = openTime; }
        public String getCloseTime() { return closeTime; }
        public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
        public Boolean getIsClosed() { return isClosed; }
        public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
    }

    public static class CinemaMediaDto {
        private String publicId;
        private String mediaType;
        private String url;
        private String title;
        private Boolean isPrimary;

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
    }

    public static class AuditoriumDto {
        private String publicId;
        private String name;
        private String screenType;
        private String soundType;
        private Integer capacity;
        private String status;

        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getScreenType() { return screenType; }
        public void setScreenType(String screenType) { this.screenType = screenType; }
        public String getSoundType() { return soundType; }
        public void setSoundType(String soundType) { this.soundType = soundType; }
        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
