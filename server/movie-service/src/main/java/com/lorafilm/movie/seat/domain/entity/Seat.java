package com.lorafilm.movie.seat.domain.entity;

import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_seats_auditorium_code", columnNames = {"auditorium_id", "seat_code"}),
        @UniqueConstraint(name = "uk_seats_auditorium_position", columnNames = {"auditorium_id", "position_row", "position_column"})
})
public class Seat extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", updatable = false, unique = true, nullable = false, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_type_id", nullable = false)
    private SeatType seatType;

    @Column(name = "row_label", nullable = false, length = 5)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "seat_code", nullable = false, length = 10)
    private String seatCode;

    @Column(name = "position_row", nullable = false)
    private Integer positionRow;

    @Column(name = "position_column", nullable = false)
    private Integer positionColumn;

    @Column(name = "pair_group", length = 30)
    private String pairGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SeatStatus status = SeatStatus.ACTIVE;

    public Seat() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Auditorium getAuditorium() {
        return auditorium;
    }

    public void setAuditorium(Auditorium auditorium) {
        this.auditorium = auditorium;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void setSeatType(SeatType seatType) {
        this.seatType = seatType;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public Integer getPositionRow() {
        return positionRow;
    }

    public void setPositionRow(Integer positionRow) {
        this.positionRow = positionRow;
    }

    public Integer getPositionColumn() {
        return positionColumn;
    }

    public void setPositionColumn(Integer positionColumn) {
        this.positionColumn = positionColumn;
    }

    public String getPairGroup() {
        return pairGroup;
    }

    public void setPairGroup(String pairGroup) {
        this.pairGroup = pairGroup;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}
