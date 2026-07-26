import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle, ArrowLeft, CalendarDays, Clock3, Film, Info, MapPin, Monitor, ShieldAlert
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
import { createBooking, getBookingHistory, getBookingDetails, getBookingTickets } from '../services/bookingService';
import { getSeatAvailability } from '../services/seatReservationService';
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
  const [toastMessage, setToastMessage] = useState('');
  const [showGapModal, setShowGapModal] = useState(false);
  const [reservationLoading, setReservationLoading] = useState(false);
  const [activeBooking, setActiveBooking] = useState(null);
  const [timeLeft, setTimeLeft] = useState(null);

  const showToast = useCallback((message) => {
    setToastMessage(message);
    setTimeout(() => setToastMessage(''), 4000);
  }, []);

  // Check for active booking drafts when layout is loaded
  useEffect(() => {
    if (!layout || !layout.showtimeId) return;

    let isMounted = true;

    const checkActiveBooking = async () => {
      try {
        const history = await getBookingHistory({ status: 'PENDING_PAYMENT' });
        if (!isMounted) return;

        const match = history?.content?.find(b => b.showtimeId === layout.showtimeId);
        if (match) {
          const details = await getBookingDetails(match.publicId);
          if (!isMounted) return;
          const tickets = await getBookingTickets(match.publicId).catch(() => []);
          if (!isMounted) return;
          setActiveBooking({ ...details, tickets });
        }
      } catch (err) {
        console.error("Failed to check active bookings:", err);
      }
    };

    checkActiveBooking();

    return () => {
      isMounted = false;
    };
  }, [layout]);

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
    setSelectedSeats(previous => previous.filter(seat => !occupiedIds.has(seat.publicId)));
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
        const next = previous.filter(seat => !unavailableIds.has(seat.publicId));
        if (next.length !== previous.length) {
          showToast('Một hoặc nhiều ghế vừa được khách khác giữ. Vui lòng chọn ghế khác.');
        }
        return next;
      });
    };
    const subscribe = () => socket.emit('seat:subscribe', showtimePublicId);

    socket.on('seat:availability-changed', onAvailabilityChanged);
    socket.on('connect', subscribe);
    socket.connect();

    return () => {
      socket.emit('seat:unsubscribe', showtimePublicId);
      socket.off('connect', subscribe);
      socket.off('seat:availability-changed', onAvailabilityChanged);
      socket.disconnect();
    };
  }, [showtimePublicId, showToast]);

  const rows = useMemo(() => {
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

  // Handle seat click selection (with couple seat pairing rules)
  const handleSeatClick = (seat) => {
    if (!seat.sellable || seat.blockedForShowtime || seat.operationalStatus !== 'ACTIVE') return;

    const isSelected = selectedSeats.some(s => s.publicId === seat.publicId);

    // Find if this is a couple seat / has a pair group
    let pairedSeat = null;
    if (seat.pairGroup) {
      pairedSeat = layout?.seats?.find(
        s => s.pairGroup === seat.pairGroup && s.publicId !== seat.publicId
      );
    }

    if (isSelected) {
      // Deselect
      if (pairedSeat) {
        setSelectedSeats(prev => prev.filter(s => s.publicId !== seat.publicId && s.publicId !== pairedSeat.publicId));
      } else {
        setSelectedSeats(prev => prev.filter(s => s.publicId !== seat.publicId));
      }
      setToastMessage('');
    } else {
      // Select
      const seatsToSelect = pairedSeat ? [seat, pairedSeat] : [seat];
      const neededSlots = seatsToSelect.length;

      const maxSeats = Number(layout?.maxSeatsPerBooking || 8);
      if (selectedSeats.length + neededSlots > maxSeats) {
        showToast(`Bạn chỉ được chọn tối đa ${maxSeats} ghế cho mỗi giao dịch!`);
        return;
      }

      setSelectedSeats(prev => [...prev, ...seatsToSelect]);
      setToastMessage('');
    }
  };

  // Check for single seat gaps in rows
  const checkSingleSeatGap = () => {
    for (const [, rowSeats] of rows) {
      const seatsWithSelection = rowSeats.map(seat => {
        let seatStatus = 'AVAILABLE';
        if (seat.blockedForShowtime) {
          seatStatus = 'BOOKED';
        } else if (seat.operationalStatus !== 'ACTIVE') {
          seatStatus = 'MAINTENANCE';
        } else if (selectedSeats.some(s => s.publicId === seat.publicId)) {
          seatStatus = 'SELECTED';
        }
        return { ...seat, seatStatus };
      });

      for (let i = 0; i < seatsWithSelection.length; i++) {
        if (seatsWithSelection[i].seatStatus === 'AVAILABLE') {
          const leftIsBlocked = i === 0 || ['SELECTED', 'BOOKED', 'MAINTENANCE'].includes(seatsWithSelection[i - 1].seatStatus);
          const rightIsBlocked = i === seatsWithSelection.length - 1 || ['SELECTED', 'BOOKED', 'MAINTENANCE'].includes(seatsWithSelection[i + 1].seatStatus);
          
          if (leftIsBlocked && rightIsBlocked) {
            const leftIsSelected = i > 0 && seatsWithSelection[i - 1].seatStatus === 'SELECTED';
            const rightIsSelected = i < seatsWithSelection.length - 1 && seatsWithSelection[i + 1].seatStatus === 'SELECTED';
            if (leftIsSelected || rightIsSelected) {
              return true;
            }
          }
        }
      }
    }
    return false;
  };

  // Handle proceed to food selection & checkout
  const handleContinue = async () => {
    if (selectedSeats.length === 0) return;
    if (checkSingleSeatGap()) {
      setShowGapModal(true);
      return;
    }
    await proceedWithHold();
  };

  const proceedWithHold = async () => {
    setShowGapModal(false);
    setReservationLoading(true);
    try {
      const storageKey = `booking:create:${layout.showtimePublicId}`;
      const idempotencyKey = sessionStorage.getItem(storageKey) || crypto.randomUUID();
      sessionStorage.setItem(storageKey, idempotencyKey);

      const bookingData = await createBooking({
        showtimePublicId: layout.showtimePublicId,
        seatPublicIds: selectedSeats.map(s => s.publicId),
        idempotencyKey
      });

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
      if (seatConflictErrorCodes.has(errorCode)) {
        await refreshAvailability().catch(() => {});
      }
      showToast(getBookingErrorMessage(err, 'Không thể giữ ghế hoặc tạo đơn hàng. Vui lòng thử lại!'));
    } finally {
      setReservationLoading(false);
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
  const legend = sortSeatLegend(layout.seats);
  const totalAmount = selectedSeats.reduce((sum, seat) => sum + (Number(seat.price) || 0), 0);

  return (
    <main className="min-h-screen bg-zinc-950 px-4 pb-28 pt-28 text-zinc-100 sm:px-6">
      <div className="mx-auto max-w-7xl">
        {/* Booking Stepper */}
        <BookingStepper currentStep={2} />

        <button onClick={() => navigate(backPath)} className={`mb-6 flex items-center gap-2 text-sm font-semibold text-zinc-400 hover:text-brand-orange ${focus}`}>
          <ArrowLeft size={16} /> Quay lại phim
        </button>

        {/* Selected Movie Info Summary */}
        <section className="rounded-3xl border border-white/10 bg-gradient-to-br from-zinc-900 to-zinc-950 p-5 shadow-2xl shadow-black/30 md:p-7 mb-8">
          <div className="flex flex-col justify-between gap-6 md:flex-row md:items-center">
            <div className="flex items-start gap-4">
              <div className="rounded-2xl border border-brand-orange/30 bg-brand-orange/10 p-3 text-brand-orange">
                <Film />
              </div>
              <div>
                <p className="text-xs font-black uppercase tracking-[.22em] text-brand-orange">Suất chiếu đã chọn</p>
                <h1 className="mt-1 text-2xl font-black text-white">{layout.movie?.title}</h1>
                <p className="mt-2 text-sm text-zinc-400">
                  {layout.movieVersion?.versionName || layout.movieVersion?.format}
                  {layout.auditorium?.screenType && ` · ${layout.auditorium.screenType}`}
                </p>
              </div>
            </div>
            <dl className="grid gap-3 text-sm sm:grid-cols-3">
              <div className="rounded-xl border border-white/10 bg-black/20 p-3">
                <dt className="flex items-center gap-2 text-xs text-zinc-500"><MapPin size={14} />Rạp</dt>
                <dd className="mt-1 font-bold text-zinc-200">{layout.cinema?.name} · {layout.auditorium?.name}</dd>
              </div>
              <div className="rounded-xl border border-white/10 bg-black/20 p-3">
                <dt className="flex items-center gap-2 text-xs text-zinc-500"><CalendarDays size={14} />Ngày phục vụ</dt>
                <dd className="mt-1 font-bold text-zinc-200">{formatServiceDate(layout.serviceDate)}</dd>
              </div>
              <div className="rounded-xl border border-white/10 bg-black/20 p-3">
                <dt className="flex items-center gap-2 text-xs text-zinc-500"><Clock3 size={14} />Giờ tại rạp</dt>
                <dd className="mt-1 font-bold text-zinc-200">
                  {formatLocalClock(layout.localStartTime)}
                  {layout.localEndTime && ` – ${formatLocalClock(layout.localEndTime)}`}
                </dd>
              </div>
            </dl>
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
                  Bạn đang giữ các ghế: <span className="text-brand-orange">{activeBooking.tickets?.map(t => t.seatLabel || t.seatCode)?.join(', ') || '...'}</span>
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
                onClick={() => navigate(`/bookings/checkout?bookingId=${activeBooking.publicId}`, { state: { showtime: layout } })}
                className="bg-brand-orange hover:bg-orange-600 text-white font-black px-5 py-3 rounded-xl text-xs uppercase tracking-wider transition-all cursor-pointer shadow-lg shadow-brand-orange/20"
              >
                Thanh toán ngay
              </button>
            </div>
          </div>
        )}

        {/* Warning Toast Alerts */}
        {toastMessage && (
          <div className="fixed bottom-28 right-6 z-50 max-w-md bg-red-950/90 border border-red-500/30 text-red-200 px-5 py-4 rounded-2xl flex items-center gap-3 text-sm animate-bounce shadow-2xl backdrop-blur-sm">
            <Info size={18} className="text-red-400 shrink-0" />
            <span>{toastMessage}</span>
          </div>
        )}

        {/* Projector Screen & Seating Map */}
        <section className="overflow-x-auto rounded-3xl border border-white/10 bg-zinc-900/70 p-5 shadow-2xl shadow-black/20 md:p-8 mb-8">
          <div className="mx-auto min-w-[680px]">
            <div className="mx-auto max-w-3xl mb-12">
              <div className="h-2 rounded-[100%] bg-gradient-to-r from-transparent via-brand-orange to-transparent shadow-[0_8px_28px_rgba(255,122,0,0.35)]" />
              <p className="mt-3 flex items-center justify-center gap-2 text-center text-[10px] font-black tracking-[.35em] text-zinc-500">
                <Monitor size={14} /> MÀN HÌNH CHIẾU
              </p>
            </div>

            <div className="mx-auto max-w-5xl space-y-4">
              {rows.map(([label, seats]) => {
                const columnCount = Math.max(16, ...seats.map(seat => (seat.positionColumn ?? 0) + 1));
                return (
                  <div key={label} className="flex items-center gap-3">
                    <span className="sticky left-0 z-10 w-8 rounded bg-zinc-900 py-1 text-center text-xs font-black text-zinc-500">{label}</span>
                    <div
                      className="grid flex-1 gap-2"
                      style={{
                        gridTemplateColumns: `repeat(${columnCount}, minmax(0, 1fr))`
                      }}
                    >
                      {seats.map(seat => {
                        const presentation = seatPresentation(seat);
                        const isSelected = selectedSeats.some(s => s.publicId === seat.publicId);
                        const column = (seat.positionColumn ?? 0) + 1;
                        const accessibleLabel = `Ghế ${seat.seatCode}, ${presentation.label}, ${money(seat.price)}, ${presentation.reason}`;

                        return (
                          <button
                            key={seat.publicId}
                            type="button"
                            onClick={() => handleSeatClick(seat)}
                            disabled={!seat.sellable || seat.blockedForShowtime}
                            aria-label={accessibleLabel}
                            title={accessibleLabel}
                            style={{
                              gridColumnStart: column
                            }}
                            className={`relative h-10 min-w-0 border px-1 text-[10px] font-black shadow-inner transition-all ${
                              presentation.wide ? 'rounded-xl border-2' : 'rounded-t-lg rounded-b-xl'
                            } ${
                              isSelected
                                ? 'bg-brand-orange border-brand-orange text-white shadow-[0_0_12px_rgba(255,122,0,0.4)] scale-105'
                                : presentation.className
                            } ${
                              seat.sellable && !seat.blockedForShowtime
                                ? 'cursor-pointer hover:scale-105'
                                : 'cursor-not-allowed opacity-40'
                            }`}
                          >
                            <span aria-hidden="true">{seat.seatCode}</span>
                            {(seat.blockedForShowtime || seat.operationalStatus !== 'ACTIVE' || !seat.priced) && (
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
          </div>
        </section>

        {/* Seat Category Legend */}
        <section className="rounded-2xl border border-white/10 bg-zinc-900/60 p-5 mb-8" aria-labelledby="seat-legend-title">
          <h2 id="seat-legend-title" className="text-sm font-black uppercase tracking-wider text-zinc-300">Loại ghế và giá suất chiếu</h2>
          <div className="mt-4 flex flex-wrap gap-3">
            {legend.map(seat => {
              const type = seatTypePresentation(seat.seatType);
              return (
                <div key={seat.seatType} className="flex items-center gap-3 rounded-xl border border-white/10 bg-black/20 px-4 py-3">
                  <span className={`h-7 rounded-t-md rounded-b-lg border ${type.wide ? 'w-12' : 'w-7'} ${type.className}`} aria-hidden="true" />
                  <span>
                    <strong className="block text-sm text-zinc-100">{seat.seatTypeName || type.label}</strong>
                    <span className="text-xs text-zinc-400">{money(seat.price)}</span>
                  </span>
                </div>
              );
            })}
          </div>
        </section>

        {/* Selected Summary & Action Footer */}
        <section className="rounded-2xl border border-white/10 bg-zinc-900 p-6 flex flex-col md:flex-row justify-between items-center gap-6">
          <div className="space-y-2 text-center md:text-left">
            <span className="text-xs text-zinc-500 uppercase tracking-widest font-black">Ghế đã chọn</span>
            <div className="flex flex-wrap justify-center md:justify-start gap-1.5 mt-1">
              {selectedSeats.length > 0 ? (
                selectedSeats.map(s => (
                  <span key={s.publicId} className="bg-brand-orange text-white text-xs font-black px-3 py-1 rounded-lg">
                    {s.seatCode}
                  </span>
                ))
              ) : (
                <span className="text-xs text-zinc-500 italic">Vui lòng chọn vị trí ngồi ưa thích của bạn</span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-6 w-full md:w-auto justify-between md:justify-end">
            <div className="text-right flex items-center gap-4 border-r border-zinc-800 pr-6 mr-2">
              <div className="hidden sm:block">
                <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Tiền Vé</span>
                <span className="text-lg font-bold text-zinc-300">{money(totalAmount)}</span>
              </div>
            </div>
            <div className="text-right">
              <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Tổng thanh toán</span>
              <span className="text-2xl font-black text-brand-orange">{money(totalAmount)}</span>
            </div>

            <button
              disabled={selectedSeats.length === 0 || reservationLoading}
              onClick={handleContinue}
              className={`px-8 py-4 rounded-xl text-xs font-black uppercase tracking-wider transition-all shadow-lg ${
                selectedSeats.length > 0 && !reservationLoading
                  ? 'bg-brand-orange text-white cursor-pointer hover:bg-orange-600 hover:scale-105 shadow-brand-orange/20'
                  : 'bg-zinc-800 text-zinc-500 border border-zinc-700 cursor-not-allowed'
              }`}
            >
              {reservationLoading ? 'Đang đặt chỗ...' : 'Tiếp Tục'}
            </button>
          </div>
        </section>
      </div>

      {/* Single Seat Gap Warning Modal */}
      {showGapModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl max-w-md w-full p-6 space-y-6 shadow-2xl">
            <div className="flex items-start gap-4">
              <div className="rounded-2xl bg-amber-500/10 p-3 text-amber-500 shrink-0">
                <AlertTriangle size={24} />
              </div>
              <div className="space-y-2">
                <h3 className="text-lg font-black text-white">Để trống ghế đơn lẻ</h3>
                <p className="text-sm text-zinc-400 leading-relaxed">
                  Lựa chọn ghế của bạn đang để lại một vị trí ghế đơn lẻ ở hàng ghế. Rạp chiếu phim không khuyến khích việc này do khó bán được các ghế đơn lẻ này.
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
              <button
                onClick={proceedWithHold}
                className="px-5 py-2 text-xs font-black uppercase bg-brand-orange hover:bg-orange-600 text-white rounded-lg transition-all shadow-lg shadow-brand-orange/20"
              >
                Vẫn tiếp tục
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
