import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertCircle, Building2, CalendarDays, ChevronDown, ChevronUp, Clock3, Film, Globe2,
  Languages, MapPin, Play, RefreshCw, Ticket, UserRound, Users, X
} from 'lucide-react';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  getBookingOptionsCacheSnapshot,
  getBookingOptionsWithCache,
  getMovieDetailCacheSnapshot,
  getMovieDetailWithCache,
  getRelatedMoviesCacheSnapshot,
  getRelatedMoviesWithCache
} from '@/features/catalog/customer/services/movieDetailCache';
import {
  addCalendarDays,
  formatLocalClock,
  formatServiceDate,
  seatSelectionPath,
  vietnamDateKey
} from '@/features/catalog/customer/utils/customerMovieFlow';
import MovieBannerCrossfade from '@/features/catalog/customer/components/MovieBannerCrossfade';
import { getMovieBannerUrls } from '@/features/catalog/customer/utils/movieBanner';
import { formatDate, formatDuration, getYoutubeEmbedUrl } from '@/utils/formatters';
import { readPreferredCinema, writePreferredCinema } from '@/features/catalog/customer/utils/customerCinemaPreference';
import {
  actorPresentation,
  formatAuditoriumLabel,
  formatCityLabel,
  formatCountryLabel,
  formatGenreLabels,
  formatLanguageLabel,
  formatScreenTypeLabel,
  formatSoundTypeLabel,
  formatVersionLabel,
  getShowtimeSalesState
} from '@/features/catalog/customer/utils/movieDetailPresentation';

const FALLBACK_POSTER = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='500' height='750'><defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'><stop stop-color='%2327272a'/><stop offset='1' stop-color='%2309090b'/></linearGradient></defs><rect width='100%25' height='100%25' fill='url(%23g)'/><text x='50%25' y='47%25' text-anchor='middle' fill='%23ff7a00' font-family='sans-serif' font-size='28' font-weight='700'>LoraFilm</text><text x='50%25' y='53%25' text-anchor='middle' fill='%23a1a1aa' font-family='sans-serif' font-size='15'>Không có ảnh bìa</text></svg>";

const surface = 'rounded-3xl border border-white/10 bg-zinc-900/80 shadow-2xl shadow-black/20';
const focus = 'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950';
const ACTOR_PREVIEW_LIMIT = 8;
const ACTOR_EXPANSION_STORAGE_PREFIX = 'lorafilm:movie-detail:actors:';
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
const bookingWindow = (movie, today) => {
  const from = movie?.releaseDate && movie.releaseDate > today
    ? movie.releaseDate
    : today;
  return { from, to: addCalendarDays(from, 13) };
};
const relatedStatus = movie => movie?.status === 'UPCOMING' ? 'UPCOMING' : 'NOW_SHOWING';
const relatedMovieItems = (page, movie) => (page?.data || page?.content || [])
  .filter(item => item.publicId !== movie?.publicId && item.slug !== movie?.slug)
  .slice(0, 4);
const readActorExpansion = movieId => {
  try {
    return window.sessionStorage.getItem(`${ACTOR_EXPANSION_STORAGE_PREFIX}${movieId}`) === 'true';
  } catch {
    return false;
  }
};
const writeActorExpansion = (movieId, expanded) => {
  try {
    window.sessionStorage.setItem(`${ACTOR_EXPANSION_STORAGE_PREFIX}${movieId}`, String(expanded));
  } catch {
    // Storage can be unavailable in privacy-restricted browser contexts.
  }
};
const formatPrice = (value, currency = 'VND') => value == null
  ? null
  : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currency || 'VND' }).format(value);
const tmdbProfileUrl = (imageUrl, size) => {
  if (!imageUrl || typeof imageUrl !== 'string') return '';
  try {
    const parsed = new URL(imageUrl);
    if (parsed.hostname !== 'image.tmdb.org') return imageUrl;
    parsed.pathname = parsed.pathname.replace(/\/t\/p\/(?:original|w\d+)\//, `/t/p/${size}/`);
    return parsed.toString();
  } catch {
    return imageUrl;
  }
};

const personDetailPath = (person, displayName) => {
  if (!person?.publicId) return null;
  const slug = String(displayName || person.stageName || person.fullName || 'nghe-si')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/gi, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
  return `/nghe-si/${encodeURIComponent(`${slug || 'nghe-si'}-${person.publicId}`)}`;
};

function InfoTile({ icon: Icon, label, value }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-zinc-950/55 p-4">
      <p className="flex items-center gap-2 text-[10px] font-black uppercase tracking-wider text-zinc-500">
        <Icon size={15} className="text-brand-orange" /> {label}
      </p>
      <p className="mt-2 text-sm font-bold leading-6 text-zinc-100">{value || 'Đang cập nhật'}</p>
    </div>
  );
}

