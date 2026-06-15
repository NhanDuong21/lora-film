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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 *
 * <p>Jackson is configured with {@link JavaTimeModule} and
 * {@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS} disabled so that
 * {@code Instant} fields serialize as ISO-8601 strings (e.g. {@code 2026-06-12T10:30:00Z})
 * and {@code LocalDate} honours the {@code @JsonFormat} annotation on each field.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Jackson {@link ObjectMapper} configured for Java-time serialization.
     * <ul>
     *   <li>{@link JavaTimeModule} registered so {@code Instant} / {@code LocalDate}
     *       are handled natively.</li>
     *   <li>{@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS} disabled so
     *       dates/times are always written as ISO-8601 strings, never as numbers.</li>
     * </ul>
     */
    private ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Builds the Kafka producer configuration map.
     *
     * <p>Serializer classes are intentionally <em>omitted</em> here because the
     * {@link ProducerFactory} is constructed with explicit serializer instances
     * (see {@link #accountCreatedProducerFactory()}).  Putting serializer-class
     * or {@link JsonSerializer}-specific keys in this map <em>at the same time</em>
     * as using setter-configured serializer instances would trigger:
     * <pre>IllegalStateException: JsonSerializer must be configured with property
     * setters, or via configuration properties; not both.</pre>
     */
    private Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Reliability settings
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return props;
    }

    /**
     * Producer factory for {@link AccountCreatedEvent} payloads.
     * Uses a custom {@link ObjectMapper} that serializes Java-time types as
     * ISO-8601 strings.
     */
    @Bean
    public ProducerFactory<String, AccountCreatedEvent> accountCreatedProducerFactory() {
        // Build serializer with the custom ObjectMapper (Java-time → ISO-8601 strings).
        // setAddTypeInfo(false) is the ONLY place we configure this flag – the props
        // map above intentionally carries no JsonSerializer keys to avoid the
        // "configured with property setters … not both" IllegalStateException.
        JsonSerializer<AccountCreatedEvent> valueSerializer =
                new JsonSerializer<>(kafkaObjectMapper());
        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(
                producerConfigs(),
                new StringSerializer(),
                valueSerializer);
    }

    /**
     * KafkaTemplate used by {@code AuthAccountEventPublisher}.
     */
    @Bean
    public KafkaTemplate<String, AccountCreatedEvent> accountCreatedKafkaTemplate() {
        return new KafkaTemplate<>(accountCreatedProducerFactory());
    }
}
