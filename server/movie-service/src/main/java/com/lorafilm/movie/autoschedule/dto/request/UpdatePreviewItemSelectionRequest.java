package com.lorafilm.movie.autoschedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdatePreviewItemSelectionRequest {

    @NotBlank
    @Size(max = 36)
    private String itemPublicId;

    @NotNull
    private Boolean selected;

    public UpdatePreviewItemSelectionRequest() {
    }

    public UpdatePreviewItemSelectionRequest(String itemPublicId, Boolean selected) {
        this.itemPublicId = itemPublicId;
        this.selected = selected;
    }

    public String getItemPublicId() {
        return itemPublicId;
    }

    public void setItemPublicId(String itemPublicId) {
        this.itemPublicId = itemPublicId;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
}
