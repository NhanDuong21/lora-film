package com.lorafilm.booking.booking.entity;

import com.lorafilm.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ticket_gate_handoffs")
public class TicketGateHandoff extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @Column(name = "employee_account_id", nullable = false)
    private Long employeeAccountId;

    @Column(name = "cinema_public_id", length = 36, nullable = false)
    private String cinemaPublicId;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "gate_label", length = 80)
    private String gateLabel;

    @Column(name = "total_scans", nullable = false)
    private int totalScans;

    @Column(name = "successful_scans", nullable = false)
    private int successfulScans;

    @Column(name = "rejected_scans", nullable = false)
    private int rejectedScans;

    @Column(name = "unresolved_incidents", nullable = false)
    private int unresolvedIncidents;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "handed_off_at", nullable = false)
    private Instant handedOffAt;

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public Long getEmployeeAccountId() { return employeeAccountId; }
    public void setEmployeeAccountId(Long employeeAccountId) { this.employeeAccountId = employeeAccountId; }
    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String cinemaPublicId) { this.cinemaPublicId = cinemaPublicId; }
    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }
    public String getGateLabel() { return gateLabel; }
    public void setGateLabel(String gateLabel) { this.gateLabel = gateLabel; }
    public int getTotalScans() { return totalScans; }
    public void setTotalScans(int totalScans) { this.totalScans = totalScans; }
    public int getSuccessfulScans() { return successfulScans; }
    public void setSuccessfulScans(int successfulScans) { this.successfulScans = successfulScans; }
    public int getRejectedScans() { return rejectedScans; }
    public void setRejectedScans(int rejectedScans) { this.rejectedScans = rejectedScans; }
    public int getUnresolvedIncidents() { return unresolvedIncidents; }
    public void setUnresolvedIncidents(int unresolvedIncidents) { this.unresolvedIncidents = unresolvedIncidents; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getHandedOffAt() { return handedOffAt; }
    public void setHandedOffAt(Instant handedOffAt) { this.handedOffAt = handedOffAt; }
}
