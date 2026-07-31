package com.project.scoreservice.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfiguration {

    @Bean
    public CommonErrorHandler scoreKafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${score.kafka.booking-events-dlt-topic:"
                    + "booking.events.v1.DLT}") String deadLetterTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                        new TopicPartition(deadLetterTopic, record.partition()));
        DefaultErrorHandler handler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        handler.setCommitRecovered(true);
        return handler;
    }
}
