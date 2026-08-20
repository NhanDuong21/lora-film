package com.project.promotionservice.automation.service;

import com.project.promotionservice.automation.entity.PromotionAudienceMember;
import com.project.promotionservice.automation.entity.PromotionPlaybook;
import com.project.promotionservice.automation.enums.PlaybookStatus;
import com.project.promotionservice.automation.repository.PromotionAudienceMemberRepository;
import com.project.promotionservice.automation.repository.PromotionPlaybookRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;

@Service
public class PromotionAutomationBudgetService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PromotionPlaybookRepository playbookRepository;
    private final PromotionAudienceMemberRepository memberRepository;

    public PromotionAutomationBudgetService(
            PromotionPlaybookRepository playbookRepository,
            PromotionAudienceMemberRepository memberRepository) {
        this.playbookRepository = playbookRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Locks the audience member first and then its playbook. This fixed lock
     * order makes retries idempotent and serializes concurrent workers against
     * the same monthly budget and quota counters.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationResult reserveForMember(
            String memberPublicId, String playbookPublicId, BigDecimal unitCost) {
        PromotionAudienceMember member = memberRepository
                .findByPublicIdForUpdate(memberPublicId).orElseThrow();
        if (positive(member.getBudgetReservedAmount())) {
            return ReservationResult.RESERVED;
        }
        PromotionPlaybook playbook = playbookRepository
                .findByPublicIdForUpdate(playbookPublicId).orElseThrow();
        if (playbook.getStatus() != PlaybookStatus.ACTIVE) {
            return ReservationResult.PLAYBOOK_INACTIVE;
        }

        String period = YearMonth.now(BUSINESS_ZONE).toString();
        if (!period.equals(playbook.getBudgetPeriodKey())) {
            playbook.setBudgetPeriodKey(period);
            playbook.setBudgetCommitted(BigDecimal.ZERO);
            playbook.setQuotaCommitted(0);
        }
        BigDecimal amount = money(unitCost);
        BigDecimal committed = money(playbook.getBudgetCommitted());
        int quotaCommitted = value(playbook.getQuotaCommitted());
        if (playbook.getQuotaLimit() != null
                && quotaCommitted >= playbook.getQuotaLimit()) {
            return ReservationResult.QUOTA_EXHAUSTED;
        }
        if (playbook.getBudgetLimit() != null
                && committed.add(amount).compareTo(playbook.getBudgetLimit()) > 0) {
            return ReservationResult.BUDGET_EXHAUSTED;
        }

        playbook.setBudgetCommitted(committed.add(amount));
        playbook.setQuotaCommitted(quotaCommitted + 1);
        playbook.setUpdatedBy("SYSTEM");
        member.setBudgetReservedAmount(amount);
        member.setBudgetPeriodKey(period);
        member.setUpdatedBy("SYSTEM");
        playbookRepository.save(playbook);
        memberRepository.save(member);
        return ReservationResult.RESERVED;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseForMember(String memberPublicId, String playbookPublicId) {
        PromotionAudienceMember member = memberRepository
                .findByPublicIdForUpdate(memberPublicId).orElse(null);
        if (member == null || !positive(member.getBudgetReservedAmount())) return;
        PromotionPlaybook playbook = playbookRepository
                .findByPublicIdForUpdate(playbookPublicId).orElse(null);
        if (playbook != null
                && member.getBudgetPeriodKey() != null
                && member.getBudgetPeriodKey().equals(playbook.getBudgetPeriodKey())) {
            playbook.setBudgetCommitted(money(playbook.getBudgetCommitted())
                    .subtract(money(member.getBudgetReservedAmount()))
                    .max(BigDecimal.ZERO));
            playbook.setQuotaCommitted(Math.max(0, value(playbook.getQuotaCommitted()) - 1));
            playbook.setUpdatedBy("SYSTEM");
            playbookRepository.save(playbook);
        }
        member.setBudgetReservedAmount(BigDecimal.ZERO);
        member.setBudgetPeriodKey(null);
        member.setUpdatedBy("SYSTEM");
        memberRepository.save(member);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    public enum ReservationResult {
        RESERVED, BUDGET_EXHAUSTED, QUOTA_EXHAUSTED, PLAYBOOK_INACTIVE
    }
}
