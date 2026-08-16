const TYPES = {
  STANDARD: { label: 'Ghế tiêu chuẩn', className: 'border-zinc-500/80 bg-zinc-800/80 text-zinc-100', order: 10 },
  VIP: { label: 'Ghế VIP', className: 'border-amber-600/80 bg-[#2a1906] text-amber-200', order: 20 },
  COUPLE: { label: 'Ghế đôi', className: 'border-purple-400/70 bg-purple-950 text-purple-200', order: 30, wide: true },
  SUPPORT: { label: 'Ghế hỗ trợ tiếp cận', className: 'border-2 border-cyan-400/70 bg-cyan-950 text-cyan-100', order: 40, accessible: true },
  DISABLED: { label: 'Ghế hỗ trợ tiếp cận', className: 'border-2 border-cyan-400/70 bg-cyan-950 text-cyan-100', order: 40, accessible: true }
};

export const seatTypePresentation = code =>
  TYPES[String(code || '').toUpperCase()] || {
    label: 'Ghế tiêu chuẩn',
    className: 'border-zinc-500/80 bg-zinc-800/80 text-zinc-100',
    order: 90
  };

export const seatStatePresentation = seat => {
  if (!seat?.priced || seat?.price == null || Number(seat.price) <= 0) {
    return { state: 'unavailable', className: 'border-zinc-700 opacity-45 grayscale', reason: 'chưa có giá hợp lệ', sellable: false };
  }
  if (seat.operationalStatus !== 'ACTIVE') {
    return { state: 'unavailable', className: 'border-zinc-700 opacity-45 grayscale', reason: 'không hoạt động', sellable: false };
  }
  if (seat.blockedForShowtime) {
    return { state: 'unavailable', className: 'border-zinc-700 opacity-45 grayscale', reason: 'bị khóa vận hành', sellable: false };
  }
  if (seat.reservationStatus === 'HELD') {
    return {
      state: 'held',
      className: 'border-zinc-500/70 opacity-45 grayscale-[0.35]',
      reason: 'đang được khách khác giữ',
      sellable: false
    };
  }
  if (seat.reservationStatus === 'BOOKED') {
    return {
      state: 'booked',
      className: 'border-zinc-700 bg-zinc-900 text-zinc-600 opacity-55 grayscale',
      reason: 'đã bán',
      sellable: false
    };
  }
  return {
    state: 'available',
    className: '',
    reason: 'còn trống',
    sellable: Boolean(seat.sellable)
  };
};

export const seatPresentation = seat => {
  const type = seatTypePresentation(seat?.seatType);
  const state = seatStatePresentation(seat);
  return {
    ...type,
    ...state,
    typeClassName: type.className,
    stateClassName: state.className,
    className: `${type.className} ${state.className}`.trim()
  };
};

export const sortSeatLegend = seats => {
  const byType = new Map();
  for (const seat of seats || []) {
    const current = byType.get(seat.seatType);
    const shouldPreferValidCouple = String(seat.seatType).toUpperCase() === 'COUPLE'
      && seat.pairValid
      && !current?.pairValid;
    if (!current || shouldPreferValidCouple) byType.set(seat.seatType, seat);
  }
  return [...byType.values()].sort((a, b) =>
    seatTypePresentation(a.seatType).order - seatTypePresentation(b.seatType).order
  );
};
