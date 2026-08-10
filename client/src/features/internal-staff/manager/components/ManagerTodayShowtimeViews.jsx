import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Film,
  ImageOff,
  LayoutList,
  MapPin,
  PanelsTopLeft,
  RefreshCw,
} from 'lucide-react';
import OperationalShowtimeTimeline from '@/features/scheduling/admin/components/OperationalShowtimeTimeline';
import {
  formatShowtimeCinemaTime,
} from '@/features/scheduling/admin/utils/showtimeCinemaDateTime';
import { formatServiceDateKey } from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import {
  getOperationalShowtimePresentation,
  getOperationalShowtimeStatus,
  isExpiredDraftShowtime,
} from '@/features/scheduling/admin/utils/schedulingPresentation';

const VIEW_MODES = [
  { value: 'DAY', label: 'Theo ngày', icon: CalendarDays },
  { value: 'MOVIE', label: 'Theo phim', icon: Film },
  { value: 'LIST', label: 'Danh sách', icon: LayoutList },
  { value: 'TIMELINE', label: 'Sơ đồ', icon: PanelsTopLeft },
];

const statusTone = {
  DRAFT: 'border-blue-500/25 bg-blue-500/10 text-blue-200',
  EXPIRED_DRAFT: 'border-red-500/25 bg-red-500/10 text-red-200',
  OPEN_FOR_BOOKING: 'border-emerald-500/25 bg-emerald-500/10 text-emerald-200',
  CLOSED: 'border-amber-500/25 bg-amber-500/10 text-amber-200',
  CANCELLED: 'border-red-500/25 bg-red-500/10 text-red-200',
  FINISHED: 'border-white/10 bg-white/5 text-zinc-500',
};

const sortShowtimes = showtimes => [...showtimes].sort((first, second) => (
  new Date(first.startTime).getTime() - new Date(second.startTime).getTime()
));

const groupShowtimes = (showtimes, getKey) => {
  const groups = new Map();
  showtimes.forEach(showtime => {
    const key = getKey(showtime);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(showtime);
  });
  return Array.from(groups.values()).map(sortShowtimes);
};

const ShowtimeStatus = ({ showtime }) => {
  const operationalStatus = getOperationalShowtimeStatus(showtime);
  return (
  <span className={`inline-flex w-fit shrink-0 rounded-full border px-2.5 py-1 text-[11px] font-black ${statusTone[operationalStatus] || statusTone.DRAFT}`}>
    {getOperationalShowtimePresentation(showtime).label}
  </span>
  );
};

