package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoScheduleGenerationStrategyRegistryTest {

    @Test
    void resolvesTheRegisteredCurrentS5StrategyAndKeepsHistoricalStrategies() {
        AutoScheduleGenerationStrategy s3 = strategy(
                AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3);
        AutoScheduleGenerationStrategy s4 = strategy(
                AutoScheduleStrategyVersions.BALANCED_V1_S4);
        AutoScheduleGenerationStrategy s5 = strategy(
                AutoScheduleStrategyVersions.BALANCED_V1_S5);
        AutoScheduleGenerationStrategyRegistry registry =
                new AutoScheduleGenerationStrategyRegistry(List.of(s5, s4, s3));

        assertSame(s5, registry.getCurrent());
        assertSame(s3, registry.require(AutoScheduleStrategyVersions.LEGACY_BALANCED_V1_S3));
        assertSame(s4, registry.require(AutoScheduleStrategyVersions.BALANCED_V1_S4));
        assertSame(s5, registry.require(AutoScheduleStrategyVersions.BALANCED_V1_S5));
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
