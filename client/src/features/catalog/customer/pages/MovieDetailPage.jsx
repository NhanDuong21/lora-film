import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertCircle, Calendar, Clock, Film, Play, RefreshCw, X } from 'lucide-react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { getBookingOptions, getMovieById } from '@/features/catalog/customer/services/movieService';
import {
  addCalendarDays,
  formatLocalClock,
  formatServiceDate,
  seatSelectionPath,
  vietnamDateKey
} from '@/features/catalog/customer/utils/customerMovieFlow';
import { formatDuration, formatGenres, getYoutubeEmbedUrl } from '@/utils/formatters';

const FALLBACK_POSTER = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="500" height="750"><rect width="100%" height="100%" fill="%23e4e4e7"/><text x="50%" y="50%" text-anchor="middle" fill="%2371717a">LoraFilm</text></svg>';

export default function MovieDetailPage() {
  const { movieId } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const today = useMemo(() => vietnamDateKey(), []);
  const dates = useMemo(() => Array.from({ length: 5 }, (_, index) => addCalendarDays(today, index)), [today]);
  const requestedDate = searchParams.get('date');
  const [selectedDate, setSelectedDate] = useState(
    dates.includes(requestedDate) ? requestedDate : dates[0]
  );
  const [movie, setMovie] = useState(null);
  const [options, setOptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showtimeLoading, setShowtimeLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showtimeError, setShowtimeError] = useState(null);
  const [trailer, setTrailer] = useState(null);

  const loadMovie = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setMovie(await getMovieById(movieId));
    } catch (requestError) {
      setError(requestError?.status === 404 ? 'Không tìm thấy thông tin phim.' : 'Không thể tải thông tin phim.');
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
        from: dates[0],
        to: dates[dates.length - 1],
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
  }, [movie, dates]);

  useEffect(() => {
    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadOptions(controller.signal);
    return () => controller.abort();
  }, [loadOptions]);

  const grouped = useMemo(() => {
    const result = new Map();
    const unique = new Map(options.map(option => [option.showtimePublicId, option]));
    for (const option of unique.values()) {
      if (option.serviceDate !== selectedDate || option.status !== 'OPEN_FOR_BOOKING') continue;
      const cinemaKey = option.cinemaPublicId;
      if (!result.has(cinemaKey)) result.set(cinemaKey, { cinema: option, versions: new Map() });
      const versionKey = option.movieVersionPublicId;
      const cinema = result.get(cinemaKey);
      if (!cinema.versions.has(versionKey)) cinema.versions.set(versionKey, { version: option, showtimes: [] });
      cinema.versions.get(versionKey).showtimes.push(option);
    }
    for (const cinema of result.values()) {
      for (const version of cinema.versions.values()) {
        version.showtimes.sort((a, b) => a.localStartTime.localeCompare(b.localStartTime));
      }
    }
    return [...result.values()];
  }, [options, selectedDate]);

  const poster = movie?.primaryPoster || movie?.media?.find(item => item.mediaType === 'POSTER')?.url || FALLBACK_POSTER;
  const backdrop = movie?.media?.find(item => item.mediaType === 'BACKDROP')?.url || poster;
  const trailerUrl = movie?.media?.find(item => item.mediaType === 'TRAILER')?.url;

  if (loading) {
    return <main className="min-h-screen bg-white pt-32 text-center text-zinc-600">Đang tải thông tin phim…</main>;
  }

  if (error || !movie) {
    return (
      <main className="min-h-screen bg-white pt-32 text-center">
        <AlertCircle className="mx-auto text-red-500" />
        <h1 className="mt-4 text-xl font-bold">{error}</h1>
        <button onClick={loadMovie} className="mt-6 rounded-full bg-brand-orange px-6 py-3 text-white">
          <RefreshCw className="mr-2 inline" size={16} /> Thử lại
        </button>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-white pb-16 pt-24 text-zinc-900">
      <section className="relative overflow-hidden">
        <img src={backdrop} alt="" className="absolute inset-0 h-full w-full object-cover opacity-15 blur-sm" />
        <div className="relative mx-auto grid max-w-7xl gap-8 px-6 py-12 md:grid-cols-[280px_1fr]">
          <img src={poster} alt={`Áp phích ${movie.title}`} className="aspect-[2/3] w-full rounded-2xl object-cover shadow-xl" onError={event => { event.currentTarget.src = FALLBACK_POSTER; }} />
          <div className="self-center">
            <h1 className="text-3xl font-black md:text-5xl">{movie.title}</h1>
            {movie.originalTitle && <p className="mt-2 text-lg text-zinc-500">{movie.originalTitle}</p>}
            <div className="mt-5 flex flex-wrap gap-4 text-sm text-zinc-600">
              <span className="flex items-center gap-2"><Clock size={16} />{formatDuration(movie.durationMinutes)}</span>
              <span className="flex items-center gap-2"><Calendar size={16} />{movie.releaseDate}</span>
              {movie.ageRating && <strong className="rounded border px-2 py-1">{movie.ageRating}</strong>}
            </div>
            <p className="mt-5 text-sm font-semibold text-brand-orange">{formatGenres(movie.genres)}</p>
            <p className="mt-5 max-w-3xl leading-7 text-zinc-700">{movie.synopsis || 'Nội dung phim đang được cập nhật.'}</p>
            {trailerUrl && (
              <button onClick={() => setTrailer(getYoutubeEmbedUrl(trailerUrl))} className="mt-6 flex items-center gap-2 rounded-full border border-zinc-900 px-6 py-3 font-bold">
                <Play size={17} /> Xem trailer
              </button>
            )}
          </div>
        </div>
      </section>

      <section className="mx-auto mt-8 max-w-7xl px-6">
        <div className="rounded-3xl border border-zinc-200 bg-zinc-50 p-6 md:p-8">
          <h2 className="text-2xl font-black">Lịch chiếu và đặt vé</h2>
          <div className="mt-6 flex gap-2 overflow-x-auto pb-3">
            {dates.map((date, index) => (
              <button
                key={date}
                type="button"
                aria-pressed={selectedDate === date}
                onClick={() => {
                  setSelectedDate(date);
                  setSearchParams({ date }, { replace: true });
                }}
                className={`min-w-24 rounded-xl border px-4 py-3 text-sm font-bold ${
                  selectedDate === date ? 'border-brand-orange bg-brand-orange text-white' : 'border-zinc-200 bg-white'
                }`}
              >
                {index === 0 ? 'Hôm nay' : index === 1 ? 'Ngày mai' : formatServiceDate(date)}
              </button>
            ))}
          </div>

          {showtimeLoading ? (
            <p className="py-12 text-center text-zinc-500">Đang tải lịch chiếu…</p>
          ) : showtimeError ? (
            <div className="py-12 text-center text-red-600">
              <p>{showtimeError}</p>
              <button onClick={() => loadOptions()} className="mt-4 rounded-full border border-red-500 px-5 py-2">Thử lại</button>
            </div>
          ) : grouped.length === 0 ? (
            <div className="py-12 text-center text-zinc-500">
              <Film className="mx-auto mb-3" />
              Không có suất chiếu trong ngày đã chọn.
            </div>
          ) : (
            <div className="mt-6 space-y-5">
              {grouped.map(({ cinema, versions }) => (
                <article key={cinema.cinemaPublicId} className="rounded-2xl border border-zinc-200 bg-white p-5">
                  <h3 className="text-lg font-black">{cinema.cinemaName}</h3>
                  {(cinema.cinemaAddress || cinema.cinemaCity) && <p className="mt-1 text-sm text-zinc-500">{cinema.cinemaAddress || cinema.cinemaCity}</p>}
                  <div className="mt-5 space-y-4">
                    {[...versions.values()].map(({ version, showtimes }) => (
                      <div key={version.movieVersionPublicId} className="grid gap-3 border-t border-zinc-100 pt-4 md:grid-cols-[180px_1fr]">
                        <strong className="text-sm text-brand-orange">
                          {[version.versionName || version.format, version.subtitleLanguage].filter(Boolean).join(' · ')}
                        </strong>
                        <div className="flex flex-wrap gap-2">
                          {showtimes.map(showtime => (
                            <button
                              key={showtime.showtimePublicId}
                              onClick={() => navigate(seatSelectionPath(showtime.showtimePublicId))}
                              className="rounded-lg border border-zinc-300 px-4 py-2 font-bold hover:border-brand-orange hover:text-brand-orange"
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4" onClick={() => setTrailer(null)}>
          <div className="relative aspect-video w-full max-w-4xl" onClick={event => event.stopPropagation()}>
            <button onClick={() => setTrailer(null)} aria-label="Đóng trailer" className="absolute right-3 top-3 z-10 rounded-full bg-black/70 p-2 text-white"><X /></button>
            <iframe src={`${trailer}?autoplay=1`} title={`Trailer ${movie.title}`} className="h-full w-full rounded-2xl" allow="autoplay; encrypted-media" allowFullScreen />
          </div>
        </div>
      )}
    </main>
  );
}
