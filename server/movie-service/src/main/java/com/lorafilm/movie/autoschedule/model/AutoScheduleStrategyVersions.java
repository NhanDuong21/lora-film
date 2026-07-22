package com.lorafilm.movie.autoschedule.model;

import java.util.Set;

public final class AutoScheduleStrategyVersions {
    public static final String LEGACY_BALANCED_V1 = "BALANCED_V1";
    public static final String LEGACY_BALANCED_V1_S2 = "BALANCED_V1_S2";
    public static final String LEGACY_BALANCED_V1_S3 = "BALANCED_V1_S3";
    public static final String BALANCED_V1_S4 = "BALANCED_V1_S4";

    /** S4 is implemented but remains non-current until its separate activation checkpoint. */
    public static final String CURRENT = LEGACY_BALANCED_V1_S3;

    public static final Set<String> SUPPORTED = Set.of(
            LEGACY_BALANCED_V1,
            LEGACY_BALANCED_V1_S2,
            LEGACY_BALANCED_V1_S3,
            BALANCED_V1_S4
    );

    public static boolean isSupported(String strategyVersion) {
        return SUPPORTED.contains(strategyVersion);
    }

    private AutoScheduleStrategyVersions() {
    }
}