function PersonLinks({ people }) {
  return (
    <span className="flex flex-wrap gap-x-1.5 gap-y-1">
      {sortedPeople(people).map((person, index) => {
        const name = person.stageName || person.fullName;
        const path = personDetailPath(person, name);
        return (
          <span key={person.publicId || name}>
            {path ? <Link to={path} className="hover:text-brand-orange hover:underline">{name}</Link> : name}
            {index < people.length - 1 ? ',' : ''}
          </span>
        );
      })}
    </span>
  );
}

function ActorCard({ actor }) {
  const presentation = actorPresentation(actor);
  const initial = presentation.name.trim().charAt(0).toUpperCase() || '?';
  const personPath = personDetailPath(actor, presentation.name);
  const profileImageSrcSet = actor.profileImageUrl
    ? `${tmdbProfileUrl(actor.profileImageUrl, 'w185')} 1x, ${tmdbProfileUrl(actor.profileImageUrl, 'w342')} 2x`
    : undefined;
  return (
    <article className="group relative flex min-w-0 items-center gap-4 rounded-2xl border border-white/10 bg-zinc-950/45 p-4 transition-colors hover:border-brand-orange/30 hover:bg-zinc-950/70">
      {personPath && (
        <Link
          to={personPath}
          aria-label={`Xem hồ sơ ${presentation.name}`}
          className="absolute inset-0 z-10 rounded-2xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
        />
      )}
      <div className="relative grid h-20 w-20 shrink-0 place-items-center overflow-hidden rounded-full bg-gradient-to-br from-brand-orange/35 to-zinc-800 text-xl font-black text-white ring-1 ring-white/15">
        <span aria-hidden="true">{initial}</span>
        {actor.profileImageUrl && (
          <img
            src={actor.profileImageUrl}
            srcSet={profileImageSrcSet}
            alt={`Ảnh chân dung ${presentation.name}`}
            width="80"
            height="80"
            loading="lazy"
            decoding="async"
            className="absolute inset-0 h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
            onError={event => event.currentTarget.remove()}
          />
        )}
      </div>
      <div className="min-w-0 transition-transform group-hover:translate-x-0.5">
        <h3 className="truncate text-base font-black text-white">{presentation.name}</h3>
        <p className="mt-1.5 truncate text-xs text-zinc-400">{presentation.character || 'Vai diễn đang cập nhật'}</p>
        <p className="mt-2 text-[10px] font-black uppercase tracking-wider text-brand-orange">{presentation.role}</p>
      </div>
    </article>
  );
}

function RelatedMovieCard({ movie }) {
  return (
    <Link
      to={`/movies/${encodeURIComponent(movie.slug || movie.publicId)}`}
      className={`group grid min-h-32 grid-cols-[88px_minmax(0,1fr)] overflow-hidden rounded-2xl border border-white/10 bg-zinc-950/45 transition-colors hover:border-brand-orange/60 hover:bg-zinc-950/70 ${focus}`}
    >
      <img
        src={movie.posterUrl || movie.primaryPoster || FALLBACK_POSTER}
        alt={`Áp phích ${movie.title}`}
        loading="lazy"
        decoding="async"
        className="h-full min-h-32 w-full object-cover transition-transform duration-500 group-hover:scale-105"
        onError={event => { event.currentTarget.src = FALLBACK_POSTER; }}
      />
      <div className="flex min-w-0 flex-col justify-center p-4">
        <h3 className="line-clamp-3 text-sm font-black leading-5 text-white group-hover:text-brand-orange">{movie.title}</h3>
        <p className="mt-2 text-xs text-zinc-500">{formatDuration(movie.durationMinutes)} · {movie.ageRating || 'P'}</p>
      </div>
    </Link>
  );
}

