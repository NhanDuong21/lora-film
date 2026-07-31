package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.entity.BusinessInsight;
import com.project.analyticsservice.entity.Recommendation;
import com.project.analyticsservice.repository.BusinessInsightRepository;
import com.project.analyticsservice.repository.RecommendationRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.text.NumberFormat;
import java.util.Locale;

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
            RecommendationTemplate template = template(insight);
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

    private RecommendationTemplate template(BusinessInsight insight) {
        String category = insight.getCategory();
        String context = context(insight);
        String caution = confidenceCaution(insight);
        return switch (category) {
            case "REVENUE_DROP", "ANOMALY_NET_REVENUE" -> new RecommendationTemplate(
                    "business-operations", "REVIEW_REVENUE_DRIVERS",
                    "Khôi phục doanh thu tại khu vực bị ảnh hưởng",
                    context
                            + "Kiểm tra lần lượt phim, khung giờ, giá vé và mức giảm giá tại rạp "
                            + "bị ảnh hưởng. Nếu còn nhiều ghế trống, thử ưu đãi giới hạn trong "
                            + "khung giờ yếu; nếu lượng vé ổn nhưng doanh thu giảm, ưu tiên rà lại "
                            + "giá và chương trình giảm giá. Không giảm giá đồng loạt toàn hệ thống. "
                            + caution,
                    impactText(insight, "VND",
                            "Theo dõi khả năng thu hẹp phần doanh thu đang thiếu trong 7 ngày tới."),
                    "VND");
            case "HIGH_REFUND_RATE", "ANOMALY_REFUND_RATE" -> new RecommendationTemplate(
                    "payment-service", "INVESTIGATE_REFUNDS",
                    "Giảm tình trạng hoàn tiền tăng cao",
                    context
                            + "Tách các trường hợp do lỗi thanh toán, hủy suất chiếu và yêu cầu "
                            + "của khách hàng. Ưu tiên xử lý nguyên nhân có số tiền hoàn lớn nhất, "
                            + "sau đó theo dõi lại tỷ lệ hoàn tiền trong 3 ngày trước khi thay đổi "
                            + "chính sách chung. " + caution,
                    impactText(insight, "PERCENT",
                            "Đưa tỷ lệ hoàn tiền trở lại mức vận hành bình thường."),
                    "PERCENT");
            case "LOW_OCCUPANCY", "ANOMALY_OCCUPANCY_RATE" -> new RecommendationTemplate(
                    "movie-service", "REVIEW_SHOWTIME_PLAN",
                    "Tối ưu phòng chiếu và khung giờ ít khách",
                    context
                            + "Xem lại các suất có tỷ lệ lấp đầy thấp liên tiếp trong 7 ngày. "
                            + "Ưu tiên chuyển sang phòng nhỏ hơn hoặc gộp suất gần nhau; chỉ thử "
                            + "ưu đãi theo giờ khi phim vẫn còn nhu cầu. Giữ nguyên các khung giờ "
                            + "đang bán tốt để tránh làm giảm doanh thu. " + caution,
                    impactText(insight, "PERCENT",
                            "Tăng tỷ lệ sử dụng ghế mà không làm giảm các suất đang bán tốt."),
                    "PERCENT");
            case "ANOMALY_TICKET_COUNT" -> new RecommendationTemplate(
                    "booking-service", "VERIFY_DEMAND_SHIFT",
                    "Điều chỉnh nguồn lực theo nhu cầu vé",
                    context
                            + demandDirection(insight)
                            + " Kiểm tra phim, rạp và khung giờ tạo ra thay đổi trước khi tăng hoặc "
                            + "giảm số suất chiếu. So sánh thêm với cùng thứ của các tuần trước để "
                            + "tránh phản ứng theo một ngày bất thường. " + caution,
                    impactText(insight, "TICKETS",
                            "Điều chỉnh số suất chiếu sát hơn với nhu cầu thực tế."),
                    "TICKETS");
            default -> new RecommendationTemplate(
                    "analytics-service", "FIX_EVENT_SNAPSHOTS",
                    "Khôi phục độ tin cậy của báo cáo",
                    context
                            + "Đối chiếu số giao dịch giữa hệ thống đặt vé, thanh toán và báo cáo; "
                            + "kiểm tra các bản ghi còn thiếu tên rạp, sức chứa phòng chiếu hoặc "
                            + "thông tin khách hàng. Hoàn tất dữ liệu trước khi dùng dự báo để ra "
                            + "quyết định kinh doanh. " + caution,
                    impactText(insight, "PERCENT",
                            "Nâng mức đầy đủ dữ liệu để báo cáo và dự báo đáng tin cậy hơn."),
                    "PERCENT");
        };
    }

    private String context(BusinessInsight insight) {
        String rootCause = insight.getRootCause();
        if (rootCause == null || rootCause.isBlank()
                || rootCause.startsWith("Đang phân tích")) {
            return "";
        }
        return rootCause.trim() + " ";
    }

    private String confidenceCaution(BusinessInsight insight) {
        BigDecimal confidence = insight.getConfidenceScore();
        if (confidence != null && confidence.compareTo(new BigDecimal("0.65")) < 0) {
            return "Mức tin cậy hiện còn hạn chế, vì vậy cần xác minh dữ liệu trước khi thực hiện.";
        }
        return "Đánh giá kết quả sau khi thực hiện để tiếp tục, điều chỉnh hoặc dừng biện pháp.";
    }

    private String demandDirection(BusinessInsight insight) {
        BigDecimal actual = insight.getActualValue();
        BigDecimal expected = insight.getExpectedValue();
        if (actual == null || expected == null) {
            return "Nhu cầu vé đang thay đổi khác mức thường thấy.";
        }
        return actual.compareTo(expected) < 0
                ? "Nhu cầu vé đang thấp hơn mức thường thấy."
                : "Nhu cầu vé đang cao hơn mức thường thấy.";
    }

    private String impactText(
            BusinessInsight insight,
            String unit,
            String fallback) {
        BigDecimal difference = estimatedImpact(insight);
        if (difference == null) {
            return fallback;
        }
        return switch (unit) {
            case "VND" -> "Mục tiêu theo dõi: thu hẹp khoảng "
                    + formatNumber(difference.setScale(0, RoundingMode.HALF_UP))
                    + " đồng so với mức thường thấy.";
            case "PERCENT" -> "Mục tiêu theo dõi: cải thiện khoảng "
                    + difference.multiply(new BigDecimal("100"))
                            .setScale(1, RoundingMode.HALF_UP)
                    + " điểm phần trăm.";
            case "TICKETS" -> "Mức chênh lệch cần theo dõi là khoảng "
                    + formatNumber(difference.setScale(0, RoundingMode.HALF_UP))
                    + " vé.";
            default -> fallback;
        };
    }

    private String formatNumber(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(
                Locale.forLanguageTag("vi-VN"));
        return formatter.format(value);
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
