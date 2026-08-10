package com.lorafilm.movie.autoschedule.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AutoSchedulePreflightRequest {
    @NotBlank
    @Size(max = 36)
    private String cinemaPublicId;

    @Min(1)
    @Max(7)
    private Integer planningDays = 1;

    @Size(max = 100)
    private List<@NotBlank @Size(max = 36) String> includeMovieVersionPublicIds;

    @Size(max = 100)
    private List<@NotBlank @Size(max = 36) String> includeAuditoriumPublicIds;

    @Size(max = 100)
    private List<@NotBlank @Size(max = 36) String> excludeMovieVersionPublicIds;

    @Size(max = 100)
    private List<@NotBlank @Size(max = 36) String> excludeAuditoriumPublicIds;

    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String cinemaPublicId) { this.cinemaPublicId = cinemaPublicId; }
    public Integer getPlanningDays() { return planningDays; }
    public void setPlanningDays(Integer planningDays) { this.planningDays = planningDays; }
    public List<String> getIncludeMovieVersionPublicIds() { return includeMovieVersionPublicIds; }
    public void setIncludeMovieVersionPublicIds(List<String> value) { this.includeMovieVersionPublicIds = value; }
    public List<String> getIncludeAuditoriumPublicIds() { return includeAuditoriumPublicIds; }
    public void setIncludeAuditoriumPublicIds(List<String> value) { this.includeAuditoriumPublicIds = value; }
    public List<String> getExcludeMovieVersionPublicIds() { return excludeMovieVersionPublicIds; }
    public void setExcludeMovieVersionPublicIds(List<String> value) { this.excludeMovieVersionPublicIds = value; }
    public List<String> getExcludeAuditoriumPublicIds() { return excludeAuditoriumPublicIds; }
    public void setExcludeAuditoriumPublicIds(List<String> value) { this.excludeAuditoriumPublicIds = value; }
}
