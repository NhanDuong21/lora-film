package com.project.promotionservice.integration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

public class EventPublishRequest {
    @NotBlank @Size(max = 50)
    private String aggregateType;
    @NotBlank @Pattern(regexp = UUID_PATTERN)
    private String aggregatePublicId;
    @NotBlank @Size(max = 100)
    private String eventType;
    @NotBlank @Size(max = 150)
    @Pattern(regexp = "^[a-z0-9][a-z0-9.-]*$")
    private String topicName;
    @NotBlank @Size(max = 200000)
    private String payloadJson;

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public String getAggregatePublicId() { return aggregatePublicId; }
    public void setAggregatePublicId(String aggregatePublicId) { this.aggregatePublicId = aggregatePublicId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
