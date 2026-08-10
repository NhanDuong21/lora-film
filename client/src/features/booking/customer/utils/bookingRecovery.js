export const getBookingStatus = booking =>
  booking?.bookingStatus || booking?.status || null;

export const getBookingDeadline = booking =>
  booking?.expiresAt || booking?.expiredAt || booking?.paymentDeadline || null;

export const getBookingRecoveryState = (booking, now = Date.now()) => {
  const status = getBookingStatus(booking);
  const deadline = getBookingDeadline(booking);
  const deadlineMs = Date.parse(deadline);
  const nowMs = now instanceof Date ? now.getTime() : Number(now);
  const hasValidTimes = Number.isFinite(deadlineMs) && Number.isFinite(nowMs);
  const isPending = status === 'PENDING_PAYMENT';
  const canRecover = isPending && hasValidTimes && deadlineMs > nowMs;

  return {
    status,
    deadline,
    deadlineMs: hasValidTimes ? deadlineMs : null,
    canRecover,
    isExpiredPending: isPending && (!hasValidTimes || deadlineMs <= nowMs),
    remainingSeconds: canRecover ? Math.ceil((deadlineMs - nowMs) / 1000) : 0
  };
};

export const formatHoldTimeLeft = remainingSeconds => {
  const safeSeconds = Math.max(0, Number(remainingSeconds) || 0);
  const minutes = String(Math.floor(safeSeconds / 60)).padStart(2, '0');
  const seconds = String(Math.floor(safeSeconds % 60)).padStart(2, '0');
  return `${minutes}:${seconds}`;
};
