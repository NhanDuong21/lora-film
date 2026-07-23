package com.lorafilm.movie.autoschedule.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UpdatePreviewItemSelectionsRequest {

    @NotNull
    @PositiveOrZero
    private Long expectedVersion;

    @NotEmpty
    @Size(max = 10000)
    @Valid
    private List<UpdatePreviewItemSelectionRequest> items;

    public UpdatePreviewItemSelectionsRequest() {
    }

    public UpdatePreviewItemSelectionsRequest(Long expectedVersion, List<UpdatePreviewItemSelectionRequest> items) {
        this.expectedVersion = expectedVersion;
        this.items = items;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }

    public List<UpdatePreviewItemSelectionRequest> getItems() {
        return items;
    }

    public void setItems(List<UpdatePreviewItemSelectionRequest> items) {
        this.items = items;
    }
}
