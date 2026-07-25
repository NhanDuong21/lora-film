package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePricingPreflightResponse;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePricingPreflightService;
import com.lorafilm.movie.pricing.dto.response.PriceSeatTypeDiagnosticDto;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoSchedulePricingPreflightServiceImpl implements AutoSchedulePricingPreflightService {

    private static final String INCOMPLETE_MESSAGE =
            "Thiếu chính sách hoặc quy tắc giá hiệu lực cho một hoặc nhiều loại ghế.";
    private static final String AMBIGUOUS_MESSAGE =
            "Có nhiều quy tắc giá cùng mức ưu tiên cho một hoặc nhiều loại ghế.";

    private final PricePolicyResolver resolver;

    public AutoSchedulePricingPreflightServiceImpl(PricePolicyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public Evaluation evaluate(List<ShowtimeSchedulePreviewItem> selectedItems) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            AutoSchedulePricingPreflightResponse empty = new AutoSchedulePricingPreflightResponse(
                    true, 0, 0, 0, 0, List.of(), List.of());
            return new Evaluation(empty, List.of());
        }

        List<Showtime> provisionalShowtimes = selectedItems.stream()
                .map(this::provisionalShowtime)
                .toList();
        List<PriceResolutionResult> resolutions = resolver.resolveAll(provisionalShowtimes);
        List<AutoSchedulePricingPreflightResponse.CandidateResult> candidates = new ArrayList<>();

        int completeCount = 0;
        int incompleteCount = 0;
        int ambiguousCount = 0;
        for (int index = 0; index < selectedItems.size(); index++) {
            ShowtimeSchedulePreviewItem item = selectedItems.get(index);
            PriceResolutionResult resolution = resolutions.get(index);
            boolean ambiguous = !resolution.ambiguousSeatTypes().isEmpty();
            boolean missing = !resolution.missingSeatTypes().isEmpty();
            String reasonCode = ambiguous ? "PRICING_AMBIGUOUS" : missing ? "PRICING_INCOMPLETE" : null;
            String displayMessage = ambiguous ? AMBIGUOUS_MESSAGE : missing ? INCOMPLETE_MESSAGE : null;
            if (resolution.isComplete()) {
                completeCount++;
            } else {
                if (missing) incompleteCount++;
                if (ambiguous) ambiguousCount++;
            }
            candidates.add(new AutoSchedulePricingPreflightResponse.CandidateResult(
                    item.getPublicId(),
                    resolution.isComplete(),
                    reasonCode,
                    displayMessage,
                    localDate(item),
                    item.getStartTime(),
                    item.getAuditorium().getPublicId(),
                    item.getAuditorium().getName(),
                    resolution.missingSeatTypes().stream().map(this::diagnostic).toList(),
                    resolution.ambiguousSeatTypes().stream().map(this::diagnostic).toList()));
        }

        List<AutoSchedulePricingPreflightResponse.ReasonGroup> groups =
                groupReasons(candidates);
        AutoSchedulePricingPreflightResponse response = new AutoSchedulePricingPreflightResponse(
                completeCount == selectedItems.size(),
                selectedItems.size(),
                completeCount,
                incompleteCount,
                ambiguousCount,
                groups,
                List.copyOf(candidates));
        return new Evaluation(response, List.copyOf(resolutions));
    }

    private Showtime provisionalShowtime(ShowtimeSchedulePreviewItem item) {
        Showtime showtime = new Showtime();
        showtime.setCinema(item.getCinema());
        showtime.setAuditorium(item.getAuditorium());
        showtime.setStartTime(item.getStartTime());
        return showtime;
    }

    private LocalDate localDate(ShowtimeSchedulePreviewItem item) {
        return item.getStartTime().atZone(ZoneId.of(item.getCinema().getTimezone())).toLocalDate();
    }

    private PriceSeatTypeDiagnosticDto diagnostic(PriceResolutionResult.SeatTypeDiagnostic source) {
        return new PriceSeatTypeDiagnosticDto(
                source.seatTypeId(),
                source.seatTypeCode(),
                source.seatTypeName(),
                source.candidateRuleIds());
    }

    private List<AutoSchedulePricingPreflightResponse.ReasonGroup> groupReasons(
            List<AutoSchedulePricingPreflightResponse.CandidateResult> candidates) {
        Map<String, List<AutoSchedulePricingPreflightResponse.CandidateResult>> grouped = new LinkedHashMap<>();
        candidates.stream()
                .filter(candidate -> !candidate.complete())
                .forEach(candidate -> {
                    if (!candidate.missingSeatTypes().isEmpty()) {
                        grouped.computeIfAbsent("PRICING_INCOMPLETE", ignored -> new ArrayList<>())
                                .add(candidate);
                    }
                    if (!candidate.ambiguousSeatTypes().isEmpty()) {
                        grouped.computeIfAbsent("PRICING_AMBIGUOUS", ignored -> new ArrayList<>())
                                .add(candidate);
                    }
                });

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<AutoSchedulePricingPreflightResponse.CandidateResult> affected = entry.getValue();
                    Map<String, AutoSchedulePricingPreflightResponse.AuditoriumRef> auditoriums =
                            new LinkedHashMap<>();
                    Map<String, AutoSchedulePricingPreflightResponse.SeatTypeRef> seatTypes =
                            new LinkedHashMap<>();
                    affected.forEach(candidate -> {
                        auditoriums.putIfAbsent(
                                candidate.auditoriumPublicId(),
                                new AutoSchedulePricingPreflightResponse.AuditoriumRef(
                                        candidate.auditoriumPublicId(), candidate.auditoriumName()));
                        List<PriceSeatTypeDiagnosticDto> diagnostics =
                                "PRICING_AMBIGUOUS".equals(entry.getKey())
                                        ? candidate.ambiguousSeatTypes() : candidate.missingSeatTypes();
                        diagnostics.forEach(seatType -> seatTypes.putIfAbsent(
                                seatType.seatTypeId(),
                                new AutoSchedulePricingPreflightResponse.SeatTypeRef(
                                        seatType.seatTypeId(), seatType.seatTypeCode(), seatType.seatTypeName())));
                    });
                    return new AutoSchedulePricingPreflightResponse.ReasonGroup(
                            entry.getKey(),
                            "PRICING_AMBIGUOUS".equals(entry.getKey())
                                    ? AMBIGUOUS_MESSAGE : INCOMPLETE_MESSAGE,
                            affected.size(),
                            affected.stream().map(AutoSchedulePricingPreflightResponse.CandidateResult::serviceDate)
                                    .distinct().sorted().toList(),
                            auditoriums.values().stream()
                                    .sorted(Comparator.comparing(AutoSchedulePricingPreflightResponse.AuditoriumRef::name))
                                    .toList(),
                            seatTypes.values().stream()
                                    .sorted(Comparator.comparing(AutoSchedulePricingPreflightResponse.SeatTypeRef::name))
                                    .toList());
                })
                .toList();
    }
}
