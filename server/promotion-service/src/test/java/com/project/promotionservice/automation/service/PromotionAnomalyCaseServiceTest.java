package com.project.promotionservice.automation.service;

import com.project.promotionservice.automation.client.AutomationActorDirectoryClient;
import com.project.promotionservice.automation.dto.AutomationDtos.ResolveAnomalyRequest;
import com.project.promotionservice.automation.entity.PromotionAnomalyCase;
import com.project.promotionservice.automation.enums.AnomalyCaseStatus;
import com.project.promotionservice.automation.enums.AnomalyResolution;
import com.project.promotionservice.automation.repository.PromotionAnomalyCaseRepository;
import com.project.promotionservice.common.audit.AuditTrailService;
import com.project.promotionservice.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionAnomalyCaseServiceTest {
    @Mock PromotionAnomalyCaseRepository repository;
    @Mock AutomationActorDirectoryClient actorDirectory;
    @Mock AuditTrailService auditTrailService;
    PromotionAnomalyCaseService service;

    @BeforeEach
    void setUp() {
        service = new PromotionAnomalyCaseService(
                repository, actorDirectory, auditTrailService);
    }

    @Test
    void productionQueueExcludesTestCasesByDefault() {
        PromotionAnomalyCase production = anomaly(false);
        when(repository.findByStatusInAndTestDataOrderByCreatedAtDesc(
                List.of(AnomalyCaseStatus.OPEN, AnomalyCaseStatus.IN_REVIEW), false))
                .thenReturn(List.of(production));

        var cases = service.openCases(false);

        assertEquals(1, cases.size());
        assertFalse(cases.getFirst().testData());
        verify(repository, never()).findByStatusInOrderByCreatedAtDesc(any());
    }

    @Test
    void dismissingTestCasePersistsResolverAndAudit() {
        PromotionAnomalyCase item = anomaly(true);
        when(repository.findByPublicId(item.getPublicId()))
                .thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);
        when(actorDirectory.displayName("operator-1")).thenReturn("Admin Operator");

        var resolved = service.resolve(item.getPublicId(),
                new ResolveAnomalyRequest(
                        AnomalyResolution.TEST_DATA, "Hoàn tất walkthrough UAT"),
                "operator-1");

        assertEquals(AnomalyCaseStatus.DISMISSED_TEST_DATA, resolved.status());
        assertEquals("operator-1", resolved.resolvedBy());
        assertNotNull(resolved.resolvedAt());
        verify(auditTrailService).record(eq("PROMOTION_ANOMALY_CASE"),
                eq(item.getPublicId()), eq("ANOMALY_CASE_RESOLVE"),
                any(), any(), eq("operator-1"));
    }

    @Test
    void productionCaseCannotBeDismissedAsTestData() {
        PromotionAnomalyCase item = anomaly(false);
        when(repository.findByPublicId(item.getPublicId()))
                .thenReturn(Optional.of(item));

        assertThrows(BusinessException.class, () -> service.resolve(
                item.getPublicId(), new ResolveAnomalyRequest(
                        AnomalyResolution.TEST_DATA, "Không hợp lệ"), "operator-1"));
        verify(repository, never()).save(any());
    }

    private PromotionAnomalyCase anomaly(boolean testData) {
        PromotionAnomalyCase item = new PromotionAnomalyCase();
        item.setPublicId(testData ? "case-uat" : "case-production");
        item.setRunPublicId("run-1");
        item.setAudienceMemberPublicId("member-1");
        item.setPlaybookCode(PromotionAutomationService.SECOND_BOOKING);
        item.setCustomerPublicId("customer-1");
        item.setReasonCode("SOURCE_BOOKING_REFUNDED_AFTER_BENEFIT_USED");
        item.setCostAmount(new BigDecimal("30000.00"));
        item.setTestData(testData);
        item.setEnvironmentTag(testData ? "UAT" : "BUSINESS");
        item.setStatus(AnomalyCaseStatus.OPEN);
        item.setCreatedAt(Instant.parse("2026-08-23T12:00:00Z"));
        return item;
    }
}
