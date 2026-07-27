import { io } from 'socket.io-client';

const ACTIVE_RESERVATION_STATUSES = new Set(['HELD', 'BOOKED']);

export const isSeatStaticallySellable = seat =>
  seat?.operationalStatus === 'ACTIVE'
  && !seat?.blockedForShowtime
  && seat?.priced !== false
  && seat?.price != null
  && Number(seat.price) > 0;

export const applySeatAvailabilityUpdates = (seats, updates = []) => {
  const updateBySeatId = new Map(
    updates
      .filter(update => update?.seatPublicId)
      .map(update => [update.seatPublicId, update])
  );

  return (seats || []).map(seat => {
    const update = updateBySeatId.get(seat.publicId);
    if (!update) return seat;

    const occupied = ACTIVE_RESERVATION_STATUSES.has(update.status);
    return {
      ...seat,
      reservationStatus: occupied ? update.status : null,
      reservationExpiresAt: occupied ? (update.expiresAt || null) : null,
      sellable: occupied ? false : isSeatStaticallySellable(seat)
    };
  });
};

export const createSeatAvailabilitySocket = () => {
  const socketUrl = import.meta.env.VITE_BOOKING_SOCKET_URL
    || import.meta.env.VITE_API_BASE_URL
    || globalThis.location?.origin
    || 'http://localhost:8080';

  return io(socketUrl, {
    path: '/socket.io',
    transports: ['websocket'],
    autoConnect: false,
    reconnection: true,
    reconnectionAttempts: 10
  });
};
