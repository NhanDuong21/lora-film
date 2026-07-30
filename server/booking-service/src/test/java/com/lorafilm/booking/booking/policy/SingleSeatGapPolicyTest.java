package com.lorafilm.booking.booking.policy;

import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse.SeatDetailDto;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleSeatGapPolicyTest {

    @Test
    void detectsEdgeGapCreatedBySelection() {
        List<SeatDetailDto> seats = List.of(
                seat(101L, "A1", "STANDARD", 1, 1, null),
                seat(102L, "A2", "STANDARD", 1, 2, null),
                seat(103L, "A3", "STANDARD", 1, 3, null));

        assertTrue(SingleSeatGapPolicy.leavesSingleSeatGap(
                seats, Set.of(102L, 103L), Set.of()));
    }

    @Test
    void includesSeatsHeldByAnotherCustomer() {
        List<SeatDetailDto> seats = List.of(
                seat(101L, "A1", "STANDARD", 1, 1, null),
                seat(102L, "A2", "STANDARD", 1, 2, null),
                seat(103L, "A3", "STANDARD", 1, 3, null));

        assertTrue(SingleSeatGapPolicy.leavesSingleSeatGap(
                seats, Set.of(103L), Set.of(101L)));
    }

    @Test
    void doesNotCrossAnAisleRepresentedByAMissingColumn() {
        List<SeatDetailDto> seats = List.of(
                seat(101L, "A1", "STANDARD", 1, 1, null),
                seat(103L, "A3", "STANDARD", 1, 3, null));

        assertFalse(SingleSeatGapPolicy.leavesSingleSeatGap(
                seats, Set.of(103L), Set.of()));
    }

    @Test
    void doesNotTreatCoupleCouchAsSingleSeatGap() {
        List<SeatDetailDto> seats = List.of(
                seat(201L, "I1", "COUPLE", 9, 1, "I-01"),
                seat(202L, "I2", "COUPLE", 9, 2, "I-01"),
                seat(203L, "I3", "STANDARD", 9, 3, null),
                seat(204L, "I4", "STANDARD", 9, 4, null));

        assertFalse(SingleSeatGapPolicy.leavesSingleSeatGap(
                seats, Set.of(203L, 204L), Set.of()));
    }

    private SeatDetailDto seat(
            Long id,
            String code,
            String type,
            int row,
            int column,
            String pairGroup) {
        SeatDetailDto seat = new SeatDetailDto(id, code, type, null, false, row, column);
        seat.setStatus("ACTIVE");
        seat.setPairGroup(pairGroup);
        return seat;
    }
}
