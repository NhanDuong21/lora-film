package com.lorafilm.movie.seat.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.dto.*;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final ShowtimeRepository showtimeRepository;

    public SeatServiceImpl(SeatRepository seatRepository, SeatTypeRepository seatTypeRepository,
            AuditoriumRepository auditoriumRepository, ShowtimeRepository showtimeRepository) {
        this.seatRepository = seatRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.showtimeRepository = showtimeRepository;
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
        assertNoShowtimeHistory(auditorium);
        
        List<BulkItemError> errors = new ArrayList<>();
        int totalItems = request.seats().size();
        int targetCapacity = request.capacity() == null
                ? auditorium.getCapacity()
                : request.capacity();

        if (totalItems > targetCapacity) {
            throw new BusinessException(ErrorCode.SEAT_CAPACITY_EXCEEDED);
        }

        Map<String, Integer> requestSeatCodes = new HashMap<>();
        Map<String, Integer> requestPositions = new HashMap<>();
        List<String> typeIds = request.seats().stream().map(BulkSeatItemRequest::seatTypePublicId)
                .filter(Objects::nonNull).collect(Collectors.toList());

        Map<String, SeatType> seatTypeMap = seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(typeIds).stream()
                .collect(Collectors.toMap(SeatType::getPublicId, t -> t));

        List<Seat> seatsToSave = new ArrayList<>();

        for (int i = 0; i < totalItems; i++) {
            BulkSeatItemRequest item = request.seats().get(i);
            String normalizedSeatCode = item.seatCode() != null ? item.seatCode().trim() : null;
            String normalizedRowLabel = item.rowLabel() != null ? item.rowLabel().trim() : null;
            String normalizedPairGroup = item.pairGroup() != null ? item.pairGroup().trim() : null;
            String posKey = item.positionRow() + "-" + item.positionColumn();

            boolean hasError = false;

            if (normalizedSeatCode == null || normalizedSeatCode.isEmpty()) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "seatCode", normalizedSeatCode, "VALIDATION_FAILED",
                        "seatCode must not be blank"));
                hasError = true;
            }
            if (item.positionRow() <= 0) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "positionRow", item.positionRow(),
                        "VALIDATION_FAILED", "positionRow must be greater than 0"));
                hasError = true;
            }
            if (item.positionColumn() <= 0) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "positionColumn", item.positionColumn(),
                        "VALIDATION_FAILED", "positionColumn must be greater than 0"));
                hasError = true;
            }
            if (item.seatTypePublicId() == null || item.seatTypePublicId().isEmpty()) {
                errors.add(new BulkItemError(i, normalizedSeatCode, "seatTypePublicId", item.seatTypePublicId(),
                        "VALIDATION_FAILED", "seatTypePublicId must not be blank"));
                hasError = true;
            }

            // In-request duplicates
            if (normalizedSeatCode != null && !normalizedSeatCode.isEmpty()) {
                if (requestSeatCodes.containsKey(normalizedSeatCode)) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "seatCode", normalizedSeatCode,
                            "DUPLICATE_SEAT_CODE_IN_REQUEST",
                            "Duplicate seatCode at index " + requestSeatCodes.get(normalizedSeatCode)));
                    hasError = true;
                } else {
                    requestSeatCodes.put(normalizedSeatCode, i);
                }
            }

            if (item.positionRow() > 0 && item.positionColumn() > 0) {
                if (requestPositions.containsKey(posKey)) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "position", posKey,
                            "DUPLICATE_SEAT_POSITION_IN_REQUEST",
                            "Duplicate position at index " + requestPositions.get(posKey)));
                    hasError = true;
                } else {
                    requestPositions.put(posKey, i);
                }
            }

            // Seat Type validation
            if (item.seatTypePublicId() != null && !item.seatTypePublicId().isEmpty()) {
                SeatType type = seatTypeMap.get(item.seatTypePublicId());
                if (type == null) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "seatTypePublicId", item.seatTypePublicId(),
                            "SEAT_TYPE_NOT_FOUND", "Seat type not found"));
                    hasError = true;
                } else if (type.getStatus() != ActiveStatus.ACTIVE) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "seatTypePublicId", item.seatTypePublicId(),
                            "SEAT_TYPE_INACTIVE", "Seat type is inactive"));
                    hasError = true;
                } else if (type.getCode() == SeatTypeCode.COUPLE
                        && (normalizedPairGroup == null || normalizedPairGroup.isEmpty())) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "pairGroup", normalizedPairGroup,
                            "COUPLE_PAIR_GROUP_REQUIRED",
                            "Couple seats must belong to a pairGroup"));
                    hasError = true;
                } else if (type.getCode() != SeatTypeCode.COUPLE
                        && normalizedPairGroup != null && !normalizedPairGroup.isEmpty()) {
                    errors.add(new BulkItemError(i, normalizedSeatCode, "pairGroup", normalizedPairGroup,
                            "PAIR_GROUP_NOT_ALLOWED",
                            "Only couple seats may define pairGroup"));
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
                seat.setStatus(
                        item.status() != null ? item.status() : com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);
                seatsToSave.add(seat);
            }
        }

        validateCouplePairGroups(request.seats(), seatTypeMap, errors);

        if (!errors.isEmpty()) {
            int invalidCount = (int) errors.stream().map(BulkItemError::index).distinct().count();

            BulkValidationErrorData errorData = new BulkValidationErrorData(
                    totalItems,
                    totalItems - invalidCount,
                    invalidCount,
                    errors);
            throw new BusinessException(ErrorCode.BULK_SEAT_VALIDATION_ERROR,
                    errorData.invalidItems() + " invalid seat(s) found", errorData);
        }

        // Saving a DRAFT room layout is a full replacement. Validate the complete
        // incoming layout first so invalid input never executes a delete statement.
        auditorium.setCapacity(targetCapacity);
        seatRepository.deleteByAuditoriumId(auditorium.getId());
        seatsToSave = seatRepository.saveAll(seatsToSave);
        return seatsToSave.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private void validateCouplePairGroups(
            List<BulkSeatItemRequest> items,
            Map<String, SeatType> seatTypeMap,
            List<BulkItemError> errors) {
        Map<String, List<Integer>> groupIndexes = new LinkedHashMap<>();
        for (int index = 0; index < items.size(); index++) {
            BulkSeatItemRequest item = items.get(index);
            SeatType type = seatTypeMap.get(item.seatTypePublicId());
            String group = item.pairGroup() == null ? null : item.pairGroup().trim();
            if (type == null || type.getCode() != SeatTypeCode.COUPLE
                    || group == null || group.isEmpty()) {
                continue;
            }
            groupIndexes.computeIfAbsent(group, ignored -> new ArrayList<>()).add(index);
        }

        groupIndexes.forEach((group, indexes) -> {
            boolean valid = indexes.size() == 2;
            if (valid) {
                BulkSeatItemRequest first = items.get(indexes.get(0));
                BulkSeatItemRequest second = items.get(indexes.get(1));
                valid = Objects.equals(normalize(first.rowLabel()), normalize(second.rowLabel()))
                        && Objects.equals(first.positionRow(), second.positionRow())
                        && Math.abs(first.positionColumn() - second.positionColumn()) == 1;
            }
            if (valid) return;

            for (Integer index : indexes) {
                BulkSeatItemRequest item = items.get(index);
                errors.add(new BulkItemError(
                        index,
                        item.seatCode(),
                        "pairGroup",
                        group,
                        "INVALID_COUPLE_PAIR_GROUP",
                        "A couple pairGroup must contain exactly two adjacent seats in the same row"));
            }
        });
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(String seatPublicId, UpdateSeatRequest request) {
        Seat seat = seatRepository.findByPublicIdAndDeletedAtIsNull(seatPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        Auditorium auditorium = auditoriumRepository
                .findByPublicIdAndDeletedAtIsNullForUpdate(seat.getAuditorium().getPublicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        boolean structuralChanged = !Objects.equals(seat.getSeatType().getPublicId(), request.seatTypePublicId()) ||
                !Objects.equals(seat.getRowLabel(), request.rowLabel()) ||
                !Objects.equals(seat.getSeatNumber(), request.seatNumber()) ||
                !Objects.equals(seat.getSeatCode(), request.seatCode()) ||
                !Objects.equals(seat.getPositionRow(), request.positionRow()) ||
                !Objects.equals(seat.getPositionColumn(), request.positionColumn()) ||
                !Objects.equals(seat.getPairGroup(), request.pairGroup());

        if (structuralChanged
                && auditorium.getStatus() != com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.DRAFT) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("auditoriumPublicId", auditorium.getPublicId());
            errorData.put("auditoriumStatus", auditorium.getStatus().name());
            errorData.put("seatPublicId", seatPublicId);
            errorData.put("message", "Thay đổi cấu trúc ghế chỉ được phép khi Auditorium ở trạng thái DRAFT.");
            throw new BusinessException(ErrorCode.AUDITORIUM_LAYOUT_NOT_EDITABLE, errorData);
        }
        if (structuralChanged) {
            assertNoShowtimeHistory(auditorium);
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
            validateUpdatedCoupleLayout(auditorium.getId(), seat);
        }

        // Always allow status update if it's ACTIVE or MAINTENANCE
        seat.setStatus(request.status());

        return mapToResponse(seat);
    }

    private void validateUpdatedCoupleLayout(Long auditoriumId, Seat updatedSeat) {
        List<Seat> layout = new ArrayList<>(
                seatRepository.findByAuditoriumIdAndDeletedAtIsNull(auditoriumId));
        layout.removeIf(existing -> Objects.equals(existing.getId(), updatedSeat.getId()));
        layout.add(updatedSeat);

        Map<String, List<Seat>> coupleGroups = new LinkedHashMap<>();
        for (Seat seat : layout) {
            boolean couple = seat.getSeatType().getCode() == SeatTypeCode.COUPLE;
            String group = normalize(seat.getPairGroup());
            if (couple && (group == null || group.isEmpty())) {
                throw new BusinessException(
                        ErrorCode.INVALID_COUPLE_PAIR_CONFIGURATION,
                        "Couple seats must belong to a pairGroup");
            }
            if (!couple && group != null && !group.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.INVALID_COUPLE_PAIR_CONFIGURATION,
                        "Only couple seats may define pairGroup");
            }
            if (couple) {
                coupleGroups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(seat);
            }
        }

        for (Map.Entry<String, List<Seat>> entry : coupleGroups.entrySet()) {
            List<Seat> pair = entry.getValue();
            boolean valid = pair.size() == 2
                    && Objects.equals(normalize(pair.get(0).getRowLabel()),
                            normalize(pair.get(1).getRowLabel()))
                    && Objects.equals(pair.get(0).getPositionRow(), pair.get(1).getPositionRow())
                    && Math.abs(pair.get(0).getPositionColumn() - pair.get(1).getPositionColumn()) == 1;
            if (!valid) {
                throw new BusinessException(
                        ErrorCode.INVALID_COUPLE_PAIR_CONFIGURATION,
                        "A couple pairGroup must contain exactly two adjacent seats in the same row");
            }
        }
    }

    private void assertNoShowtimeHistory(Auditorium auditorium) {
        if (showtimeRepository.existsByAuditoriumId(auditorium.getId())) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("auditoriumPublicId", auditorium.getPublicId());
            errorData.put("message",
                    "Phòng đã có lịch sử suất chiếu. Hãy tạo phiên bản sơ đồ mới thay vì ghi đè seat identity.");
            throw new BusinessException(ErrorCode.AUDITORIUM_LAYOUT_HAS_SHOWTIME_HISTORY, errorData);
        }
    }

    private SeatResponse mapToResponse(Seat s) {
        SeatTypeResponse typeResp = new SeatTypeResponse(
                s.getSeatType().getPublicId(),
                s.getSeatType().getCode(),
                s.getSeatType().getName(),
                s.getSeatType().getDescription(),
                s.getSeatType().getStatus(),
                s.getSeatType().getCreatedAt(),
                s.getSeatType().getUpdatedAt());
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
                s.getUpdatedAt());
    }

    @Override
    public List<Seat> getSeatsByAuditoriumId(Long auditoriumId) {
        return seatRepository.findByAuditoriumIdAndDeletedAtIsNull(auditoriumId);
    }

    @Override
    public List<Seat> getSeatsByIds(List<Long> seatIds) {
        return seatRepository.findByIdInAndDeletedAtIsNull(seatIds);
    }

    @Override
    public List<Seat> getSeatsByPublicIds(List<String> seatPublicIds) {
        return seatRepository.findByPublicIdInAndDeletedAtIsNull(seatPublicIds);
    }
}
