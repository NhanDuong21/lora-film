package com.lorafilm.movie.seat.service.impl;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.dto.CreateSeatTypeRequest;
import com.lorafilm.movie.seat.dto.SeatTypeResponse;
import com.lorafilm.movie.seat.dto.UpdateSeatTypeRequest;
import com.lorafilm.movie.seat.dto.UpdateSeatTypeStatusRequest;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.seat.service.SeatTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SeatTypeServiceImpl implements SeatTypeService {

    private final SeatTypeRepository seatTypeRepository;
    private final SeatRepository seatRepository;

    public SeatTypeServiceImpl(SeatTypeRepository seatTypeRepository, SeatRepository seatRepository) {
        this.seatTypeRepository = seatTypeRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional
    public SeatTypeResponse createSeatType(CreateSeatTypeRequest request) {
        if (seatTypeRepository.existsByCodeAndDeletedAtIsNull(request.code())) {
            throw new BusinessException(ErrorCode.SEAT_TYPE_CODE_ALREADY_EXISTS);
        }

        SeatType seatType = new SeatType();
        seatType.setPublicId(UUID.randomUUID().toString());
        seatType.setCode(request.code());
        seatType.setName(request.name().trim());
        seatType.setDescription(request.description());
        seatType.setStatus(ActiveStatus.ACTIVE);

        seatType = seatTypeRepository.save(seatType);
        return mapToResponse(seatType);
    }

    @Override
    @Transactional
    public SeatTypeResponse updateSeatType(String publicId, UpdateSeatTypeRequest request) {
        SeatType seatType = seatTypeRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND));

        validateStatusTransition(seatType.getStatus(), request.status(), seatType);

        seatType.setName(request.name().trim());
        seatType.setDescription(request.description());
        seatType.setStatus(request.status());

        return mapToResponse(seatType);
    }

    @Override
    @Transactional
    public SeatTypeResponse updateStatus(String publicId, UpdateSeatTypeStatusRequest request) {
        SeatType seatType = seatTypeRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND));

        validateStatusTransition(seatType.getStatus(), request.status(), seatType);
        seatType.setStatus(request.status());
        
        return mapToResponse(seatType);
    }

    private void validateStatusTransition(ActiveStatus current, ActiveStatus target, SeatType seatType) {
        if (current == target) return;
        
        if (target == ActiveStatus.INACTIVE) {
            if (seatRepository.existsBySeatTypeIdAndDeletedAtIsNull(seatType.getId())) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_IN_USE);
            }
        }
    }

    private SeatTypeResponse mapToResponse(SeatType t) {
        return new SeatTypeResponse(
            t.getPublicId(),
            t.getCode(),
            t.getName(),
            t.getDescription(),
            t.getStatus(),
            t.getCreatedAt(),
            t.getUpdatedAt()
        );
    }
}
