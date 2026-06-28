package com.project.scoreservice.dto;
 
import com.project.scoreservice.enumtype.ScoreTransactionType;
import java.time.LocalDateTime;
 
public class ScoreHistoryResponse {
    private Long historyId;
    private String eventId;
    private Long bookingId;
    private Integer pointChange;
    private ScoreTransactionType transactionType;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private Integer accumulatedBefore;
    private Integer accumulatedAfter;
    private Long referenceHistoryId;
    private String description;
 
    public ScoreHistoryResponse() {
    }
 
    public ScoreHistoryResponse(Long historyId, String eventId, Long bookingId, Integer pointChange, ScoreTransactionType transactionType, Integer balanceBefore, Integer balanceAfter, Integer accumulatedBefore, Integer accumulatedAfter, Long referenceHistoryId, String description) {
        this.historyId = historyId;
        this.eventId = eventId;
        this.bookingId = bookingId;
        this.pointChange = pointChange;
        this.transactionType = transactionType;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.accumulatedBefore = accumulatedBefore;
        this.accumulatedAfter = accumulatedAfter;
        this.referenceHistoryId = referenceHistoryId;
        this.description = description;
    }
 
    public Long getHistoryId() {
        return historyId;
    }
 
    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }
 
    public String getEventId() {
        return eventId;
    }
 
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
 
    public Long getBookingId() {
        return bookingId;
    }
 
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
 
    public Integer getPointChange() {
        return pointChange;
    }
 
    public void setPointChange(Integer pointChange) {
        this.pointChange = pointChange;
    }
 
    public ScoreTransactionType getTransactionType() {
        return transactionType;
    }
 
    public void setTransactionType(ScoreTransactionType transactionType) {
        this.transactionType = transactionType;
    }
 
    public Integer getBalanceBefore() {
        return balanceBefore;
    }
 
    public void setBalanceBefore(Integer balanceBefore) {
        this.balanceBefore = balanceBefore;
    }
 
    public Integer getBalanceAfter() {
        return balanceAfter;
    }
 
    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
 
    public Integer getAccumulatedBefore() {
        return accumulatedBefore;
    }
 
    public void setAccumulatedBefore(Integer accumulatedBefore) {
        this.accumulatedBefore = accumulatedBefore;
    }
 
    public Integer getAccumulatedAfter() {
        return accumulatedAfter;
    }
 
    public void setAccumulatedAfter(Integer accumulatedAfter) {
        this.accumulatedAfter = accumulatedAfter;
    }
 
    public Long getReferenceHistoryId() {
        return referenceHistoryId;
    }
 
    public void setReferenceHistoryId(Long referenceHistoryId) {
        this.referenceHistoryId = referenceHistoryId;
    }
 
    public String getDescription() {
        return description;
    }
 
    public void setDescription(String description) {
        this.description = description;
    }
}
