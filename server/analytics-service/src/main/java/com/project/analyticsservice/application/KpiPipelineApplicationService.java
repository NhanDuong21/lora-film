package com.project.analyticsservice.application;

import com.project.analyticsservice.domain.service.KpiPipelineDomainService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class KpiPipelineApplicationService {
    private final KpiPipelineDomainService domainService;

    public KpiPipelineApplicationService(KpiPipelineDomainService domainService) {
        this.domainService = domainService;
    }

    public void calculate(LocalDate statDate) {
        domainService.calculate(statDate);
    }

    public boolean calculateIfStale(LocalDate statDate) {
        return domainService.calculateIfStale(statDate);
    }
}
