const normalized = value => String(value || '').toUpperCase();

const seatKey = seat => String(seat?.publicId ?? seat?.id ?? '');

const isUnavailable = seat => (
  seat?.blockedForShowtime === true
  || normalized(seat?.operationalStatus) !== 'ACTIVE'
  || ['HELD', 'BOOKED'].includes(normalized(seat?.reservationStatus))
  || seat?.sellable === false
  || seat?.priced === false
);

/**
 * Returns true when the current selection creates an unsellable, isolated
 * standard seat. positionColumn is used deliberately so a missing column
 * (an aisle) is treated as a row boundary rather than an adjacent seat.
 */
export const hasSingleSeatGap = (seats, selectedSeatIds) => {
  const selectedIds = selectedSeatIds instanceof Set
    ? selectedSeatIds
    : new Set(selectedSeatIds || []);
  const rows = new Map();

  for (const seat of seats || []) {
    const rowKey = `${seat?.positionRow ?? ''}:${seat?.rowLabel ?? ''}`;
    if (!rows.has(rowKey)) rows.set(rowKey, []);
    rows.get(rowKey).push({
      ...seat,
      selected: selectedIds.has(seatKey(seat)),
      unavailable: isUnavailable(seat)
    });
  }

  for (const rowSeats of rows.values()) {
    const coupleGroups = new Map();
    for (const seat of rowSeats) {
      if (normalized(seat.seatType) !== 'COUPLE' || !seat.pairGroup) continue;
      if (!coupleGroups.has(seat.pairGroup)) coupleGroups.set(seat.pairGroup, []);
      coupleGroups.get(seat.pairGroup).push(seat);
    }

    for (const groupSeats of coupleGroups.values()) {
      if (groupSeats.some(seat => seat.unavailable && !seat.selected)) {
        groupSeats.forEach(seat => {
          seat.unavailable = true;
        });
      }
    }

    const seatsByColumn = new Map(
      rowSeats.map(seat => [Number(seat.positionColumn), seat])
    );

    for (const seat of rowSeats) {
      if (
        seat.selected
        || seat.unavailable
        || normalized(seat.seatType) === 'COUPLE'
        || !Number.isFinite(Number(seat.positionColumn))
      ) {
        continue;
      }

      const column = Number(seat.positionColumn);
      const left = seatsByColumn.get(column - 1);
      const right = seatsByColumn.get(column + 1);
      const leftBlocked = !left || left.selected || left.unavailable;
      const rightBlocked = !right || right.selected || right.unavailable;
      const selectionCreatedGap = left?.selected || right?.selected;

      if (leftBlocked && rightBlocked && selectionCreatedGap) return true;
    }
  }

  return false;
};
