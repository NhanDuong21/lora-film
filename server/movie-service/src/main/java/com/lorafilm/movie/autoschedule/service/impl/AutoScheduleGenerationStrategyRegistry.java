package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.AutoScheduleEngine;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoScheduleGenerationStrategyRegistry {

    private final Map<String, AutoScheduleGenerationStrategy> strategies;

    public AutoScheduleGenerationStrategyRegistry(List<AutoScheduleGenerationStrategy> strategies) {
        Map<String, AutoScheduleGenerationStrategy> registered = new LinkedHashMap<>();
        for (AutoScheduleGenerationStrategy strategy : strategies) {
            String version = strategy.getStrategyVersion();
            if (!AutoScheduleStrategyVersions.isSupported(version)) {
                throw new IllegalStateException("Unsupported auto-schedule generation strategy: " + version);
            }
            if (registered.putIfAbsent(version, strategy) != null) {
                throw new IllegalStateException("Duplicate auto-schedule generation strategy: " + version);
            }
        }
        this.strategies = Map.copyOf(registered);
    }

    public AutoScheduleGenerationStrategy getCurrent() {
        AutoScheduleGenerationStrategy strategy = strategies.get(AutoScheduleStrategyVersions.CURRENT);
        if (strategy == null) {
            throw new IllegalStateException(
                    "No generation strategy is registered for current version "
                            + AutoScheduleStrategyVersions.CURRENT);
        }
        return strategy;
    }

    public AutoScheduleGenerationStrategy getForCinema(Cinema cinema) {
        if (cinema != null && cinema.getAutoScheduleEngine() == AutoScheduleEngine.LEGACY) {
            return require(AutoScheduleStrategyVersions.BALANCED_V1_S5);
        }
        return getCurrent();
    }

    public AutoScheduleGenerationStrategy require(String strategyVersion) {
        AutoScheduleGenerationStrategy strategy = strategies.get(strategyVersion);
        if (strategy == null) {
            throw new IllegalStateException(
                    "No generation strategy is registered for version " + strategyVersion);
        }
        return strategy;
    }
}
