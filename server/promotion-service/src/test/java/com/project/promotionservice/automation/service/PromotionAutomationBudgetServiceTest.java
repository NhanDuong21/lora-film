package com.project.promotionservice.automation.service;

import com.project.promotionservice.automation.entity.PromotionAudienceMember;
import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import com.project.promotionservice.automation.entity.PromotionPlaybook;
import com.project.promotionservice.automation.repository.PromotionAudienceMemberRepository;
import com.project.promotionservice.automation.repository.PromotionAutomationRunRepository;
import com.project.promotionservice.automation.repository.PromotionPlaybookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionAutomationBudgetServiceTest {

    @Mock private PromotionPlaybookRepository playbookRepository;
    @Mock private PromotionAudienceMemberRepository memberRepository;
    @Mock private PromotionAutomationRunRepository runRepository;

    private PromotionAutomationBudgetService service;

    @BeforeEach
    void setUp() {
        service = new PromotionAutomationBudgetService(
                playbookRepository, memberRepository, runRepository);
    }

    @Test
    void expiredOrRevokedWalletReleasesLiabilityAndQuotaExactlyOnce() {
        PromotionAutomationRun run = new PromotionAutomationRun();
        run.setPublicId("run-1");
        run.setPlaybookPublicId("playbook-1");

        PromotionAudienceMember member = new PromotionAudienceMember();
        member.setPublicId("member-1");
        member.setBudgetReservedAmount(new BigDecimal("50000.00"));
        member.setBudgetPeriodKey("2026-08");

        PromotionPlaybook playbook = new PromotionPlaybook();
        playbook.setPublicId("playbook-1");
        playbook.setBudgetPeriodKey("2026-08");
        playbook.setBudgetCommitted(new BigDecimal("100000.00"));
        playbook.setQuotaCommitted(2);

        when(runRepository.findByPublicId("run-1")).thenReturn(Optional.of(run));
        when(memberRepository.findByPublicIdForUpdate("member-1"))
                .thenReturn(Optional.of(member));
        when(playbookRepository.findByPublicIdForUpdate("playbook-1"))
                .thenReturn(Optional.of(playbook));

        service.releaseForWallet("member-1", "run-1");
        service.releaseForWallet("member-1", "run-1");

        assertThat(playbook.getBudgetCommitted()).isEqualByComparingTo("50000.00");
        assertThat(playbook.getQuotaCommitted()).isEqualTo(1);
        assertThat(member.getBudgetReservedAmount()).isEqualByComparingTo("0.00");
        assertThat(member.getBudgetPeriodKey()).isNull();
        verify(playbookRepository, times(1)).save(playbook);
        verify(memberRepository, times(1)).save(member);
    }
}
