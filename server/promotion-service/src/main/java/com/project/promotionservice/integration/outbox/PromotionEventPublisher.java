package com.project.promotionservice.integration.outbox;

public interface PromotionEventPublisher {

    void publish(PromotionOutboxEvent event);
}
