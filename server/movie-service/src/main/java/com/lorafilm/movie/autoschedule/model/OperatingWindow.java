package com.lorafilm.movie.autoschedule.model;

import java.time.Instant;

public class OperatingWindow {
    private final Instant openInstant;
    private final Instant closeInstant;

    public OperatingWindow(Instant openInstant, Instant closeInstant) {
        this.openInstant = openInstant;
        this.closeInstant = closeInstant;
    }

    public Instant getOpenInstant() {
        return openInstant;
    }

    public Instant getCloseInstant() {
        return closeInstant;
    }
}
