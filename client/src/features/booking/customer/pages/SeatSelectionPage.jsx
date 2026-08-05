import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle, ArrowLeft, CalendarDays, Clock3, Film, MapPin, Monitor, Projector, ShieldAlert
} from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { getSeatLayout } from '@/features/catalog/customer/services/movieService';
import { formatLocalClock, formatServiceDate } from '@/features/catalog/customer/utils/customerMovieFlow';
import {
  seatPresentation,
  seatTypePresentation,
  sortSeatLegend
} from '@/features/booking/customer/utils/seatPresentation';
import {
  applySeatAvailabilityUpdates,
  createSeatAvailabilitySocket
} from '../services/seatAvailabilitySocket';
import {
  getBookingErrorCode,
  getBookingErrorMessage,
  seatConflictErrorCodes
} from '../utils/bookingErrorMessages';
import {
  clearBookingCreationAttempt,
  getOrCreateBookingCreationKey
} from '../utils/bookingCreationIdempotency';
import {
  buildSeatUnits,
  removeUnavailableSeatUnits
} from '../utils/seatUnits';
import { hasSingleSeatGap } from '../utils/seatGapPolicy';
import {
  cancelBooking,
  createBooking,
  getActiveBookingForShowtime,
  getBookingDetails
} from '../services/bookingService';
import { getSeatAvailability } from '../services/seatReservationService';
import ActiveBookingConflictModal from '../components/ActiveBookingConflictModal';
import BookingNoticeModal from '../components/BookingNoticeModal';
import BookingStepper from '../components/BookingStepper';

const money = value => value == null
  ? 'Chưa có giá'
  : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

const focus = 'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950';

