package com.lorafilm.movie.showtime.dto;

public class ShowtimeMovieVersionDto {
    private String publicId;
    private String versionName;
    private String format;
    private String audioLanguage;
    private String subtitleLanguage;

    public ShowtimeMovieVersionDto() {}

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
}
