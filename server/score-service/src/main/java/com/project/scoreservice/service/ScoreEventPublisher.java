package com.project.scoreservice.service;

import com.project.scoreservice.entity.OutboxEvent;

public interface ScoreEventPublisher {
    void publish(OutboxEvent event);
}
