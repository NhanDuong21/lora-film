package com.project.paymentservice.service;

import com.project.paymentservice.dto.request.CloseCounterCashSessionRequest;
import com.project.paymentservice.dto.request.OpenCounterCashSessionRequest;
import com.project.paymentservice.dto.response.CounterCashSessionResponse;
import com.project.paymentservice.entity.CounterCashSession;
import com.project.paymentservice.enumtype.CounterCashSessionStatus;
import com.project.paymentservice.enumtype.CashVerificationStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.repository.CashPaymentDetailRepository;
import com.project.paymentservice.repository.CounterCashSessionRepository;
import com.project.paymentservice.repository.PaymentRefundRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CounterCashSessionService {
    private final CounterCashSessionRepository sessionRepository;
    private final CashPaymentDetailRepository cashRepository;
    private final PaymentRefundRepository refundRepository;

    public CounterCashSessionService(
            CounterCashSessionRepository sessionRepository,
            CashPaymentDetailRepository cashRepository,
            PaymentRefundRepository refundRepository) {
        this.sessionRepository = sessionRepository;
        this.cashRepository = cashRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional(readOnly = true)
    public CounterCashSessionResponse current(Long employeeAccountId, String cinemaPublicId) {
        return sessionRepository
                .findFirstByEmployeeAccountIdAndStatusOrderByOpenedAtDesc(
                        employeeAccountId, CounterCashSessionStatus.OPEN)
                .map(session -> {
                    requireCinema(session, cinemaPublicId);
                    return response(session, Instant.now());
                })
                .orElse(null);
    }

    @Transactional
    public CounterCashSessionResponse open(
            Long employeeAccountId,
            String cinemaPublicId,
            OpenCounterCashSessionRequest request) {
        if (sessionRepository.findFirstByEmployeeAccountIdAndStatusOrderByOpenedAtDesc(
                employeeAccountId, CounterCashSessionStatus.OPEN).isPresent()) {
            throw new BusinessException(
                    "COUNTER_CASH_SESSION_ALREADY_OPEN",
                    "Bạn đang có một ca thu ngân chưa chốt. Vui lòng chốt ca hiện tại trước khi mở ca mới.",
                    HttpStatus.CONFLICT);
        }
        CounterCashSession session = new CounterCashSession();
        session.setPublicId(UUID.randomUUID().toString());
        session.setEmployeeAccountId(employeeAccountId);
        session.setCinemaPublicId(normalizeCinema(cinemaPublicId));
        session.setOpeningFloat(money(request.openingFloat()));
        session.setOpeningNoteSanitized(sanitize(request.note(), 500));
        session.setOpenedAt(Instant.now());
        session.setStatus(CounterCashSessionStatus.OPEN);
        return response(sessionRepository.save(session), Instant.now());
    }

    @Transactional
    public CounterCashSessionResponse close(
            Long employeeAccountId,
            String cinemaPublicId,
            String sessionPublicId,
            CloseCounterCashSessionRequest request) {
        CounterCashSession session = sessionRepository.findByPublicIdForUpdate(sessionPublicId)
                .orElseThrow(() -> new BusinessException(
                        "COUNTER_CASH_SESSION_NOT_FOUND",
                        "Không tìm thấy ca thu ngân cần chốt.",
                        HttpStatus.NOT_FOUND));
        if (!employeeAccountId.equals(session.getEmployeeAccountId())) {
            throw new BusinessException(
                    "COUNTER_CASH_SESSION_ACCESS_DENIED",
                    "Bạn chỉ được chốt ca thu ngân của chính mình.",
                    HttpStatus.FORBIDDEN);
        }
        requireCinema(session, cinemaPublicId);
        if (session.getStatus() != CounterCashSessionStatus.OPEN) {
            return response(session, session.getClosedAt());
        }
        Instant closedAt = Instant.now();
        CashSnapshot snapshot = snapshot(session, closedAt);
        BigDecimal countedCash = money(request.countedCash());
        session.setCashSales(snapshot.sales());
        session.setCashTransactionCount(snapshot.saleCount());
        session.setCashRefunds(snapshot.refunds());
        session.setCashRefundCount(snapshot.refundCount());
        session.setExpectedCash(snapshot.expected());
        session.setCountedCash(countedCash);
        session.setVarianceAmount(countedCash.subtract(snapshot.expected()).setScale(2, RoundingMode.HALF_UP));
        session.setVerificationStatus(session.getVarianceAmount().compareTo(BigDecimal.ZERO) == 0
                ? CashVerificationStatus.PENDING_VERIFICATION
                : CashVerificationStatus.DISCREPANCY_REVIEW);
        session.setClosingNoteSanitized(sanitize(request.note(), 1000));
        session.setClosedAt(closedAt);
        session.setStatus(CounterCashSessionStatus.CLOSED);
        return response(sessionRepository.save(session), closedAt);
    }

    @Transactional(readOnly = true)
    public List<CounterCashSessionResponse> history(Long employeeAccountId, String cinemaPublicId) {
        return sessionRepository.findTop10ByEmployeeAccountIdOrderByOpenedAtDesc(employeeAccountId)
                .stream()
                .filter(session -> normalizeCinema(cinemaPublicId).equals(session.getCinemaPublicId()))
                .map(session -> response(session,
                        session.getClosedAt() == null ? Instant.now() : session.getClosedAt()))
                .toList();
    }

    private CounterCashSessionResponse response(CounterCashSession session, Instant until) {
        CashSnapshot snapshot = session.getStatus() == CounterCashSessionStatus.OPEN
                ? snapshot(session, until)
                : new CashSnapshot(
                        money(session.getCashSales()), value(session.getCashTransactionCount()),
                        money(session.getCashRefunds()), value(session.getCashRefundCount()),
                        money(session.getExpectedCash()));
        return new CounterCashSessionResponse(
                session.getPublicId(), session.getStatus().name(), session.getEmployeeAccountId(),
                session.getCinemaPublicId(), money(session.getOpeningFloat()),
                snapshot.sales(), snapshot.saleCount(), snapshot.refunds(), snapshot.refundCount(),
                snapshot.expected(), session.getCountedCash() == null ? null : money(session.getCountedCash()),
                session.getVarianceAmount() == null ? null : money(session.getVarianceAmount()),
                session.getOpeningNoteSanitized(), session.getClosingNoteSanitized(),
                session.getOpenedAt(), session.getClosedAt());
    }

    private CashSnapshot snapshot(CounterCashSession session, Instant until) {
        BigDecimal sales = money(cashRepository.sumSuccessfulCashCollected(
                session.getEmployeeAccountId(), session.getOpenedAt(), until));
        long saleCount = cashRepository.countSuccessfulCashCollected(
                session.getEmployeeAccountId(), session.getOpenedAt(), until);
        BigDecimal refunds = money(refundRepository.sumSuccessfulCashRefunds(
                session.getEmployeeAccountId(), session.getOpenedAt(), until));
        long refundCount = refundRepository.countSuccessfulCashRefunds(
                session.getEmployeeAccountId(), session.getOpenedAt(), until);
        BigDecimal expected = money(session.getOpeningFloat()).add(sales).subtract(refunds)
                .setScale(2, RoundingMode.HALF_UP);
        return new CashSnapshot(sales, saleCount, refunds, refundCount, expected);
    }

    private void requireCinema(CounterCashSession session, String cinemaPublicId) {
        if (!normalizeCinema(cinemaPublicId).equals(session.getCinemaPublicId())) {
            throw new BusinessException(
                    "COUNTER_CASH_SESSION_CINEMA_DENIED",
                    "Ca thu ngân không thuộc rạp bạn đang được phân công.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private String normalizeCinema(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private String sanitize(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }

    private record CashSnapshot(
            BigDecimal sales,
            long saleCount,
            BigDecimal refunds,
            long refundCount,
            BigDecimal expected) {
    }
}
