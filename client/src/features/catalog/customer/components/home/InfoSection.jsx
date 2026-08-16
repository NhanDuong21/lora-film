import { useCallback, useEffect, useState } from 'react';
import { ArrowRight, Building2, CalendarDays, Loader2, MapPin, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getCinemas, getShowtimes } from '@/features/catalog/customer/services/movieService';
import { getCustomerErrorMessage } from '@/utils/customerErrorMessages';

const CITY_LABELS = {
  'Ho Chi Minh City': 'TP. Hồ Chí Minh',
};

const DISTRICT_LABELS = {
  'Binh Thanh': 'Bình Thạnh',
  'District 7': 'Quận 7',
  'Thu Duc City': 'TP. Thủ Đức',
};

const ADDRESS_LABELS = {
  '208 Nguyen Huu Canh, Vinhomes Central Park': '208 Nguyễn Hữu Cảnh, Vinhomes Central Park',
  '101 Ton Dat Tien, Tan Phu Ward': '101 Tôn Dật Tiên, phường Tân Phú',
  '01 Vo Van Ngan, Linh Chieu Ward': '01 Võ Văn Ngân, phường Linh Chiểu',
};

const pageContent = page => page?.data || page?.content || [];
const localDate = date => [
  date.getFullYear(),
  String(date.getMonth() + 1).padStart(2, '0'),
  String(date.getDate()).padStart(2, '0'),
].join('-');

const cinemaLocationLabel = cinema => [
  DISTRICT_LABELS[cinema?.district] || cinema?.district,
  CITY_LABELS[cinema?.city] || cinema?.city,
].filter(Boolean).join(', ');

const cinemaAddressLabel = cinema => ADDRESS_LABELS[cinema?.address]
  || cinema?.address
  || 'Địa chỉ đang được cập nhật';

export default function InfoSection() {
  const [cinemas, setCinemas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const cinemaPage = await getCinemas({ page: 0, size: 6 });
      const cinemaList = pageContent(cinemaPage).slice(0, 3);
      const today = localDate(new Date());
      const showtimeResults = await Promise.allSettled(
        cinemaList.map(cinema => getShowtimes({
          cinemaSlug: cinema.slug,
          date: today,
          page: 0,
          size: 1,
        })),
      );
      setCinemas(cinemaList.map((cinema, index) => {
        const result = showtimeResults[index];
        if (result.status !== 'fulfilled') return { ...cinema, todayShowtimeCount: null };
        const page = result.value;
        return {
          ...cinema,
          todayShowtimeCount: Number(page?.totalElements ?? pageContent(page).length),
        };
      }));
    } catch (requestError) {
      setError(getCustomerErrorMessage(
        requestError,
        'Không thể tải hệ thống rạp. Vui lòng thử lại.',
      ));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  return (
    <section id="rap" className="w-full border-t border-zinc-900 bg-zinc-950 py-16 text-zinc-100">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header className="flex flex-col gap-3 border-b border-zinc-800 pb-5 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="flex items-center gap-2 text-[11px] font-black uppercase tracking-[0.22em] text-brand-orange">
              <Building2 className="h-4 w-4" /> Hệ thống rạp LoraFilm
            </div>
            <h2 className="mt-2 text-xl font-black uppercase text-white md:text-2xl">
              Rạp LoraFilm gần bạn
            </h2>
            <p className="mt-2 text-sm text-zinc-500">
              Chọn cụm rạp thuận tiện và xem ngay các suất đang mở hôm nay.
            </p>
          </div>
          <Link
            to="/booking"
            className="inline-flex items-center gap-2 text-sm font-bold text-zinc-400 transition-colors hover:text-brand-orange"
          >
            Xem lịch toàn hệ thống <ArrowRight className="h-4 w-4" />
          </Link>
        </header>

        {loading ? (
          <div className="flex min-h-56 items-center justify-center gap-2 text-sm font-bold text-zinc-600">
            <Loader2 className="h-5 w-5 animate-spin" /> Đang tải hệ thống rạp...
          </div>
        ) : error ? (
          <div className="mt-8 flex min-h-48 flex-col items-center justify-center rounded-2xl border border-red-500/20 bg-red-500/[0.04] px-6 text-center">
            <p className="text-sm text-red-300">{error}</p>
            <button
              type="button"
              onClick={() => void load()}
              className="mt-4 inline-flex items-center gap-2 rounded-xl border border-red-500/30 px-4 py-2 text-xs font-black text-red-200 hover:bg-red-500/10"
            >
              <RefreshCw className="h-4 w-4" /> Thử lại
            </button>
          </div>
        ) : (
          <div className="grid gap-6 pt-9 lg:grid-cols-3">
            {cinemas.map((cinema, index) => (
              <article
                key={cinema.publicId || cinema.slug}
                className="group relative overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/45 p-6 transition-all hover:-translate-y-1 hover:border-brand-orange/40 hover:bg-zinc-900/70"
              >
                <span className="absolute right-5 top-4 text-5xl font-black text-white/[0.035]">
                  {String(index + 1).padStart(2, '0')}
                </span>
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-orange/10 text-brand-orange">
                  <MapPin className="h-5 w-5" />
                </div>
                <h3 className="mt-5 pr-8 text-lg font-black text-white">{cinema.name}</h3>
                <p className="mt-2 text-xs font-bold uppercase tracking-wide text-brand-orange">
                  {cinemaLocationLabel(cinema) || 'Khu vực đang cập nhật'}
                </p>
                <p className="mt-3 min-h-10 text-sm leading-5 text-zinc-500">
                  {cinemaAddressLabel(cinema)}
                </p>
                <div className="mt-6 flex items-center gap-2 border-y border-zinc-800 py-4 text-sm font-bold text-zinc-300">
                  <CalendarDays className="h-4 w-4 text-brand-orange" />
                  {cinema.todayShowtimeCount == null
                    ? 'Đang cập nhật lịch hôm nay'
                    : `${cinema.todayShowtimeCount} suất đang mở hôm nay`}
                </div>
                <Link
                  to={`/cinema/${cinema.slug}`}
                  className="mt-5 inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-xl border border-zinc-700 text-sm font-black text-zinc-200 transition-colors group-hover:border-brand-orange group-hover:text-brand-orange"
                >
                  Xem lịch chiếu <ArrowRight className="h-4 w-4" />
                </Link>
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
