package com.lorafilm.booking.food.dto.response;

import com.lorafilm.booking.food.enums.FoodOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class FoodSummaryResponse {

    private String publicId;
    private Integer totalQuantity;
    private BigDecimal finalAmount;
    private FoodOrderStatus status;
    private Instant updatedAt;

    public FoodSummaryResponse() {
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public FoodOrderStatus getStatus() {
        return status;
    }

    public void setStatus(FoodOrderStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
