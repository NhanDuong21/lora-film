package com.lorafilm.movie.autoschedule.validation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OccupancyOverlapValidatorTest {

    @Test
    void rejectsNestedIntervalsUsingMaximumPriorEnd() {
        List<OccupancyInterval> intervals = List.of(
                interval(1L, "outer", "10:00:00", "14:00:00"),
                interval(1L, "nested", "11:00:00", "12:00:00"),
                interval(1L, "later", "13:00:00", "15:00:00"));

        OccupancyOverlapConflict conflict = OccupancyOverlapValidator.findConflict(intervals).orElseThrow();

        assertThat(conflict.auditoriumId()).isEqualTo(1L);
        assertThat(List.of(conflict.priorItemPublicId(), conflict.currentItemPublicId()))
                .containsExactly("outer", "nested");
    }

    @Test
    void rejectsCleaningOnlyOverlap() {
        List<OccupancyInterval> intervals = List.of(
                interval(1L, "first", "10:00:00", "11:15:00"),
                interval(1L, "second", "11:05:00", "12:00:00"));

        assertThat(OccupancyOverlapValidator.findConflict(intervals)).isPresent();
    }

    @Test
    void acceptsExactHalfOpenAdjacency() {
        List<OccupancyInterval> intervals = List.of(
                interval(1L, "first", "10:00:00", "11:15:00"),
                interval(1L, "second", "11:15:00", "12:00:00"));

        assertThat(OccupancyOverlapValidator.findConflict(intervals)).isEmpty();
    }

    @Test
    void partitionsOnlyByAuditorium() {
        List<OccupancyInterval> intervals = List.of(
                interval(1L, "first", "10:00:00", "12:00:00"),
                interval(2L, "second", "10:00:00", "12:00:00"));

        assertThat(OccupancyOverlapValidator.findConflict(intervals)).isEmpty();
    }

    @Test
    void conflictIsDeterministicRegardlessOfInputOrder() {
        OccupancyInterval first = interval(1L, "a", "10:00:00", "12:00:00");
        OccupancyInterval second = interval(1L, "b", "10:30:00", "11:00:00");
        OccupancyInterval third = interval(2L, "c", "09:00:00", "11:00:00");
        OccupancyInterval fourth = interval(2L, "d", "10:00:00", "12:00:00");

        var forward = OccupancyOverlapValidator.findConflict(List.of(first, second, third, fourth));
        var reversed = OccupancyOverlapValidator.findConflict(List.of(fourth, third, second, first));

        assertThat(reversed).isEqualTo(forward);
    }

    @Test
    void detectsCrossMidnightConflictWithoutServiceDatePartitioning() {
        List<OccupancyInterval> intervals = List.of(
                new OccupancyInterval(1L,
                        Instant.parse("2026-07-22T23:30:00Z"),
                        Instant.parse("2026-07-23T00:30:00Z"), "first"),
                new OccupancyInterval(1L,
                        Instant.parse("2026-07-23T00:15:00Z"),
                        Instant.parse("2026-07-23T01:00:00Z"), "second"));

        assertThat(OccupancyOverlapValidator.findConflict(intervals)).isPresent();
    }

    @Test
    void doesNotMutateInputOrder() {
        List<OccupancyInterval> intervals = new ArrayList<>(List.of(
                interval(1L, "later", "12:00:00", "13:00:00"),
                interval(1L, "earlier", "10:00:00", "11:00:00")));
        List<OccupancyInterval> original = List.copyOf(intervals);

        OccupancyOverlapValidator.findConflict(intervals);

        assertThat(intervals).containsExactlyElementsOf(original);
    }

    private OccupancyInterval interval(Long auditoriumId, String publicId, String start, String end) {
        return new OccupancyInterval(
                auditoriumId,
                Instant.parse("2026-07-22T" + start + "Z"),
                Instant.parse("2026-07-22T" + end + "Z"),
                publicId);
    }
}
