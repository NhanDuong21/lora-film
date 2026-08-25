package com.project.promotionservice.promotion.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.promotion.enums.CampaignPresentationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "promotion_campaign_presentations")
public class CampaignPresentation extends BaseAuditableEntity {

    @Column(name = "campaign_public_id", length = 36, nullable = false, unique = true)
    private String campaignPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private CampaignPresentationStatus status = CampaignPresentationStatus.DRAFT;

    @Column(name = "headline", length = 180, nullable = false)
    private String headline;

    @Column(name = "summary", length = 500, nullable = false)
    private String summary;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(name = "cover_image_storage_key", length = 255)
    private String coverImageStorageKey;

    @Column(name = "cover_image_storage_provider", length = 30)
    private String coverImageStorageProvider;

    @Column(name = "cover_image_content_type", length = 100)
    private String coverImageContentType;

    @Column(name = "cover_image_bytes")
    private Long coverImageBytes;

    @Column(name = "image_alt_text", length = 240)
    private String imageAltText;

    @Column(name = "featured", nullable = false)
    private Boolean featured = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 100;

    @Column(name = "show_on_home", nullable = false)
    private Boolean showOnHome = false;

    @Column(name = "show_in_promotion_center", nullable = false)
    private Boolean showInPromotionCenter = false;

    @Column(name = "show_in_wallet", nullable = false)
    private Boolean showInWallet = false;

    @Column(name = "primary_promotion_public_id", length = 36)
    private String primaryPromotionPublicId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by", length = 36)
    private String publishedBy;

    public String getCampaignPublicId() { return campaignPublicId; }
    public void setCampaignPublicId(String value) { campaignPublicId = value; }
    public CampaignPresentationStatus getStatus() { return status; }
    public void setStatus(CampaignPresentationStatus value) { status = value; }
    public String getHeadline() { return headline; }
    public void setHeadline(String value) { headline = value; }
    public String getSummary() { return summary; }
    public void setSummary(String value) { summary = value; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String value) { coverImageUrl = value; }
    public String getCoverImageStorageKey() { return coverImageStorageKey; }
    public void setCoverImageStorageKey(String value) { coverImageStorageKey = value; }
    public String getCoverImageStorageProvider() { return coverImageStorageProvider; }
    public void setCoverImageStorageProvider(String value) { coverImageStorageProvider = value; }
    public String getCoverImageContentType() { return coverImageContentType; }
    public void setCoverImageContentType(String value) { coverImageContentType = value; }
    public Long getCoverImageBytes() { return coverImageBytes; }
    public void setCoverImageBytes(Long value) { coverImageBytes = value; }
    public String getImageAltText() { return imageAltText; }
    public void setImageAltText(String value) { imageAltText = value; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean value) { featured = value; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer value) { displayOrder = value; }
    public Boolean getShowOnHome() { return showOnHome; }
    public void setShowOnHome(Boolean value) { showOnHome = value; }
    public Boolean getShowInPromotionCenter() { return showInPromotionCenter; }
    public void setShowInPromotionCenter(Boolean value) { showInPromotionCenter = value; }
    public Boolean getShowInWallet() { return showInWallet; }
    public void setShowInWallet(Boolean value) { showInWallet = value; }
    public String getPrimaryPromotionPublicId() { return primaryPromotionPublicId; }
    public void setPrimaryPromotionPublicId(String value) { primaryPromotionPublicId = value; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant value) { publishedAt = value; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String value) { publishedBy = value; }
}
