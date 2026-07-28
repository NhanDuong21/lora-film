package com.lorafilm.movie.autoschedule.model;

import java.util.Set;

public final class AutoScheduleStrategyVersions {
    public static final String LEGACY_BALANCED_V1 = "BALANCED_V1";
    public static final String LEGACY_BALANCED_V1_S2 = "BALANCED_V1_S2";
    public static final String LEGACY_BALANCED_V1_S3 = "BALANCED_V1_S3";
    public static final String BALANCED_V1_S4 = "BALANCED_V1_S4";
    public static final String BALANCED_V1_S5 = "BALANCED_V1_S5";

    /**
     * S5 retains S4 coverage and adds a deterministic, quality-guarded distribution
     * pass per service date. Historical previews retain their persisted strategy
     * version; only newly generated previews use the current strategy.
     */
    public static final String CURRENT = BALANCED_V1_S5;

    public static final Set<String> SUPPORTED = Set.of(
            LEGACY_BALANCED_V1,
            LEGACY_BALANCED_V1_S2,
            LEGACY_BALANCED_V1_S3,
            BALANCED_V1_S4,
            BALANCED_V1_S5
    );

    public static boolean isSupported(String strategyVersion) {
        return SUPPORTED.contains(strategyVersion);
    }

    private AutoScheduleStrategyVersions() {
    }
}
