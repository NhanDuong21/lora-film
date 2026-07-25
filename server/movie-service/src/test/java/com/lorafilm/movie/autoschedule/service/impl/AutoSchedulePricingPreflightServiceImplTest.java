package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePricingPreflightServiceImplTest {

    @Mock
    private PricePolicyResolver resolver;

    @Test
    void groupsMissingPricingWithAffectedDateRoomAndSeatType() {
        Cinema cinema = new Cinema();
        cinema.setId(10L);
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        Auditorium auditorium = new Auditorium();
        auditorium.setId(20L);
        auditorium.setPublicId("auditorium-20");
        auditorium.setName("Phòng 2");
        auditorium.setCinema(cinema);
        ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
        item.setPublicId("item-1");
        item.setCinema(cinema);
        item.setAuditorium(auditorium);
        item.setStartTime(Instant.parse("2026-09-30T13:00:00Z"));
        PriceResolutionResult result = new PriceResolutionResult(
                "VND",
                "Asia/Ho_Chi_Minh",
                Instant.parse("2026-07-25T00:00:00Z"),
                List.of(),
                List.of(new PriceResolutionResult.SeatTypeDiagnostic(
                        "seat-vip", "VIP", "Ghế VIP", List.of())),
                List.of());
        ArgumentCaptor<List<com.lorafilm.movie.showtime.domain.entity.Showtime>> captor =
                ArgumentCaptor.forClass(List.class);
        when(resolver.resolveAll(captor.capture())).thenReturn(List.of(result));

        var evaluation = new AutoSchedulePricingPreflightServiceImpl(resolver).evaluate(List.of(item));

        assertThat(evaluation.response().complete()).isFalse();
        assertThat(evaluation.response().incompleteCandidateCount()).isEqualTo(1);
        assertThat(evaluation.response().reasonGroups()).singleElement().satisfies(group -> {
            assertThat(group.reasonCode()).isEqualTo("PRICING_INCOMPLETE");
            assertThat(group.affectedDates()).containsExactly(java.time.LocalDate.of(2026, 9, 30));
            assertThat(group.auditoriums()).extracting("name").containsExactly("Phòng 2");
            assertThat(group.seatTypes()).extracting("name").containsExactly("Ghế VIP");
        });
        assertThat(captor.getValue()).singleElement().satisfies(showtime -> {
            assertThat(showtime.getCinema()).isSameAs(cinema);
            assertThat(showtime.getAuditorium()).isSameAs(auditorium);
            assertThat(showtime.getStartTime()).isEqualTo(item.getStartTime());
        });
    }

    @Test
    void distinguishesCompleteMissingAndAmbiguousCandidates() {
        Cinema cinema = cinema();
        Auditorium auditorium = auditorium(cinema);
        List<ShowtimeSchedulePreviewItem> items = List.of(
                item("complete", cinema, auditorium, "2026-10-01T01:00:00Z"),
                item("missing", cinema, auditorium, "2026-10-01T03:00:00Z"),
                item("ambiguous", cinema, auditorium, "2026-10-01T05:00:00Z"));
        PriceResolutionResult complete = result(List.of(), List.of());
        PriceResolutionResult missing = result(
                List.of(new PriceResolutionResult.SeatTypeDiagnostic("normal", "NORMAL", "Ghế thường", List.of())),
                List.of());
        PriceResolutionResult ambiguous = result(
                List.of(),
                List.of(new PriceResolutionResult.SeatTypeDiagnostic(
                        "vip", "VIP", "Ghế VIP", List.of("rule-1", "rule-2"))));
        when(resolver.resolveAll(org.mockito.ArgumentMatchers.any())).thenReturn(
                List.of(complete, missing, ambiguous));

        var response = new AutoSchedulePricingPreflightServiceImpl(resolver).evaluate(items).response();

        assertThat(response.completeCandidateCount()).isEqualTo(1);
        assertThat(response.incompleteCandidateCount()).isEqualTo(1);
        assertThat(response.ambiguousCandidateCount()).isEqualTo(1);
        assertThat(response.reasonGroups()).extracting("reasonCode")
                .containsExactly("PRICING_AMBIGUOUS", "PRICING_INCOMPLETE");
    }

    private Cinema cinema() {
        Cinema cinema = new Cinema();
        cinema.setId(10L);
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        return cinema;
    }

    private Auditorium auditorium(Cinema cinema) {
        Auditorium auditorium = new Auditorium();
        auditorium.setId(20L);
        auditorium.setPublicId("auditorium-20");
        auditorium.setName("Phòng 2");
        auditorium.setCinema(cinema);
        return auditorium;
    }

    private ShowtimeSchedulePreviewItem item(
            String publicId, Cinema cinema, Auditorium auditorium, String startTime) {
        ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
        item.setPublicId(publicId);
        item.setCinema(cinema);
        item.setAuditorium(auditorium);
        item.setStartTime(Instant.parse(startTime));
        return item;
    }

    private PriceResolutionResult result(
            List<PriceResolutionResult.SeatTypeDiagnostic> missing,
            List<PriceResolutionResult.SeatTypeDiagnostic> ambiguous) {
        return new PriceResolutionResult(
                "VND",
                "Asia/Ho_Chi_Minh",
                Instant.parse("2026-07-25T00:00:00Z"),
                List.of(),
                missing,
                ambiguous);
    }
}
