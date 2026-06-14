package com.project.authservice.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.project.authservice.event.dto.AccountCreatedEvent;

/**
 * Production-safe Kafka producer configuration.
 *
 * <p>Settings applied:
 * <ul>
 *   <li>acks=all – wait for all in-sync replicas to acknowledge</li>
 *   <li>enable.idempotence=true – exactly-once producer semantics</li>
 *   <li>retries=5 – retry on transient failures</li>
 *   <li>max.in.flight.requests.per.connection=5 – safe with idempotence enabled</li>
 * </ul>
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Builds the producer configuration map.
     */
    private Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serializers
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability settings
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Do not add type headers – consumer controls deserialization explicitly
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return props;
    }

    /**
     * Producer factory for {@link AccountCreatedEvent} payloads.
     */
    @Bean
    public ProducerFactory<String, AccountCreatedEvent> accountCreatedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * KafkaTemplate used by {@code AuthAccountEventPublisher}.
     */
    @Bean
    public KafkaTemplate<String, AccountCreatedEvent> accountCreatedKafkaTemplate() {
        return new KafkaTemplate<>(accountCreatedProducerFactory());
    }
}
