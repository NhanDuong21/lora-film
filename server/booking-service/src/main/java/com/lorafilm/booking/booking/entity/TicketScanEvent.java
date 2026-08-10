package com.lorafilm.booking.booking.entity;

import com.lorafilm.booking.booking.enums.TicketScanResult;
import com.lorafilm.booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ticket_scan_events")
public class TicketScanEvent extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false, unique = true)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private BookingTicket ticket;

    @Column(name = "entered_code", length = 255, nullable = false)
    private String enteredCode;

    @Column(name = "employee_account_id", nullable = false)
    private Long employeeAccountId;

    @Column(name = "cinema_public_id", length = 36, nullable = false)
    private String cinemaPublicId;

    @Column(name = "gate_label", length = 80)
    private String gateLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 30, nullable = false)
    private TicketScanResult result;

    @Column(name = "reason_code", length = 50, nullable = false)
    private String reasonCode;

    @Column(name = "reason_message", length = 500, nullable = false)
    private String reasonMessage;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public BookingTicket getTicket() { return ticket; }
    public void setTicket(BookingTicket ticket) { this.ticket = ticket; }
    public String getEnteredCode() { return enteredCode; }
    public void setEnteredCode(String enteredCode) { this.enteredCode = enteredCode; }
    public Long getEmployeeAccountId() { return employeeAccountId; }
    public void setEmployeeAccountId(Long employeeAccountId) { this.employeeAccountId = employeeAccountId; }
    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String cinemaPublicId) { this.cinemaPublicId = cinemaPublicId; }
    public String getGateLabel() { return gateLabel; }
    public void setGateLabel(String gateLabel) { this.gateLabel = gateLabel; }
    public TicketScanResult getResult() { return result; }
    public void setResult(TicketScanResult result) { this.result = result; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonMessage() { return reasonMessage; }
    public void setReasonMessage(String reasonMessage) { this.reasonMessage = reasonMessage; }
    public Instant getScannedAt() { return scannedAt; }
    public void setScannedAt(Instant scannedAt) { this.scannedAt = scannedAt; }
}
