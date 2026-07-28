package com.project.promotionservice.promotion.dto.request;

import com.project.promotionservice.common.constant.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to clone an existing promotion rule")
public class RuleCloneRequest {

    @NotBlank(message = "newCode is required")
    @Size(min = 2, max = 100, message = "newCode must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "newCode must contain only alphanumeric characters, underscores, or hyphens")
    private String newCode;

    @NotBlank(message = "newName is required")
    @Size(min = 2, max = 255, message = "newName must be between 2 and 255 characters")
    private String newName;

    @Pattern(regexp = ValidationConstants.UUID_PATTERN, message = "targetCampaignPublicId must be a valid UUID")
    private String targetCampaignPublicId;

    public RuleCloneRequest() {
    }

    public String getNewCode() {
        return newCode;
    }

    public void setNewCode(String newCode) {
        this.newCode = newCode;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public String getTargetCampaignPublicId() {
        return targetCampaignPublicId;
    }

    public void setTargetCampaignPublicId(String targetCampaignPublicId) {
        this.targetCampaignPublicId = targetCampaignPublicId;
    }
}
