package com.lorafilm.booking.booking.client;

import com.lorafilm.booking.common.exception.IntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Component
public class MovieServiceShowtimeClient implements ShowtimeClient {

    private final RestClient restClient;
    private final String internalToken;

    public MovieServiceShowtimeClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.movie-service.url:http://localhost:8082}") String movieServiceUrl,
            @Value("${app.internal-token}") String internalToken) {
        this.restClient = restClientBuilder.baseUrl(movieServiceUrl).build();
        this.internalToken = internalToken;
    }

    @Override
    public ShowtimeBookingContext getBookingContext(Long showtimeId, List<Long> seatIds) {
        try {
            BookingContextEnvelope envelope = restClient.post()
                    .uri("/internal/showtimes/{showtimeId}/booking-context", showtimeId)
                    .header("X-Internal-Token", internalToken)
                    .body(new BookingContextRequest(seatIds))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IntegrationException(
                                "Movie Service rejected booking context request with status " + response.getStatusCode());
                    })
                    .body(BookingContextEnvelope.class);

            if (envelope == null || envelope.data == null) {
                throw new IntegrationException("Movie Service returned an empty booking context");
            }
            return envelope.data.toDomain();
        } catch (IntegrationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new IntegrationException("Cannot retrieve booking context from Movie Service", ex);
        }
    }

    @Override
    public ShowtimeBookingContext getBookingContextByPublicId(String showtimePublicId, List<String> seatPublicIds) {
        try {
            BookingContextEnvelope envelope = restClient.post()
                    .uri("/internal/showtimes/by-public-id/{showtimePublicId}/booking-context", showtimePublicId)
                    .header("X-Internal-Token", internalToken)
                    .body(new PublicBookingContextRequest(seatPublicIds))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IntegrationException(
                                "Movie Service rejected public booking context request with status " + response.getStatusCode());
                    })
                    .body(BookingContextEnvelope.class);
            if (envelope == null || envelope.data == null) {
                throw new IntegrationException("Movie Service returned an empty booking context");
            }
            return envelope.data.toDomain();
        } catch (IntegrationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new IntegrationException("Cannot retrieve public booking context from Movie Service", ex);
        }
    }

    private record BookingContextRequest(List<Long> seatIds) {
    }

    private record PublicBookingContextRequest(List<String> seatPublicIds) {
    }

    private static class BookingContextEnvelope {
        public BookingContextPayload data;
    }

    private static class BookingContextPayload {
        public ShowtimePayload showtime;
        public Long movieId;
        public Long cinemaId;
        public Long auditoriumId;
        public ResourcePayload movie;
        public ResourcePayload cinema;
        public ResourcePayload auditorium;
        public List<SeatPayload> selectedSeats;
        public PricingPayload pricing;
        public OffsetDateTime bookingExpiredAt;

        private ShowtimeBookingContext toDomain() {
            if (showtime == null || pricing == null || movie == null || cinema == null || auditorium == null) {
                throw new IntegrationException("Movie Service returned an incomplete booking context");
            }
            List<ShowtimeBookingContext.SeatContext> seatContexts = selectedSeats == null
                    ? List.of()
                    : selectedSeats.stream().map(SeatPayload::toDomain).toList();
            return new ShowtimeBookingContext(
                    requireId(showtime.id, "showtime"),
                    requirePublicId(showtime.publicId, "showtime"),
                    requireId(firstNonNull(movieId, movie.id), "movie"),
                    requirePublicId(movie.publicId, "movie"),
                    requireId(firstNonNull(cinemaId, cinema.id), "cinema"),
                    requirePublicId(cinema.publicId, "cinema"),
                    requireId(firstNonNull(auditoriumId, auditorium.id), "auditorium"),
                    showtime.status,
                    toInstant(showtime.startAt),
                    toInstant(showtime.endAt),
                    toInstant(bookingExpiredAt),
                    defaultZero(pricing.seatAmount),
                    defaultZero(pricing.serviceFee),
                    defaultZero(pricing.discountAmount),
                    defaultZero(pricing.totalAmount),
                    pricing.currency,
                    movie.title,
                    movie.posterUrl,
                    cinema.name,
                    auditorium.name,
                    seatContexts);
        }
    }

    private static class ShowtimePayload {
        public Long id;
        public String publicId;
        public String status;
        public LocalDate serviceDate;
        public OffsetDateTime startAt;
        public OffsetDateTime endAt;
    }

    private static class ResourcePayload {
        public Long id;
        public String publicId;
        public String title;
        public String posterUrl;
        public String name;
    }

    private static class SeatPayload {
        public Long seatId;
        public String seatPublicId;
        public String seatCode;
        public String seatType;
        public String pairGroup;
        public BigDecimal price;
        public String currency;

        private ShowtimeBookingContext.SeatContext toDomain() {
            return new ShowtimeBookingContext.SeatContext(
                    seatId, seatPublicId, seatCode, seatType, price, currency, pairGroup);
        }
    }

    private static class PricingPayload {
        public BigDecimal seatAmount;
        public BigDecimal discountAmount;
        public BigDecimal serviceFee;
        public BigDecimal totalAmount;
        public String currency;
    }

    private static Long requireId(Long id, String resourceName) {
        if (id == null || id <= 0) {
            throw new IntegrationException("Movie Service did not provide the numeric " + resourceName + " ID");
        }
        return id;
    }

    private static Long firstNonNull(Long primaryId, Long fallbackId) {
        return primaryId != null ? primaryId : fallbackId;
    }

    private static String requirePublicId(String publicId, String resourceName) {
        if (publicId == null || publicId.isBlank()) {
            throw new IntegrationException("Movie Service did not provide the " + resourceName + " public ID");
        }
        return publicId;
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
