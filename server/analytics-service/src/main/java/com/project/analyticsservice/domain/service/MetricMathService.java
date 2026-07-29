package com.project.analyticsservice.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

@Service
public class MetricMathService {
    public BigDecimal money(BigDecimal value) {
        return zero(value).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal ratio(BigDecimal numerator, long denominator) {
        return denominator <= 0
                ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : zero(numerator).divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }

    public BigDecimal ratio(long numerator, long denominator) {
        return ratio(BigDecimal.valueOf(numerator), denominator);
    }

    public BigDecimal divide(BigDecimal numerator, BigDecimal denominator, int scale) {
        return denominator == null || denominator.signum() == 0
                ? BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP)
                : zero(numerator).divide(denominator, scale, RoundingMode.HALF_UP);
    }

    public BigDecimal sum(Collection<BigDecimal> values) {
        return values.stream().map(this::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
