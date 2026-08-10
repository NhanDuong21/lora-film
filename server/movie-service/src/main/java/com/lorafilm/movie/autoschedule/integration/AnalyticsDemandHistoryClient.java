package com.lorafilm.movie.autoschedule.integration;

import com.lorafilm.movie.autoschedule.model.DemandHistorySnapshot;
import com.lorafilm.movie.autoschedule.service.DemandHistoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class AnalyticsDemandHistoryClient implements DemandHistoryProvider {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsDemandHistoryClient.class);

    private final RestClient restClient;
    private final String internalToken;
    private final boolean enabled;
    private final Clock clock;

    public AnalyticsDemandHistoryClient(
            @Value("${services.analytics-service.url:http://localhost:8089}") String baseUrl,
            @Value("${services.analytics-service.internal-token:${app.internal-token}}") String internalToken,
            @Value("${autoschedule.demand.analytics-enabled:true}") boolean enabled,
            @Value("${autoschedule.demand.analytics-timeout-millis:1500}") long timeoutMillis,
            Clock clock) {
        Duration timeout = Duration.ofMillis(Math.max(100, timeoutMillis));
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl)
                .requestFactory(requestFactory).build();
        this.internalToken = internalToken;
        this.enabled = enabled;
        this.clock = clock;
    }

    @Override
    public DemandHistorySnapshot load(String cinemaPublicId,
                                      ZoneId cinemaZone,
                                      LocalDate historyFrom,
                                      LocalDate historyTo,
                                      List<String> moviePublicIds) {
        if (!enabled) {
            return DemandHistorySnapshot.unavailable(historyFrom, historyTo, clock.instant());
        }
        try {
            Envelope response = restClient.post()
                    .uri("/internal/analytics/demand-snapshot")
                    .header("X-Internal-Token", internalToken)
                    .body(new Request(cinemaPublicId, historyFrom, historyTo,
                            cinemaZone.getId(), moviePublicIds))
                    .retrieve()
                    .body(Envelope.class);
            if (response == null || !response.success() || response.data() == null) {
                throw new IllegalStateException("Analytics returned an empty demand snapshot");
            }
            DemandHistorySnapshot data = response.data();
            return new DemandHistorySnapshot(true, data.snapshotVersion(), data.generatedAt(),
                    data.historyFrom(), data.historyTo(), data.sourceBookingFactCount(),
                    data.factsWithShowtimeContext(),
                    data.cinemaPrior() == null ? DemandHistorySnapshot.Aggregate.empty() : data.cinemaPrior(),
                    data.movies() == null ? List.of() : List.copyOf(data.movies()),
                    data.slots() == null ? List.of() : List.copyOf(data.slots()),
                    data.formats() == null ? List.of() : List.copyOf(data.formats()));
        } catch (Exception exception) {
            log.warn("Demand history unavailable for cinema {}; using explicit cold-start priors: {}",
                    cinemaPublicId, exception.getClass().getSimpleName());
            return DemandHistorySnapshot.unavailable(historyFrom, historyTo, clock.instant());
        }
    }

    private record Request(String cinemaPublicId,
                           LocalDate historyFrom,
                           LocalDate historyTo,
                           String cinemaTimezone,
                           List<String> moviePublicIds) {
    }

    private record Envelope(boolean success,
                            String message,
                            String errorCode,
                            DemandHistorySnapshot data) {
    }
}
