import { CalendarDays, ChevronRight, Film, ImageOff, MapPin } from 'lucide-react';
import {
  formatShowtimeCinemaDate,
  formatShowtimeCinemaTime,
} from '@/features/scheduling/admin/utils/showtimeCinemaDateTime';
import { formatServiceDateKey, getCinemaDateKey } from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import { getShowtimeStatusPresentation } from '@/features/scheduling/admin/utils/schedulingPresentation';

const statusTone = {
  DRAFT: 'border-zinc-700 bg-zinc-800 text-zinc-300',
  OPEN_FOR_BOOKING: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300',
  CLOSED: 'border-amber-500/30 bg-amber-500/10 text-amber-300',
  CANCELLED: 'border-rose-500/30 bg-rose-500/10 text-rose-300',
  FINISHED: 'border-zinc-700 bg-zinc-900 text-zinc-500',
};

const CompactStatus = ({ status }) => (
  <span className={`inline-flex shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-black ${statusTone[status] || statusTone.DRAFT}`}>
    {getShowtimeStatusPresentation(status).label}
  </span>
);

const showtimeSort = (left, right) => (
  new Date(left.startTime).getTime() - new Date(right.startTime).getTime()
  || String(left.showtimePublicId).localeCompare(String(right.showtimePublicId))
);

const getServiceDate = showtime => (
  showtime.serviceDate || getCinemaDateKey(showtime.startTime, showtime.cinema?.timezone)
);

