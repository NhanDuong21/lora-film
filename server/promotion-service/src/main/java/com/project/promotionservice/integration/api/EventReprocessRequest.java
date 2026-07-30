package com.project.promotionservice.integration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

public class EventReprocessRequest {
    @NotBlank
    @Pattern(regexp = UUID_PATTERN)
    private String eventPublicId;
    public String getEventPublicId() { return eventPublicId; }
    public void setEventPublicId(String eventPublicId) { this.eventPublicId = eventPublicId; }
}
