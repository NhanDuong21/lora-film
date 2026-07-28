package com.project.paymentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.dto.request.ReconciliationAssignRequest;
import com.project.paymentservice.dto.request.ReconciliationResolveRequest;
import com.project.paymentservice.dto.response.AdminPaymentDetailResponse;
import com.project.paymentservice.dto.response.PaymentDetailResponse;
import com.project.paymentservice.entity.CashPaymentDetail;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import com.project.paymentservice.entity.PaymentLog;
import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.entity.PaymentReconciliationCase;
import com.project.paymentservice.entity.PaymentWebhookEvent;
import com.project.paymentservice.enumtype.OutboxStatus;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.ReconciliationCaseStatus;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import com.project.paymentservice.enumtype.WebhookProcessingStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.mapper.PaymentMapper;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.repository.CashPaymentDetailRepository;
import com.project.paymentservice.repository.PaymentAnalyticsSnapshotRepository;
import com.project.paymentservice.repository.PaymentLogRepository;
import com.project.paymentservice.repository.PaymentOutboxEventRepository;
import com.project.paymentservice.repository.PaymentReconciliationCaseRepository;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.repository.PaymentWebhookEventRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminPaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentAnalyticsSnapshotRepository snapshotRepository;
    private final CashPaymentDetailRepository cashRepository;
    private final PaymentLogRepository logRepository;
    private final PaymentWebhookEventRepository webhookRepository;
    private final PaymentOutboxEventRepository outboxRepository;
    private final PaymentReconciliationCaseRepository reconciliationRepository;
    private final PaymentOutboxService outboxService;
    private final OutboxDeliveryStateService outboxStateService;
    private final PaymentTransactionService transactionService;
    private final ObjectMapper objectMapper;

    public AdminPaymentService(
            PaymentRepository paymentRepository,
            PaymentAnalyticsSnapshotRepository snapshotRepository,
            CashPaymentDetailRepository cashRepository,
            PaymentLogRepository logRepository,
            PaymentWebhookEventRepository webhookRepository,
            PaymentOutboxEventRepository outboxRepository,
            PaymentReconciliationCaseRepository reconciliationRepository,
            PaymentOutboxService outboxService,
            OutboxDeliveryStateService outboxStateService,
            PaymentTransactionService transactionService,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.snapshotRepository = snapshotRepository;
        this.cashRepository = cashRepository;
        this.logRepository = logRepository;
        this.webhookRepository = webhookRepository;
        this.outboxRepository = outboxRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.outboxService = outboxService;
        this.outboxStateService = outboxStateService;
        this.transactionService = transactionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<PaymentDetailResponse> search(
            String query, PaymentStatus status, ProviderCode provider,
            ReconciliationStatus reconciliationStatus,
            Instant from, Instant to, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(specification(
                query, status, provider, reconciliationStatus, from, to), pageable);
        Map<Long, PaymentAnalyticsSnapshot> snapshots = snapshotRepository.findAllById(
                        payments.getContent().stream().map(Payment::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        PaymentAnalyticsSnapshot::getPaymentId,
                        Function.identity()));
        return payments.map(payment -> detail(payment, snapshots.get(payment.getId())));
    }

    @Transactional(readOnly = true)
    public AdminPaymentDetailResponse detail(String paymentPublicId) {
        Payment payment = paymentRepository.findByPublicId(paymentPublicId)
                .orElseThrow(() -> notFound(paymentPublicId));
        PaymentAnalyticsSnapshot snapshot = snapshotRepository
                .findByPaymentId(payment.getId()).orElse(null);
        CashPaymentDetail cash = cashRepository.findById(payment.getId()).orElse(null);
        return new AdminPaymentDetailResponse(
                detail(payment, snapshot),
                snapshot(snapshot),
                cash(cash),
                logRepository.findByPaymentIdOrderByCreatedAtAsc(payment.getId())
                        .stream().map(this::log).toList(),
                webhookRepository.findByPaymentIdOrderByReceivedAtDesc(payment.getId())
                        .stream().map(this::webhook).toList(),
                outboxRepository.findByAggregateIdOrderByCreatedAtDesc(payment.getPublicId())
                        .stream().map(this::outbox).toList(),
                reconciliationRepository.findByPaymentIdOrderByCreatedAtDesc(payment.getId())
                        .stream().map(this::reconciliation).toList());
    }

    @Transactional(readOnly = true)
    public String exportCsv(
            String query, PaymentStatus status, ProviderCode provider,
            ReconciliationStatus reconciliationStatus, Instant from, Instant to) {
        List<Payment> payments = paymentRepository.findAll(
                specification(query, status, provider, reconciliationStatus, from, to),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        StringBuilder csv = new StringBuilder(
                "\uFEFFMã giao dịch,Payment UUID,Booking UUID,Provider,Trạng thái,Số tiền,Tiền tệ,Đối soát,Thời điểm tạo\n");
        for (Payment payment : payments) {
            csv.append(cell(payment.getPaymentTransactionCode())).append(',')
                    .append(cell(payment.getPublicId())).append(',')
                    .append(cell(payment.getBookingPublicId())).append(',')
                    .append(payment.getProviderCode()).append(',')
                    .append(payment.getStatus()).append(',')
                    .append(payment.getAmount()).append(',')
                    .append(payment.getCurrency()).append(',')
                    .append(payment.getReconciliationStatus()).append(',')
                    .append(payment.getCreatedAt()).append('\n');
        }
        return csv.toString();
    }

    public Page<PaymentWebhookEvent> webhooks(
            WebhookProcessingStatus status, Pageable pageable) {
        return status == null ? webhookRepository.findAll(pageable)
                : webhookRepository.findByProcessingStatus(status, pageable);
    }

    public Page<PaymentOutboxEvent> outbox(OutboxStatus status, Pageable pageable) {
        return status == null ? outboxRepository.findAll(pageable)
                : outboxRepository.findByStatus(status, pageable);
    }

    public Page<PaymentReconciliationCase> reconciliations(
            ReconciliationCaseStatus status, Pageable pageable) {
        return status == null ? reconciliationRepository.findAll(pageable)
                : reconciliationRepository.findByStatus(status, pageable);
    }

    public void replayOutbox(String eventId) {
        outboxStateService.replay(eventId);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void replayWebhook(Long webhookId) {
        PaymentWebhookEvent event = webhookRepository.findAndLockById(webhookId)
                .orElseThrow(() -> new BusinessException(
                        "WEBHOOK_NOT_FOUND", "Không tìm thấy webhook", HttpStatus.NOT_FOUND));
        if (!Boolean.TRUE.equals(event.getSignatureValid())) {
            throw new BusinessException("WEBHOOK_SIGNATURE_INVALID",
                    "Webhook sai chữ ký không được phép replay", HttpStatus.CONFLICT);
        }
        if (event.getPaymentId() == null) {
            throw new BusinessException("PAYMENT_ORDER_NOT_FOUND",
                    "Webhook chưa liên kết được giao dịch", HttpStatus.CONFLICT);
        }
        Payment payment = paymentRepository.findById(event.getPaymentId())
                .orElseThrow(() -> notFound(String.valueOf(event.getPaymentId())));
        ProviderCallbackResult result = replayResult(event, payment);
        transactionService.applyProviderResult(event.getProviderCode(), result, event.getId());
        event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
        event.setProcessedAt(Instant.now());
        event.setLastErrorSanitized(null);
        webhookRepository.save(event);
    }

    @Transactional
    public PaymentReconciliationCase assign(
            String publicId, ReconciliationAssignRequest request) {
        PaymentReconciliationCase item = reconciliationRepository.findByPublicId(publicId)
                .orElseThrow(() -> reconciliationNotFound(publicId));
        if (item.getStatus() == ReconciliationCaseStatus.RESOLVED
                || item.getStatus() == ReconciliationCaseStatus.IGNORED) {
            throw new BusinessException("RECONCILIATION_ALREADY_CLOSED",
                    "Hồ sơ đối soát đã đóng", HttpStatus.CONFLICT);
        }
        item.setAssignedToAccountId(request.getAssigneeAccountId());
        item.setStatus(ReconciliationCaseStatus.IN_REVIEW);
        Payment payment = paymentRepository.findById(item.getPaymentId())
                .orElseThrow(() -> notFound(String.valueOf(item.getPaymentId())));
        payment.setReconciliationStatus(ReconciliationStatus.IN_REVIEW);
        paymentRepository.save(payment);
        return reconciliationRepository.save(item);
    }

    @Transactional
    public PaymentReconciliationCase resolve(
            String publicId, Long adminAccountId, ReconciliationResolveRequest request) {
        PaymentReconciliationCase item = reconciliationRepository.findByPublicId(publicId)
                .orElseThrow(() -> reconciliationNotFound(publicId));
        if (item.getStatus() == ReconciliationCaseStatus.RESOLVED
                || item.getStatus() == ReconciliationCaseStatus.IGNORED) {
            return item;
        }
        Instant now = Instant.now();
        item.setStatus(request.isIgnored()
                ? ReconciliationCaseStatus.IGNORED : ReconciliationCaseStatus.RESOLVED);
        item.setResolutionCode(normalize(request.getResolutionCode(), 100));
        item.setResolutionNoteSanitized(normalize(request.getNote(), 2000));
        item.setResolvedByAccountId(adminAccountId);
        item.setResolvedAt(now);
        Payment payment = paymentRepository.findById(item.getPaymentId())
                .orElseThrow(() -> notFound(String.valueOf(item.getPaymentId())));
        payment.setReconciliationStatus(ReconciliationStatus.RESOLVED);
        payment.setReconciliationResolutionCode(item.getResolutionCode());
        payment.setReconciliationNoteSanitized(item.getResolutionNoteSanitized());
        payment.setReconciliationResolvedByAccountId(adminAccountId);
        payment.setReconciliationResolvedAt(now);
        paymentRepository.save(payment);
        return reconciliationRepository.save(item);
    }

    private Specification<Payment> specification(
            String query, PaymentStatus status, ProviderCode provider,
            ReconciliationStatus reconciliationStatus, Instant from, Instant to) {
        return (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("paymentTransactionCode")), pattern),
                        builder.like(builder.lower(root.get("publicId")), pattern),
                        builder.like(builder.lower(root.get("bookingPublicId")), pattern),
                        builder.like(builder.lower(root.get("externalTransactionId")), pattern)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (provider != null) {
                predicates.add(builder.equal(root.get("providerCode"), provider));
            }
            if (reconciliationStatus != null) {
                predicates.add(builder.equal(root.get("reconciliationStatus"), reconciliationStatus));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), to));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private ProviderCallbackResult replayResult(PaymentWebhookEvent event, Payment payment) {
        try {
            JsonNode json = objectMapper.readTree(event.getSanitizedPayload());
            String responseCode = event.getProviderCode() == ProviderCode.VNPAY
                    ? json.path("vnp_ResponseCode").asText()
                    : json.path("resultCode").asText();
            String result = ("00".equals(responseCode) || "0".equals(responseCode))
                    ? "SUCCESS"
                    : (("24".equals(responseCode) || "1006".equals(responseCode))
                    ? "CANCELLED" : "FAILED");
            BigDecimal amount = event.getProviderCode() == ProviderCode.VNPAY
                    ? new BigDecimal(json.path("vnp_Amount").asText("0")).movePointLeft(2)
                    : new BigDecimal(json.path("amount").asText("0"));
            ProviderCallbackResult replay = new ProviderCallbackResult();
            replay.setSignatureValid(true);
            replay.setDeduplicationKey(event.getDeduplicationKey());
            replay.setProviderOrderId(event.getProviderOrderId());
            replay.setExternalTransactionId(event.getExternalTransactionId());
            replay.setResult(result);
            replay.setResponseCode(responseCode);
            replay.setAmount(amount);
            replay.setCurrency(payment.getCurrency());
            replay.setOccurredAt(event.getReceivedAt() == null ? Instant.now() : event.getReceivedAt());
            replay.setSanitizedPayload(event.getSanitizedPayload());
            return replay;
        } catch (Exception exception) {
            throw new BusinessException("WEBHOOK_PAYLOAD_INVALID",
                    "Không thể đọc payload webhook đã lưu", HttpStatus.CONFLICT);
        }
    }

    private PaymentDetailResponse detail(
            Payment payment,
            PaymentAnalyticsSnapshot snapshot) {
        PaymentDetailResponse response = PaymentMapper.toDetailResponse(payment);
        response.setBookingDeliveryStatus(outboxService.deliveryStatus(payment.getPublicId()));
        if (snapshot != null) {
            response.setMovieTitle(snapshot.getMovieTitle());
            response.setTicketCount(snapshot.getTicketCount());
            response.setTicketAmount(snapshot.getTicketAmount());
            response.setFoodAmount(snapshot.getFoodAmount());
            response.setDiscountAmount(snapshot.getDiscountAmount());
        }
        return response;
    }

    private Map<String, Object> snapshot(PaymentAnalyticsSnapshot value) {
        if (value == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("movieId", value.getMovieId());
        map.put("moviePublicId", value.getMoviePublicId());
        map.put("movieTitle", value.getMovieTitle());
        map.put("showtimePublicId", value.getShowtimePublicId());
        map.put("cinemaPublicId", value.getCinemaPublicId());
        map.put("ticketCount", value.getTicketCount());
        map.put("ticketAmount", value.getTicketAmount());
        map.put("foodAmount", value.getFoodAmount());
        map.put("discountAmount", value.getDiscountAmount());
        map.put("totalAmount", value.getTotalAmount());
        map.put("currency", value.getCurrency());
        return map;
    }

    private Map<String, Object> cash(CashPaymentDetail value) {
        if (value == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("receivedAmount", value.getReceivedAmount());
        map.put("changeAmount", value.getChangeAmount());
        map.put("collectedByAccountId", value.getCollectedByAccountId());
        map.put("collectedAt", value.getCollectedAt());
        map.put("note", value.getNoteSanitized());
        return map;
    }

    private Map<String, Object> log(PaymentLog value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventType", value.getEventType());
        map.put("source", value.getSource());
        map.put("actorType", value.getActorType());
        map.put("actorAccountId", value.getActorAccountId());
        map.put("previousStatus", value.getPreviousStatus());
        map.put("currentStatus", value.getCurrentStatus());
        map.put("message", value.getMessageSanitized());
        map.put("createdAt", value.getCreatedAt());
        return map;
    }

    private Map<String, Object> webhook(PaymentWebhookEvent value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", value.getId());
        map.put("provider", value.getProviderCode());
        map.put("signatureValid", value.getSignatureValid());
        map.put("processingStatus", value.getProcessingStatus());
        map.put("externalTransactionId", value.getExternalTransactionId());
        map.put("receivedAt", value.getReceivedAt());
        map.put("lastError", value.getLastErrorSanitized());
        return map;
    }

    private Map<String, Object> outbox(PaymentOutboxEvent value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", value.getEventId());
        map.put("eventType", value.getEventType());
        map.put("destination", value.getDestination());
        map.put("status", value.getStatus());
        map.put("attemptCount", value.getAttemptCount());
        map.put("lastError", value.getLastErrorSanitized());
        map.put("createdAt", value.getCreatedAt());
        map.put("publishedAt", value.getPublishedAt());
        return map;
    }

    private Map<String, Object> reconciliation(PaymentReconciliationCase value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("publicId", value.getPublicId());
        map.put("reasonCode", value.getReasonCode());
        map.put("status", value.getStatus());
        map.put("assignedToAccountId", value.getAssignedToAccountId());
        map.put("resolutionCode", value.getResolutionCode());
        map.put("resolutionNote", value.getResolutionNoteSanitized());
        map.put("openedAt", value.getOpenedAt());
        map.put("resolvedAt", value.getResolvedAt());
        return map;
    }

    private String cell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String normalize(String value, int max) {
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private BusinessException notFound(String id) {
        return new BusinessException("PAYMENT_NOT_FOUND",
                "Không tìm thấy giao dịch: " + id, HttpStatus.NOT_FOUND);
    }

    private BusinessException reconciliationNotFound(String id) {
        return new BusinessException("RECONCILIATION_NOT_FOUND",
                "Không tìm thấy hồ sơ đối soát: " + id, HttpStatus.NOT_FOUND);
    }
}
