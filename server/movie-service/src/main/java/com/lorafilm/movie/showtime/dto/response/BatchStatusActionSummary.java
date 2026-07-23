package com.lorafilm.movie.showtime.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BatchStatusActionSummary {

    private String batchId;
    private String targetStatus;
    private int totalCount;
    private int eligibleCount;
    private int alreadyTargetCount;
    private int skippedCount;
    private int failedCount;
    private int affectedCount;
    private boolean atomic = true;
    private boolean actionAllowed;
    private List<BatchStatusReasonGroup> reasonGroups = new ArrayList<>();
    private Long actorId;
    private Instant actionAt;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(int eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public int getAlreadyTargetCount() {
        return alreadyTargetCount;
    }

    public void setAlreadyTargetCount(int alreadyTargetCount) {
        this.alreadyTargetCount = alreadyTargetCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getAffectedCount() {
        return affectedCount;
    }

    public void setAffectedCount(int affectedCount) {
        this.affectedCount = affectedCount;
    }

    public boolean isAtomic() {
        return atomic;
    }

    public void setAtomic(boolean atomic) {
        this.atomic = atomic;
    }

    public boolean isActionAllowed() {
        return actionAllowed;
    }

    public void setActionAllowed(boolean actionAllowed) {
        this.actionAllowed = actionAllowed;
    }

    public List<BatchStatusReasonGroup> getReasonGroups() {
        return reasonGroups;
    }

    public void setReasonGroups(List<BatchStatusReasonGroup> reasonGroups) {
        this.reasonGroups = reasonGroups;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public Instant getActionAt() {
        return actionAt;
    }

    public void setActionAt(Instant actionAt) {
        this.actionAt = actionAt;
    }
}
