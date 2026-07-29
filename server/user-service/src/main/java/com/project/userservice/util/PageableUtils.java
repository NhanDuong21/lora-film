package com.project.userservice.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableUtils {
    private PageableUtils() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedSorts,
                                    String defaultSort, Sort.Direction defaultDirection) {
        var orders = pageable.getSort().stream()
                .filter(order -> allowedSorts.contains(order.getProperty()))
                .toList();
        Sort sort = orders.isEmpty()
                ? Sort.by(defaultDirection, defaultSort)
                : Sort.by(orders);
        return PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), 100),
                sort);
    }
}
