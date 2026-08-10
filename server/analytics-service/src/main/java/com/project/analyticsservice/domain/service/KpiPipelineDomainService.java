package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.domain.service.calculator.KpiCalculator;
import com.project.analyticsservice.entity.KpiCalculationRun;
import com.project.analyticsservice.repository.KpiCalculationRunRepository;
import com.project.analyticsservice.repository.ProcessedAnalyticsEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class KpiPipelineDomainService {
    private final List<KpiCalculator> calculators;
    private final KpiCalculationRunRepository runRepository;
    private final MeterRegistry meterRegistry;
    private final ProcessedAnalyticsEventRepository processedEventRepository;
    private final ReentrantLock calculationLock = new ReentrantLock();

    public KpiPipelineDomainService(
            List<KpiCalculator> calculators,
            KpiCalculationRunRepository runRepository,
            ProcessedAnalyticsEventRepository processedEventRepository,
            MeterRegistry meterRegistry) {
        this.calculators = List.copyOf(calculators);
        this.runRepository = runRepository;
        this.processedEventRepository = processedEventRepository;
        this.meterRegistry = meterRegistry;
    }

    public void calculate(LocalDate statDate) {
        calculationLock.lock();
        try {
            doCalculate(statDate);
        } finally {
            calculationLock.unlock();
        }
    }

    public boolean calculateIfStale(LocalDate statDate) {
        KpiCalculationRun lastSuccess = runRepository
                .findFirstByStatDateAndStatusOrderByStartedAtDesc(statDate, "SUCCESS")
                .orElse(null);
        Instant latestEvent = processedEventRepository.findFirstByOrderByProcessedAtDesc()
                .map(value -> value.getProcessedAt())
                .orElse(null);
        if (lastSuccess != null
                && (latestEvent == null
                    || (lastSuccess.getCompletedAt() != null
                        && !latestEvent.isAfter(lastSuccess.getCompletedAt())))) {
            return false;
        }
        calculate(statDate);
        return true;
    }

    private void doCalculate(LocalDate statDate) {
        KpiCalculationRun run = new KpiCalculationRun();
        run.setRunId(UUID.randomUUID().toString());
        run.setStatDate(statDate);
        run.setStatus("RUNNING");
        run.setStartedAt(Instant.now());
        runRepository.save(run);

        try {
            for (KpiCalculator calculator : calculators) {
                calculator.calculate(statDate);
                run.setCompletedStage(calculator.stage());
                runRepository.save(run);
            }
            run.setStatus("SUCCESS");
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            meterRegistry.counter("analytics.kpi.pipeline", "status", "success").increment();
        } catch (RuntimeException exception) {
            run.setStatus("FAILED");
            run.setErrorMessage(limit(exception.getMessage()));
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            meterRegistry.counter("analytics.kpi.pipeline", "status", "failed").increment();
            throw exception;
        }
    }

    private String limit(String message) {
        if (message == null) {
            return "Unknown KPI pipeline failure";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
