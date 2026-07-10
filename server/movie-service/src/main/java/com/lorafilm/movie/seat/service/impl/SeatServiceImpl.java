package com.lorafilm.movie.seat.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
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

        long activeSeatCount = seatRepository.countByAuditoriumIdAndDeletedAtIsNull(auditorium.getId());
        if (activeSeatCount + request.seats().size() > auditorium.getCapacity()) {
            throw new BusinessException(ErrorCode.SEAT_CAPACITY_EXCEEDED);
        }

        // Validate duplicates within request
        Set<String> requestSeatCodes = new HashSet<>();
        Set<String> requestPositions = new HashSet<>();
        List<String> typeIds = new ArrayList<>();
        
        for (BulkSeatItemRequest item : request.seats()) {
            if (!requestSeatCodes.add(item.seatCode())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SEAT_CODE_IN_REQUEST);
            }
            String posKey = item.positionRow() + "-" + item.positionColumn();
            if (!requestPositions.add(posKey)) {
                throw new BusinessException(ErrorCode.DUPLICATE_SEAT_POSITION_IN_REQUEST);
            }
            typeIds.add(item.seatTypePublicId());
        }

        Map<String, SeatType> seatTypeMap = seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(typeIds).stream()
                .collect(Collectors.toMap(SeatType::getPublicId, t -> t));

        List<com.lorafilm.movie.seat.repository.SeatConflictProjection> existingConflicts = seatRepository.findConflictDataByAuditoriumId(auditorium.getId());
        Set<String> existingCodes = existingConflicts.stream().map(c -> c.getSeatCode()).collect(Collectors.toSet());
        Set<String> existingPositions = existingConflicts.stream().map(c -> c.getPositionRow() + "-" + c.getPositionColumn()).collect(Collectors.toSet());

        List<Seat> seatsToSave = new ArrayList<>();
        for (BulkSeatItemRequest item : request.seats()) {
            String normalizedSeatCode = item.seatCode() != null ? item.seatCode().trim() : null;
            String normalizedRowLabel = item.rowLabel() != null ? item.rowLabel().trim() : null;
            String normalizedPairGroup = item.pairGroup() != null ? item.pairGroup().trim() : null;

            if (existingCodes.contains(normalizedSeatCode)) {
                throw new BusinessException(ErrorCode.DUPLICATE_SEAT_CODE);
            }
            if (existingPositions.contains(item.positionRow() + "-" + item.positionColumn())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SEAT_POSITION);
            }

            SeatType type = seatTypeMap.get(item.seatTypePublicId());
            if (type == null) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND);
            }
            if (type.getStatus() != ActiveStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_INACTIVE);
            }

            Seat seat = new Seat();
            seat.setPublicId(UUID.randomUUID().toString());
            seat.setAuditorium(auditorium);
            seat.setSeatType(type);
            seat.setRowLabel(normalizedRowLabel);
            seat.setSeatNumber(item.seatNumber());
            seat.setSeatCode(normalizedSeatCode);
            seat.setPositionRow(item.positionRow());
            seat.setPositionColumn(item.positionColumn());
            seat.setPairGroup(normalizedPairGroup);
            seat.setStatus(item.status());
            
            seatsToSave.add(seat);
        }

        seatsToSave = seatRepository.saveAll(seatsToSave);
        return seatsToSave.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(String seatPublicId, UpdateSeatRequest request) {
        Seat seat = seatRepository.findByPublicIdAndDeletedAtIsNull(seatPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        Auditorium auditorium = seat.getAuditorium();

        String normalizedSeatCode = request.seatCode() != null ? request.seatCode().trim() : null;
        String normalizedRowLabel = request.rowLabel() != null ? request.rowLabel().trim() : null;
        String normalizedPairGroup = request.pairGroup() != null ? request.pairGroup().trim() : null;

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
        seat.setStatus(request.status());

        return mapToResponse(seat);
    }

    @Override
    @Transactional
    public SeatResponse updateSeatStatus(String seatPublicId, UpdateSeatStatusRequest request) {
        Seat seat = seatRepository.findByPublicIdAndDeletedAtIsNull(seatPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

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
