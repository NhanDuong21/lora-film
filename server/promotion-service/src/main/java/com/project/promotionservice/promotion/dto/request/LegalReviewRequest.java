package com.project.promotionservice.promotion.dto.request;

import com.project.promotionservice.promotion.enums.LegalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LegalReviewRequest {

    @NotNull
    private LegalStatus status;

    @NotBlank
    @Size(max = 500)
    private String comment;

    @Size(max = 150)
    private String legalNotificationRef;

    public LegalStatus getStatus() {
        return status;
    }

    public void setStatus(LegalStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getLegalNotificationRef() {
        return legalNotificationRef;
    }

    public void setLegalNotificationRef(String legalNotificationRef) {
        this.legalNotificationRef = legalNotificationRef;
    }
}
