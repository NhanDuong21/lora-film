package com.project.userservice.entity;

import com.project.userservice.enumtype.EmploymentActionType;
import com.project.userservice.enumtype.EmployeeStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employment_actions", indexes = {
        @Index(name = "idx_employment_actions_employee_created", columnList = "employee_account_id,created_at")
})
public class EmploymentAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_account_id", nullable = false)
    private Long employeeAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private EmploymentActionType actionType;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private EmployeeStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private EmployeeStatus newStatus;

    @Column(name = "previous_department_id")
    private Long previousDepartmentId;

    @Column(name = "new_department_id")
    private Long newDepartmentId;

    @Column(name = "previous_position_id")
    private Long previousPositionId;

    @Column(name = "new_position_id")
    private Long newPositionId;

    @Column(name = "previous_base_salary", precision = 15, scale = 2)
    private BigDecimal previousBaseSalary;

    @Column(name = "new_base_salary", precision = 15, scale = 2)
    private BigDecimal newBaseSalary;

    @Column(name = "performed_by")
    private Long performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getEmployeeAccountId() { return employeeAccountId; }
    public void setEmployeeAccountId(Long employeeAccountId) { this.employeeAccountId = employeeAccountId; }
    public EmploymentActionType getActionType() { return actionType; }
    public void setActionType(EmploymentActionType actionType) { this.actionType = actionType; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public EmployeeStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(EmployeeStatus previousStatus) { this.previousStatus = previousStatus; }
    public EmployeeStatus getNewStatus() { return newStatus; }
    public void setNewStatus(EmployeeStatus newStatus) { this.newStatus = newStatus; }
    public Long getPreviousDepartmentId() { return previousDepartmentId; }
    public void setPreviousDepartmentId(Long previousDepartmentId) { this.previousDepartmentId = previousDepartmentId; }
    public Long getNewDepartmentId() { return newDepartmentId; }
    public void setNewDepartmentId(Long newDepartmentId) { this.newDepartmentId = newDepartmentId; }
    public Long getPreviousPositionId() { return previousPositionId; }
    public void setPreviousPositionId(Long previousPositionId) { this.previousPositionId = previousPositionId; }
    public Long getNewPositionId() { return newPositionId; }
    public void setNewPositionId(Long newPositionId) { this.newPositionId = newPositionId; }
    public BigDecimal getPreviousBaseSalary() { return previousBaseSalary; }
    public void setPreviousBaseSalary(BigDecimal previousBaseSalary) { this.previousBaseSalary = previousBaseSalary; }
    public BigDecimal getNewBaseSalary() { return newBaseSalary; }
    public void setNewBaseSalary(BigDecimal newBaseSalary) { this.newBaseSalary = newBaseSalary; }
    public Long getPerformedBy() { return performedBy; }
    public void setPerformedBy(Long performedBy) { this.performedBy = performedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