const ShowtimeLine = ({ showtime, onOpenQuickDetail, includeDate = false }) => {
  const timezone = showtime.cinema?.timezone;
  return (
    <button
      type="button"
      onClick={() => onOpenQuickDetail(showtime)}
      aria-label={`Xem nhanh ${showtime.movie?.title || 'suất chiếu'} lúc ${formatShowtimeCinemaTime(showtime.startTime, timezone)}`}
      className="group flex w-full items-center gap-3 rounded-xl border border-zinc-800 bg-zinc-950/55 p-3 text-left transition-colors hover:border-brand-orange/40 hover:bg-zinc-900"
    >
      <div className="w-14 shrink-0">
        <p className="text-base font-black text-white">{formatShowtimeCinemaTime(showtime.startTime, timezone)}</p>
        <p className="mt-0.5 text-[10px] font-bold text-zinc-600">{formatShowtimeCinemaTime(showtime.endTime, timezone)}</p>
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-black text-zinc-100 group-hover:text-brand-orange">{showtime.movie?.title || 'Phim chưa xác định'}</p>
        <p className="mt-1 truncate text-[11px] text-zinc-500">
          {includeDate ? `${formatShowtimeCinemaDate(showtime.startTime, timezone)} · ` : ''}
          {showtime.auditorium?.name || 'Chưa có phòng'} · {showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Chưa có phiên bản'}
        </p>
      </div>
      <CompactStatus status={showtime.status} />
      <ChevronRight className="h-4 w-4 shrink-0 text-zinc-600 group-hover:text-brand-orange" aria-hidden="true" />
    </button>
  );
};

export function ShowtimeDayView({ showtimes = [], onOpenQuickDetail }) {
  const groups = new Map();
  showtimes.forEach(showtime => {
    const serviceDate = getServiceDate(showtime);
    if (!groups.has(serviceDate)) groups.set(serviceDate, new Map());
    const roomKey = showtime.auditorium?.publicId || showtime.auditorium?.name || 'khong-co-phong';
    const rooms = groups.get(serviceDate);
    if (!rooms.has(roomKey)) rooms.set(roomKey, []);
    rooms.get(roomKey).push(showtime);
  });
  const days = Array.from(groups.entries()).sort(([left], [right]) => left.localeCompare(right, 'vi'));

  return (
    <section className="space-y-5" aria-label="Lịch chiếu theo ngày">
      {days.map(([serviceDate, rooms]) => {
        const roomEntries = Array.from(rooms.values());
        const count = roomEntries.reduce((total, items) => total + items.length, 0);
        return (
          <article key={serviceDate} className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/30">
            <header className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-800 bg-zinc-900/70 px-4 py-4 md:px-5">
              <div className="flex items-center gap-3">
                <span className="rounded-xl bg-brand-orange/10 p-2 text-brand-orange"><CalendarDays className="h-5 w-5" aria-hidden="true" /></span>
                <div><h3 className="font-black text-white">{formatServiceDateKey(serviceDate, { weekday: true })}</h3><p className="mt-0.5 text-xs text-zinc-500">{rooms.size} phòng được sử dụng</p></div>
              </div>
              <span className="rounded-full border border-zinc-700 px-3 py-1 text-xs font-black text-zinc-300">{count} suất trong trang này</span>
            </header>
            <div className="grid gap-4 p-4 xl:grid-cols-2">
              {roomEntries.map(items => {
                const sorted = [...items].sort(showtimeSort);
                const room = sorted[0]?.auditorium;
                return (
                  <section key={room?.publicId || room?.name || 'khong-co-phong'} className="rounded-2xl border border-zinc-800 bg-zinc-950/35 p-3" aria-label={room?.name || 'Chưa có phòng'}>
                    <div className="mb-3 flex items-center justify-between gap-3 px-1">
                      <h4 className="flex min-w-0 items-center gap-2 truncate text-sm font-black text-zinc-200"><MapPin className="h-4 w-4 shrink-0 text-blue-300" />{room?.name || 'Chưa có phòng'}</h4>
                      <span className="text-[11px] font-bold text-zinc-600">{sorted.length} suất</span>
                    </div>
                    <div className="space-y-2">{sorted.map(item => <ShowtimeLine key={item.showtimePublicId} showtime={item} onOpenQuickDetail={onOpenQuickDetail} />)}</div>
                  </section>
                );
              })}
            </div>
          </article>
        );
      })}
    </section>
  );
}

export function ShowtimeMovieView({ showtimes = [], movies = [], onOpenQuickDetail }) {
  const catalog = new Map();
  movies.forEach(movie => {
    if (movie.publicId) catalog.set(`id:${movie.publicId}`, movie);
    if (movie.slug) catalog.set(`slug:${movie.slug}`, movie);
  });
  const groups = new Map();
  showtimes.forEach(showtime => {
    const key = showtime.movie?.publicId || showtime.movie?.slug || showtime.movie?.title || showtime.showtimePublicId;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(showtime);
  });
  const movieGroups = Array.from(groups.values()).sort((left, right) => (
    String(left[0]?.movie?.title || '').localeCompare(String(right[0]?.movie?.title || ''), 'vi')
  ));

  return (
    <section className="grid gap-4 lg:grid-cols-2" aria-label="Lịch chiếu theo phim">
      {movieGroups.map(items => {
        const sorted = [...items].sort(showtimeSort);
        const movieSummary = sorted[0]?.movie || {};
        const movie = catalog.get(`id:${movieSummary.publicId}`) || catalog.get(`slug:${movieSummary.slug}`) || movieSummary;
        const poster = movie.primaryPoster || movie.posterUrl || movie.image || '';
        return (
          <article key={movieSummary.publicId || movieSummary.slug || movieSummary.title} className="flex min-h-64 overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/35">
            <div className="relative hidden w-36 shrink-0 overflow-hidden bg-gradient-to-br from-zinc-800 to-zinc-950 sm:block">
              {poster ? <img src={poster} alt={`Poster ${movieSummary.title}`} loading="lazy" decoding="async" className="h-full w-full object-cover" /> : <div className="flex h-full flex-col items-center justify-center gap-2 px-3 text-center text-zinc-600"><ImageOff className="h-7 w-7" /><span className="text-[10px] font-black uppercase">Chưa có poster</span></div>}
              <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/90 to-transparent" aria-hidden="true" />
            </div>
            <div className="min-w-0 flex-1 p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0"><p className="text-[10px] font-black uppercase tracking-[0.18em] text-brand-orange">Phim đang xếp lịch</p><h3 className="mt-1 line-clamp-2 font-black text-white">{movieSummary.title || 'Phim chưa xác định'}</h3></div>
                <span className="shrink-0 rounded-full border border-zinc-700 px-2.5 py-1 text-[11px] font-black text-zinc-300">{sorted.length} suất</span>
              </div>
              <div className="mt-4 max-h-72 space-y-2 overflow-y-auto pr-1">
                {sorted.map(item => <ShowtimeLine key={item.showtimePublicId} showtime={item} onOpenQuickDetail={onOpenQuickDetail} includeDate />)}
              </div>
            </div>
          </article>
        );
      })}
      {movieGroups.length === 0 && <div className="col-span-full flex flex-col items-center gap-3 rounded-2xl border border-zinc-800 p-12 text-zinc-500"><Film className="h-8 w-8" /><p>Chưa có phim trong trang dữ liệu này.</p></div>}
    </section>
  );
}
