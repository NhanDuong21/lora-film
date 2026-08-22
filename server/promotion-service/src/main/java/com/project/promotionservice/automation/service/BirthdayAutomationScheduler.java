package com.project.promotionservice.automation.service;

import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import com.project.promotionservice.automation.enums.AutomationRunStatus;
import com.project.promotionservice.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class BirthdayAutomationScheduler {
    private static final Logger log = LoggerFactory.getLogger(BirthdayAutomationScheduler.class);
    private final PromotionAutomationService service;

    public BirthdayAutomationScheduler(PromotionAutomationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${promotion.automation.birthday-cron:0 5 0 * * *}",
            zone = "Asia/Ho_Chi_Minh")
    public void runDaily() {
        try {
            PromotionAutomationRun run = service.createBirthdayRun(
                    LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")),
                    "SYSTEM", "SCHEDULE");
            if (run.getStatus() == AutomationRunStatus.AUDIENCE_READY) {
                service.createIssueJob(run.getPublicId(), 200);
            }
        } catch (BusinessException exception) {
            log.info("Birthday automation did not run: {}", exception.getMessage());
        }
    }
}
