import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ArrowLeft, Film, Info } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { getSeatLayout } from '@/features/catalog/customer/services/movieService';
import { formatLocalClock, formatServiceDate } from '@/features/catalog/customer/utils/customerMovieFlow';

const money = value => value == null
  ? 'Chưa có giá'
  : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);

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
    return [...grouped.entries()].map(([label, seats]) => [
      label,
      seats.sort((a, b) => (a.positionColumn ?? 999) - (b.positionColumn ?? 999))
    ]);
  }, [layout]);

  if (loading) {
    return <main className="min-h-screen bg-white pt-32 text-center text-zinc-600">Đang tải sơ đồ ghế…</main>;
  }

  if (error || !layout) {
    return (
      <main className="min-h-screen bg-white pt-32 px-4 text-center">
        <AlertTriangle className="mx-auto mb-4 text-red-500" />
        <h1 className="text-xl font-bold">Không thể tải thông tin suất chiếu</h1>
        <p className="mt-2 text-zinc-600">{error}</p>
        <button onClick={() => navigate('/movies')} className="mt-6 rounded-full bg-brand-orange px-6 py-3 text-white">
          Quay lại danh sách phim
        </button>
      </main>
    );
  }

  const backPath = layout.movie?.slug ? `/movies/${layout.movie.slug}` : '/movies';

  return (
    <main className="min-h-screen bg-white px-4 pb-16 pt-28 text-zinc-900">
      <div className="mx-auto max-w-7xl">
        <button onClick={() => navigate(backPath)} className="mb-6 flex items-center gap-2 text-sm font-semibold text-zinc-600">
          <ArrowLeft size={16} /> Quay lại phim
        </button>

        <section className="mb-6 rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm">
          <div className="flex items-start gap-3">
            <Film className="mt-1 text-brand-orange" />
            <div>
              <h1 className="text-xl font-black">{layout.movie?.title}</h1>
              <p className="mt-1 text-sm text-zinc-600">
                {layout.cinema?.name} · {layout.auditorium?.name} · {layout.movieVersion?.versionName || layout.movieVersion?.format}
              </p>
              <p className="mt-1 text-sm font-semibold">
                {formatServiceDate(layout.serviceDate)} · {formatLocalClock(layout.localStartTime)}
              </p>
            </div>
          </div>
        </section>

        <div className="mb-8 flex gap-3 rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900" role="status">
          <Info className="shrink-0" size={20} />
          <div>
            <strong>Chưa thể xác nhận tình trạng ghế.</strong>
            <p>Sơ đồ dưới đây chỉ thể hiện cấu hình và giá tĩnh. Tính khả dụng cần được Booking Service xác nhận.</p>
          </div>
        </div>

        <section className="overflow-x-auto rounded-3xl border border-zinc-200 bg-zinc-50 p-6">
          <div className="mx-auto mb-12 h-2 min-w-[520px] max-w-2xl rounded-full bg-gradient-to-r from-transparent via-brand-orange to-transparent" />
          <p className="-mt-9 mb-10 text-center text-xs font-bold tracking-[.3em] text-zinc-500">MÀN HÌNH</p>
          <div className="mx-auto min-w-[620px] max-w-4xl space-y-3">
            {rows.map(([label, seats]) => (
              <div key={label} className="flex items-center gap-3">
                <span className="w-8 text-center text-xs font-bold text-zinc-500">{label}</span>
                <div className="grid flex-1 gap-2" style={{ gridTemplateColumns: 'repeat(16, minmax(0, 1fr))' }}>
                  {seats.map(seat => {
                    const disabled = !seat.sellable;
                    const reason = !seat.priced
                      ? 'Ghế này chưa có giá bán hợp lệ'
                      : seat.blockedForShowtime || seat.operationalStatus !== 'ACTIVE'
                        ? 'Ghế không phục vụ'
                        : 'Chưa xác nhận tình trạng';
                    return (
                      <button
                        key={seat.publicId}
                        type="button"
                        disabled
                        style={{ gridColumnStart: (seat.positionColumn ?? 0) + 1 }}
                        aria-label={`Ghế ${seat.seatCode}, ${seat.seatTypeName || seat.seatType}, ${money(seat.price)}. ${reason}`}
                        title={`Ghế ${seat.seatCode} · ${money(seat.price)} · ${reason}`}
                        className={`h-9 rounded-lg border text-[10px] font-bold ${
                          disabled ? 'cursor-not-allowed border-zinc-300 bg-zinc-200 text-zinc-500'
                            : 'border-brand-orange bg-orange-50 text-brand-orange'
                        }`}
                      >
                        {seat.seatCode}
                      </button>
                    );
                  })}
                </div>
                <span className="w-8 text-center text-xs font-bold text-zinc-500">{label}</span>
              </div>
            ))}
          </div>
        </section>

        <div className="mt-6 flex flex-wrap gap-4 text-sm">
          {[...new Map((layout.seats || []).map(seat => [seat.seatType, seat])).values()].map(seat => (
            <span key={seat.seatType} className="rounded-full border border-zinc-200 px-4 py-2">
              {seat.seatTypeName || seat.seatType}: {money(seat.price)}
            </span>
          ))}
        </div>

        <button disabled className="mt-8 w-full rounded-xl bg-zinc-200 py-4 font-bold text-zinc-500">
          Tiếp tục đặt vé — chưa có xác nhận từ Booking Service
        </button>
      </div>
    </main>
  );
}
