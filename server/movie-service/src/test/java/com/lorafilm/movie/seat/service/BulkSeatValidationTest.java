package com.lorafilm.movie.seat.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.BulkSeatItemRequest;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.seat.service.impl.SeatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BulkSeatValidationTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatTypeRepository seatTypeRepository;

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @InjectMocks
    private SeatServiceImpl seatService;

    private Auditorium auditorium;

    @BeforeEach
    void setUp() {
        auditorium = new Auditorium();
        auditorium.setId(1L);
        auditorium.setPublicId("aud-1");
        auditorium.setStatus(AuditoriumStatus.DRAFT);
        auditorium.setCapacity(100);
    }

    @Test
    void shouldAccumulateAllErrorsWithoutFailingFast() {
        when(auditoriumRepository.findByPublicIdAndDeletedAtIsNullForUpdate(anyString()))
                .thenReturn(Optional.of(auditorium));

        // Seat 0: Invalid SeatType (Not found)
        BulkSeatItemRequest seat0 = new BulkSeatItemRequest("invalid-type", "A", 1, "A1", 1, 1, null,
                com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);

        // Seat 1: Inactive SeatType
        BulkSeatItemRequest seat1 = new BulkSeatItemRequest("inactive-type", "A", 2, "A2", 1, 2, null,
                com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);

        // Inactive and active types used by this replacement layout.
        SeatType inactiveType = new SeatType();
        inactiveType.setId(2L);
        inactiveType.setPublicId("inactive-type");
        inactiveType.setStatus(ActiveStatus.INACTIVE);

        SeatType validType = new SeatType();
        validType.setId(3L);
        validType.setPublicId("valid-type");
        validType.setStatus(ActiveStatus.ACTIVE);

        when(seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(any()))
                .thenReturn(List.of(inactiveType, validType));

        // Seat 2: valid baseline item. Saving a DRAFT layout replaces the previous
        // layout, so rows already in the database are intentionally not conflicts.
        BulkSeatItemRequest seat2 = new BulkSeatItemRequest("valid-type", "A", 3, "A3", 1, 3, null,
                com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);

        // Seat 3: Duplicate position in request (duplicate with Seat 2)
        BulkSeatItemRequest seat3 = new BulkSeatItemRequest("valid-type", "A", 4, "A4", 1, 3, null,
                com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);

        // Seat 4: Duplicate seatCode in request (duplicate with Seat 2)
        BulkSeatItemRequest seat4 = new BulkSeatItemRequest("valid-type", "A", 5, "A3", 1, 5, null,
                com.lorafilm.movie.seat.domain.enums.SeatStatus.ACTIVE);

        BulkCreateSeatsRequest request = new BulkCreateSeatsRequest(List.of(seat0, seat1, seat2, seat3, seat4));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> seatService.bulkCreateSeats("aud-1", request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    org.assertj.core.api.Assertions.assertThat(be.getErrorCode())
                            .isEqualTo(ErrorCode.BULK_SEAT_VALIDATION_ERROR);

                    com.lorafilm.movie.seat.dto.BulkValidationErrorData errorData = (com.lorafilm.movie.seat.dto.BulkValidationErrorData) be
                            .getErrorData();
                    org.assertj.core.api.Assertions.assertThat(errorData.errors()).hasSize(4);
                    verify(seatRepository, never()).deleteByAuditoriumId(anyLong());
                });
    }
}
