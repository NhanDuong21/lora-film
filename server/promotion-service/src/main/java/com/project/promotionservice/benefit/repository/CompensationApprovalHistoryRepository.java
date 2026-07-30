package com.project.promotionservice.benefit.repository;

import com.project.promotionservice.benefit.entity.CompensationApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompensationApprovalHistoryRepository
        extends JpaRepository<CompensationApprovalHistory, Long> {

    List<CompensationApprovalHistory> findByTargetTypeAndTargetPublicIdAndDeletedAtIsNullOrderByApprovedAtAsc(
            String targetType, String targetPublicId);
}