export default function SeatSelectionPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const showtimePublicId = useMemo(
    () => new URLSearchParams(location.search).get('showtimeId'),
    [location.search]
  );
  const [layout, setLayout] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Seat Selection States
  const [selectedSeats, setSelectedSeats] = useState([]);
  const [notice, setNotice] = useState(null);
  const [showGapModal, setShowGapModal] = useState(false);
  const [reservationLoading, setReservationLoading] = useState(false);
  const [activeBooking, setActiveBooking] = useState(null);
  const [timeLeft, setTimeLeft] = useState(null);
  const [activeConflictOpen, setActiveConflictOpen] = useState(false);
  const [activeConflictError, setActiveConflictError] = useState(null);
  const [cancellingActiveBooking, setCancellingActiveBooking] = useState(false);

  const showNotice = useCallback((message, {
    title = 'Không thể tiếp tục',
    variant = 'warning'
  } = {}) => {
    setNotice({ message, title, variant });
  }, []);

  const loadActiveBooking = useCallback(async targetShowtimePublicId => {
    if (!targetShowtimePublicId) return null;
    const active = await getActiveBookingForShowtime(targetShowtimePublicId);
    if (!active) {
      setActiveBooking(null);
      return null;
    }
    const details = await getBookingDetails(active.publicId);
    const normalized = {
      ...active,
      ...details,
      paymentDeadline: details.paymentDeadline || active.expiredAt
    };
    setActiveBooking(normalized);
    return normalized;
  }, []);

  // Read the server-authoritative active Booking instead of searching one page of history.
  useEffect(() => {
    let isMounted = true;
    if (!layout?.showtimePublicId) return undefined;

    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadActiveBooking(layout.showtimePublicId).catch(errorValue => {
      if (isMounted) {
        console.error('Không thể kiểm tra đơn đang giữ ghế:', errorValue);
      }
    });
    return () => {
      isMounted = false;
    };
  }, [layout?.showtimePublicId, loadActiveBooking]);

  // Expiration countdown logic for active reservation
  useEffect(() => {
    if (!activeBooking || !activeBooking.paymentDeadline) return;

    const calculateTimeLeft = () => {
      const diff = new Date(activeBooking.paymentDeadline) - new Date();
      if (diff <= 0) {
        setTimeLeft(0);
        return;
      }
      setTimeLeft(Math.floor(diff / 1000));
    };

    calculateTimeLeft();
    const interval = setInterval(calculateTimeLeft, 1000);

    return () => clearInterval(interval);
  }, [activeBooking]);

  useEffect(() => {
    if (timeLeft === 0) {
      // The countdown owns cleanup when the persisted deadline is reached.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setActiveBooking(null);
      setTimeLeft(null);
    }
  }, [timeLeft]);

  const formattedTimeLeft = useMemo(() => {
    if (timeLeft === null) return null;
    const mins = String(Math.floor(timeLeft / 60)).padStart(2, '0');
    const secs = String(timeLeft % 60).padStart(2, '0');
    return `${mins}:${secs}`;
  }, [timeLeft]);

  const load = useCallback(async signal => {
    if (!showtimePublicId) {
      setError('Thiếu thông tin suất chiếu.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [data, availability] = await Promise.all([
        getSeatLayout(showtimePublicId, { signal }),
        getSeatAvailability(showtimePublicId, { signal })
      ]);
      const occupiedByPublicId = new Map(
        (availability?.occupiedSeats || []).map(item => [item.seatPublicId, item])
      );
      setLayout({
        ...data,
        maxSeatsPerBooking: Number(availability?.maxSeatsPerBooking || 8),
        seats: (data?.seats || []).map(seat => {
          const reservation = occupiedByPublicId.get(seat.publicId);
          return reservation
            ? {
              ...seat,
              reservationStatus: reservation.status,
              reservationExpiresAt: reservation.expiresAt,
              sellable: false
            }
            : seat;
        })
      });
    } catch (requestError) {
      if (requestError?.name !== 'CanceledError') {
        setError('Suất chiếu này không còn mở bán hoặc không thể tải sơ đồ ghế.');
      }
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [showtimePublicId]);

  useEffect(() => {
    const controller = new AbortController();
    // Fetch both the static Movie layout and Booking's DB availability overlay.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const refreshAvailability = useCallback(async () => {
    if (!showtimePublicId) return;
    const availability = await getSeatAvailability(showtimePublicId);
    const occupiedIds = new Set(
      (availability?.occupiedSeats || [])
        .filter(item => item?.seatPublicId)
        .map(item => item.seatPublicId)
    );
    setLayout(previous => previous
      ? {
        ...previous,
        maxSeatsPerBooking: Number(
          availability?.maxSeatsPerBooking || previous.maxSeatsPerBooking || 8
        ),
        seats: applySeatAvailabilityUpdates(
          previous.seats,
          availability?.occupiedSeats || []
        )
      }
      : previous);
    setSelectedSeats(previous => removeUnavailableSeatUnits(previous, occupiedIds));
  }, [showtimePublicId]);

  useEffect(() => {
    if (!showtimePublicId) return undefined;

    const socket = createSeatAvailabilitySocket();
    const onAvailabilityChanged = event => {
      if (event?.showtimePublicId !== showtimePublicId) return;
      const updates = event?.seats || [];
      const unavailableIds = new Set(
        updates
          .filter(update => ['HELD', 'BOOKED'].includes(update?.status))
          .map(update => update.seatPublicId)
      );
      setLayout(previous => previous
        ? { ...previous, seats: applySeatAvailabilityUpdates(previous.seats, updates) }
        : previous);
      setSelectedSeats(previous => {
        const next = removeUnavailableSeatUnits(previous, unavailableIds);
        if (next.length !== previous.length) {
          showNotice(
            'Một hoặc nhiều ghế vừa được khách khác giữ. Sơ đồ ghế đã được cập nhật, vui lòng chọn ghế khác.',
            { title: 'Ghế vừa có người giữ' }
          );
        }
        return next;
      });
    };
    const subscribe = () => socket.emit(
      'seat:subscribe',
      showtimePublicId,
      acknowledgement => {
        if (acknowledgement?.ok) {
          // Recover any committed changes missed while the socket was
          // disconnected before continuing with incremental events.
          refreshAvailability().catch(() => {
            // The initial layout remains usable; the next reconnect or
            // mutation conflict will refresh authoritative availability.
          });
        }
      }
    );

    socket.on('seat:availability-changed', onAvailabilityChanged);
    socket.on('connect', subscribe);
    socket.connect();

    return () => {
      socket.emit('seat:unsubscribe', showtimePublicId);
      socket.off('connect', subscribe);
      socket.off('seat:availability-changed', onAvailabilityChanged);
      socket.disconnect();
    };
  }, [refreshAvailability, showtimePublicId, showNotice]);

  const seatRows = useMemo(() => {
    const grouped = new Map();
    for (const seat of layout?.seats || []) {
      const key = seat.rowLabel || `Hàng ${seat.positionRow ?? '?'}`;
      if (!grouped.has(key)) grouped.set(key, []);
      grouped.get(key).push(seat);
    }
    return [...grouped.entries()]
      .sort(([, left], [, right]) => (left[0]?.positionRow ?? 999) - (right[0]?.positionRow ?? 999))
      .map(([label, seats]) => [
        label,
        seats.sort((a, b) => (a.positionColumn ?? 999) - (b.positionColumn ?? 999))
      ]);
  }, [layout]);

  const rows = useMemo(
    () => seatRows.map(([label, seats]) => [label, buildSeatUnits(seats)]),
    [seatRows]
  );

  // Calculate the real occupied span for every row. The previous layout forced
  // every row into 16 columns, which left unused columns on the right and made
  // shorter rows appear shifted to the left of the projector screen.
  const seatGridMetrics = useMemo(() => {
    const rowMetrics = new Map();
    let columnCount = 1;

    rows.forEach(([label, seatUnits]) => {
      const occupiedColumns = seatUnits.map(seatUnit => {
        const start = Number(seatUnit.positionColumn ?? 0);
        const span = Math.max(1, Number(seatUnit.columnSpan ?? 1));
        return {
          start: Number.isFinite(start) ? start : 0,
          end: (Number.isFinite(start) ? start : 0) + span - 1
        };
      });

      const firstColumn = occupiedColumns.length > 0
        ? Math.min(...occupiedColumns.map(column => column.start))
        : 0;
      const lastColumn = occupiedColumns.length > 0
        ? Math.max(...occupiedColumns.map(column => column.end))
        : 0;
      const span = Math.max(1, lastColumn - firstColumn + 1);

      rowMetrics.set(label, { firstColumn, span });
      columnCount = Math.max(columnCount, span);
    });

    return { columnCount, rowMetrics };
  }, [rows]);

  const selectedSeatUnits = useMemo(
    () => buildSeatUnits(selectedSeats),
    [selectedSeats]
  );

  // A couple seat is one visual control backed by two authoritative seat IDs.
  const handleSeatClick = (seatUnit) => {
    if (
      !seatUnit.sellable
      || seatUnit.blockedForShowtime
      || seatUnit.operationalStatus !== 'ACTIVE'
      || !seatUnit.pairValid
    ) return;

    const unitSeatIds = new Set(seatUnit.seats.map(seat => seat.publicId));
    const isSelected = seatUnit.seats.every(seat =>
      selectedSeats.some(selected => selected.publicId === seat.publicId)
    );

    if (isSelected) {
      setSelectedSeats(previous =>
        previous.filter(seat => !unitSeatIds.has(seat.publicId))
      );
    } else {
      const seatsToAdd = seatUnit.seats.filter(seat =>
        !selectedSeats.some(selected => selected.publicId === seat.publicId)
      );
      const maxSeats = Number(layout?.maxSeatsPerBooking || 8);
      if (selectedSeats.length + seatsToAdd.length > maxSeats) {
        showNotice(
          `Bạn chỉ được chọn tối đa ${maxSeats} ghế cho mỗi giao dịch.`,
          { title: 'Đã đạt giới hạn số ghế' }
        );
        return;
      }

      setSelectedSeats(previous => [...previous, ...seatsToAdd]);
    }
  };

  // Handle proceed to food selection & checkout
  const handleContinue = async () => {
    if (selectedSeats.length === 0) return;
    if (activeBooking && timeLeft > 0) {
      setActiveConflictError(null);
      setActiveConflictOpen(true);
      return;
    }
    if (hasSingleSeatGap(
      layout?.seats,
      new Set(selectedSeats.map(seat => String(seat.publicId)))
    )) {
      setShowGapModal(true);
      return;
    }
    await proceedWithHold();
  };

  const proceedWithHold = async () => {
    setShowGapModal(false);
    setReservationLoading(true);
    try {
      const seatPublicIds = selectedSeats.map(seat => seat.publicId);
      const idempotencyKey = getOrCreateBookingCreationKey({
        showtimePublicId: layout.showtimePublicId,
        seatPublicIds
      });

      const bookingData = await createBooking({
        showtimePublicId: layout.showtimePublicId,
        seatPublicIds,
        idempotencyKey
      });
      clearBookingCreationAttempt(layout.showtimePublicId);

      // Navigate to checkout/F&B page
      navigate(`/bookings/checkout?bookingId=${bookingData.publicId}`, {
        state: {
          showtime: {
            showtimePublicId: layout.showtimePublicId,
            movieTitle: layout.movie?.title,
            moviePoster: layout.movie?.primaryPoster,
            cinemaName: layout.cinema?.name,
            auditoriumName: layout.auditorium?.name,
            startTime: layout.startTime,
            endTime: layout.endTime,
            seatCount: selectedSeats.length,
            duration: layout.movie?.durationMinutes
          },
          selectedSeats: selectedSeats.map(s => ({
            id: s.id,
            publicId: s.publicId,
            seatCode: s.seatCode,
            seatType: s.seatType,
            price: s.price
          }))
        }
      });
    } catch (err) {
      const errorCode = getBookingErrorCode(err);
      if (errorCode === 'BOOKING_ACTIVE_SHOWTIME_EXISTS') {
        clearBookingCreationAttempt(layout.showtimePublicId);
        try {
          const existingBooking = await loadActiveBooking(layout.showtimePublicId);
          if (existingBooking) {
            setActiveConflictError(null);
            setActiveConflictOpen(true);
            return;
          }
        } catch {
          // Fall through to the translated modal when the recovery read also fails.
        }
      }
      if (seatConflictErrorCodes.has(errorCode)) {
        await refreshAvailability().catch(() => {});
      }
      if (
        errorCode === 'IDEMPOTENCY_PAYLOAD_CONFLICT'
        || errorCode === 'BOOKING_IDEMPOTENCY_PAYLOAD_CONFLICT'
      ) {
        clearBookingCreationAttempt(layout.showtimePublicId);
      }
      showNotice(
        getBookingErrorMessage(
          err,
          'Không thể giữ ghế hoặc tạo đơn hàng. Vui lòng thử lại.'
        ),
        {
          title: (
            errorCode === 'IDEMPOTENCY_PAYLOAD_CONFLICT'
            || errorCode === 'BOOKING_IDEMPOTENCY_PAYLOAD_CONFLICT'
          )
            ? 'Phiên đặt vé đã thay đổi'
            : 'Không thể giữ ghế',
          variant: 'error'
        }
      );
    } finally {
      setReservationLoading(false);
    }
  };

  const resumeActiveBooking = () => {
    if (!activeBooking?.publicId) return;
    setActiveConflictOpen(false);
    navigate(`/bookings/checkout?bookingId=${activeBooking.publicId}`, {
      state: { showtime: layout }
    });
  };

  const cancelActiveBookingAndChooseAgain = async () => {
    if (!activeBooking?.publicId || cancellingActiveBooking) return;
    setCancellingActiveBooking(true);
    setActiveConflictError(null);
    try {
      await cancelBooking(activeBooking.publicId);
      clearBookingCreationAttempt(layout.showtimePublicId);
      setActiveBooking(null);
      setTimeLeft(null);
      setActiveConflictOpen(false);
      await refreshAvailability();
    } catch (cancelError) {
      setActiveConflictError(getBookingErrorMessage(
        cancelError,
        'Không thể hủy đơn đang giữ ghế. Vui lòng thử lại.'
      ));
    } finally {
      setCancellingActiveBooking(false);
    }
  };

  if (loading) {
    return (
      <main className="min-h-screen bg-zinc-950 px-6 pt-32 text-center text-zinc-400">
        <div className="mx-auto h-12 w-12 animate-spin rounded-full border-2 border-zinc-700 border-t-brand-orange" />
        <p className="mt-5">Đang tải sơ đồ ghế…</p>
      </main>
    );
  }

  if (error || !layout) {
    return (
      <main className="min-h-screen bg-zinc-950 px-4 pt-32 text-center text-zinc-100">
        <AlertTriangle className="mx-auto mb-4 text-red-400" />
        <h1 className="text-xl font-bold">Không thể tải thông tin suất chiếu</h1>
        <p className="mt-2 text-zinc-400">{error}</p>
        <button onClick={() => navigate('/movies')} className={`mt-6 rounded-full bg-brand-orange px-6 py-3 font-bold text-white hover:bg-orange-600 ${focus}`}>
          Quay lại danh sách phim
        </button>
      </main>
    );
  }

  const backPath = layout.movie?.slug ? `/movies/${layout.movie.slug}` : '/movies';
  const legend = sortSeatLegend(rows.flatMap(([, seatUnits]) => seatUnits));
  const totalAmount = selectedSeats.reduce((sum, seat) => sum + (Number(seat.price) || 0), 0);

  return (
    <main className="min-h-screen bg-zinc-950 px-4 pb-40 pt-6 text-zinc-100 sm:px-6">
      <div className="mx-auto max-w-7xl">
        {/* Booking Stepper */}
        <BookingStepper currentStep={2} />

        <section className="mb-5 rounded-2xl border border-white/10 bg-zinc-900/80 p-4 shadow-xl">
          <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
            <div className="flex min-w-0 items-center gap-3">
              <div className="rounded-xl border border-brand-orange/30 bg-brand-orange/10 p-2.5 text-brand-orange">
                <Film className="h-5 w-5" />
              </div>
              <div className="min-w-0">
                <p className="text-[10px] font-black uppercase tracking-[.18em] text-brand-orange">
                  Suất chiếu đã chọn
                </p>
                <h1 className="truncate text-lg font-black text-white">{layout.movie?.title}</h1>
              </div>
            </div>

            <dl className="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-zinc-300">
              <div className="flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-zinc-500" />
                <dd className="font-bold">{layout.cinema?.name} · {layout.auditorium?.name}</dd>
              </div>
              <div className="flex items-center gap-1.5">
                <CalendarDays className="h-3.5 w-3.5 text-zinc-500" />
                <dd className="font-bold">{formatServiceDate(layout.serviceDate)}</dd>
              </div>
              <div className="flex items-center gap-1.5">
                <Clock3 className="h-3.5 w-3.5 text-zinc-500" />
                <dd className="font-bold text-white">
                  {formatLocalClock(layout.localStartTime)}
                  {layout.localEndTime && ` – ${formatLocalClock(layout.localEndTime)}`}
                </dd>
              </div>
            </dl>

            <button
              onClick={() => navigate(backPath)}
              className={`flex shrink-0 items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-bold text-zinc-300 transition-colors hover:border-brand-orange/50 hover:text-brand-orange ${focus}`}
            >
              <ArrowLeft size={14} /> Đổi suất chiếu
            </button>
          </div>
        </section>

        {/* Active Booking Banner */}
        {activeBooking && timeLeft > 0 && (
          <div className="mb-8 bg-zinc-900 border border-brand-orange/30 p-5 rounded-3xl flex flex-col sm:flex-row justify-between items-center gap-4 shadow-xl">
            <div className="flex items-center gap-3">
              <div className="rounded-2xl bg-brand-orange/10 p-3 text-brand-orange animate-pulse">
                <Clock3 className="w-6 h-6" />
              </div>
              <div>
                <p className="text-xs font-black uppercase tracking-wider text-brand-orange">Đơn hàng đang xử lý</p>
                <h3 className="mt-1 text-sm font-bold text-white">
                  Bạn đang giữ các ghế: <span className="text-brand-orange">{activeBooking.seatNames || 'Đang cập nhật'}</span>
                </h3>
                <p className="text-xs text-zinc-400 mt-1">Vui lòng thanh toán hoặc hủy vé cũ trước khi đặt thêm ghế mới.</p>
              </div>
            </div>
            <div className="flex items-center gap-4 w-full sm:w-auto justify-between sm:justify-end">
              <div className="text-right">
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Thời gian còn lại</span>
                <span className="text-xl font-black text-brand-orange tracking-widest">{formattedTimeLeft}</span>
              </div>
              <button
                onClick={resumeActiveBooking}
                className="bg-brand-orange hover:bg-orange-600 text-white font-black px-5 py-3 rounded-xl text-xs uppercase tracking-wider transition-all cursor-pointer shadow-lg shadow-brand-orange/20"
              >
                Thanh toán ngay
              </button>
            </div>
          </div>
        )}

        <section className="mb-4 rounded-2xl border border-white/10 bg-zinc-900/60 p-4" aria-labelledby="seat-legend-title">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <h2 id="seat-legend-title" className="text-sm font-black text-white">Chọn ghế ngồi</h2>
              <p className="mt-1 text-xs text-zinc-500">Chọn tối đa {layout.maxSeatsPerBooking || 8} ghế · Vuốt ngang để xem toàn bộ sơ đồ</p>
            </div>
            <div className="flex flex-wrap gap-2">
              {legend.map(seat => {
                const type = seatTypePresentation(seat.seatType);
                return (
                  <div key={seat.seatType} className="flex items-center gap-2 rounded-lg border border-white/10 bg-black/20 px-2.5 py-2">
                    <span className={`h-5 rounded border ${type.wide ? 'w-8' : 'w-5'} ${type.className}`} aria-hidden="true" />
                    <span className="text-[10px] font-bold text-zinc-300">
                      {seat.seatTypeName || type.label} · {money(seat.price)}
                    </span>
                  </div>
                );
              })}
              <div className="flex items-center gap-2 rounded-lg border border-white/10 bg-black/20 px-2.5 py-2">
                <span className="h-5 w-5 rounded border-2 border-brand-orange bg-white" aria-hidden="true" />
                <span className="text-[10px] font-bold text-zinc-300">Đang chọn</span>
              </div>
              <div className="flex items-center gap-2 rounded-lg border border-white/10 bg-black/20 px-2.5 py-2">
                <span className="h-5 w-5 rounded border border-zinc-700 bg-zinc-900 opacity-60" aria-hidden="true" />
                <span className="text-[10px] font-bold text-zinc-300">Không khả dụng</span>
              </div>
            </div>
          </div>
        </section>

        {/* Projector Screen & Seating Map */}
        <section className="mb-8 overflow-x-auto rounded-3xl border border-white/10 bg-zinc-900/70 p-4 shadow-2xl shadow-black/20 md:p-6">
          <div className="relative mx-auto min-w-[680px]">
            <div
              aria-hidden="true"
              className="pointer-events-none absolute inset-x-0 bottom-16 top-8 z-0 overflow-hidden opacity-80"
            >
              <div
                className="absolute inset-0 animate-pulse bg-gradient-to-t from-transparent via-amber-400/[0.025] to-orange-300/[0.14] blur-[1px]"
                style={{
                  clipPath: 'polygon(18% 0%, 82% 0%, 55% 100%, 45% 100%)'
                }}
              />
              <div
                className="absolute left-1/2 top-0 h-full w-px -translate-x-1/2 bg-gradient-to-b from-orange-300/30 via-brand-orange/10 to-transparent"
              />
            </div>

            <div className="mx-auto max-w-3xl mb-12">
              <div className="h-2 rounded-[100%] bg-gradient-to-r from-transparent via-brand-orange to-transparent shadow-[0_8px_28px_rgba(255,122,0,0.35)]" />
              <p className="mt-3 flex items-center justify-center gap-2 text-center text-[10px] font-black tracking-[.35em] text-zinc-500">
                <Monitor size={14} /> MÀN HÌNH CHIẾU
              </p>
            </div>

            <div className="mx-auto max-w-4xl space-y-4">
              {rows.map(([label, seatUnits]) => {
                const rowMetric = seatGridMetrics.rowMetrics.get(label) || {
                  firstColumn: 0,
                  span: seatGridMetrics.columnCount
                };
                const rowOffset = Math.floor(
                  (seatGridMetrics.columnCount - rowMetric.span) / 2
                );
                return (
                  <div key={label} className="flex items-center gap-3">
                    <span className="sticky left-0 z-10 w-8 rounded bg-zinc-900 py-1 text-center text-xs font-black text-zinc-500">{label}</span>
                    <div
                      className="grid w-full max-w-3xl flex-1 gap-2"
                      style={{
                        gridTemplateColumns: `repeat(${seatGridMetrics.columnCount}, minmax(0, 1fr))`
                      }}
                    >
                      {seatUnits.map(seatUnit => {
                        const presentation = seatPresentation(seatUnit);
                        const isSelected = seatUnit.seats.every(seat =>
                          selectedSeats.some(selected => selected.publicId === seat.publicId)
                        );
                        const column = rowOffset
                          + Number(seatUnit.positionColumn ?? 0)
                          - rowMetric.firstColumn
                          + 1;
                        const reason = seatUnit.pairValid
                          ? presentation.reason
                          : 'cấu hình ghế đôi không hợp lệ';
                        const accessibleLabel = `${seatUnit.isCouple ? 'Ghế đôi' : 'Ghế'} ${seatUnit.seatCode}, ${presentation.label}, ${money(seatUnit.price)}, ${reason}`;

                        return (
                          <button
                            key={seatUnit.key}
                            type="button"
                            onClick={() => handleSeatClick(seatUnit)}
                            disabled={!seatUnit.sellable || seatUnit.blockedForShowtime}
                            aria-pressed={isSelected}
                            aria-label={accessibleLabel}
                            title={accessibleLabel}
                            style={{
                              gridColumnStart: column,
                              gridColumnEnd: `span ${seatUnit.columnSpan || 1}`
                            }}
                            className={`relative h-10 min-w-0 border px-1 text-[10px] font-black shadow-inner transition-all ${
                              presentation.wide ? 'rounded-xl border-2' : 'rounded-t-lg rounded-b-xl'
                            } ${
                              isSelected
                                ? 'border-brand-orange bg-white text-zinc-950 ring-2 ring-brand-orange/80 shadow-[0_0_14px_rgba(255,122,0,0.45)] scale-105'
                                : presentation.className
                            } ${
                              seatUnit.sellable && !seatUnit.blockedForShowtime
                                ? 'cursor-pointer hover:scale-105'
                                : 'cursor-not-allowed opacity-40'
                            }`}
                          >
                            <span aria-hidden="true">{seatUnit.seatCode}</span>
                            {(seatUnit.blockedForShowtime
                              || seatUnit.operationalStatus !== 'ACTIVE'
                              || !seatUnit.priced
                              || !seatUnit.pairValid) && (
                              <span className="absolute -right-1 -top-1 rounded-full bg-zinc-950 p-0.5 text-red-300" aria-hidden="true">
                                <ShieldAlert size={10} />
                              </span>
                            )}
                          </button>
                        );
                      })}
                    </div>
                    <span className="w-8 text-center text-xs font-black text-zinc-500">{label}</span>
                  </div>
                );
              })}
            </div>

            <div className="relative z-20 mt-10 flex flex-col items-center justify-center pb-2">
              <div className="absolute -top-5 h-20 w-40 rounded-full bg-orange-500/10 blur-2xl animate-pulse" aria-hidden="true" />
              <div className="relative flex items-center gap-2 rounded-2xl border border-orange-400/25 bg-zinc-950/95 px-4 py-2.5 shadow-[0_0_28px_rgba(249,115,22,0.16)]">
                <Projector className="h-5 w-5 text-orange-300" aria-hidden="true" />
                <span className="text-[10px] font-black uppercase tracking-[.22em] text-orange-200">
                  Máy chiếu
                </span>
              </div>
              <span className="mt-2 text-[9px] font-bold uppercase tracking-[.28em] text-zinc-600">
                Hướng về màn hình
              </span>
            </div>
          </div>
        </section>

      </div>

      <section
        aria-label="Ghế đã chọn và tổng tiền"
        className="fixed inset-x-0 bottom-0 z-40 border-t border-zinc-700/80 bg-zinc-950/95 px-4 py-3 shadow-[0_-12px_35px_rgba(0,0,0,0.55)] backdrop-blur-xl sm:px-6"
      >
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4">
          <div className="min-w-0 flex-1">
            <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Ghế đã chọn</span>
            <div className="mt-1 flex min-w-0 items-center gap-1.5 overflow-x-auto">
              {selectedSeatUnits.length > 0 ? selectedSeatUnits.map(seatUnit => (
                <span key={seatUnit.key} className="shrink-0 rounded-lg bg-brand-orange/15 px-2.5 py-1 text-xs font-black text-brand-orange">
                  {seatUnit.seatCode}
                </span>
              )) : (
                <span className="truncate text-xs text-zinc-500">Chọn ghế trên sơ đồ để tiếp tục</span>
              )}
            </div>
          </div>

          <div className="shrink-0 text-right">
            <span className="block text-[10px] font-black uppercase tracking-wider text-zinc-500">Tổng tiền</span>
            <span className="text-lg font-black text-white sm:text-xl">{money(totalAmount)}</span>
          </div>

          <button
            aria-label="Tiếp tục"
            disabled={selectedSeats.length === 0 || reservationLoading}
            onClick={handleContinue}
            className={`shrink-0 rounded-xl px-4 py-3 text-xs font-black uppercase tracking-wider transition-all sm:px-7 ${
              selectedSeats.length > 0 && !reservationLoading
                ? 'cursor-pointer bg-brand-orange text-white shadow-lg shadow-brand-orange/20 hover:bg-orange-600'
                : 'cursor-not-allowed border border-zinc-700 bg-zinc-800 text-zinc-500'
            }`}
          >
            {reservationLoading
              ? 'Đang giữ ghế...'
              : <><span className="sm:hidden">Tiếp tục</span><span className="hidden sm:inline">Tiếp tục · {money(totalAmount)}</span></>}
          </button>
        </div>
      </section>

      {notice && (
        <BookingNoticeModal
          title={notice.title}
          message={notice.message}
          variant={notice.variant}
          onClose={() => setNotice(null)}
        />
      )}

      {activeConflictOpen && activeBooking && (
        <ActiveBookingConflictModal
          bookingCode={activeBooking.bookingCode}
          seatNames={activeBooking.seatNames}
          timeLeft={formattedTimeLeft}
          error={activeConflictError}
          pending={cancellingActiveBooking}
          onClose={() => {
            if (!cancellingActiveBooking) {
              setActiveConflictError(null);
              setActiveConflictOpen(false);
            }
          }}
          onResume={resumeActiveBooking}
          onCancel={cancelActiveBookingAndChooseAgain}
        />
      )}

      {/* Single Seat Gap Blocking Modal */}
      {showGapModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="single-seat-gap-title"
            className="bg-zinc-900 border border-zinc-800 rounded-3xl max-w-md w-full p-6 space-y-6 shadow-2xl"
          >
            <div className="flex items-start gap-4">
              <div className="rounded-2xl bg-amber-500/10 p-3 text-amber-500 shrink-0">
                <AlertTriangle size={24} />
              </div>
              <div className="space-y-2">
                <h3 id="single-seat-gap-title" className="text-lg font-black text-white">
                  Không thể để trống ghế đơn lẻ
                </h3>
                <p className="text-sm text-zinc-400 leading-relaxed">
                  Lựa chọn hiện tại đang để lại một ghế trống đơn lẻ trong hàng.
                  Vui lòng chọn lại ghế để không tạo khoảng trống một ghế.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowGapModal(false)}
                className="px-4 py-2 text-xs font-bold text-zinc-400 hover:text-white rounded-lg transition-colors border border-zinc-800 hover:bg-zinc-800"
              >
                Chọn lại ghế
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
