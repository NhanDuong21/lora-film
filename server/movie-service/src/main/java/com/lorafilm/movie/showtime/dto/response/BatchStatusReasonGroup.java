package com.lorafilm.movie.showtime.dto.response;

import java.util.ArrayList;
import java.util.List;

public class BatchStatusReasonGroup {

    private String reasonCode;
    private String reason;
    private int count;
    private List<String> sampleShowtimePublicIds = new ArrayList<>();

    public BatchStatusReasonGroup() {}

    public BatchStatusReasonGroup(String reasonCode, String reason, int count) {
        this(reasonCode, reason, count, List.of());
    }

    public BatchStatusReasonGroup(String reasonCode,
                                  String reason,
                                  int count,
                                  List<String> sampleShowtimePublicIds) {
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.count = count;
        this.sampleShowtimePublicIds = sampleShowtimePublicIds;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getSampleShowtimePublicIds() {
        return sampleShowtimePublicIds;
    }

    public void setSampleShowtimePublicIds(List<String> sampleShowtimePublicIds) {
        this.sampleShowtimePublicIds = sampleShowtimePublicIds;
    }
}
