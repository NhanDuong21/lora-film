package com.lorafilm.booking.booking.policy;

import com.lorafilm.booking.infrastructure.client.dto.ShowtimeSeatLayoutResponse.SeatDetailDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SingleSeatGapPolicy {

    private SingleSeatGapPolicy() {
    }

    public static boolean leavesSingleSeatGap(
            List<SeatDetailDto> seats,
            Set<Long> selectedSeatIds,
            Set<Long> occupiedSeatIds) {
        Set<Long> selected = selectedSeatIds == null ? Set.of() : selectedSeatIds;
        Set<Long> occupied = occupiedSeatIds == null ? Set.of() : occupiedSeatIds;
        Map<Integer, List<SeatState>> rows = new HashMap<>();

        for (SeatDetailDto seat : seats == null ? List.<SeatDetailDto>of() : seats) {
            boolean isSelected = selected.contains(seat.getSeatId());
            boolean unavailable = seat.isBlocked()
                    || !"ACTIVE".equals(normalize(seat.getStatus()))
                    || occupied.contains(seat.getSeatId());
            rows.computeIfAbsent(seat.getRowIndex(), ignored -> new ArrayList<>())
                    .add(new SeatState(seat, isSelected, unavailable));
        }

        for (List<SeatState> rowSeats : rows.values()) {
            markUnavailableCoupleGroups(rowSeats);
            Map<Integer, SeatState> seatsByColumn = new HashMap<>();
            for (SeatState seat : rowSeats) {
                seatsByColumn.put(seat.detail.getColumnIndex(), seat);
            }

            for (SeatState seat : rowSeats) {
                if (seat.selected || seat.unavailable || isCouple(seat.detail)) {
                    continue;
                }

                int column = seat.detail.getColumnIndex();
                SeatState left = seatsByColumn.get(column - 1);
                SeatState right = seatsByColumn.get(column + 1);
                boolean leftBlocked = left == null || left.selected || left.unavailable;
                boolean rightBlocked = right == null || right.selected || right.unavailable;
                boolean selectionCreatedGap = (left != null && left.selected)
                        || (right != null && right.selected);

                if (leftBlocked && rightBlocked && selectionCreatedGap) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void markUnavailableCoupleGroups(List<SeatState> rowSeats) {
        Map<String, List<SeatState>> coupleGroups = new HashMap<>();
        for (SeatState seat : rowSeats) {
            String pairGroup = seat.detail.getPairGroup();
            if (!isCouple(seat.detail) || pairGroup == null || pairGroup.isBlank()) {
                continue;
            }
            coupleGroups.computeIfAbsent(pairGroup, ignored -> new ArrayList<>()).add(seat);
        }

        for (List<SeatState> group : coupleGroups.values()) {
            if (group.stream().anyMatch(seat -> seat.unavailable && !seat.selected)) {
                group.forEach(seat -> seat.unavailable = true);
            }
        }
    }

    private static boolean isCouple(SeatDetailDto seat) {
        return "COUPLE".equals(normalize(seat.getSeatType()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static final class SeatState {
        private final SeatDetailDto detail;
        private final boolean selected;
        private boolean unavailable;

        private SeatState(SeatDetailDto detail, boolean selected, boolean unavailable) {
            this.detail = detail;
            this.selected = selected;
            this.unavailable = unavailable;
        }
    }
}
