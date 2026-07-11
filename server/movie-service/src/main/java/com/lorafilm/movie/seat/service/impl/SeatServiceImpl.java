package com.lorafilm.movie.seat.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.dto.*;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.seat.service.SeatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final AuditoriumRepository auditoriumRepository;

    public SeatServiceImpl(SeatRepository seatRepository, SeatTypeRepository seatTypeRepository, AuditoriumRepository auditoriumRepository) {
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.auditoriumRepository = auditoriumRepository;
    }

    @Override
    @Transactional
    public List<SeatResponse> bulkCreateSeats(String auditoriumPublicId, BulkCreateSeatsRequest request) {
        if (request.seats() == null || request.seats().isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_SEAT_BULK_REQUEST);
        }

        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        if (auditorium.getStatus() != com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.DRAFT) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("auditoriumPublicId", auditorium.getPublicId());
            errorData.put("message", "Thay đổi cấu trúc ghế chỉ được phép khi Auditorium ở trạng thái DRAFT.");
            throw new BusinessException(ErrorCode.AUDITORIUM_LAYOUT_NOT_EDITABLE, errorData);
        }

        List<BulkItemError> errors = new ArrayList<>();
        int totalItems = request.seats().size();

        long activeSeatCount = seatRepository.countByAuditoriumIdAndDeletedAtIsNull(auditorium.getId());
        if (activeSeatCount + totalItems > auditorium.getCapacity()) {
            throw new BusinessException(ErrorCode.SEAT_CAPACITY_EXCEEDED);
        }

        Map<String, Integer> requestSeatCodes = new HashMap<>();
        Map<String, Integer> requestPositions = new HashMap<>();
        List<String> typeIds = request.seats().stream().map(BulkSeatItemRequest::seatTypePublicId).filter(Objects::nonNull).collect(Collectors.toList());

        Map<String, SeatType> seatTypeMap = seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(typeIds).stream()
                .collect(Collectors.toMap(SeatType::getPublicId, t -> t));

        List<com.lorafilm.movie.seat.repository.SeatConflictProjection> existingConflicts = seatRepository.findConflictDataByAuditoriumId(auditorium.getId());
        Set<String> existingCodes = existingConflicts.stream().map(c -> c.getSeatCode()).collect(Collectors.toSet());
        Set<String> existingPositions = existingConflicts.stream().map(c -> c.getPositionRow() + "-" + c.getPositionColumn()).collect(Collectors.toSet());

        List<Seat> seatsToSave = new ArrayList<>();
        
        for (int i = 0; i < totalItems; i++) {
            BulkSeatItemRequest item = request.seats().get(i);
            String normalizedSeatCode = item.seatCode() != null ? item.seatCode().trim() : null;
            String normalizedRowLabel = item.rowLabel() != null ? item.rowLabel().trim() : null;
            String normalizedPairGroup = item.pairGroup() != null ? item.pairGroup().trim() : null;
            String posKey = item.positionRow() + "-" + item.positionColumn();

            boolean hasError = false;

            // Null checks
            if (normalizedSeatCode == null || normalizedSeatCode.isEmpty()) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "seatCode", normalizedSeatCode, "VALIDATION_FAILED", "seatCode không được để trống"));
                hasError = true;
            }
            if (item.positionRow() <= 0) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "positionRow", item.positionRow(), "VALIDATION_FAILED", "positionRow phải lớn hơn 0"));
                hasError = true;
            }
            if (item.positionColumn() <= 0) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "positionColumn", item.positionColumn(), "VALIDATION_FAILED", "positionColumn phải lớn hơn 0"));
                hasError = true;
            }
            if (item.seatTypePublicId() == null || item.seatTypePublicId().isEmpty()) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "seatTypePublicId", item.seatTypePublicId(), "VALIDATION_FAILED", "seatTypePublicId không được để trống"));
                hasError = true;
            }

            // In-request duplicates
            if (normalizedSeatCode != null && !normalizedSeatCode.isEmpty()) {
                if (requestSeatCodes.containsKey(normalizedSeatCode)) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "seatCode", normalizedSeatCode, "DUPLICATE_SEAT_CODE_IN_REQUEST", "seatCode bị trùng với phần tử tại index " + requestSeatCodes.get(normalizedSeatCode)));
                    hasError = true;
                } else {
                    requestSeatCodes.put(normalizedSeatCode, i);
                }
            }

            if (item.positionRow() > 0 && item.positionColumn() > 0) {
                if (requestPositions.containsKey(posKey)) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "position", posKey, "DUPLICATE_SEAT_POSITION_IN_REQUEST", "Vị trí bị trùng với phần tử tại index " + requestPositions.get(posKey)));
                    hasError = true;
                } else {
                    requestPositions.put(posKey, i);
                }
            }

            // Database conflicts
            if (normalizedSeatCode != null && existingCodes.contains(normalizedSeatCode)) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "seatCode", normalizedSeatCode, "DUPLICATE_SEAT_CODE", "Mã ghế đã tồn tại trong khán phòng"));
                hasError = true;
            }
            if (item.positionRow() > 0 && item.positionColumn() > 0 && existingPositions.contains(posKey)) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "position", posKey, "DUPLICATE_SEAT_POSITION", "Vị trí này đã được sử dụng bởi một ghế khác"));
                hasError = true;
            }

            // Seat Type validation
            if (item.seatTypePublicId() != null && !item.seatTypePublicId().isEmpty()) {
                SeatType type = seatTypeMap.get(item.seatTypePublicId());
                if (type == null) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "seatTypePublicId", item.seatTypePublicId(), "SEAT_TYPE_NOT_FOUND", "Không tìm thấy loại ghế"));
                    hasError = true;
                } else if (type.getStatus() != ActiveStatus.ACTIVE) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "seatTypePublicId", item.seatTypePublicId(), "SEAT_TYPE_INACTIVE", "Loại ghế đang không hoạt động"));
                    hasError = true;
                }
            }

            if (!hasError) {
                Seat seat = new Seat();
                seat.setPublicId(UUID.randomUUID().toString());
                seat.setAuditorium(auditorium);
                seat.setSeatType(seatTypeMap.get(item.seatTypePublicId()));
                seat.setRowLabel(normalizedRowLabel);
                seat.setSeatNumber(item.seatNumber());
                seat.setSeatCode(normalizedSeatCode);
                seat.setPositionRow(item.positionRow());
                seat.setPositionColumn(item.positionColumn());
                seat.setPairGroup(normalizedPairGroup);
                seat.setStatus(item.status() != null ? item.status() : com.lorafilm.movie.common.enums.SeatStatus.ACTIVE);
                seatsToSave.add(seat);
            }
        }

        if (!errors.isEmpty()) {
            BulkValidationErrorData errorData = new BulkValidationErrorData(
                totalItems, totalItems - errors.stream().map(BulkItemError::index).distinct().count(), (int) errors.stream().map(BulkItemError::index).distinct().count(), errors
            );
            throw new BusinessException(ErrorCode.BULK_SEAT_VALIDATION_ERROR, "Có " + errorData.invalidItems() + " ghế không hợp lệ", errorData);
        }

        seatsToSave = seatRepository.saveAll(seatsToSave);
        return seatsToSave.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(String seatPublicId, UpdateSeatRequest request) {
        Seat seat = seatRepository.findByPublicIdAndDeletedAtIsNull(seatPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(seat.getAuditorium().getPublicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        boolean structuralChanged = 
            !Objects.equals(seat.getSeatType().getPublicId(), request.seatTypePublicId()) ||
            !Objects.equals(seat.getRowLabel(), request.rowLabel()) ||
            !Objects.equals(seat.getSeatNumber(), request.seatNumber()) ||
            !Objects.equals(seat.getSeatCode(), request.seatCode()) ||
            !Objects.equals(seat.getPositionRow(), request.positionRow()) ||
            !Objects.equals(seat.getPositionColumn(), request.positionColumn()) ||
            !Objects.equals(seat.getPairGroup(), request.pairGroup());

        if (structuralChanged && auditorium.getStatus() != com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.DRAFT) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("auditoriumPublicId", auditorium.getPublicId());
            errorData.put("auditoriumStatus", auditorium.getStatus().name());
            errorData.put("seatPublicId", seatPublicId);
            errorData.put("message", "Thay đổi cấu trúc ghế chỉ được phép khi Auditorium ở trạng thái DRAFT.");
            throw new BusinessException(ErrorCode.AUDITORIUM_LAYOUT_NOT_EDITABLE, errorData);
        }

        String normalizedSeatCode = request.seatCode() != null ? request.seatCode().trim() : null;
        String normalizedRowLabel = request.rowLabel() != null ? request.rowLabel().trim() : null;
        String normalizedPairGroup = request.pairGroup() != null ? request.pairGroup().trim() : null;

        if (structuralChanged) {
            if (seatRepository.existsByAuditoriumIdAndSeatCodeAndIdNotAndDeletedAtIsNull(
                    auditorium.getId(), normalizedSeatCode, seat.getId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SEAT_CODE);
            }

            if (seatRepository.existsByAuditoriumIdAndPositionRowAndPositionColumnAndIdNotAndDeletedAtIsNull(
                    auditorium.getId(), request.positionRow(), request.positionColumn(), seat.getId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SEAT_POSITION);
            }

            SeatType type = seatTypeRepository.findByPublicIdAndDeletedAtIsNull(request.seatTypePublicId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND));
                    
            if (type.getStatus() != ActiveStatus.ACTIVE && !type.getId().equals(seat.getSeatType().getId())) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_INACTIVE);
            }

            seat.setSeatType(type);
            seat.setRowLabel(normalizedRowLabel);
            seat.setSeatNumber(request.seatNumber());
            seat.setSeatCode(normalizedSeatCode);
            seat.setPositionRow(request.positionRow());
            seat.setPositionColumn(request.positionColumn());
            seat.setPairGroup(normalizedPairGroup);
        }

        // Always allow status update if it's ACTIVE or MAINTENANCE
        seat.setStatus(request.status());

        return mapToResponse(seat);
    }


    private SeatResponse mapToResponse(Seat s) {
        SeatTypeResponse typeResp = new SeatTypeResponse(
            s.getSeatType().getPublicId(),
            s.getSeatType().getCode(),
            s.getSeatType().getName(),
            s.getSeatType().getDescription(),
            s.getSeatType().getStatus(),
            s.getSeatType().getCreatedAt(),
            s.getSeatType().getUpdatedAt()
        );
        return new SeatResponse(
            s.getPublicId(),
            s.getRowLabel(),
            s.getSeatNumber(),
            s.getSeatCode(),
            s.getPositionRow(),
            s.getPositionColumn(),
            s.getPairGroup(),
            s.getStatus(),
            typeResp,
            s.getCreatedAt(),
            s.getUpdatedAt()
        );
    }
}
