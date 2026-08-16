import { useCallback, useEffect, useMemo, useState } from "react";
import { Clock3, CreditCard, TicketCheck, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import {
  BOOKING_CHANGED_EVENT,
  cancelBooking,
  getBookingHistory,
} from "../services/bookingService";
import {
  formatHoldTimeLeft,
  getBookingRecoveryState,
} from "../utils/bookingRecovery";
import BookingCancellationModal from "./BookingCancellationModal";
import { getBookingErrorMessage } from "../utils/bookingErrorMessages";

export default function ActiveBookingRecoveryBanner() {
  const { isAuthenticated, isInitializing, userRole } = useAuth();
  const [pendingBookings, setPendingBookings] = useState([]);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [cancelling, setCancelling] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelError, setCancelError] = useState("");
  const [expanded, setExpanded] = useState(true);
  const normalizedRole = userRole?.replace(/^ROLE_/, "");
  const canLoadBookings =
    isAuthenticated && !isInitializing && normalizedRole === "CUSTOMER";

  const loadPendingBookings = useCallback(async () => {
    if (!canLoadBookings) {
      setPendingBookings([]);
      return;
    }

    try {
      const page = await getBookingHistory({
        page: 0,
        size: 20,
        status: "PENDING_PAYMENT",
        sort: "createdAt,desc",
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

  const activeRecovery = useMemo(
    () =>
      pendingBookings
        .map((booking) => ({
          booking,
          recovery: getBookingRecoveryState(booking, nowMs),
        }))
        .filter((item) => item.recovery.canRecover)
        .sort(
          (left, right) => left.recovery.deadlineMs - right.recovery.deadlineMs,
        )[0] || null,
    [nowMs, pendingBookings],
  );

  const activePublicId =
    activeRecovery?.booking?.publicId || activeRecovery?.booking?.id || "";

  useEffect(() => {
    if (!activePublicId) return undefined;
    const collapseTimer = window.setTimeout(() => setExpanded(false), 7_000);
    return () => window.clearTimeout(collapseTimer);
  }, [activePublicId]);

  const handleCancel = async () => {
    if (!activeRecovery || cancelling) return;

    const publicId =
      activeRecovery.booking.publicId || activeRecovery.booking.id;
    setCancelling(true);
    setCancelError("");
    try {
      await cancelBooking(
        publicId,
        "Khách hàng chủ động hủy giữ ghế từ thẻ khôi phục",
      );
      setCancelModalOpen(false);
      setPendingBookings((current) =>
        current.filter(
          (booking) => (booking.publicId || booking.id) !== publicId,
        ),
      );
    } catch (requestError) {
      setCancelError(
        getBookingErrorMessage(
          requestError,
          "Không thể hủy giữ ghế. Vui lòng thử lại.",
        ),
      );
    } finally {
      setCancelling(false);
    }
  };

  if (!canLoadBookings || !activeRecovery) return null;

  const { booking, recovery } = activeRecovery;
  const publicId = booking.publicId || booking.id;
  const presentation = booking.presentation || booking.snapshot || {};
  const seats = Array.isArray(presentation.seats) ? presentation.seats : [];
  const seatNames =
    booking.seatNames ||
    seats
      .map((seat) => seat.label)
      .filter(Boolean)
      .join(", ");
  const seatCount =
    seats.length ||
    String(seatNames || "")
      .split(",")
      .filter(Boolean).length;
  const movieTitle =
    booking.movieTitle ||
    presentation.movieTitle ||
    "Đơn đặt vé đang chờ thanh toán";
  const cinemaName = booking.cinemaName || presentation.cinemaName || "";
  const showtimeStart = booking.showtimeStart || presentation.showtimeStart;
  const showtimeLabel = showtimeStart
    ? new Date(showtimeStart).toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      })
    : "";
  const paymentStatus = String(booking.paymentStatus || "").toUpperCase();
  const actionLabel = ["PENDING", "PROCESSING"].includes(paymentStatus)
    ? "Kiểm tra thanh toán"
    : booking.amountLockedAt
      ? "Tiếp tục thanh toán"
      : "Tiếp tục đặt vé";
  const recoveryHref = `/bookings/checkout?bookingId=${encodeURIComponent(publicId)}`;

  return (
    <>
      {expanded ? (
        <aside
          aria-label="Đơn đặt vé đang giữ ghế"
          className="fixed bottom-5 right-6 z-40 hidden w-[400px] rounded-2xl border border-amber-400/30 bg-zinc-950 p-4 text-white shadow-2xl shadow-black/60 sm:block"
        >
          <div className="flex items-start gap-3">
            <div className="rounded-xl bg-brand-orange/15 p-2.5 text-brand-orange">
              <TicketCheck className="h-5 w-5" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center justify-between gap-3">
                <p className="text-[10px] font-black uppercase tracking-[.18em] text-brand-orange">
                  Bạn đang giữ {seatCount || ""} ghế
                </p>
                <span className="flex shrink-0 items-center gap-1 text-sm font-black text-amber-300">
                  <Clock3 className="h-4 w-4" />
                  {formatHoldTimeLeft(recovery.remainingSeconds)}
                </span>
              </div>
              <p className="mt-1 truncate text-sm font-black text-white">
                {movieTitle}
              </p>
              <p className="mt-1 line-clamp-2 text-xs leading-5 text-zinc-400">
                {[seatNames, showtimeLabel, cinemaName]
                  .filter(Boolean)
                  .join(" · ")}
              </p>
              {booking.bookingCode && (
                <p className="mt-1 text-[9px] font-bold text-zinc-600">
                  Mã đơn {booking.bookingCode}
                </p>
              )}
            </div>
          </div>

          <div className="mt-4 grid grid-cols-2 gap-2">
            <Link
              to={recoveryHref}
              className="flex items-center justify-center gap-1.5 rounded-xl bg-brand-orange px-3 py-2.5 text-[10px] font-black uppercase tracking-wider text-white transition-colors hover:bg-orange-600"
            >
              <CreditCard className="h-4 w-4" />
              {actionLabel}
            </Link>
            <button
              type="button"
              disabled={cancelling}
              onClick={() => {
                setCancelError("");
                setCancelModalOpen(true);
              }}
              className="flex items-center justify-center gap-1.5 rounded-xl border border-red-500/25 px-3 py-2.5 text-[10px] font-black uppercase tracking-wider text-red-400 transition-colors hover:bg-red-500/10 disabled:cursor-wait disabled:opacity-60"
            >
              <Trash2 className="h-4 w-4" />
              {cancelling ? "Đang hủy..." : "Hủy giữ ghế"}
            </button>
          </div>
        </aside>
      ) : (
        <button
          type="button"
          aria-label="Mở chi tiết đơn đang giữ ghế"
          onClick={() => setExpanded(true)}
          className="fixed bottom-5 right-6 z-40 hidden items-center gap-3 rounded-full border border-amber-400/30 bg-zinc-950 px-4 py-3 text-left text-white shadow-2xl shadow-black/60 sm:flex"
        >
          <TicketCheck className="h-4 w-4 text-brand-orange" />
          <span className="text-xs font-black">
            Đang giữ {seatCount || ""} ghế
          </span>
          <span className="text-xs font-black text-amber-300">
            {formatHoldTimeLeft(recovery.remainingSeconds)}
          </span>
        </button>
      )}

      <Link
        aria-label={`Tiếp tục đơn đang giữ ${seatCount || ""} ghế`}
        to={recoveryHref}
        className="fixed inset-x-3 bottom-3 z-40 flex items-center gap-3 rounded-2xl border border-amber-400/30 bg-zinc-950 px-4 py-3 text-white shadow-2xl shadow-black/60 sm:hidden"
      >
        <TicketCheck className="h-5 w-5 shrink-0 text-brand-orange" />
        <span className="min-w-0 flex-1 truncate text-xs font-black">
          Đang giữ {seatCount || ""} ghế · {movieTitle}
        </span>
        <span className="shrink-0 text-xs font-black text-amber-300">
          {formatHoldTimeLeft(recovery.remainingSeconds)}
        </span>
      </Link>

      {cancelModalOpen && (
        <BookingCancellationModal
          bookingCode={booking.bookingCode}
          seatLabels={seatNames}
          error={cancelError}
          pending={cancelling}
          onClose={() => {
            setCancelError("");
            setCancelModalOpen(false);
          }}
          onConfirm={handleCancel}
        />
      )}
    </>
  );
}
