package com.lorafilm.movie.seat.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.dto.*;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.service.SeatLayoutQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatLayoutQueryServiceImpl implements SeatLayoutQueryService {

    private final SeatRepository seatRepository;
    private final AuditoriumRepository auditoriumRepository;

    public SeatLayoutQueryServiceImpl(SeatRepository seatRepository, AuditoriumRepository auditoriumRepository) {
        this.seatRepository = seatRepository;
        this.auditoriumRepository = auditoriumRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerSeatLayoutResponse getCustomerSeatLayout(String auditoriumPublicId) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        List<Seat> seats = seatRepository.findCustomerLayoutByAuditoriumId(auditorium.getId());

        Map<Integer, List<CustomerSeatItemResponse>> groupedByRow = new TreeMap<>();
        Map<Integer, String> rowLabels = new HashMap<>();

        for (Seat seat : seats) {
            CustomerSeatTypeResponse tr = new CustomerSeatTypeResponse(
                    seat.getSeatType().getPublicId(),
                    seat.getSeatType().getCode(),
                    seat.getSeatType().getName()
            );

            CustomerSeatItemResponse cir = new CustomerSeatItemResponse(
                    seat.getPublicId(), seat.getSeatCode(), seat.getSeatNumber(),
                    seat.getPositionColumn(), seat.getPairGroup(), tr
            );

            groupedByRow.computeIfAbsent(seat.getPositionRow(), k -> new ArrayList<>()).add(cir);
            rowLabels.putIfAbsent(seat.getPositionRow(), seat.getRowLabel());
        }

        List<SeatRowLayoutResponse<CustomerSeatItemResponse>> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<CustomerSeatItemResponse>> entry : groupedByRow.entrySet()) {
            rows.add(new SeatRowLayoutResponse<>(entry.getKey(), rowLabels.get(entry.getKey()), entry.getValue()));
        }

        return new CustomerSeatLayoutResponse(
                auditorium.getPublicId(),
                auditorium.getName(),
                auditorium.getCapacity(),
                auditorium.getScreenType(),
                rows
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSeatLayoutResponse getAdminSeatLayout(String auditoriumPublicId) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        List<Seat> seats = seatRepository.findAdminLayoutByAuditoriumId(auditorium.getId());

        int totalSeats = seats.size();
        int activeSeats = 0;
        int maintenanceSeats = 0;

        Map<Integer, List<AdminSeatItemResponse>> groupedByRow = new TreeMap<>();
        Map<Integer, String> rowLabels = new HashMap<>();

        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.ACTIVE) activeSeats++;
            else if (seat.getStatus() == SeatStatus.MAINTENANCE) maintenanceSeats++;

            SeatTypeResponse tr = new SeatTypeResponse(seat.getSeatType().getPublicId(), seat.getSeatType().getCode(),
                    seat.getSeatType().getName(), seat.getSeatType().getDescription(), seat.getSeatType().getStatus(),
                    seat.getSeatType().getCreatedAt(), seat.getSeatType().getUpdatedAt());

            AdminSeatItemResponse air = new AdminSeatItemResponse(
                    seat.getPublicId(), seat.getSeatCode(), seat.getRowLabel(), seat.getSeatNumber(),
                    seat.getPositionRow(), seat.getPositionColumn(), seat.getPairGroup(), seat.getStatus(),
                    tr, seat.getCreatedAt(), seat.getUpdatedAt()
            );

            groupedByRow.computeIfAbsent(seat.getPositionRow(), k -> new ArrayList<>()).add(air);
            rowLabels.putIfAbsent(seat.getPositionRow(), seat.getRowLabel());
        }

        List<SeatRowLayoutResponse<AdminSeatItemResponse>> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<AdminSeatItemResponse>> entry : groupedByRow.entrySet()) {
            rows.add(new SeatRowLayoutResponse<>(entry.getKey(), rowLabels.get(entry.getKey()), entry.getValue()));
        }

        return new AdminSeatLayoutResponse(
                auditorium.getPublicId(), auditorium.getName(), auditorium.getCapacity(),
                auditorium.getScreenType(), auditorium.getSoundType(), auditorium.getCleaningBufferMinutes(),
                auditorium.getStatus(), totalSeats, activeSeats, maintenanceSeats, rows
        );
    }
}
