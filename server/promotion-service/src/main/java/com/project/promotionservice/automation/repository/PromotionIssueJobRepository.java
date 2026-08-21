package com.project.promotionservice.automation.repository;

import com.project.promotionservice.automation.entity.PromotionIssueJob;
import com.project.promotionservice.automation.enums.IssueJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionIssueJobRepository extends JpaRepository<PromotionIssueJob, Long> {
    Optional<PromotionIssueJob> findByPublicId(String publicId);
    Optional<PromotionIssueJob> findFirstByStatusOrderByCreatedAtAsc(IssueJobStatus status);
    List<PromotionIssueJob> findByRunPublicIdOrderByCreatedAtDesc(String runPublicId);
    boolean existsByRunPublicId(String runPublicId);
    boolean existsByRunPublicIdAndStatusIn(String runPublicId, List<IssueJobStatus> statuses);
}