const ShowtimeLine = ({ showtime, includeMovie = true, includeRoom = true }) => {
  const timezone = showtime.cinema?.timezone;
  return (
    <article className="grid gap-3 rounded-xl border border-white/10 bg-black/20 p-3 sm:grid-cols-[82px_1fr_auto] sm:items-center">
      <div>
        <p className="flex items-center gap-2 text-lg font-black text-white"><Clock3 size={15} className="text-brand-orange" />{formatShowtimeCinemaTime(showtime.startTime, timezone)}</p>
        <p className="mt-1 pl-6 text-[10px] font-bold text-zinc-600">đến {formatShowtimeCinemaTime(showtime.endTime, timezone)}</p>
      </div>
      <div className="min-w-0">
        {includeMovie && <p className="truncate text-sm font-black text-white">{showtime.movie?.title || 'Chưa có tên phim'}</p>}
        <p className={`truncate text-xs text-zinc-500 ${includeMovie ? 'mt-1' : ''}`}>
          {includeRoom ? `${showtime.auditorium?.name || 'Chưa xếp phòng'} · ` : ''}
          {showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Bản chiếu tiêu chuẩn'}
        </p>
      </div>
      <ShowtimeStatus showtime={showtime} />
    </article>
  );
};

const DayView = ({ showtimes, serviceDate }) => {
  const roomGroups = useMemo(() => groupShowtimes(showtimes, showtime => (
    showtime.auditorium?.publicId || showtime.auditorium?.name || 'khong-co-phong'
  )), [showtimes]);

  return (
    <section className="space-y-4 p-4 md:p-5" aria-label="Lịch cần xử lý theo ngày">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-brand-orange/20 bg-brand-orange/5 p-4">
        <div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-brand-orange/10 text-brand-orange"><CalendarDays size={19} /></span><div><h3 className="font-black text-white">{formatServiceDateKey(serviceDate, { weekday: true })}</h3><p className="mt-1 text-xs text-zinc-500">{roomGroups.length} phòng có suất cần theo dõi</p></div></div>
        <span className="rounded-full border border-white/10 px-3 py-1 text-xs font-black text-zinc-300">{showtimes.length} suất</span>
      </div>
      <div className="grid gap-4 xl:grid-cols-2">
        {roomGroups.map(items => {
          const room = items[0]?.auditorium;
          return (
            <article key={room?.publicId || room?.name || 'khong-co-phong'} className="rounded-2xl border border-white/10 bg-white/[0.02] p-3">
              <header className="mb-3 flex items-center justify-between gap-3 px-1"><h4 className="flex min-w-0 items-center gap-2 truncate text-sm font-black text-zinc-200"><MapPin size={15} className="shrink-0 text-blue-300" />{room?.name || 'Chưa xếp phòng'}</h4><span className="text-[11px] font-bold text-zinc-600">{items.length} suất</span></header>
              <div className="space-y-2">{items.map(showtime => <ShowtimeLine key={showtime.showtimePublicId} showtime={showtime} includeRoom={false} />)}</div>
            </article>
          );
        })}
      </div>
    </section>
  );
};

const MovieView = ({ showtimes }) => {
  const movieGroups = useMemo(() => groupShowtimes(showtimes, showtime => (
    showtime.movie?.publicId || showtime.movie?.slug || showtime.movie?.title || showtime.showtimePublicId
  )).sort((left, right) => String(left[0]?.movie?.title || '').localeCompare(String(right[0]?.movie?.title || ''), 'vi')), [showtimes]);

  return (
    <section className="grid gap-4 p-4 lg:grid-cols-2 md:p-5" aria-label="Lịch cần xử lý theo phim">
      {movieGroups.map(items => {
        const movie = items[0]?.movie;
        const poster = movie?.posterUrl || movie?.primaryPoster || '';
        return (
          <article key={movie?.publicId || movie?.slug || movie?.title || items[0]?.showtimePublicId} className="flex min-h-72 overflow-hidden rounded-2xl border border-white/10 bg-white/[0.02]">
            <div className="relative hidden w-36 shrink-0 overflow-hidden bg-gradient-to-br from-zinc-800 to-zinc-950 sm:block">
              {poster ? <img src={poster} alt={`Poster ${movie?.title || 'phim'}`} loading="lazy" decoding="async" className="h-full w-full object-cover" /> : <div className="flex h-full flex-col items-center justify-center gap-2 px-3 text-center text-zinc-600"><ImageOff size={28} /><span className="text-[10px] font-black uppercase">Chưa có poster</span></div>}
              <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/90 to-transparent" aria-hidden="true" />
            </div>
            <div className="min-w-0 flex-1 p-4">
              <header className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="text-[10px] font-black uppercase tracking-[0.18em] text-brand-orange">Phim chiếu hôm nay</p><h3 className="mt-1 line-clamp-2 font-black text-white">{movie?.title || 'Phim chưa xác định'}</h3></div><span className="shrink-0 rounded-full border border-white/10 px-2.5 py-1 text-[11px] font-black text-zinc-300">{items.length} suất</span></header>
              <div className="mt-4 max-h-80 space-y-2 overflow-y-auto pr-1">{items.map(showtime => <ShowtimeLine key={showtime.showtimePublicId} showtime={showtime} includeMovie={false} />)}</div>
            </div>
          </article>
        );
      })}
    </section>
  );
};

const ListView = ({ showtimes }) => (
  <section className="divide-y divide-white/5" aria-label="Danh sách suất cần xử lý">
    {showtimes.map(showtime => {
      const timezone = showtime.cinema?.timezone;
      return (
        <article key={showtime.showtimePublicId} className="grid gap-3 p-4 md:grid-cols-[120px_1fr_180px_150px] md:items-center md:px-5">
          <div><p className="flex items-center gap-2 text-xl font-black text-white"><Clock3 size={17} className="text-brand-orange" />{formatShowtimeCinemaTime(showtime.startTime, timezone)}</p><p className="mt-1 pl-6 text-[10px] font-bold text-zinc-600">đến {formatShowtimeCinemaTime(showtime.endTime, timezone)}</p></div>
          <div><p className="font-bold text-white">{showtime.movie?.title || 'Chưa có tên phim'}</p><p className="mt-1 text-xs text-zinc-600">{showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Bản chiếu tiêu chuẩn'}</p></div>
          <p className="text-sm font-semibold text-zinc-300">{showtime.auditorium?.name || 'Chưa xếp phòng'}</p>
          <ShowtimeStatus showtime={showtime} />
        </article>
      );
    })}
  </section>
);

export default function ManagerTodayShowtimeViews({
  showtimes = [],
  serviceDate,
  isLoading = false,
  error = '',
  onRetry,
  now = null,
}) {
  const [viewMode, setViewMode] = useState('DAY');
  const [mountedAt] = useState(() => Date.now());
  const comparisonNow = now ?? mountedAt;
  const expiredDraftCount = useMemo(
    () => showtimes.filter(showtime => isExpiredDraftShowtime(showtime, comparisonNow)).length,
    [comparisonNow, showtimes],
  );
  const sortedShowtimes = useMemo(
    () => sortShowtimes(showtimes.filter(showtime => !isExpiredDraftShowtime(showtime, comparisonNow))),
    [comparisonNow, showtimes],
  );
  const draftCount = sortedShowtimes.filter(showtime => showtime.status === 'DRAFT').length;
  const openCount = sortedShowtimes.filter(showtime => showtime.status === 'OPEN_FOR_BOOKING').length;

  return (
    <section className="overflow-hidden rounded-2xl border border-white/10 bg-white/[0.02]" aria-labelledby="manager-today-showtimes-title">
      <header className="flex flex-col gap-4 border-b border-white/10 p-5 xl:flex-row xl:items-center xl:justify-between">
        <div><h2 id="manager-today-showtimes-title" className="font-black">Việc cần xử lý hôm nay</h2><p className="mt-1 text-xs text-zinc-600">Đổi góc nhìn theo ngày, phim, danh sách hoặc sơ đồ phòng chiếu.</p></div>
        <Link to="/manager/showtimes" className="shrink-0 text-xs font-black text-brand-orange hover:underline">Xem toàn bộ lịch</Link>
      </header>

      <div className="flex flex-col gap-3 border-b border-white/10 bg-black/10 p-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap gap-2 text-xs font-bold">
          <span className="rounded-full border border-white/10 px-3 py-1.5 text-zinc-300">{sortedShowtimes.length} suất còn xử lý ngày {formatServiceDateKey(serviceDate)}</span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-blue-500/25 bg-blue-500/10 px-3 py-1.5 text-blue-200"><Clock3 size={13} />{draftCount} đang soạn</span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/25 bg-emerald-500/10 px-3 py-1.5 text-emerald-200"><CheckCircle2 size={13} />{openCount} mở bán</span>
          {expiredDraftCount > 0 && <span className="inline-flex items-center gap-1.5 rounded-full border border-red-500/25 bg-red-500/10 px-3 py-1.5 text-red-200"><AlertTriangle size={13} />{expiredDraftCount} quá giờ đã ẩn</span>}
        </div>
        <div className="flex max-w-full flex-wrap rounded-xl border border-white/10 bg-zinc-950 p-1" role="group" aria-label="Chế độ xem việc cần xử lý hôm nay">
          {VIEW_MODES.map(({ value, label, icon: Icon }) => (
            <button key={value} type="button" aria-pressed={viewMode === value} onClick={() => setViewMode(value)} className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-bold ${viewMode === value ? 'bg-zinc-700 text-white' : 'text-zinc-500 hover:text-zinc-300'}`}>
              <Icon size={15} aria-hidden="true" />{label}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? <p className="p-10 text-center text-sm text-zinc-500">Đang tải lịch chiếu…</p> : error ? <div className="p-10 text-center"><p className="text-sm text-red-300">{error}</p><button type="button" onClick={onRetry} className="mt-3 inline-flex items-center gap-2 text-xs font-bold text-brand-orange"><RefreshCw size={14} /> Tải lại</button></div> : sortedShowtimes.length === 0 ? <p className="p-10 text-center text-sm text-zinc-500">Không còn suất chiếu nào cần xử lý trong hôm nay.</p> : (
        <>
          {viewMode === 'DAY' && <DayView showtimes={sortedShowtimes} serviceDate={serviceDate} />}
          {viewMode === 'MOVIE' && <MovieView showtimes={sortedShowtimes} />}
          {viewMode === 'LIST' && <ListView showtimes={sortedShowtimes} />}
          {viewMode === 'TIMELINE' && <div className="p-4 md:p-5"><OperationalShowtimeTimeline showtimes={sortedShowtimes} requestedDate={serviceDate} readOnly /></div>}
        </>
      )}
    </section>
  );
}
