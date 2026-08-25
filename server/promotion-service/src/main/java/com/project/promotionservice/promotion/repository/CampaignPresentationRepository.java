package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.CampaignPresentation;
import com.project.promotionservice.promotion.enums.CampaignPresentationStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface CampaignPresentationRepository extends JpaRepository<CampaignPresentation, Long> {

    Optional<CampaignPresentation> findByCampaignPublicIdAndDeletedAtIsNull(String campaignPublicId);

    @Query("""
            select cp from CampaignPresentation cp, PromotionCampaign c
            where cp.campaignPublicId = c.publicId
              and cp.status = :presentationStatus
              and cp.showOnHome = true
              and c.status = :campaignStatus
              and c.legalStatus = :legalStatus
              and c.testData = false
              and c.killSwitch = false
              and c.startAt <= :now
              and c.endAt > :now
              and cp.deletedAt is null
              and c.deletedAt is null
            order by cp.featured desc, cp.displayOrder asc, cp.createdAt asc
            """)
    Page<CampaignPresentation> findPublicHome(
            @Param("presentationStatus") CampaignPresentationStatus presentationStatus,
            @Param("campaignStatus") CampaignStatus campaignStatus,
            @Param("legalStatus") LegalStatus legalStatus,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            select cp from CampaignPresentation cp, PromotionCampaign c
            where cp.campaignPublicId = c.publicId
              and cp.status = :presentationStatus
              and cp.showInPromotionCenter = true
              and c.status = :campaignStatus
              and c.legalStatus = :legalStatus
              and c.testData = false
              and c.killSwitch = false
              and c.startAt <= :now
              and c.endAt > :now
              and cp.deletedAt is null
              and c.deletedAt is null
            order by cp.featured desc, cp.displayOrder asc, cp.createdAt asc
            """)
    Page<CampaignPresentation> findPublicPromotionCenter(
            @Param("presentationStatus") CampaignPresentationStatus presentationStatus,
            @Param("campaignStatus") CampaignStatus campaignStatus,
            @Param("legalStatus") LegalStatus legalStatus,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            select cp from CampaignPresentation cp, PromotionCampaign c
            where cp.campaignPublicId = c.publicId
              and cp.status = :presentationStatus
              and cp.showInWallet = true
              and c.status = :campaignStatus
              and c.legalStatus = :legalStatus
              and c.testData = false
              and c.killSwitch = false
              and c.startAt <= :now
              and c.endAt > :now
              and cp.deletedAt is null
              and c.deletedAt is null
            order by cp.featured desc, cp.displayOrder asc, cp.createdAt asc
            """)
    Page<CampaignPresentation> findPublicWallet(
            @Param("presentationStatus") CampaignPresentationStatus presentationStatus,
            @Param("campaignStatus") CampaignStatus campaignStatus,
            @Param("legalStatus") LegalStatus legalStatus,
            @Param("now") Instant now,
            Pageable pageable);
}
