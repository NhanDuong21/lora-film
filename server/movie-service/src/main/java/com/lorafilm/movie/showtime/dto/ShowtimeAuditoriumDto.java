package com.lorafilm.movie.showtime.dto;

public class ShowtimeAuditoriumDto {
    private String publicId;
    private String name;
    private String screenType;
    private String soundType;
    private Integer capacity;

    public ShowtimeAuditoriumDto() {}

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
}
