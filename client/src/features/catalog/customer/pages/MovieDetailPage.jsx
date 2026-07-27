import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle, Building2, CalendarDays, Clock3, Film, Globe2, MapPin, Play, RefreshCw, Users, X
} from 'lucide-react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { getBookingOptions, getMovieById } from '@/features/catalog/customer/services/movieService';
import {
  addCalendarDays,
  formatLocalClock,
  formatServiceDate,
  isFutureBookableShowtime,
  seatSelectionPath,
  vietnamDateKey
} from '@/features/catalog/customer/utils/customerMovieFlow';
import { formatDuration, formatGenres, getYoutubeEmbedUrl } from '@/utils/formatters';

const FALLBACK_POSTER = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='500' height='750'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop stop-color='%2327272a'/><stop offset='1' stop-color='%2309090b'/></linearGradient></defs><rect width='100%25' height='100%25' fill='url(%23g)'/><text x='50%25' y='47%25' text-anchor='middle' fill='%23ff7a00' font-family='sans-serif' font-size='28' font-weight='700'>LoraFilm</text><text x='50%25' y='53%25' text-anchor='middle' fill='%23a1a1aa' font-family='sans-serif' font-size='15'>Không có ảnh bìa</text></svg>";

const surface = 'rounded-3xl border border-white/10 bg-zinc-900/80 shadow-2xl shadow-black/20';
const focus = 'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950';
const sortedPeople = people => [...(people || [])].sort(
  (left, right) => (left.displayOrder ?? Number.MAX_SAFE_INTEGER) - (right.displayOrder ?? Number.MAX_SAFE_INTEGER)
);
const peopleNames = (people, includeCharacter = false) => sortedPeople(people)
  .map(person => {
    if (!person.fullName) return null;
    return includeCharacter && person.characterName
      ? `${person.fullName} — ${person.characterName}`
      : person.fullName;
  })
  .filter(Boolean);
const companyNames = companies => (companies || []).map(company => company.name).filter(Boolean);

function CreditLine({ label, values }) {
  if (!values.length) return null;
  return (
    <div className="border-b border-white/10 py-3 last:border-0">
      <dt className="text-xs font-black uppercase tracking-wider text-zinc-500">{label}</dt>
      <dd className="mt-2">
        <ul className="grid gap-2 md:grid-cols-2">
          {values.map((value, index) => (
            <li
              key={`${value}-${index}`}
              className="rounded-lg border border-white/10 bg-zinc-900/70 px-3 py-2 text-sm leading-5 text-zinc-200"
            >
              {value}
            </li>
          ))}
        </ul>
      </dd>
    </div>
  );
}

