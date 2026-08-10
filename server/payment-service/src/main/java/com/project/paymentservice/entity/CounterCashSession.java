package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.CounterCashSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "counter_cash_sessions")
public class CounterCashSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private String publicId;

    @Column(name = "employee_account_id", nullable = false)
    private Long employeeAccountId;

    @Column(name = "cinema_public_id", nullable = false, columnDefinition = "char(36)")
    private String cinemaPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CounterCashSessionStatus status = CounterCashSessionStatus.OPEN;

    @Column(name = "opening_float", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingFloat = BigDecimal.ZERO;

    @Column(name = "cash_sales", precision = 12, scale = 2)
    private BigDecimal cashSales;

    @Column(name = "cash_transaction_count")
    private Long cashTransactionCount;

    @Column(name = "cash_refunds", precision = 12, scale = 2)
    private BigDecimal cashRefunds;

    @Column(name = "cash_refund_count")
    private Long cashRefundCount;

    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "counted_cash", precision = 12, scale = 2)
    private BigDecimal countedCash;

    @Column(name = "variance_amount", precision = 12, scale = 2)
    private BigDecimal varianceAmount;

    @Column(name = "opening_note_sanitized", length = 500)
    private String openingNoteSanitized;

    @Column(name = "closing_note_sanitized", length = 1000)
    private String closingNoteSanitized;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public Long getEmployeeAccountId() { return employeeAccountId; }
    public void setEmployeeAccountId(Long employeeAccountId) { this.employeeAccountId = employeeAccountId; }
    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String cinemaPublicId) { this.cinemaPublicId = cinemaPublicId; }
    public CounterCashSessionStatus getStatus() { return status; }
    public void setStatus(CounterCashSessionStatus status) { this.status = status; }
    public BigDecimal getOpeningFloat() { return openingFloat; }
    public void setOpeningFloat(BigDecimal openingFloat) { this.openingFloat = openingFloat; }
    public BigDecimal getCashSales() { return cashSales; }
    public void setCashSales(BigDecimal cashSales) { this.cashSales = cashSales; }
    public Long getCashTransactionCount() { return cashTransactionCount; }
    public void setCashTransactionCount(Long cashTransactionCount) { this.cashTransactionCount = cashTransactionCount; }
    public BigDecimal getCashRefunds() { return cashRefunds; }
    public void setCashRefunds(BigDecimal cashRefunds) { this.cashRefunds = cashRefunds; }
    public Long getCashRefundCount() { return cashRefundCount; }
    public void setCashRefundCount(Long cashRefundCount) { this.cashRefundCount = cashRefundCount; }
    public BigDecimal getExpectedCash() { return expectedCash; }
    public void setExpectedCash(BigDecimal expectedCash) { this.expectedCash = expectedCash; }
    public BigDecimal getCountedCash() { return countedCash; }
    public void setCountedCash(BigDecimal countedCash) { this.countedCash = countedCash; }
    public BigDecimal getVarianceAmount() { return varianceAmount; }
    public void setVarianceAmount(BigDecimal varianceAmount) { this.varianceAmount = varianceAmount; }
    public String getOpeningNoteSanitized() { return openingNoteSanitized; }
    public void setOpeningNoteSanitized(String openingNoteSanitized) { this.openingNoteSanitized = openingNoteSanitized; }
    public String getClosingNoteSanitized() { return closingNoteSanitized; }
    public void setClosingNoteSanitized(String closingNoteSanitized) { this.closingNoteSanitized = closingNoteSanitized; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
