package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.entity.BusinessAlert;
import com.project.analyticsservice.entity.BusinessInsight;
import com.project.analyticsservice.repository.BusinessAlertRepository;
import com.project.analyticsservice.repository.BusinessInsightRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Component
@Order(90)
public class AlertKpiCalculator implements KpiCalculator {
    private static final Set<String> ALERT_SEVERITIES = Set.of("HIGH", "CRITICAL");
    private final BusinessInsightRepository insightRepository;
    private final BusinessAlertRepository alertRepository;

    public AlertKpiCalculator(
            BusinessInsightRepository insightRepository,
            BusinessAlertRepository alertRepository) {
        this.insightRepository = insightRepository;
        this.alertRepository = alertRepository;
    }

    @Override
    public String stage() {
        return "ALERT";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        for (BusinessInsight insight : insightRepository.findAllByStatDate(statDate)) {
            alertRepository.findByInsightId(insight.getId()).ifPresent(alert -> {
                if (Boolean.TRUE.equals(insight.getResolved())) {
                    alert.setResolved(true);
                    alert.setResolvedAt(java.time.Instant.now());
                    alertRepository.save(alert);
                }
            });
        }
        insightRepository.findAllByStatDate(statDate).stream()
                .filter(insight -> !Boolean.TRUE.equals(insight.getResolved()))
                .filter(insight -> ALERT_SEVERITIES.contains(insight.getSeverity()))
                .filter(insight -> alertRepository.findByInsightId(insight.getId()).isEmpty())
                .forEach(this::createAlert);
    }

    private void createAlert(BusinessInsight insight) {
        BusinessAlert alert = new BusinessAlert();
        alert.setInsightId(insight.getId());
        alert.setEntityType(insight.getEntityType());
        alert.setEntityKey(insight.getEntityKey());
        alert.setSeverity("CRITICAL".equals(insight.getSeverity()) ? "CRITICAL" : "WARNING");
        alert.setTitle(insight.getTitle());
        alert.setMessage(insight.getSummary());
        alert.setAcknowledged(false);
        alert.setResolved(false);
        alertRepository.save(alert);
    }
}
