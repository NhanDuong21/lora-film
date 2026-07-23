package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ExistingShowtimeServiceDateClassifier {

    private static final Comparator<OperatingWindow> WINDOW_ORDER = Comparator
            .comparing(OperatingWindow::getServiceDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(OperatingWindow::getOpenInstant)
            .thenComparing(OperatingWindow::getCloseInstant);

    /**
     * Assigns an existing start instant to a resolved authoritative operating window.
     * Window membership is [open, close); an instant outside every window is unclassifiable.
     */
    public Optional<LocalDate> classify(Instant startTime,
                                        ZoneId cinemaZone,
                                        List<OperatingWindow> operatingWindows) {
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(cinemaZone, "cinemaZone");
        Objects.requireNonNull(operatingWindows, "operatingWindows");

        ZonedDateTime cinemaStart = startTime.atZone(cinemaZone);
        return operatingWindows.stream()
                .filter(window -> window.getServiceDate() != null)
                .sorted(WINDOW_ORDER)
                .filter(window -> !cinemaStart.isBefore(window.getOpenInstant().atZone(cinemaZone))
                        && cinemaStart.isBefore(window.getCloseInstant().atZone(cinemaZone)))
                .map(OperatingWindow::getServiceDate)
                .findFirst();
    }
}