export default function MovieDetailPage() {
  const { movieId } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const today = useMemo(() => vietnamDateKey(), []);
  const requestedDate = searchParams.get('date');
  const [selectedDate, setSelectedDate] = useState(requestedDate || '');
  const [movie, setMovie] = useState(null);
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showtimeLoading, setShowtimeLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showtimeError, setShowtimeError] = useState(null);
  const [trailer, setTrailer] = useState(null);
  const [currentTimeMs, setCurrentTimeMs] = useState(() => Date.now());
  const availableDates = useMemo(
    () => [...new Set(
      options
        .filter(option => option.serviceDate
          && isFutureBookableShowtime(option, currentTimeMs))
        .map(option => option.serviceDate)
    )].sort(),
    [currentTimeMs, options]
  );

  const loadMovie = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setMovie(await getMovieById(movieId));
    } catch (requestError) {
      setError(requestError?.status === 404
        ? 'Không tìm thấy thông tin phim.'
        : 'Không thể tải thông tin phim.');
    } finally {
      setLoading(false);
    }
  }, [movieId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadMovie();
  }, [loadMovie]);

  const loadOptions = useCallback(async signal => {
    if (!movie?.slug) return;
    setShowtimeLoading(true);
    setShowtimeError(null);
    try {
      setOptions(await getBookingOptions(movie.slug, {
        from: today,
        to: addCalendarDays(today, 13),
        signal
      }));
    } catch (requestError) {
      if (requestError?.name !== 'CanceledError') {
        setOptions([]);
        setShowtimeError('Không thể tải lịch chiếu. Vui lòng thử lại.');
      }
    } finally {
      if (!signal?.aborted) setShowtimeLoading(false);
    }
  }, [movie, today]);

  useEffect(() => {
    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadOptions(controller.signal);
    return () => controller.abort();
  }, [loadOptions]);

  useEffect(() => {
    const timer = window.setInterval(() => setCurrentTimeMs(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!availableDates.length) return;
    const nextDate = availableDates.includes(selectedDate) ? selectedDate : availableDates[0];
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (nextDate !== selectedDate) setSelectedDate(nextDate);
    if (searchParams.get('date') !== nextDate) {
      setSearchParams({ date: nextDate }, { replace: true });
    }
  }, [availableDates, searchParams, selectedDate, setSearchParams]);

  const grouped = useMemo(() => {
    const result = new Map();
    const unique = new Map(options.map(option => [option.showtimePublicId, option]));
    for (const option of unique.values()) {
      if (option.serviceDate !== selectedDate
        || !isFutureBookableShowtime(option, currentTimeMs)) continue;
      if (!result.has(option.cinemaPublicId)) {
        result.set(option.cinemaPublicId, { cinema: option, versions: new Map() });
      }
      const cinema = result.get(option.cinemaPublicId);
      if (!cinema.versions.has(option.movieVersionPublicId)) {
        cinema.versions.set(option.movieVersionPublicId, { version: option, showtimes: [] });
      }
      cinema.versions.get(option.movieVersionPublicId).showtimes.push(option);
    }
    for (const cinema of result.values()) {
      for (const version of cinema.versions.values()) {
        version.showtimes.sort((a, b) => a.localStartTime.localeCompare(b.localStartTime));
      }
    }
    return [...result.values()];
  }, [currentTimeMs, options, selectedDate]);

  const poster = movie?.primaryPoster || movie?.media?.find(item => item.mediaType === 'POSTER')?.url || FALLBACK_POSTER;
  const backdrop = movie?.media?.find(item => item.mediaType === 'BACKDROP')?.url;
  const trailerUrl = movie?.media?.find(item => item.mediaType === 'TRAILER')?.url;

  if (loading) {
    return (
      <main className="min-h-screen bg-zinc-950 px-6 pt-32 text-center text-zinc-400">
        <div className="mx-auto h-12 w-12 animate-spin rounded-full border-2 border-zinc-700 border-t-brand-orange" />
        <p className="mt-5">Đang tải thông tin phim…</p>
      </main>
    );
  }

  if (error || !movie) {
    return (
      <main className="min-h-screen bg-zinc-950 px-6 pt-32 text-center text-zinc-100">
        <AlertCircle className="mx-auto text-red-400" />
        <h1 className="mt-4 text-xl font-bold">{error}</h1>
        <button onClick={loadMovie} className={`mt-6 rounded-full bg-brand-orange px-6 py-3 font-bold text-white hover:bg-orange-600 ${focus}`}>
          <RefreshCw className="mr-2 inline" size={16} /> Thử lại
        </button>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-zinc-950 pb-20 text-zinc-100">
      <section className="relative isolate overflow-hidden border-b border-white/10">
        {backdrop && <img src={backdrop} alt="" className="absolute inset-0 -z-20 h-full w-full object-cover" />}
        <div className="absolute inset-0 -z-10 bg-gradient-to-r from-zinc-950 via-zinc-950/90 to-zinc-950/60" />
        <div className="absolute inset-0 -z-10 bg-gradient-to-t from-zinc-950 via-transparent to-black/50" />
        <div className="mx-auto grid max-w-7xl gap-9 px-6 py-12 md:grid-cols-[260px_1fr] lg:py-16">
          <img
            src={poster}
            alt={`Áp phích ${movie.title}`}
            className="aspect-[2/3] w-full max-w-[260px] justify-self-center rounded-2xl border border-white/10 object-cover shadow-2xl shadow-black/60 md:justify-self-start"
            onError={event => { event.currentTarget.src = FALLBACK_POSTER; }}
          />
          <div className="self-center">
            <p className="text-xs font-black uppercase tracking-[.28em] text-brand-orange">LoraFilm giới thiệu</p>
            <h1 className="mt-3 text-4xl font-black leading-tight text-white md:text-6xl">{movie.title}</h1>
            {movie.originalTitle && <p className="mt-2 text-lg text-zinc-400">{movie.originalTitle}</p>}
            <div className="mt-6 flex flex-wrap items-center gap-3 text-sm text-zinc-300">
              {movie.ageRating && <strong className="rounded-md bg-brand-orange px-2.5 py-1 text-xs text-white">{movie.ageRating}</strong>}
              <span className="flex items-center gap-2"><Clock3 size={16} className="text-brand-orange" />{formatDuration(movie.durationMinutes)}</span>
              <span className="flex items-center gap-2"><CalendarDays size={16} className="text-brand-orange" />{movie.releaseDate}</span>
            </div>
            <p className="mt-5 text-sm font-bold text-amber-400">{formatGenres(movie.genres)}</p>
            <p className="mt-5 max-w-3xl text-base leading-7 text-zinc-300">{movie.synopsis || 'Nội dung phim đang được cập nhật.'}</p>
            {trailerUrl && (
              <button
                onClick={() => setTrailer(getYoutubeEmbedUrl(trailerUrl))}
                className={`mt-7 flex items-center gap-2 rounded-full border border-white/20 bg-white/5 px-6 py-3 font-bold text-white hover:border-brand-orange hover:text-brand-orange ${focus}`}
              >
                <Play size={17} /> Xem trailer
              </button>
            )}
          </div>
        </div>
      </section>

      <section className="mx-auto mt-10 max-w-7xl px-6">
        <div className={`${surface} p-5 md:p-8`}>
          <div className="flex items-start gap-4">
            <div className="rounded-xl border border-brand-orange/30 bg-brand-orange/10 p-2.5 text-brand-orange">
              <Users size={20} />
            </div>
            <div>
              <p className="text-xs font-black uppercase tracking-[.22em] text-brand-orange">Thông tin phim</p>
              <h2 className="mt-2 text-2xl font-black leading-tight text-white">Đội ngũ và thông tin sản xuất</h2>
            </div>
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <dl className="rounded-2xl border border-white/10 bg-zinc-950/50 px-5">
              <CreditLine label="Đạo diễn" values={peopleNames(movie.directors)} />
              <CreditLine label="Diễn viên — vai diễn" values={peopleNames(movie.actors, true)} />
              <CreditLine label="Biên kịch" values={peopleNames(movie.writers)} />
              <CreditLine label="Nhà sản xuất" values={peopleNames(movie.producers)} />
            </dl>

            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-2xl border border-white/10 bg-zinc-950/50 p-4">
                  <p className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-zinc-500">
                    <Globe2 size={15} className="text-brand-orange" /> Quốc gia
                  </p>
                  <p className="mt-2 text-sm font-bold text-zinc-100">{movie.country || 'Đang cập nhật'}</p>
                </div>
                <div className="rounded-2xl border border-white/10 bg-zinc-950/50 p-4">
                  <p className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-zinc-500">
                    <Film size={15} className="text-brand-orange" /> Ngôn ngữ
                  </p>
                  <p className="mt-2 text-sm font-bold text-zinc-100">
                    {[...new Set((movie.versions || []).map(version => version.audioLanguage).filter(Boolean))].join(', ') || 'Đang cập nhật'}
                  </p>
                </div>
              </div>
              {companyNames(movie.productionCompanies).length > 0 && (
                <div className="rounded-2xl border border-white/10 bg-zinc-950/50 p-4">
                  <p className="flex items-center gap-2 text-xs font-black uppercase tracking-wider text-zinc-500">
                    <Building2 size={15} className="text-brand-orange" /> Đơn vị sản xuất
                  </p>
                  <p className="mt-2 text-sm leading-6 text-zinc-200">{companyNames(movie.productionCompanies).join(', ')}</p>
                </div>
              )}
              {(companyNames(movie.distributors).length > 0 || companyNames(movie.studios).length > 0) && (
                <div className="grid gap-3 sm:grid-cols-2">
                  {companyNames(movie.distributors).length > 0 && (
                    <div className="rounded-2xl border border-white/10 bg-zinc-950/50 p-4">
                      <p className="text-xs font-black uppercase tracking-wider text-zinc-500">Phát hành</p>
                      <p className="mt-2 text-sm leading-6 text-zinc-200">{companyNames(movie.distributors).join(', ')}</p>
                    </div>
                  )}
                  {companyNames(movie.studios).length > 0 && (
                    <div className="rounded-2xl border border-white/10 bg-zinc-950/50 p-4">
                      <p className="text-xs font-black uppercase tracking-wider text-zinc-500">Hãng phim</p>
                      <p className="mt-2 text-sm leading-6 text-zinc-200">{companyNames(movie.studios).join(', ')}</p>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto mt-10 max-w-7xl px-6">
        <div className={`${surface} p-5 md:p-8`}>
          <div>
            <p className="text-xs font-black uppercase tracking-[.22em] text-brand-orange">Chọn suất chiếu</p>
            <h2 className="mt-2 text-2xl font-black text-white md:text-3xl">Lịch chiếu và đặt vé</h2>
            <p className="mt-2 text-sm text-zinc-400">Ngày hiển thị là ngày phục vụ chính thức của rạp.</p>
          </div>

          <div className="mt-7 flex gap-3 overflow-x-auto pb-3">
            {availableDates.map((date, index) => (
              <button
                key={date}
                type="button"
                aria-pressed={selectedDate === date}
                onClick={() => {
                  setSelectedDate(date);
                  setSearchParams({ date }, { replace: true });
                }}
                className={`min-w-28 rounded-xl border px-4 py-3 text-left text-sm font-bold transition-colors ${focus} ${
                  selectedDate === date
                    ? 'border-brand-orange bg-brand-orange text-white shadow-lg shadow-brand-orange/20'
                    : 'border-white/10 bg-zinc-950/70 text-zinc-300 hover:border-brand-orange/60 hover:text-white'
                }`}
              >
                <span className="block text-xs uppercase tracking-wide">
                  {date === today ? 'Hôm nay' : index === 1 && availableDates[0] === today ? 'Ngày mai' : formatServiceDate(date)}
                </span>
                {date === today || (index === 1 && availableDates[0] === today) ? (
                  <span className="mt-1 block text-xs font-medium opacity-80">{formatServiceDate(date)}</span>
                ) : null}
              </button>
            ))}
          </div>

          {showtimeLoading ? (
            <p className="py-14 text-center text-zinc-400">Đang tải lịch chiếu…</p>
          ) : showtimeError ? (
            <div className="my-8 rounded-2xl border border-red-500/20 bg-red-950/20 py-10 text-center text-red-300">
              <p>{showtimeError}</p>
              <button onClick={() => loadOptions()} className={`mt-4 rounded-full border border-red-400/40 px-5 py-2 hover:bg-red-950/50 ${focus}`}>Thử lại</button>
            </div>
          ) : !availableDates.length ? (
            <div className="my-8 rounded-2xl border border-dashed border-white/10 bg-black/20 py-14 text-center text-zinc-400">
              <Film className="mx-auto mb-3 text-zinc-600" />
              Phim hiện chưa có suất chiếu mở bán trong phạm vi ngày tìm kiếm.
            </div>
          ) : grouped.length === 0 ? (
            <div className="my-8 rounded-2xl border border-dashed border-white/10 bg-black/20 py-14 text-center text-zinc-400">
              <Film className="mx-auto mb-3 text-zinc-600" />
              Không có suất chiếu trong ngày đã chọn.
            </div>
          ) : (
            <div className="mt-7 space-y-5">
              {grouped.map(({ cinema, versions }) => (
                <article key={cinema.cinemaPublicId} className="rounded-2xl border border-white/10 bg-zinc-950/55 p-5 md:p-6">
                  <h3 className="text-xl font-black text-white">{cinema.cinemaName}</h3>
                  {(cinema.cinemaAddress || cinema.cinemaCity) && (
                    <p className="mt-2 flex items-center gap-2 text-sm text-zinc-400">
                      <MapPin size={15} className="text-brand-orange" />
                      {[cinema.cinemaAddress, cinema.cinemaCity].filter(Boolean).join(', ')}
                    </p>
                  )}
                  <div className="mt-5 divide-y divide-white/10">
                    {[...versions.values()].map(({ version, showtimes }) => (
                      <div key={version.movieVersionPublicId} className="grid gap-4 py-5 first:pt-0 last:pb-0 md:grid-cols-[220px_1fr]">
                        <div>
                          <strong className="text-sm text-amber-400">{version.versionName || version.format}</strong>
                          <p className="mt-1 text-xs leading-5 text-zinc-500">
                            {[version.audioLanguage, version.subtitleLanguage && `Phụ đề ${version.subtitleLanguage}`, version.screenType]
                              .filter(Boolean).join(' · ')}
                          </p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {showtimes.map(showtime => (
                            <button
                              key={showtime.showtimePublicId}
                              onClick={() => navigate(seatSelectionPath(showtime.showtimePublicId))}
                              aria-label={`Chọn suất ${formatLocalClock(showtime.localStartTime)} tại ${cinema.cinemaName}`}
                              className={`rounded-xl border border-white/15 bg-zinc-900 px-5 py-2.5 font-black text-white transition-colors hover:border-brand-orange hover:bg-brand-orange/10 hover:text-brand-orange ${focus}`}
                            >
                              {formatLocalClock(showtime.localStartTime)}
                            </button>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>

      {trailer && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4 backdrop-blur-sm" onClick={() => setTrailer(null)}>
          <div className="relative aspect-video w-full max-w-4xl" onClick={event => event.stopPropagation()}>
            <button onClick={() => setTrailer(null)} aria-label="Đóng trailer" className={`absolute right-3 top-3 z-10 rounded-full bg-black/80 p-2 text-white ${focus}`}><X /></button>
            <iframe src={`${trailer}?autoplay=1`} title={`Trailer ${movie.title}`} className="h-full w-full rounded-2xl border border-white/10" allow="autoplay; encrypted-media" allowFullScreen />
          </div>
        </div>
      )}
    </main>
  );
}
