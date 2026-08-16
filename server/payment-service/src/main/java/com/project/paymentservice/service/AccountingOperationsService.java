package com.project.paymentservice.service;

import com.project.paymentservice.dto.request.*;
import com.project.paymentservice.dto.response.*;
import com.project.paymentservice.entity.*;
import com.project.paymentservice.enumtype.*;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.repository.*;
import com.project.paymentservice.security.AccountingScopeService;
import com.project.paymentservice.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountingOperationsService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<CashVerificationStatus> CASH_PENDING = List.of(
            CashVerificationStatus.PENDING_VERIFICATION,
            CashVerificationStatus.DISCREPANCY_REVIEW);
    private static final List<ReconciliationCaseStatus> CASES_OPEN = List.of(
            ReconciliationCaseStatus.OPEN, ReconciliationCaseStatus.IN_REVIEW);

    private final SettlementBatchRepository batchRepository;
    private final SettlementEntryRepository entryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAnalyticsSnapshotRepository snapshotRepository;
    private final PaymentReconciliationCaseRepository caseRepository;
    private final CounterCashSessionRepository cashRepository;
    private final AccountingPeriodRepository periodRepository;
    private final AccountingAuditEventRepository auditRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AccountingScopeService scopeService;

    public AccountingOperationsService(
            SettlementBatchRepository batchRepository,
            SettlementEntryRepository entryRepository,
            PaymentRepository paymentRepository,
            PaymentAnalyticsSnapshotRepository snapshotRepository,
            PaymentReconciliationCaseRepository caseRepository,
            CounterCashSessionRepository cashRepository,
            AccountingPeriodRepository periodRepository,
            AccountingAuditEventRepository auditRepository,
            CurrentUserProvider currentUserProvider,
            AccountingScopeService scopeService) {
        this.batchRepository = batchRepository;
        this.entryRepository = entryRepository;
        this.paymentRepository = paymentRepository;
        this.snapshotRepository = snapshotRepository;
        this.caseRepository = caseRepository;
        this.cashRepository = cashRepository;
        this.periodRepository = periodRepository;
        this.auditRepository = auditRepository;
        this.currentUserProvider = currentUserProvider;
        this.scopeService = scopeService;
    }

    @Transactional(readOnly = true)
    public AccountingOverviewResponse overview(String requestedCinema) {
        String cinema = scopeService.resolveCinema(requestedCinema);
        long settlementReview = cinema == null
                ? batchRepository.countByStatus(SettlementBatchStatus.NEEDS_REVIEW)
                : batchRepository.countByStatusAndCinemaPublicId(
                        SettlementBatchStatus.NEEDS_REVIEW, cinema);
        long cashPending = cinema == null
                ? cashRepository.countByStatusAndVerificationStatusIn(
                        CounterCashSessionStatus.CLOSED, CASH_PENDING)
                : cashRepository.countByStatusAndCinemaPublicIdAndVerificationStatusIn(
                        CounterCashSessionStatus.CLOSED, cinema, CASH_PENDING);
        BigDecimal variance = cinema == null
                ? cashRepository.sumAbsoluteVariance(CounterCashSessionStatus.CLOSED, CASH_PENDING)
                : cashRepository.sumAbsoluteVarianceForCinema(
                        CounterCashSessionStatus.CLOSED, cinema, CASH_PENDING);
        return new AccountingOverviewResponse(
                cinema,
                settlementReview,
                caseRepository.countByStatusIn(CASES_OPEN),
                cashPending,
                money(variance),
                cinema == null
                        ? periodRepository.countByStatus(AccountingPeriodStatus.OPEN)
                        : periodRepository.countByStatusAndScopeKey(AccountingPeriodStatus.OPEN, cinema));
    }

    @Transactional
    public SettlementBatchResponse importSettlement(SettlementBatchRequest request) {
        validateRange(request.periodStart(), request.periodEnd());
        String batchCode = normalizeRequired(request.batchCode(), 100);
        if (batchRepository.existsByProviderCodeAndBatchCode(request.providerCode(), batchCode)) {
            throw conflict("SETTLEMENT_BATCH_DUPLICATE",
                    "Mã lô đối soát này đã được nhập cho nhà cung cấp đã chọn.");
        }
        String cinema = scopeService.resolveCinema(request.cinemaPublicId());
        Long actorId = currentUserProvider.getCurrentUserId();
        SettlementBatch batch = new SettlementBatch();
        batch.setPublicId(UUID.randomUUID().toString());
        batch.setProviderCode(request.providerCode());
        batch.setBatchCode(batchCode);
        batch.setCinemaPublicId(cinema);
        batch.setPeriodStart(request.periodStart());
        batch.setPeriodEnd(request.periodEnd());
        batch.setSourceFileName(sanitize(request.sourceFileName(), 255));
        batch.setNoteSanitized(sanitize(request.note(), 1000));
        batch.setCreatedByAccountId(actorId);
        batch.setStatus(SettlementBatchStatus.IMPORTED);
        batch = batchRepository.saveAndFlush(batch);

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal providerNet = BigDecimal.ZERO;
        BigDecimal bankCredit = BigDecimal.ZERO;
        int matched = 0;
        int mismatch = 0;
        for (SettlementEntryRequest item : request.entries()) {
            SettlementEntry entry = matchEntry(batch, item, cinema);
            entryRepository.save(entry);
            gross = gross.add(entry.getProviderGrossAmount());
            fee = fee.add(entry.getProviderFeeAmount());
            providerNet = providerNet.add(entry.getProviderNetAmount());
            bankCredit = bankCredit.add(entry.getBankCreditAmount());
            if (entry.getStatus() == SettlementEntryStatus.MATCHED) matched++;
            else mismatch++;
        }
        batch.setEntryCount(request.entries().size());
        batch.setMatchedCount(matched);
        batch.setMismatchCount(mismatch);
        batch.setGrossAmount(money(gross));
        batch.setFeeAmount(money(fee));
        batch.setProviderNetAmount(money(providerNet));
        batch.setBankCreditAmount(money(bankCredit));
        batch.setStatus(mismatch == 0
                ? SettlementBatchStatus.RECONCILED
                : SettlementBatchStatus.NEEDS_REVIEW);
        batch = batchRepository.save(batch);
        audit("SETTLEMENT_IMPORTED", "SETTLEMENT_BATCH", batch.getPublicId(),
                "Đã nhập " + batch.getEntryCount() + " dòng; " + mismatch + " dòng cần kiểm tra.");
        return batchResponse(batch, true);
    }

    @Transactional(readOnly = true)
    public Page<SettlementBatchResponse> settlements(
            String requestedCinema, SettlementBatchStatus status, Pageable pageable) {
        String cinema = scopeService.resolveCinema(requestedCinema);
        Page<SettlementBatch> page;
        if (cinema == null) {
            page = status == null ? batchRepository.findAll(pageable)
                    : batchRepository.findByStatus(status, pageable);
        } else {
            page = status == null ? batchRepository.findByCinemaPublicId(cinema, pageable)
                    : batchRepository.findByCinemaPublicIdAndStatus(cinema, status, pageable);
        }
        return page.map(batch -> batchResponse(batch, false));
    }

    @Transactional(readOnly = true)
    public SettlementBatchResponse settlement(String publicId) {
        SettlementBatch batch = findBatch(publicId);
        scopeService.resolveCinema(batch.getCinemaPublicId());
        return batchResponse(batch, true);
    }

    @Transactional
    public SettlementBatchResponse lockSettlement(String publicId, Integer expectedVersion, String note) {
        SettlementBatch batch = findBatch(publicId);
        scopeService.resolveCinema(batch.getCinemaPublicId());
        assertVersion(batch.getVersion(), expectedVersion);
        if (batch.getStatus() == SettlementBatchStatus.LOCKED) return batchResponse(batch, true);
        if (batch.getStatus() != SettlementBatchStatus.RECONCILED || batch.getMismatchCount() > 0) {
            throw conflict("SETTLEMENT_NOT_RECONCILED",
                    "Chỉ được khóa lô khi mọi dòng đã khớp và không còn chênh lệch.");
        }
        Long actorId = currentUserProvider.getCurrentUserId();
        if (!scopeService.isAdmin() && actorId.equals(batch.getCreatedByAccountId())) {
            throw conflict("SETTLEMENT_MAKER_CHECKER",
                    "Người nhập lô không được tự khóa chính lô đó. Hãy chuyển người kiểm soát độc lập.");
        }
        batch.setStatus(SettlementBatchStatus.LOCKED);
        batch.setLockedByAccountId(actorId);
        batch.setLockedAt(Instant.now());
        batch.setNoteSanitized(sanitize(note, 1000));
        batch = batchRepository.save(batch);
        audit("SETTLEMENT_LOCKED", "SETTLEMENT_BATCH", batch.getPublicId(), note);
        return batchResponse(batch, true);
    }

    @Transactional(readOnly = true)
    public Page<CashControlResponse> cashSessions(
            String requestedCinema, CashVerificationStatus verificationStatus, Pageable pageable) {
        String cinema = scopeService.resolveCinema(requestedCinema);
        Page<CounterCashSession> page;
        if (cinema == null) {
            page = verificationStatus == null
                    ? cashRepository.findByStatus(CounterCashSessionStatus.CLOSED, pageable)
                    : cashRepository.findByStatusAndVerificationStatus(
                            CounterCashSessionStatus.CLOSED, verificationStatus, pageable);
        } else {
            page = verificationStatus == null
                    ? cashRepository.findByStatusAndCinemaPublicId(
                            CounterCashSessionStatus.CLOSED, cinema, pageable)
                    : cashRepository.findByStatusAndCinemaPublicIdAndVerificationStatus(
                            CounterCashSessionStatus.CLOSED, cinema, verificationStatus, pageable);
        }
        return page.map(this::cashResponse);
    }

    @Transactional
    public CashControlResponse verifyCashSession(
            String publicId, CashSessionVerificationRequest request) {
        CounterCashSession session = cashRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound("CASH_SESSION_NOT_FOUND",
                        "Không tìm thấy ca tiền mặt cần xác minh."));
        scopeService.resolveCinema(session.getCinemaPublicId());
        if (session.getStatus() != CounterCashSessionStatus.CLOSED) {
            throw conflict("CASH_SESSION_NOT_SUBMITTED",
                    "Nhân viên chưa chốt và gửi ca tiền mặt này cho kế toán.");
        }
        if (session.getVerificationStatus() == CashVerificationStatus.VERIFIED) {
            return cashResponse(session);
        }
        Long actorId = currentUserProvider.getCurrentUserId();
        if (!scopeService.isAdmin() && actorId.equals(session.getEmployeeAccountId())) {
            throw conflict("CASH_SESSION_MAKER_CHECKER",
                    "Người thu và đếm tiền không được tự xác minh ca của mình.");
        }
        session.setVerificationStatus(CashVerificationStatus.VERIFIED);
        session.setVerifiedByAccountId(actorId);
        session.setVerifiedAt(Instant.now());
        session.setVerificationNoteSanitized(sanitize(request.note(), 1000));
        session = cashRepository.save(session);
        audit("CASH_SESSION_VERIFIED", "COUNTER_CASH_SESSION", session.getPublicId(),
                "Chênh lệch " + money(session.getVarianceAmount()) + ". " + request.note());
        return cashResponse(session);
    }

    @Transactional(readOnly = true)
    public Page<AccountingPeriodResponse> periods(String requestedCinema, Pageable pageable) {
        String cinema = scopeService.resolveCinema(requestedCinema);
        String scopeKey = cinema == null ? "ALL" : cinema;
        return periodRepository.findByScopeKey(scopeKey, pageable).map(this::periodResponse);
    }

    @Transactional
    public AccountingPeriodResponse createPeriod(AccountingPeriodRequest request) {
        validateRange(request.periodStart(), request.periodEnd());
        String cinema = scopeService.resolveCinema(request.cinemaPublicId());
        String scopeKey = cinema == null ? "ALL" : cinema;
        if (periodRepository.existsByPeriodCodeAndScopeKey(request.periodCode(), scopeKey)) {
            throw conflict("ACCOUNTING_PERIOD_DUPLICATE",
                    "Kỳ kế toán này đã tồn tại trong phạm vi đang chọn.");
        }
        AccountingPeriod period = new AccountingPeriod();
        period.setPublicId(UUID.randomUUID().toString());
        period.setPeriodCode(request.periodCode());
        period.setScopeKey(scopeKey);
        period.setCinemaPublicId(cinema);
        period.setPeriodStart(request.periodStart());
        period.setPeriodEnd(request.periodEnd());
        period.setCreatedByAccountId(currentUserProvider.getCurrentUserId());
        period.setNoteSanitized(sanitize(request.note(), 1000));
        period.setStatus(AccountingPeriodStatus.OPEN);
        period = periodRepository.save(period);
        audit("ACCOUNTING_PERIOD_CREATED", "ACCOUNTING_PERIOD", period.getPublicId(), request.note());
        return periodResponse(period);
    }

    @Transactional
    public AccountingPeriodResponse applyPeriodAction(
            String publicId, AccountingPeriodActionRequest request) {
        AccountingPeriod period = periodRepository.findByPublicId(publicId)
                .orElseThrow(() -> notFound("ACCOUNTING_PERIOD_NOT_FOUND",
                        "Không tìm thấy kỳ kế toán."));
        scopeService.resolveCinema(period.getCinemaPublicId());
        assertVersion(period.getVersion(), request.expectedVersion());
        Long actorId = currentUserProvider.getCurrentUserId();
        List<String> blockers = blockers(period);
        switch (request.action()) {
            case RECONCILE -> {
                requireAuthority("ACCOUNTING_PERIOD_RECONCILE",
                        "Bạn không có quyền xác nhận kỳ đã đối soát.");
                if (period.getStatus() != AccountingPeriodStatus.OPEN) {
                    throw conflict("ACCOUNTING_PERIOD_INVALID_STATE",
                            "Chỉ kỳ đang mở mới có thể đánh dấu đã đối soát.");
                }
                if (!blockers.isEmpty()) throw blockers(blockers);
                period.setStatus(AccountingPeriodStatus.RECONCILED);
                period.setReconciledByAccountId(actorId);
                period.setReconciledAt(Instant.now());
            }
            case LOCK -> {
                requireAuthority("ACCOUNTING_PERIOD_CLOSE",
                        "Bạn không có quyền khóa kỳ kế toán.");
                if (period.getStatus() != AccountingPeriodStatus.RECONCILED) {
                    throw conflict("ACCOUNTING_PERIOD_NOT_RECONCILED",
                            "Kỳ phải hoàn tất đối soát trước khi khóa.");
                }
                if (!blockers.isEmpty()) throw blockers(blockers);
                if (!scopeService.isAdmin() && actorId.equals(period.getCreatedByAccountId())) {
                    throw conflict("ACCOUNTING_PERIOD_MAKER_CHECKER",
                            "Người mở kỳ không được tự khóa kỳ. Hãy chuyển người kiểm soát độc lập.");
                }
                period.setStatus(AccountingPeriodStatus.LOCKED);
                period.setLockedByAccountId(actorId);
                period.setLockedAt(Instant.now());
            }
            case REOPEN -> {
                if (!scopeService.isAdmin()) {
                    throw new BusinessException("ACCOUNTING_PERIOD_REOPEN_DENIED",
                            "Chỉ quản trị viên được mở lại kỳ đã khóa.", HttpStatus.FORBIDDEN);
                }
                if (period.getStatus() != AccountingPeriodStatus.LOCKED) {
                    throw conflict("ACCOUNTING_PERIOD_INVALID_STATE", "Chỉ kỳ đã khóa mới có thể mở lại.");
                }
                period.setStatus(AccountingPeriodStatus.ADJUSTMENT);
            }
        }
        period.setNoteSanitized(sanitize(request.note(), 1000));
        period = periodRepository.save(period);
        audit("ACCOUNTING_PERIOD_" + request.action().name(),
                "ACCOUNTING_PERIOD", period.getPublicId(), request.note());
        return periodResponse(period);
    }

    @Transactional(readOnly = true)
    public Page<AccountingAuditEvent> auditEvents(String aggregateType, Pageable pageable) {
        return aggregateType == null || aggregateType.isBlank()
                ? auditRepository.findAll(pageable)
                : auditRepository.findByAggregateType(aggregateType.trim().toUpperCase(Locale.ROOT), pageable);
    }

    private SettlementEntry matchEntry(
            SettlementBatch batch, SettlementEntryRequest request, String cinema) {
        SettlementEntry entry = new SettlementEntry();
        entry.setBatch(batch);
        entry.setPaymentTransactionCode(normalizeRequired(request.paymentTransactionCode(), 100));
        entry.setProviderTransactionId(normalizeRequired(request.providerTransactionId(), 150));
        entry.setBankCreditReference(sanitize(request.bankCreditReference(), 150));
        entry.setProviderGrossAmount(money(request.providerGrossAmount()));
        entry.setProviderFeeAmount(money(request.providerFeeAmount()));
        entry.setProviderNetAmount(money(request.providerNetAmount()));
        entry.setBankCreditAmount(money(request.bankCreditAmount()));

        Payment payment = paymentRepository.findByPaymentTransactionCode(entry.getPaymentTransactionCode())
                .orElse(null);
        List<String> reasons = new ArrayList<>();
        if (payment == null) {
            entry.setStatus(SettlementEntryStatus.UNMATCHED);
            entry.setMismatchReasonSanitized("Không tìm thấy giao dịch LoraFilm theo mã đã nhập.");
            return entry;
        }
        entry.setPaymentId(payment.getId());
        if (payment.getProviderCode() != batch.getProviderCode()) {
            reasons.add("Nhà cung cấp không khớp với giao dịch LoraFilm");
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            reasons.add("Giao dịch LoraFilm chưa ở trạng thái thanh toán thành công");
        }
        if (money(payment.getAmount()).compareTo(entry.getProviderGrossAmount()) != 0) {
            reasons.add("Số tiền provider không khớp số phải thu trên LoraFilm");
        }
        if (entry.getProviderGrossAmount().subtract(entry.getProviderFeeAmount())
                .setScale(2, RoundingMode.HALF_UP).compareTo(entry.getProviderNetAmount()) != 0) {
            reasons.add("Tiền thực nhận của provider không bằng tiền gộp trừ phí");
        }
        if (entry.getProviderNetAmount().compareTo(entry.getBankCreditAmount()) != 0) {
            reasons.add("Tiền ngân hàng ghi có không khớp tiền provider thông báo");
        }
        if (cinema != null) {
            String paymentCinema = snapshotRepository.findByPaymentId(payment.getId())
                    .map(PaymentAnalyticsSnapshot::getCinemaPublicId).orElse(null);
            if (paymentCinema == null || !cinema.equals(paymentCinema.toLowerCase(Locale.ROOT))) {
                reasons.add("Giao dịch không thuộc rạp của lô đối soát");
            }
        }
        if (reasons.isEmpty()) {
            entry.setStatus(SettlementEntryStatus.MATCHED);
        } else {
            String reason = String.join("; ", reasons);
            entry.setStatus(SettlementEntryStatus.MISMATCH);
            entry.setMismatchReasonSanitized(reason);
            payment.setReconciliationStatus(ReconciliationStatus.REQUIRED);
            payment.setReconciliationReason(reason);
            paymentRepository.save(payment);
            createSettlementCase(payment, batch, entry, reason);
        }
        return entry;
    }

    private void createSettlementCase(
            Payment payment, SettlementBatch batch, SettlementEntry entry, String reason) {
        String sourceReference = sanitize(batch.getBatchCode() + ":" + entry.getProviderTransactionId(), 150);
        PaymentReconciliationCase item = caseRepository
                .findByPaymentIdAndReasonCodeAndSourceReference(
                        payment.getId(), "SETTLEMENT_MISMATCH", sourceReference)
                .orElseGet(PaymentReconciliationCase::new);
        if (item.getId() == null) {
            item.setPublicId(UUID.randomUUID().toString());
            item.setPaymentId(payment.getId());
            item.setReasonCode("SETTLEMENT_MISMATCH");
            item.setSourceReference(sourceReference);
            item.setStatus(ReconciliationCaseStatus.OPEN);
        }
        item.setDetailSanitized(sanitize(reason, 2000));
        caseRepository.save(item);
    }

    private SettlementBatchResponse batchResponse(SettlementBatch batch, boolean includeEntries) {
        List<SettlementEntryResponse> entries = includeEntries
                ? entryRepository.findByBatchIdOrderByIdAsc(batch.getId()).stream()
                    .map(value -> {
                        Payment payment = value.getPaymentId() == null ? null
                                : paymentRepository.findById(value.getPaymentId()).orElse(null);
                        return new SettlementEntryResponse(
                                value.getId(), value.getPaymentId(), value.getPaymentTransactionCode(),
                                payment == null ? null : payment.getAmount(),
                                payment == null ? null : payment.getStatus().name(),
                                value.getProviderTransactionId(), value.getBankCreditReference(),
                                value.getProviderGrossAmount(), value.getProviderFeeAmount(),
                                value.getProviderNetAmount(), value.getBankCreditAmount(),
                                value.getStatus().name(), value.getMismatchReasonSanitized());
                    })
                    .toList()
                : List.of();
        Long actor = currentUserProvider.getCurrentUserId();
        boolean makerBlocked = !scopeService.isAdmin() && actor.equals(batch.getCreatedByAccountId());
        boolean canLock = batch.getStatus() == SettlementBatchStatus.RECONCILED
                && batch.getMismatchCount() == 0 && !makerBlocked;
        String blocked = null;
        if (batch.getStatus() == SettlementBatchStatus.LOCKED) blocked = "Lô đã được khóa.";
        else if (batch.getMismatchCount() > 0) blocked = "Còn dòng chênh lệch cần xử lý.";
        else if (makerBlocked) blocked = "Người nhập lô không được tự khóa lô.";
        else if (batch.getStatus() != SettlementBatchStatus.RECONCILED) blocked = "Lô chưa hoàn tất đối soát.";
        return new SettlementBatchResponse(
                batch.getPublicId(), batch.getProviderCode().name(), batch.getBatchCode(),
                batch.getCinemaPublicId(), batch.getPeriodStart(), batch.getPeriodEnd(),
                batch.getSourceFileName(), batch.getStatus().name(), batch.getEntryCount(),
                batch.getMatchedCount(), batch.getMismatchCount(), batch.getGrossAmount(),
                batch.getFeeAmount(), batch.getProviderNetAmount(), batch.getBankCreditAmount(),
                batch.getCreatedByAccountId(), batch.getLockedByAccountId(), batch.getLockedAt(),
                batch.getNoteSanitized(), batch.getVersion(), batch.getCreatedAt(), entries,
                canLock, blocked);
    }

    private CashControlResponse cashResponse(CounterCashSession session) {
        Long actor = currentUserProvider.getCurrentUserId();
        boolean self = !scopeService.isAdmin() && actor.equals(session.getEmployeeAccountId());
        boolean canVerify = session.getStatus() == CounterCashSessionStatus.CLOSED
                && session.getVerificationStatus() != CashVerificationStatus.VERIFIED && !self;
        String blocked = null;
        if (session.getStatus() != CounterCashSessionStatus.CLOSED) blocked = "Nhân viên chưa chốt ca.";
        else if (session.getVerificationStatus() == CashVerificationStatus.VERIFIED) blocked = "Ca đã được xác minh.";
        else if (self) blocked = "Người thu tiền không được tự xác minh ca của mình.";
        return new CashControlResponse(
                session.getPublicId(), session.getEmployeeAccountId(), session.getCinemaPublicId(),
                session.getStatus().name(), session.getVerificationStatus().name(),
                session.getOpeningFloat(), session.getCashSales(), session.getCashTransactionCount(),
                session.getCashRefunds(), session.getCashRefundCount(), session.getExpectedCash(),
                session.getCountedCash(), session.getVarianceAmount(), session.getClosingNoteSanitized(),
                session.getVerifiedByAccountId(), session.getVerifiedAt(),
                session.getVerificationNoteSanitized(), session.getOpenedAt(), session.getClosedAt(),
                session.getVersion(), canVerify, blocked);
    }

    private AccountingPeriodResponse periodResponse(AccountingPeriod period) {
        List<String> blockers = blockers(period);
        Long actor = currentUserProvider.getCurrentUserId();
        boolean makerBlocked = !scopeService.isAdmin() && actor.equals(period.getCreatedByAccountId());
        return new AccountingPeriodResponse(
                period.getPublicId(), period.getPeriodCode(), period.getCinemaPublicId(),
                period.getPeriodStart(), period.getPeriodEnd(), period.getStatus().name(),
                period.getCreatedByAccountId(), period.getReconciledByAccountId(), period.getReconciledAt(),
                period.getLockedByAccountId(), period.getLockedAt(), period.getNoteSanitized(),
                period.getVersion(), period.getCreatedAt(), blockers,
                period.getStatus() == AccountingPeriodStatus.OPEN && blockers.isEmpty(),
                period.getStatus() == AccountingPeriodStatus.RECONCILED
                        && blockers.isEmpty() && !makerBlocked);
    }

    private List<String> blockers(AccountingPeriod period) {
        String cinema = period.getCinemaPublicId();
        Instant periodStart = period.getPeriodStart().atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant periodEndExclusive = period.getPeriodEnd().plusDays(1)
                .atStartOfDay(BUSINESS_ZONE).toInstant();
        List<String> values = new ArrayList<>();
        long settlement = cinema == null
                ? batchRepository.countByStatusAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                        SettlementBatchStatus.NEEDS_REVIEW,
                        period.getPeriodEnd(), period.getPeriodStart())
                : batchRepository.countByStatusAndCinemaPublicIdAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
                        SettlementBatchStatus.NEEDS_REVIEW, cinema,
                        period.getPeriodEnd(), period.getPeriodStart());
        long cash = cinema == null
                ? cashRepository.countByStatusAndVerificationStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(
                        CounterCashSessionStatus.CLOSED, CASH_PENDING,
                        periodStart, periodEndExclusive)
                : cashRepository.countByStatusAndCinemaPublicIdAndVerificationStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(
                        CounterCashSessionStatus.CLOSED, cinema, CASH_PENDING,
                        periodStart, periodEndExclusive);
        long cases = cinema == null
                ? caseRepository.countOpenInPeriod(CASES_OPEN, periodStart, periodEndExclusive)
                : caseRepository.countOpenInPeriodForCinema(
                        CASES_OPEN, cinema, periodStart, periodEndExclusive);
        if (settlement > 0) values.add(settlement + " lô settlement còn chênh lệch");
        if (cash > 0) values.add(cash + " ca tiền mặt chưa xác minh");
        if (cases > 0) values.add(cases + " hồ sơ đối soát chưa đóng");
        return values;
    }

    private void audit(String action, String aggregate, String publicId, String detail) {
        AccountingAuditEvent event = new AccountingAuditEvent();
        event.setActionCode(action);
        event.setAggregateType(aggregate);
        event.setAggregatePublicId(publicId);
        event.setActorAccountId(currentUserProvider.getCurrentUserId());
        event.setDetailSanitized(sanitize(detail, 2000));
        auditRepository.save(event);
    }

    private SettlementBatch findBatch(String publicId) {
        return batchRepository.findByPublicId(publicId)
                .orElseThrow(() -> notFound("SETTLEMENT_BATCH_NOT_FOUND", "Không tìm thấy lô đối soát."));
    }

    private void validateRange(java.time.LocalDate start, java.time.LocalDate end) {
        if (start.isAfter(end)) {
            throw new BusinessException("ACCOUNTING_DATE_RANGE_INVALID",
                    "Ngày bắt đầu không được sau ngày kết thúc.", HttpStatus.BAD_REQUEST);
        }
    }

    private void assertVersion(Integer actual, Integer expected) {
        if (expected == null || !expected.equals(actual)) {
            throw conflict("ACCOUNTING_VERSION_CONFLICT",
                    "Dữ liệu đã thay đổi. Vui lòng tải lại trước khi tiếp tục.");
        }
    }

    private void requireAuthority(String authority, String message) {
        if (!scopeService.isAdmin() && !scopeService.hasAuthority(authority)) {
            throw new BusinessException("ACCOUNTING_ACTION_DENIED", message, HttpStatus.FORBIDDEN);
        }
    }

    private BusinessException blockers(List<String> blockers) {
        return conflict("ACCOUNTING_PERIOD_HAS_OPEN_ITEMS",
                "Chưa thể chốt kỳ: " + String.join("; ", blockers) + ".");
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }

    private BusinessException notFound(String code, String message) {
        return new BusinessException(code, message, HttpStatus.NOT_FOUND);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, int max) {
        return sanitize(value, max).toUpperCase(Locale.ROOT);
    }

    private String sanitize(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }
}
