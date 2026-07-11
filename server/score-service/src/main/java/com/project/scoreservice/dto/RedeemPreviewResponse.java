package com.project.scoreservice.dto;
 
public class RedeemPreviewResponse {
    private Long bookingId;
    private Integer availablePoints;
    private Integer requestedPoints;
    private Integer redeemValue;
    private String currency = "VND";
    private boolean previewOnly = true;
 
    public RedeemPreviewResponse() {
    }
 
    public RedeemPreviewResponse(Long bookingId, Integer availablePoints, Integer requestedPoints, Integer redeemValue) {
        this.bookingId = bookingId;
        this.availablePoints = availablePoints;
        this.requestedPoints = requestedPoints;
        this.redeemValue = redeemValue;
    }
 
    public Long getBookingId() {
        return bookingId;
    }
 
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
 
    public Integer getAvailablePoints() {
        return availablePoints;
    }
 
    public void setAvailablePoints(Integer availablePoints) {
        this.availablePoints = availablePoints;
    }
 
    public Integer getRequestedPoints() {
        return requestedPoints;
    }
 
    public void setRequestedPoints(Integer requestedPoints) {
        this.requestedPoints = requestedPoints;
    }
 
    public Integer getRedeemValue() {
        return redeemValue;
    }
 
    public void setRedeemValue(Integer redeemValue) {
        this.redeemValue = redeemValue;
    }
 
    public String getCurrency() {
        return currency;
    }
 
    public void setCurrency(String currency) {
        this.currency = currency;
    }
 
    public boolean isPreviewOnly() {
        return previewOnly;
    }
 
    public void setPreviewOnly(boolean previewOnly) {
        this.previewOnly = previewOnly;
    }
}
