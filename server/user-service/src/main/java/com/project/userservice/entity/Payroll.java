package com.project.userservice.entity;

import com.project.userservice.enumtype.PayrollStatus;
import com.project.userservice.enumtype.PayrollSourceType;
import com.project.userservice.enumtype.ReconciliationStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payrolls", uniqueConstraints = @UniqueConstraint(
        name = "uk_payroll_employee_month", columnNames = {"employee_id", "salary_month"}))
public class Payroll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "salary_month", nullable = false)
    private LocalDate salaryMonth;

    @Column(name = "basic_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "allowance", nullable = false, precision = 15, scale = 2)
    private BigDecimal allowance = BigDecimal.ZERO;

    @Column(name = "bonus", nullable = false, precision = 15, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal deduction = BigDecimal.ZERO;

    @Column(name = "total_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_by")
    private Long paidBy;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "bank_batch_reference", length = 100)
    private String bankBatchReference;

    @Column(name = "accounting_reference", length = 100)
    private String accountingReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NOT_SUBMITTED;

    @Column(name = "reconciled_by")
    private Long reconciledBy;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "reconciliation_note", length = 500)
    private String reconciliationNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private PayrollSourceType sourceType = PayrollSourceType.MANUAL_EXCEPTION;

    @Column(name = "source_checksum", length = 64, columnDefinition = "CHAR(64)")
    private String sourceChecksum;

    @Column(name = "scheduled_minutes", nullable = false)
    private Integer scheduledMinutes = 0;

    @Column(name = "worked_minutes", nullable = false)
    private Integer workedMinutes = 0;

    @Column(name = "paid_leave_minutes", nullable = false)
    private Integer paidLeaveMinutes = 0;

    @Column(name = "overtime_minutes", nullable = false)
    private Integer overtimeMinutes = 0;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollDetail> details = new ArrayList<>();

    @Version
    private Integer version = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDate getSalaryMonth() { return salaryMonth; }
    public void setSalaryMonth(LocalDate salaryMonth) { this.salaryMonth = salaryMonth; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public BigDecimal getAllowance() { return allowance; }
    public void setAllowance(BigDecimal allowance) { this.allowance = allowance; }
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
    public BigDecimal getDeduction() { return deduction; }
    public void setDeduction(BigDecimal deduction) { this.deduction = deduction; }
    public BigDecimal getTotalSalary() { return totalSalary; }
    public void setTotalSalary(BigDecimal totalSalary) { this.totalSalary = totalSalary; }
    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus status) { this.status = status; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public Long getPaidBy() { return paidBy; }
    public void setPaidBy(Long paidBy) { this.paidBy = paidBy; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getBankBatchReference() { return bankBatchReference; }
    public void setBankBatchReference(String bankBatchReference) { this.bankBatchReference = bankBatchReference; }
    public String getAccountingReference() { return accountingReference; }
    public void setAccountingReference(String accountingReference) { this.accountingReference = accountingReference; }
    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public Long getReconciledBy() { return reconciledBy; }
    public void setReconciledBy(Long reconciledBy) { this.reconciledBy = reconciledBy; }
    public LocalDateTime getReconciledAt() { return reconciledAt; }
    public void setReconciledAt(LocalDateTime reconciledAt) { this.reconciledAt = reconciledAt; }
    public String getReconciliationNote() { return reconciliationNote; }
    public void setReconciliationNote(String reconciliationNote) { this.reconciliationNote = reconciliationNote; }
    public PayrollSourceType getSourceType() { return sourceType; }
    public void setSourceType(PayrollSourceType sourceType) { this.sourceType = sourceType; }
    public String getSourceChecksum() { return sourceChecksum; }
    public void setSourceChecksum(String sourceChecksum) { this.sourceChecksum = sourceChecksum; }
    public Integer getScheduledMinutes() { return scheduledMinutes; }
    public void setScheduledMinutes(Integer scheduledMinutes) { this.scheduledMinutes = scheduledMinutes; }
    public Integer getWorkedMinutes() { return workedMinutes; }
    public void setWorkedMinutes(Integer workedMinutes) { this.workedMinutes = workedMinutes; }
    public Integer getPaidLeaveMinutes() { return paidLeaveMinutes; }
    public void setPaidLeaveMinutes(Integer paidLeaveMinutes) { this.paidLeaveMinutes = paidLeaveMinutes; }
    public Integer getOvertimeMinutes() { return overtimeMinutes; }
    public void setOvertimeMinutes(Integer overtimeMinutes) { this.overtimeMinutes = overtimeMinutes; }
    public Long getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Long cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public List<PayrollDetail> getDetails() { return details; }
    public Integer getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