const statusToneClass = tone => ({
  success: 'text-emerald-400',
  warning: 'text-amber-300',
  danger: 'text-red-300',
  muted: 'text-zinc-500'
}[tone] || 'text-zinc-500');

export default function MovieDetailPage() {
  const { movieId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const today = useMemo(() => vietnamDateKey(), []);
  const initialMovieSnapshot = useMemo(() => getMovieDetailCacheSnapshot(movieId), [movieId]);
  const initialMovie = initialMovieSnapshot?.data || null;
  const initialBookingRange = initialMovie ? bookingWindow(initialMovie, today) : null;
  const initialBookingSnapshot = initialMovie?.slug
    ? getBookingOptionsCacheSnapshot(initialMovie.slug, initialBookingRange)
    : null;
  const initialRelatedSnapshot = initialMovie
    ? getRelatedMoviesCacheSnapshot(relatedStatus(initialMovie))
    : null;
  const requestedDate = searchParams.get('date');
  const preferredCinema = useMemo(
    () => location.state?.preferredCinema || readPreferredCinema(),
    [location.state]
  );
  const [selectedDate, setSelectedDate] = useState(requestedDate || '');
  const [selectedCity, setSelectedCity] = useState(
    searchParams.get('city') || preferredCinema?.city || 'ALL'
  );
  const [selectedCinema, setSelectedCinema] = useState(
    searchParams.get('cinema') || preferredCinema?.publicId || 'ALL'
  );
  const [movie, setMovie] = useState(initialMovie);
  const [options, setOptions] = useState(initialBookingSnapshot?.data || []);
  const [relatedMovies, setRelatedMovies] = useState(
    initialRelatedSnapshot ? relatedMovieItems(initialRelatedSnapshot.data, initialMovie) : []
  );
  const [loading, setLoading] = useState(!initialMovie);
  const [showtimeLoading, setShowtimeLoading] = useState(Boolean(initialMovie && !initialBookingSnapshot));
  const [error, setError] = useState(null);
  const [showtimeError, setShowtimeError] = useState(null);
  const [trailer, setTrailer] = useState(null);
  const [showAllActors, setShowAllActors] = useState(() => readActorExpansion(movieId));
  const [currentTimeMs, setCurrentTimeMs] = useState(() => Date.now());
  const showtimeSectionRef = useRef(null);
  const moviePreview = location.state?.moviePreview;
  const availableCities = useMemo(() => [...new Set(options
    .map(option => option.cinemaCity)
    .filter(Boolean))]
    .sort((left, right) => formatCityLabel(left).localeCompare(formatCityLabel(right), 'vi')), [options]);
  const effectiveCity = selectedCity === 'ALL' || availableCities.includes(selectedCity)
    ? selectedCity
    : 'ALL';
  const cinemaOptions = useMemo(() => [...new Map(options
    .filter(option => effectiveCity === 'ALL' || option.cinemaCity === effectiveCity)
    .map(option => [option.cinemaPublicId, {
      publicId: option.cinemaPublicId,
      slug: option.cinemaSlug,
      name: option.cinemaName,
      city: option.cinemaCity
    }])).values()]
    .sort((left, right) => left.name.localeCompare(right.name, 'vi')), [effectiveCity, options]);
  const effectiveCinema = selectedCinema === 'ALL'
    || cinemaOptions.some(cinema => cinema.publicId === selectedCinema)
    ? selectedCinema
    : 'ALL';
  const filteredOptions = useMemo(() => options.filter(option => (
    (effectiveCity === 'ALL' || option.cinemaCity === effectiveCity)
    && (effectiveCinema === 'ALL' || option.cinemaPublicId === effectiveCinema)
  )), [effectiveCinema, effectiveCity, options]);
  const availableDates = useMemo(
    () => [...new Set(
      filteredOptions
        .filter(option => option.serviceDate && option.serviceDate >= today)
        .map(option => option.serviceDate)
    )].sort(),
    [filteredOptions, today]
  );

  const loadMovie = useCallback(async () => {
    const cachedMovie = getMovieDetailCacheSnapshot(movieId)?.data || null;
    if (cachedMovie) {
      setMovie(cachedMovie);
      setLoading(false);
    } else {
      setMovie(null);
      setLoading(true);
    }
    setError(null);
    try {
      setMovie(await getMovieDetailWithCache(movieId));
    } catch (requestError) {
      if (requestError?.status === 404 && moviePreview?.title) {
        setMovie({
          ...moviePreview,
          primaryPoster: moviePreview.primaryPoster || moviePreview.posterUrl,
          media: moviePreview.media || [],
          versions: moviePreview.versions || []
        });
      } else if (!cachedMovie) {
        setError(requestError?.status === 404
          ? 'Không tìm thấy thông tin phim.'
          : 'Không thể tải thông tin phim.');
      }
    } finally {
      setLoading(false);
    }
  }, [movieId, moviePreview, setError, setLoading, setMovie]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadMovie();
  }, [loadMovie]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setShowAllActors(readActorExpansion(movieId));
  }, [movieId]);

  useEffect(() => {
    if (!movie) return undefined;
    const controller = new AbortController();
    const loadRelatedMovies = async () => {
      const status = relatedStatus(movie);
      const cachedPage = getRelatedMoviesCacheSnapshot(status)?.data;
      if (cachedPage) setRelatedMovies(relatedMovieItems(cachedPage, movie));
      try {
        const page = await getRelatedMoviesWithCache(status, { signal: controller.signal });
        if (!controller.signal.aborted) {
          setRelatedMovies(relatedMovieItems(page, movie));
        }
      } catch (requestError) {
        if (!cachedPage && requestError?.name !== 'CanceledError' && !controller.signal.aborted) {
          setRelatedMovies([]);
        }
      }
    };
    loadRelatedMovies();
    return () => controller.abort();
  }, [movie]);

  const loadOptions = useCallback(async signal => {
    if (!movie?.slug) return;
    const range = bookingWindow(movie, today);
    const cachedOptions = getBookingOptionsCacheSnapshot(movie.slug, range)?.data;
    if (cachedOptions) {
      setOptions(cachedOptions);
      setShowtimeLoading(false);
    } else {
      setShowtimeLoading(true);
    }
    setShowtimeError(null);
    try {
      setOptions(await getBookingOptionsWithCache(movie.slug, {
        ...range,
        signal
      }));
    } catch (requestError) {
      if (requestError?.name !== 'CanceledError') {
        if (!cachedOptions) {
          setOptions([]);
          setShowtimeError('Không thể tải lịch chiếu. Vui lòng thử lại.');
        }
      }
    } finally {
      if (!signal?.aborted) setShowtimeLoading(false);
    }
  }, [movie, setOptions, setShowtimeError, setShowtimeLoading, today]);

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

  const updateBookingParams = useCallback(updates => {
    const nextParams = new URLSearchParams(searchParams);
    Object.entries(updates).forEach(([key, value]) => {
      if (value == null || value === '' || value === 'ALL') nextParams.delete(key);
      else nextParams.set(key, value);
    });
    setSearchParams(nextParams, { replace: true });
  }, [searchParams, setSearchParams]);

  useEffect(() => {
    if (!availableDates.length) return;
    const nextDate = availableDates.includes(selectedDate) ? selectedDate : availableDates[0];
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (nextDate !== selectedDate) setSelectedDate(nextDate);
    if (searchParams.get('date') !== nextDate) {
      updateBookingParams({ date: nextDate });
    }
  }, [availableDates, searchParams, selectedDate, updateBookingParams]);

  const grouped = useMemo(() => {
    const result = new Map();
    const unique = new Map(filteredOptions.map(option => [option.showtimePublicId, option]));
    for (const option of unique.values()) {
      if (option.serviceDate !== selectedDate) continue;
      if (!result.has(option.cinemaPublicId)) {
        result.set(option.cinemaPublicId, { cinema: option, auditoriums: new Map() });
      }
      const cinema = result.get(option.cinemaPublicId);
      if (!cinema.auditoriums.has(option.auditoriumPublicId)) {
        cinema.auditoriums.set(option.auditoriumPublicId, {
          auditorium: option,
          versions: new Map()
        });
      }
      const auditorium = cinema.auditoriums.get(option.auditoriumPublicId);
      if (!auditorium.versions.has(option.movieVersionPublicId)) {
        auditorium.versions.set(option.movieVersionPublicId, { version: option, showtimes: [] });
      }
      auditorium.versions.get(option.movieVersionPublicId).showtimes.push(option);
    }
    for (const cinema of result.values()) {
      for (const auditorium of cinema.auditoriums.values()) {
        for (const version of auditorium.versions.values()) {
          version.showtimes.sort((a, b) => (
            a.localStartTime.localeCompare(b.localStartTime)
            || a.showtimePublicId.localeCompare(b.showtimePublicId)
          ));
        }
      }
    }
    return [...result.values()]
      .sort((left, right) => left.cinema.cinemaName.localeCompare(right.cinema.cinemaName, 'vi'))
      .map(cinema => ({
        ...cinema,
        auditoriums: [...cinema.auditoriums.values()].sort((left, right) => (
          (left.auditorium.auditoriumName || '').localeCompare(
            right.auditorium.auditoriumName || '',
            'vi',
            { numeric: true }
          )
        ))
      }));
  }, [filteredOptions, selectedDate]);

  const poster = movie?.primaryPoster || movie?.media?.find(item => item.mediaType === 'POSTER')?.url || FALLBACK_POSTER;
  const backdrop = movie?.media?.find(item => item.mediaType === 'BACKDROP')?.url;
  const banners = getMovieBannerUrls(movie?.media);
  const trailerUrl = movie?.media?.find(item => item.mediaType === 'TRAILER')?.url;
  const genreLabels = formatGenreLabels(movie?.genres);
  const actors = sortedPeople(movie?.actors);
  const visibleActors = showAllActors ? actors : actors.slice(0, ACTOR_PREVIEW_LIMIT);
  const writers = peopleNames(movie?.writers);
  const producers = peopleNames(movie?.producers);
  const languages = [...new Set((movie?.versions || [])
    .map(version => version.audioLanguage)
    .filter(Boolean))]
    .map(formatLanguageLabel);

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
        <MovieBannerCrossfade
          images={banners}
          fallbackImage={backdrop}
          movieTitle={movie.title}
        />
        <div className="absolute inset-0 -z-10 bg-gradient-to-r from-zinc-950/85 via-zinc-950/70 to-zinc-950/25" />
        <div className="absolute inset-0 -z-10 bg-gradient-to-t from-zinc-950 via-zinc-950/10 to-black/35" />
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
              <span className="flex items-center gap-2">
                <CalendarDays size={16} className="text-brand-orange" />
                Khởi chiếu {formatDate(movie.releaseDate)}
              </span>
            </div>
            {genreLabels.length > 0 && (
              <div className="mt-5 flex flex-wrap gap-2" aria-label="Thể loại phim">
                {genreLabels.map(genre => (
                  <span key={genre} className="rounded-full border border-amber-400/25 bg-amber-400/10 px-3 py-1 text-xs font-bold text-amber-300">
                    {genre}
                  </span>
                ))}
              </div>
            )}
            <p className="mt-5 max-w-3xl text-base leading-7 text-zinc-300">{movie.synopsis || 'Nội dung phim đang được cập nhật.'}</p>
            <div className="mt-7 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => showtimeSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
                className={`flex items-center gap-2 rounded-full bg-brand-orange px-6 py-3 font-black text-white shadow-lg shadow-brand-orange/25 transition-colors hover:bg-orange-600 ${focus}`}
              >
                <Ticket size={17} /> {movie.bookable || availableDates.length ? 'Chọn suất chiếu' : 'Xem lịch chiếu'}
              </button>
              {trailerUrl && (
                <button
                  type="button"
                  onClick={() => setTrailer(getYoutubeEmbedUrl(trailerUrl))}
                  className={`flex items-center gap-2 rounded-full border border-white/20 bg-white/5 px-6 py-3 font-bold text-white hover:border-brand-orange hover:text-brand-orange ${focus}`}
                >
                  <Play size={17} /> Xem trailer
                </button>
              )}
            </div>
          </div>
        </div>
      </section>

      <section
        id="lich-chieu"
        ref={showtimeSectionRef}
        className="mx-auto mt-10 max-w-7xl scroll-mt-24 px-6"
      >
        <div className={`${surface} p-5 md:p-8`}>
          <div className="flex items-start gap-4">
            <div className="rounded-xl border border-brand-orange/30 bg-brand-orange/10 p-2.5 text-brand-orange">
              <Ticket size={20} />
            </div>
            <div>
              <p className="text-xs font-black uppercase tracking-[.22em] text-brand-orange">Chọn suất chiếu</p>
              <h2 className="mt-2 text-2xl font-black text-white md:text-3xl">Lịch chiếu và đặt vé</h2>
              <p className="mt-2 text-sm text-zinc-400">Chọn khu vực, rạp và ngày phù hợp với bạn.</p>
            </div>
          </div>

          {options.length > 0 && (
            <div className="mt-7 grid gap-3 rounded-2xl border border-white/10 bg-black/20 p-4 md:grid-cols-2">
              <label className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
                Tỉnh/thành phố
                <select
                  aria-label="Tỉnh/thành phố"
                  value={effectiveCity}
                  onChange={event => {
                    const city = event.target.value;
                    setSelectedCity(city);
                    setSelectedCinema('ALL');
                    setSelectedDate('');
                    writePreferredCinema(null);
                    updateBookingParams({ city, cinema: null, date: null });
                  }}
                  className={`mt-2 w-full rounded-xl border border-white/10 bg-zinc-950 px-4 py-3 text-sm font-bold normal-case text-white ${focus}`}
                >
                  <option value="ALL">Tất cả tỉnh/thành phố</option>
                  {availableCities.map(city => (
                    <option key={city} value={city}>{formatCityLabel(city)}</option>
                  ))}
                </select>
              </label>
              <label className="text-[10px] font-black uppercase tracking-wider text-zinc-500">
                Rạp chiếu
                <select
                  aria-label="Rạp chiếu"
                  value={effectiveCinema}
                  onChange={event => {
                    const cinemaPublicId = event.target.value;
                    setSelectedCinema(cinemaPublicId);
                    setSelectedDate('');
                    const cinema = cinemaOptions.find(item => item.publicId === cinemaPublicId);
                    writePreferredCinema(cinema || null);
                    updateBookingParams({ cinema: cinemaPublicId, date: null });
                  }}
                  className={`mt-2 w-full rounded-xl border border-white/10 bg-zinc-950 px-4 py-3 text-sm font-bold normal-case text-white ${focus}`}
                >
                  <option value="ALL">Tất cả rạp</option>
                  {cinemaOptions.map(cinema => (
                    <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>
                  ))}
                </select>
              </label>
            </div>
          )}

          <div className="mt-5 flex gap-3 overflow-x-auto pb-3">
            {availableDates.map((date, index) => (
              <button
                key={date}
                type="button"
                aria-pressed={selectedDate === date}
                onClick={() => {
                  setSelectedDate(date);
                  updateBookingParams({ date });
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
            <div className="my-8 rounded-2xl border border-dashed border-white/10 bg-black/20 px-5 py-12 text-center text-zinc-400">
              <Film className="mx-auto mb-3 text-zinc-600" />
              <h3 className="font-black text-zinc-200">Lịch chiếu chưa được công bố</h3>
              <p className="mx-auto mt-2 max-w-lg text-sm leading-6">Phim hiện chưa có suất chiếu mở bán trong 14 ngày tới.</p>
              {movie.status === 'UPCOMING' && (
                <button
                  type="button"
                  disabled
                  title="Hệ thống chưa hỗ trợ đăng ký thông báo mở bán"
                  className="mt-5 cursor-not-allowed rounded-full border border-white/10 px-5 py-2.5 text-xs font-black text-zinc-600"
                >
                  Nhận thông báo khi mở bán · Sắp có
                </button>
              )}
            </div>
          ) : grouped.length === 0 ? (
            <div className="my-8 rounded-2xl border border-dashed border-white/10 bg-black/20 py-14 text-center text-zinc-400">
              <Film className="mx-auto mb-3 text-zinc-600" />
              Không có suất chiếu trong ngày đã chọn.
            </div>
          ) : (
            <div className="mt-7 space-y-5">
              {grouped.map(({ cinema, auditoriums }) => (
                <article key={cinema.cinemaPublicId} className="overflow-hidden rounded-2xl border border-white/10 bg-zinc-950/55">
                  <header className="border-b border-white/10 px-5 py-4 md:px-6">
                    <h3 className="text-lg font-black text-white">{cinema.cinemaName}</h3>
                    {(cinema.cinemaAddress || cinema.cinemaCity) && (
                      <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-zinc-400">
                        <p className="flex items-center gap-2">
                          <MapPin size={15} className="text-brand-orange" />
                          {[cinema.cinemaAddress, formatCityLabel(cinema.cinemaCity)].filter(Boolean).join(', ')}
                        </p>
                        <a
                          href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent([cinema.cinemaAddress, cinema.cinemaCity].filter(Boolean).join(', '))}`}
                          target="_blank"
                          rel="noreferrer"
                          className={`text-xs font-black text-brand-orange hover:text-orange-300 ${focus}`}
                        >
                          Xem bản đồ
                        </a>
                      </div>
                    )}
                  </header>
                  <div className="divide-y divide-white/10 px-5 md:px-6">
                    {auditoriums.flatMap(({ auditorium, versions }) => (
                      [...versions.values()].map(({ version, showtimes }) => {
                        const roomName = formatAuditoriumLabel(auditorium.auditoriumName);
                        const screenType = formatScreenTypeLabel(auditorium.screenType);
                        const soundType = formatSoundTypeLabel(auditorium.soundType);
                        const roomNameAlreadyHasTier = /(Tiêu chuẩn|Premium|IMAX|4DX)/i.test(roomName);
                        const roomDetails = [
                          roomName,
                          screenType && !roomNameAlreadyHasTier ? screenType : null,
                          soundType
                        ].filter(Boolean).join(' · ');
                        return (
                          <div key={`${auditorium.auditoriumPublicId}-${version.movieVersionPublicId}`} className="grid gap-4 py-5 lg:grid-cols-[280px_1fr] lg:items-center">
                            <div>
                              <strong className="text-sm text-amber-300">{formatVersionLabel(version)}</strong>
                              <p className="mt-1.5 text-xs leading-5 text-zinc-500">{roomDetails}</p>
                            </div>
                            <div className="flex flex-wrap gap-2.5">
                              {showtimes.map(showtime => {
                                const salesState = getShowtimeSalesState(showtime, currentTimeMs);
                                return (
                                  <button
                                    key={showtime.showtimePublicId}
                                    type="button"
                                    disabled={salesState.disabled}
                                    onClick={() => navigate(seatSelectionPath(showtime.showtimePublicId))}
                                    aria-label={`${salesState.disabled ? salesState.label : 'Chọn suất'} ${formatLocalClock(showtime.localStartTime)} tại ${cinema.cinemaName}, ${roomName}`}
                                    className={`min-w-28 rounded-xl border px-4 py-2.5 text-left transition-colors ${focus} ${
                                      salesState.disabled
                                        ? 'cursor-not-allowed border-white/5 bg-zinc-950 text-zinc-600 opacity-70'
                                        : 'border-white/15 bg-zinc-900 text-white hover:border-brand-orange hover:bg-brand-orange/10'
                                    }`}
                                  >
                                    <span className="block text-base font-black">{formatLocalClock(showtime.localStartTime)}</span>
                                    {showtime.priceFrom != null && (
                                      <span className="mt-0.5 block text-[10px] font-semibold text-zinc-500">
                                        Từ {formatPrice(showtime.priceFrom, showtime.currency)}
                                      </span>
                                    )}
                                    <span className={`mt-1.5 block text-[10px] font-black ${statusToneClass(salesState.tone)}`}>
                                      {salesState.label}
                                    </span>
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        );
                      })
                    ))}
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="mx-auto mt-10 max-w-7xl px-6">
        <div className={`${surface} p-5 md:p-8`}>
          <div className="flex items-start gap-4">
            <div className="rounded-xl border border-brand-orange/30 bg-brand-orange/10 p-2.5 text-brand-orange">
              <Film size={20} />
            </div>
            <div>
              <p className="text-xs font-black uppercase tracking-[.22em] text-brand-orange">Thông tin phim</p>
              <h2 className="mt-2 text-2xl font-black leading-tight text-white">Thông tin tổng quan</h2>
            </div>
          </div>

          <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <InfoTile icon={Globe2} label="Quốc gia sản xuất" value={formatCountryLabel(movie.country)} />
            <InfoTile icon={Languages} label="Ngôn ngữ suất chiếu" value={languages.join(', ')} />
            <InfoTile
              icon={UserRound}
              label="Đạo diễn"
              value={movie?.directors?.length ? <PersonLinks people={movie.directors} /> : null}
            />
            <InfoTile
              icon={Building2}
              label="Đơn vị sản xuất"
              value={companyNames(movie.productionCompanies).join(', ') || companyNames(movie.studios).join(', ')}
            />
          </div>

          {(writers.length > 0 || producers.length > 0 || companyNames(movie.distributors).length > 0) && (
            <dl className="mt-5 grid gap-4 rounded-2xl border border-white/10 bg-black/20 p-5 md:grid-cols-3">
              {writers.length > 0 && <div><dt className="text-xs font-black text-zinc-500">Biên kịch</dt><dd className="mt-1.5 text-sm leading-6 text-zinc-200">{writers.join(', ')}</dd></div>}
              {producers.length > 0 && <div><dt className="text-xs font-black text-zinc-500">Nhà sản xuất</dt><dd className="mt-1.5 text-sm leading-6 text-zinc-200">{producers.join(', ')}</dd></div>}
              {companyNames(movie.distributors).length > 0 && <div><dt className="text-xs font-black text-zinc-500">Phát hành</dt><dd className="mt-1.5 text-sm leading-6 text-zinc-200">{companyNames(movie.distributors).join(', ')}</dd></div>}
            </dl>
          )}
        </div>
      </section>

      {(actors.length > 0 || relatedMovies.length > 0) && (
        <section className="mx-auto mt-10 max-w-7xl px-6">
          <div className={`grid items-start gap-6 ${actors.length > 0 && relatedMovies.length > 0 ? 'lg:grid-cols-[minmax(0,1fr)_320px]' : ''}`}>
            {actors.length > 0 && (
              <div className={`${surface} p-5 md:p-8`} aria-labelledby="featured-cast-title">
                <div className="flex flex-wrap items-end justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <div className="rounded-xl border border-brand-orange/30 bg-brand-orange/10 p-2.5 text-brand-orange">
                      <Users size={20} />
                    </div>
                    <div>
                      <p className="text-xs font-black uppercase tracking-[.22em] text-brand-orange">Gương mặt trong phim</p>
                      <h2 id="featured-cast-title" className="mt-2 text-2xl font-black leading-tight text-white">Diễn viên nổi bật</h2>
                    </div>
                  </div>
                  <p className="text-xs text-zinc-500">Hiển thị {visibleActors.length}/{actors.length} người</p>
                </div>

                <div className="mt-6 grid gap-3 sm:grid-cols-2">
                  {visibleActors.map(actor => (
                    <ActorCard key={actor.publicId || `${actor.fullName}-${actor.displayOrder}`} actor={actor} />
                  ))}
                </div>

                {actors.length > ACTOR_PREVIEW_LIMIT && (
                  <button
                    type="button"
                    aria-expanded={showAllActors}
                    onClick={() => setShowAllActors(value => {
                      const expanded = !value;
                      writeActorExpansion(movieId, expanded);
                      return expanded;
                    })}
                    className={`mx-auto mt-6 flex items-center gap-2 rounded-full border border-white/15 px-5 py-2.5 text-sm font-black text-zinc-200 hover:border-brand-orange hover:text-brand-orange ${focus}`}
                  >
                    {showAllActors ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                    {showAllActors ? 'Thu gọn danh sách' : `Xem toàn bộ ${actors.length} diễn viên`}
                  </button>
                )}
              </div>
            )}

            {relatedMovies.length > 0 && (
              <aside className={`${surface} p-5 md:p-6 lg:sticky lg:top-24`} aria-labelledby="related-movies-title">
                <div className="flex items-end justify-between gap-4">
                  <div>
                    <p className="text-xs font-black uppercase tracking-[.22em] text-brand-orange">Khám phá thêm</p>
                    <h2 id="related-movies-title" className="mt-2 text-2xl font-black text-white">Phim liên quan</h2>
                  </div>
                  <Link to="/movies" className={`shrink-0 text-xs font-black text-zinc-400 hover:text-brand-orange ${focus}`}>Xem tất cả</Link>
                </div>
                <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
                  {relatedMovies.map(item => (
                    <RelatedMovieCard key={item.publicId || item.slug} movie={item} />
                  ))}
                </div>
              </aside>
            )}
          </div>
        </section>
      )}

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
