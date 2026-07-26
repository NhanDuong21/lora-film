const TYPES = {
  STANDARD: { label: 'Ghế thường', className: 'border-zinc-500 bg-zinc-800 text-zinc-100', order: 10 },
  VIP: { label: 'Ghế VIP', className: 'border-amber-500/80 bg-amber-950 text-amber-200', order: 20 },
  COUPLE: { label: 'Ghế đôi', className: 'border-purple-400/70 bg-purple-950 text-purple-200', order: 30, wide: true },
  SUPPORT: { label: 'Ghế hỗ trợ', className: 'border-2 border-cyan-400/70 bg-cyan-950 text-cyan-100', order: 40 },
  DISABLED: { label: 'Ghế hỗ trợ', className: 'border-2 border-cyan-400/70 bg-cyan-950 text-cyan-100', order: 40 }
};

export const seatTypePresentation = code =>
  TYPES[String(code || '').toUpperCase()] || {
    label: 'Ghế tiêu chuẩn',
    className: 'border-zinc-500 bg-zinc-800 text-zinc-100',
    order: 90
  };

export const seatStatePresentation = seat => {
  if (!seat?.priced || seat?.price == null || Number(seat.price) <= 0) {
    return { className: 'border-red-400/40 bg-zinc-900 text-red-300 opacity-70', reason: 'chưa có giá hợp lệ', sellable: false };
  }
  if (seat.operationalStatus !== 'ACTIVE') {
    return { className: 'border-zinc-700 bg-zinc-900 text-zinc-600 opacity-60', reason: 'không hoạt động', sellable: false };
  }
  if (seat.blockedForShowtime) {
    return { className: 'border-zinc-600 bg-zinc-900 text-zinc-500 opacity-70', reason: 'bị khóa vận hành', sellable: false };
  }
  if (seat.reservationStatus === 'HELD') {
    return {
      className: 'border-orange-500/70 bg-orange-950 text-orange-200 opacity-80',
      reason: 'đang được khách khác giữ',
      sellable: false
    };
  }
  if (seat.reservationStatus === 'BOOKED') {
    return {
      className: 'border-red-500/70 bg-red-950 text-red-200 opacity-80',
      reason: 'đã được đặt',
      sellable: false
    };
  }
  return {
    className: '',
    reason: 'chưa xác nhận tình trạng',
    sellable: Boolean(seat.sellable)
  };
};

export const seatPresentation = seat => {
  const type = seatTypePresentation(seat?.seatType);
  const state = seatStatePresentation(seat);
  return {
    ...type,
    ...state,
    className: `${type.className} ${state.className}`.trim()
  };
};

export const sortSeatLegend = seats => [...new Map(
  (seats || []).map(seat => [seat.seatType, seat])
).values()].sort((a, b) =>
  seatTypePresentation(a.seatType).order - seatTypePresentation(b.seatType).order
);
