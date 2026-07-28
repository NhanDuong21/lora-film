package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.ApprovalHistory;
import com.project.promotionservice.promotion.enums.ApprovalTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long>, JpaSpecificationExecutor<ApprovalHistory> {
    Optional<ApprovalHistory> findByPublicId(String publicId);
    List<ApprovalHistory> findByTargetTypeAndTargetPublicIdAndDeletedAtIsNullOrderByApprovedAtDesc(ApprovalTargetType targetType, String targetPublicId);
}
