import { useCallback, useEffect, useMemo, useState } from 'react';
import { Clock3, CreditCard, TicketCheck, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import {
  BOOKING_CHANGED_EVENT,
  cancelBooking,
  getBookingHistory
} from '../services/bookingService';
import {
  formatHoldTimeLeft,
  getBookingRecoveryState
} from '../utils/bookingRecovery';
import BookingCancellationModal from './BookingCancellationModal';

export default function ActiveBookingRecoveryBanner() {
  const { isAuthenticated, isInitializing, userRole } = useAuth();
  const [pendingBookings, setPendingBookings] = useState([]);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [cancelling, setCancelling] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelError, setCancelError] = useState('');
  const normalizedRole = userRole?.replace(/^ROLE_/, '');
  const canLoadBookings = isAuthenticated
    && !isInitializing
    && normalizedRole === 'CUSTOMER';

  const loadPendingBookings = useCallback(async () => {
    if (!canLoadBookings) {
      setPendingBookings([]);
      return;
    }

    try {
      const page = await getBookingHistory({
        page: 0,
        size: 20,
        status: 'PENDING_PAYMENT',
        sort: 'createdAt,desc'
      });
      setPendingBookings(page?.content || []);
    } catch {
      // The recovery card must never prevent the public homepage from loading.
      setPendingBookings([]);
    }
  }, [canLoadBookings]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadPendingBookings();
    if (!canLoadBookings) return undefined;

    const refreshTimer = window.setInterval(loadPendingBookings, 30_000);
    window.addEventListener(BOOKING_CHANGED_EVENT, loadPendingBookings);
    return () => {
      window.clearInterval(refreshTimer);
      window.removeEventListener(BOOKING_CHANGED_EVENT, loadPendingBookings);
    };
  }, [canLoadBookings, loadPendingBookings]);

  useEffect(() => {
    if (!canLoadBookings) return undefined;
    const countdownTimer = window.setInterval(() => setNowMs(Date.now()), 1000);
    return () => window.clearInterval(countdownTimer);
  }, [canLoadBookings]);

  const activeRecovery = useMemo(() => pendingBookings
    .map(booking => ({
      booking,
      recovery: getBookingRecoveryState(booking, nowMs)
    }))
    .filter(item => item.recovery.canRecover)
    .sort((left, right) =>
      left.recovery.deadlineMs - right.recovery.deadlineMs)[0] || null,
  [nowMs, pendingBookings]);

  const handleCancel = async reason => {
    if (!activeRecovery || cancelling) return;

    const publicId = activeRecovery.booking.publicId || activeRecovery.booking.id;
    setCancelling(true);
    setCancelError('');
    try {
      await cancelBooking(
        publicId,
        reason || 'Khách hàng chủ động hủy giữ ghế từ thẻ khôi phục'
      );
      setCancelModalOpen(false);
      setPendingBookings(current =>
        current.filter(booking => (booking.publicId || booking.id) !== publicId));
    } catch (requestError) {
      setCancelError(
        `Không thể hủy giữ ghế: ${requestError.message || 'Vui lòng thử lại.'}`
      );
    } finally {
      setCancelling(false);
    }
  };

  if (!canLoadBookings || !activeRecovery) return null;

  const { booking, recovery } = activeRecovery;
  const publicId = booking.publicId || booking.id;

  return (
    <>
      <aside
        aria-label="Đơn đặt vé đang giữ ghế"
        className="fixed bottom-4 left-4 right-4 z-40 mx-auto max-w-md rounded-2xl border border-amber-400/30 bg-zinc-950/95 p-4 text-white shadow-2xl shadow-black/60 backdrop-blur-xl sm:left-auto sm:right-6 sm:mx-0 sm:w-[390px]"
      >
      <div className="flex items-start gap-3">
        <div className="rounded-xl bg-brand-orange/15 p-2.5 text-brand-orange">
          <TicketCheck className="h-5 w-5" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-[10px] font-black uppercase tracking-[.18em] text-brand-orange">
            Bạn đang giữ ghế
          </p>
          <div className="mt-1 flex items-center justify-between gap-3">
            <p className="truncate text-sm font-black text-white">
              Đơn {booking.bookingCode || publicId}
            </p>
            <span className="flex shrink-0 items-center gap-1 text-sm font-black text-amber-300">
              <Clock3 className="h-4 w-4" />
              {formatHoldTimeLeft(recovery.remainingSeconds)}
            </span>
          </div>
          <p className="mt-1 text-xs leading-5 text-zinc-400">
            Hoàn tất thanh toán trước khi thời gian giữ ghế kết thúc.
          </p>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2">
        <Link
          to={`/bookings/checkout?bookingId=${encodeURIComponent(publicId)}`}
          className="flex items-center justify-center gap-1.5 rounded-xl bg-brand-orange px-3 py-2.5 text-[10px] font-black uppercase tracking-wider text-white transition-colors hover:bg-orange-600"
        >
          <CreditCard className="h-4 w-4" />
          Tiếp tục thanh toán
        </Link>
        <button
          type="button"
          disabled={cancelling}
          onClick={() => {
            setCancelError('');
            setCancelModalOpen(true);
          }}
          className="flex items-center justify-center gap-1.5 rounded-xl border border-red-500/25 px-3 py-2.5 text-[10px] font-black uppercase tracking-wider text-red-400 transition-colors hover:bg-red-500/10 disabled:cursor-wait disabled:opacity-60"
        >
          <Trash2 className="h-4 w-4" />
          {cancelling ? 'Đang hủy...' : 'Hủy giữ ghế'}
        </button>
      </div>
      </aside>

      {cancelModalOpen && (
        <BookingCancellationModal
          bookingCode={booking.bookingCode}
          error={cancelError}
          pending={cancelling}
          onClose={() => {
            setCancelError('');
            setCancelModalOpen(false);
          }}
          onConfirm={handleCancel}
        />
      )}
    </>
  );
}
