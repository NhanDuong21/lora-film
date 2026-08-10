package com.lorafilm.movie.autoschedule.model;

import java.time.Instant;
import java.time.LocalDate;

public class OperatingWindow {
    private final LocalDate serviceDate;
    private final Instant openInstant;
    private final Instant closeInstant;

    public OperatingWindow(Instant openInstant, Instant closeInstant) {
        this(null, openInstant, closeInstant);
    }

    public OperatingWindow(LocalDate serviceDate, Instant openInstant, Instant closeInstant) {
        this.serviceDate = serviceDate;
        this.openInstant = openInstant;
        this.closeInstant = closeInstant;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public Instant getOpenInstant() {
        return openInstant;
    }

    public Instant getCloseInstant() {
        return closeInstant;
    }
}
