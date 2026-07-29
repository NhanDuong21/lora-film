package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.entity.BusinessInsight;
import com.project.analyticsservice.entity.Recommendation;
import com.project.analyticsservice.repository.BusinessInsightRepository;
import com.project.analyticsservice.repository.RecommendationRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@Order(80)
public class RecommendationKpiCalculator implements KpiCalculator {
    private final BusinessInsightRepository insightRepository;
    private final RecommendationRepository recommendationRepository;

    public RecommendationKpiCalculator(
            BusinessInsightRepository insightRepository,
            RecommendationRepository recommendationRepository) {
        this.insightRepository = insightRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Override
    public String stage() {
        return "RECOMMENDATION";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        for (BusinessInsight insight : insightRepository.findAllByStatDate(statDate)) {
            if (Boolean.TRUE.equals(insight.getResolved())) {
                continue;
            }
            RecommendationTemplate template = template(insight.getCategory());
            Recommendation recommendation = recommendationRepository
                    .findByInsightIdAndActionType(insight.getId(), template.actionType())
                    .orElseGet(Recommendation::new);
            if (recommendation.getId() != null
                    && !"PENDING".equals(recommendation.getStatus())) {
                continue;
            }
            recommendation.setInsightId(insight.getId());
            recommendation.setTargetService(template.targetService());
            recommendation.setActionType(template.actionType());
            recommendation.setPriority(priority(insight.getSeverity()));
            recommendation.setTitle(template.title());
            recommendation.setDescription(template.description());
            recommendation.setExpectedImpact(template.expectedImpact());
            recommendation.setEstimatedImpactValue(estimatedImpact(insight));
            recommendation.setImpactUnit(template.impactUnit());
            recommendation.setConfidenceScore(insight.getConfidenceScore());
            recommendation.setStatus("PENDING");
            recommendation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
            recommendationRepository.save(recommendation);
        }
    }

    private RecommendationTemplate template(String category) {
        return switch (category) {
            case "REVENUE_DROP", "ANOMALY_NET_REVENUE" -> new RecommendationTemplate(
                    "promotion-service", "REVIEW_REVENUE_DRIVERS",
                    "Rà soát doanh thu theo rạp và khung giờ",
                    "Ưu tiên kiểm tra các rạp đóng góp nhiều nhất vào mức giảm, sau đó thử ưu đãi "
                            + "có giới hạn ở khung giờ yếu thay vì giảm giá toàn hệ thống.",
                    "Khôi phục một phần chênh lệch doanh thu với tác động có kiểm soát.",
                    "VND");
            case "HIGH_REFUND_RATE", "ANOMALY_REFUND_RATE" -> new RecommendationTemplate(
                    "payment-service", "INVESTIGATE_REFUNDS",
                    "Điều tra cụm hoàn tiền bất thường",
                    "Đối chiếu rạp, booking và lỗi provider trong các root-cause factor trước khi "
                            + "thay đổi chính sách hoàn tiền.",
                    "Giảm thất thoát và sự cố vận hành liên quan hoàn tiền.",
                    "VND");
            case "LOW_OCCUPANCY", "ANOMALY_OCCUPANCY_RATE" -> new RecommendationTemplate(
                    "movie-service", "REVIEW_SHOWTIME_PLAN",
                    "Điều chỉnh kế hoạch suất chiếu",
                    "Rà soát phòng chiếu và khung giờ có công suất thấp; cân nhắc chuyển phòng, "
                            + "giảm suất yếu hoặc thử ưu đãi theo giờ.",
                    "Tăng hiệu suất sử dụng ghế và giảm chi phí cơ hội.",
                    "PERCENT");
            case "ANOMALY_TICKET_COUNT" -> new RecommendationTemplate(
                    "booking-service", "VERIFY_DEMAND_SHIFT",
                    "Xác minh biến động nhu cầu vé",
                    "Kiểm tra phim, rạp và khung giờ tạo ra biến động trước khi điều chỉnh năng lực.",
                    "Phản ứng sớm với thay đổi nhu cầu thực.",
                    "TICKETS");
            default -> new RecommendationTemplate(
                    "analytics-service", "FIX_EVENT_SNAPSHOTS",
                    "Bổ sung snapshot trong event",
                    "Yêu cầu producer cung cấp đầy đủ cinema, customer, capacity và membership "
                            + "snapshot theo event contract.",
                    "Tăng độ tin cậy của KPI, forecast và recommendation.",
                    "PERCENT");
        };
    }

    private BigDecimal estimatedImpact(BusinessInsight insight) {
        if (insight.getExpectedValue() == null || insight.getActualValue() == null) {
            return null;
        }
        return insight.getExpectedValue().subtract(insight.getActualValue()).abs();
    }

    private String priority(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "URGENT";
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private record RecommendationTemplate(
            String targetService,
            String actionType,
            String title,
            String description,
            String expectedImpact,
            String impactUnit) {
    }
}
