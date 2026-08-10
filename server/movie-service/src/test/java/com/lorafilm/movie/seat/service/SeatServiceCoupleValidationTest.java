package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.BulkSeatItemRequest;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.seat.service.impl.SeatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceCoupleValidationTest {

    @Mock
    private SeatRepository seatRepository;
    @Mock
    private SeatTypeRepository seatTypeRepository;
    @Mock
    private AuditoriumRepository auditoriumRepository;

    private SeatServiceImpl seatService;
    private Auditorium auditorium;
    private SeatType coupleType;

    @BeforeEach
    void setUp() {
        seatService = new SeatServiceImpl(
                seatRepository, seatTypeRepository, auditoriumRepository);

        auditorium = new Auditorium();
        auditorium.setId(10L);
        auditorium.setPublicId("auditorium-10");
        auditorium.setCapacity(20);
        auditorium.setStatus(AuditoriumStatus.DRAFT);

        coupleType = new SeatType();
        coupleType.setId(3L);
        coupleType.setPublicId("couple-type");
        coupleType.setCode(SeatTypeCode.COUPLE);
        coupleType.setStatus(ActiveStatus.ACTIVE);

        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate("auditorium-10"))
                .thenReturn(Optional.of(auditorium));
        when(seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(coupleType));
    }

    @Test
    void rejectsCoupleGroupWithOnlyOneSeat() {
        BulkCreateSeatsRequest request = new BulkCreateSeatsRequest(List.of(
                coupleSeat("I1", 1, "I-01")));

        assertThatThrownBy(() -> seatService.bulkCreateSeats("auditorium-10", request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BULK_SEAT_VALIDATION_ERROR);

        verify(seatRepository, never()).deleteByAuditoriumId(10L);
        verify(seatRepository, never()).saveAll(anyList());
    }

    @Test
    void acceptsExactlyTwoAdjacentSeatsInCoupleGroup() {
        BulkCreateSeatsRequest request = new BulkCreateSeatsRequest(List.of(
                coupleSeat("I1", 1, "I-01"),
                coupleSeat("I2", 2, "I-01")));
        when(seatRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = seatService.bulkCreateSeats("auditorium-10", request);

        assertThat(response).hasSize(2);
        assertThat(response).extracting("pairGroup").containsOnly("I-01");
        verify(seatRepository).deleteByAuditoriumId(10L);
    }

    private BulkSeatItemRequest coupleSeat(
            String seatCode, int positionColumn, String pairGroup) {
        return new BulkSeatItemRequest(
                coupleType.getPublicId(),
                "I",
                positionColumn,
                seatCode,
                9,
                positionColumn,
                pairGroup,
                SeatStatus.ACTIVE);
    }
}
