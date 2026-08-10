package com.project.userservice.entity;

import com.project.userservice.enumtype.AttendanceStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false, unique = true)
    private WorkShift shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;

    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "worked_minutes", nullable = false)
    private Integer workedMinutes = 0;

    @Column(name = "overtime_minutes", nullable = false)
    private Integer overtimeMinutes = 0;

    @Column(name = "source", nullable = false, length = 30)
    private String source = "SELF_SERVICE";

    @Column(name = "corrected_by")
    private Long correctedBy;

    @Column(name = "correction_reason", length = 500)
    private String correctionReason;

    @Version
    private Integer version = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public WorkShift getShift() { return shift; }
    public void setShift(WorkShift shift) { this.shift = shift; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime checkInAt) { this.checkInAt = checkInAt; }
    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime checkOutAt) { this.checkOutAt = checkOutAt; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public Integer getWorkedMinutes() { return workedMinutes; }
    public void setWorkedMinutes(Integer workedMinutes) { this.workedMinutes = workedMinutes; }
    public Integer getOvertimeMinutes() { return overtimeMinutes; }
    public void setOvertimeMinutes(Integer overtimeMinutes) { this.overtimeMinutes = overtimeMinutes; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getCorrectedBy() { return correctedBy; }
    public void setCorrectedBy(Long correctedBy) { this.correctedBy = correctedBy; }
    public String getCorrectionReason() { return correctionReason; }
    public void setCorrectionReason(String correctionReason) { this.correctionReason = correctionReason; }
    public Integer getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
