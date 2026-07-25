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

  const load = useCallback(async signal => {
    if (!showtimePublicId) {
      setError('Thiếu thông tin suất chiếu.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setLayout(await getSeatLayout(showtimePublicId, { signal }));
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
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

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

  return (
    <main className="min-h-screen bg-zinc-950 px-4 pb-28 pt-28 text-zinc-100 sm:px-6">
      <div className="mx-auto max-w-7xl">
        <button onClick={() => navigate(backPath)} className={`mb-6 flex items-center gap-2 text-sm font-semibold text-zinc-400 hover:text-brand-orange ${focus}`}>
          <ArrowLeft size={16} /> Quay lại phim
        </button>

        <section className="rounded-3xl border border-white/10 bg-gradient-to-br from-zinc-900 to-zinc-950 p-5 shadow-2xl shadow-black/30 md:p-7">
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

        <div className="my-7 flex gap-3 rounded-2xl border border-amber-500/25 bg-amber-950/25 p-4 text-sm text-amber-100" role="status">
          <Info className="mt-0.5 shrink-0 text-amber-400" size={20} />
          <div>
            <strong>Chưa thể xác nhận tình trạng ghế</strong>
            <p className="mt-1 leading-6 text-amber-100/75">
              Sơ đồ hiện hiển thị cấu hình phòng và giá theo suất chiếu. Tình trạng ghế trống,
              đang giữ hoặc đã đặt cần được Booking Service xác nhận.
            </p>
          </div>
        </div>

        <section className="overflow-x-auto rounded-3xl border border-white/10 bg-zinc-900/70 p-5 shadow-2xl shadow-black/20 md:p-8">
          <div className="mx-auto min-w-[680px]">
            <div className="mx-auto max-w-3xl">
              <div className="h-2 rounded-[100%] bg-gradient-to-r from-transparent via-brand-orange to-transparent shadow-[0_8px_28px_rgba(255,122,0,0.35)]" />
              <p className="mt-3 flex items-center justify-center gap-2 text-center text-[10px] font-black tracking-[.35em] text-zinc-500">
                <Monitor size={14} /> MÀN HÌNH
              </p>
            </div>

            <div className="mx-auto mt-14 max-w-5xl space-y-4">
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
                      const column = (seat.positionColumn ?? 0) + 1;
                      const accessibleLabel = `Ghế ${seat.seatCode}, ${presentation.label}, ${money(seat.price)}, ${presentation.reason}`;
                      return (
                        <button
                          key={seat.publicId}
                          type="button"
                          disabled
                          aria-disabled="true"
                          aria-label={accessibleLabel}
                          title={accessibleLabel}
                          style={{
                            gridColumnStart: column
                          }}
                          className={`relative h-10 min-w-0 cursor-not-allowed border px-1 text-[10px] font-black shadow-inner ${
                            presentation.wide ? 'rounded-xl border-2' : 'rounded-t-lg rounded-b-xl'
                          } ${presentation.className}`}
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

        <section className="mt-7 rounded-2xl border border-white/10 bg-zinc-900/60 p-5" aria-labelledby="seat-legend-title">
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

        <section className="mt-8 rounded-2xl border border-white/10 bg-zinc-900 p-5 text-center">
          <button disabled aria-disabled="true" className="w-full cursor-not-allowed rounded-xl border border-white/10 bg-zinc-800 py-4 font-black text-zinc-500">
            Chưa thể tiếp tục đặt vé
          </button>
          <p className="mt-3 text-xs text-zinc-500">Tình trạng ghế cần được Booking Service xác nhận.</p>
        </section>
      </div>
    </main>
  );
}
