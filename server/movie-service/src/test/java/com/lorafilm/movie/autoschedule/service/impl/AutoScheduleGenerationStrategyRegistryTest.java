package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.AutoScheduleEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoScheduleGenerationStrategyRegistryTest {

    @Test
    void resolvesTheRegisteredCurrentDemandStrategyAndKeepsHistoricalStrategies() {
        AutoScheduleGenerationStrategy s3 = strategy(
                AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3);
        AutoScheduleGenerationStrategy s4 = strategy(
                AutoScheduleStrategyVersions.BALANCED_V1_S4);
        AutoScheduleGenerationStrategy s5 = strategy(
                AutoScheduleStrategyVersions.BALANCED_V1_S5);
        AutoScheduleGenerationStrategy demand = strategy(
                AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1);
        AutoScheduleGenerationStrategyRegistry registry =
                new AutoScheduleGenerationStrategyRegistry(List.of(demand, s5, s4, s3));

        assertSame(demand, registry.getCurrent());
        assertSame(s3, registry.require(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3));
        assertSame(s4, registry.require(AutoScheduleStrategyVersions.BALANCED_V1_S4));
        assertSame(s5, registry.require(AutoScheduleStrategyVersions.BALANCED_V1_S5));
        assertSame(demand, registry.require(AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1));
    }

    @Test
    void resolvesExplicitPerCinemaLegacyFlagWithoutChangingTheDefault() {
        AutoScheduleGenerationStrategy demand = strategy(
                AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1);
        AutoScheduleGenerationStrategy legacy = strategy(
                AutoScheduleStrategyVersions.BALANCED_V1_S5);
        AutoScheduleGenerationStrategyRegistry registry =
                new AutoScheduleGenerationStrategyRegistry(List.of(demand, legacy));
        Cinema cinema = new Cinema();
        cinema.setAutoScheduleEngine(AutoScheduleEngine.LEGACY);

        assertSame(legacy, registry.getForCinema(cinema));
        assertSame(demand, registry.getCurrent());
    }

    @Test
    void rejectsDuplicateUnsupportedAndMissingRegistrations() {
        AutoScheduleGenerationStrategy first = strategy(
                AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3);
        AutoScheduleGenerationStrategy duplicate = strategy(
                AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3);
        AutoScheduleGenerationStrategy unsupported = strategy("BALANCED_UNKNOWN");

        assertThrows(IllegalStateException.class,
                () -> new AutoScheduleGenerationStrategyRegistry(List.of(first, duplicate)));
        assertThrows(IllegalStateException.class,
                () -> new AutoScheduleGenerationStrategyRegistry(List.of(unsupported)));
        assertThrows(IllegalStateException.class,
                () -> new AutoScheduleGenerationStrategyRegistry(List.of()).getCurrent());
        assertThrows(IllegalStateException.class,
                () -> new AutoScheduleGenerationStrategyRegistry(List.of(first))
                        .require(AutoScheduleStrategyVersions.BALANCED_V1_S5));
    }

    private AutoScheduleGenerationStrategy strategy(String version) {
        AutoScheduleGenerationStrategy strategy = mock(AutoScheduleGenerationStrategy.class);
        when(strategy.getStrategyVersion()).thenReturn(version);
        return strategy;
    }
}
