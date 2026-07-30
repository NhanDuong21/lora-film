const normalizedSeatType = seat => String(seat?.seatType || '').toUpperCase();

const compareSeats = (left, right) => (
  (left?.positionRow ?? Number.MAX_SAFE_INTEGER)
    - (right?.positionRow ?? Number.MAX_SAFE_INTEGER)
  || (left?.positionColumn ?? Number.MAX_SAFE_INTEGER)
    - (right?.positionColumn ?? Number.MAX_SAFE_INTEGER)
  || String(left?.seatCode || '').localeCompare(String(right?.seatCode || ''))
);

const reservationStatus = seats => {
  if (seats.some(seat => seat.reservationStatus === 'BOOKED')) return 'BOOKED';
  if (seats.some(seat => seat.reservationStatus === 'HELD')) return 'HELD';
  return undefined;
};

const isValidCouplePair = seats => (
  seats.length === 2
  && seats.every(seat => normalizedSeatType(seat) === 'COUPLE')
  && seats.every(seat => seat.pairGroup && seat.pairGroup === seats[0].pairGroup)
  && seats.every(seat => seat.rowLabel === seats[0].rowLabel)
  && seats.every(seat => seat.positionRow === seats[0].positionRow)
  && Math.abs(
    (seats[0].positionColumn ?? Number.MAX_SAFE_INTEGER)
      - (seats[1].positionColumn ?? Number.MIN_SAFE_INTEGER)
  ) === 1
);

const toSeatUnit = (seats, { pairValid = true } = {}) => {
  const orderedSeats = [...seats].sort(compareSeats);
  const first = orderedSeats[0];
  const isCouple = normalizedSeatType(first) === 'COUPLE';
  const allPriced = orderedSeats.every(
    seat => seat.priced && seat.price != null && Number(seat.price) > 0
  );
  const price = allPriced
    ? orderedSeats.reduce((sum, seat) => sum + Number(seat.price), 0)
    : null;
  const operationalStatus = orderedSeats.every(seat => seat.operationalStatus === 'ACTIVE')
    ? 'ACTIVE'
    : orderedSeats.find(seat => seat.operationalStatus !== 'ACTIVE')?.operationalStatus;

  return {
    ...first,
    key: isCouple && pairValid
      ? `couple:${first.pairGroup}`
      : `seat:${first.publicId}`,
    seats: orderedSeats,
    isCouple,
    pairValid,
    columnSpan: isCouple && pairValid ? 2 : 1,
    positionColumn: Math.min(...orderedSeats.map(seat => seat.positionColumn ?? 0)),
    seatCode: orderedSeats.map(seat => seat.seatCode).join('–'),
    price,
    priced: allPriced,
    operationalStatus,
    blockedForShowtime: orderedSeats.some(seat => seat.blockedForShowtime),
    reservationStatus: reservationStatus(orderedSeats),
    sellable: pairValid && orderedSeats.every(
      seat => (
        seat.sellable
        && !seat.blockedForShowtime
        && seat.operationalStatus === 'ACTIVE'
      )
    )
  };
};

export const buildSeatUnits = seats => {
  const orderedSeats = [...(seats || [])].sort(compareSeats);
  const coupleGroups = new Map();

  for (const seat of orderedSeats) {
    if (normalizedSeatType(seat) !== 'COUPLE' || !seat.pairGroup) continue;
    if (!coupleGroups.has(seat.pairGroup)) coupleGroups.set(seat.pairGroup, []);
    coupleGroups.get(seat.pairGroup).push(seat);
  }

  const emittedGroups = new Set();
  const units = [];
  for (const seat of orderedSeats) {
    if (normalizedSeatType(seat) !== 'COUPLE') {
      units.push(toSeatUnit([seat]));
      continue;
    }

    const groupedSeats = seat.pairGroup ? coupleGroups.get(seat.pairGroup) || [] : [];
    if (isValidCouplePair(groupedSeats)) {
      if (!emittedGroups.has(seat.pairGroup)) {
        emittedGroups.add(seat.pairGroup);
        units.push(toSeatUnit(groupedSeats));
      }
      continue;
    }

    units.push(toSeatUnit([seat], { pairValid: false }));
  }

  return units.sort((left, right) => (
    (left.positionColumn ?? Number.MAX_SAFE_INTEGER)
      - (right.positionColumn ?? Number.MAX_SAFE_INTEGER)
  ));
};

export const removeUnavailableSeatUnits = (selectedSeats, unavailableIds) => {
  const blockedIds = unavailableIds instanceof Set
    ? unavailableIds
    : new Set(unavailableIds || []);
  const affectedPairGroups = new Set(
    (selectedSeats || [])
      .filter(seat => blockedIds.has(seat.publicId) && seat.pairGroup)
      .map(seat => seat.pairGroup)
  );

  return (selectedSeats || []).filter(
    seat => (
      !blockedIds.has(seat.publicId)
      && !(seat.pairGroup && affectedPairGroups.has(seat.pairGroup))
    )
  );
};
