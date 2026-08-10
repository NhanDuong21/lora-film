import { useEffect, useMemo, useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useMoviesQuery } from '@/features/catalog/customer/hooks/useHomepageMovies';
import { getBookingOptions } from '@/features/catalog/customer/services/movieService';
import {
  addCalendarDays,
  formatLocalClock,
  formatServiceDate,
  seatSelectionPath,
  vietnamDateKey
} from '@/features/catalog/customer/utils/customerMovieFlow';

const SelectField = ({ label, value, onChange, disabled, children }) => (
  <label className={`relative flex min-w-0 flex-1 flex-col border-b border-white/10 px-4 py-3 last:border-b-0 md:border-b-0 md:border-r ${disabled ? 'opacity-40' : ''}`}>
    <span className="text-[9px] font-black uppercase tracking-widest text-zinc-500">{label}</span>
    <select
      aria-label={label}
      value={value}
      onChange={onChange}
      disabled={disabled}
      className="[color-scheme:dark] w-full appearance-none truncate border-0 bg-transparent p-0 text-xs font-bold text-zinc-200 outline-none focus-visible:text-brand-orange disabled:cursor-not-allowed"
    >
      {children}
    </select>
    <ChevronDown className="pointer-events-none absolute right-3 top-5 h-4 w-4 text-zinc-500" />
  </label>
);

export default function Hero() {
  const navigate = useNavigate();
  const nowShowingQuery = useMoviesQuery({
    status: 'NOW_SHOWING',
    sort: 'releaseDate,asc',
    size: 100
  });
  const upcomingQuery = useMoviesQuery({
    status: 'UPCOMING',
    sort: 'releaseDate,asc',
    size: 100
  });
  const [movieSlug, setMovieSlug] = useState('');
  const [cinemaId, setCinemaId] = useState('');
  const [serviceDate, setServiceDate] = useState('');
  const [showtimeId, setShowtimeId] = useState('');
  const [options, setOptions] = useState([]);
  const [optionLoading, setOptionLoading] = useState(false);
  const [optionError, setOptionError] = useState('');

  const nowShowingMovies = nowShowingQuery.movies;
  const upcomingMovies = useMemo(() => {
    const nowShowingIds = new Set(nowShowingMovies.map(movie => movie.publicId || movie.slug));
    return upcomingQuery.movies.filter(movie => !nowShowingIds.has(movie.publicId || movie.slug));
  }, [nowShowingMovies, upcomingQuery.movies]);
  const movies = useMemo(
    () => [...nowShowingMovies, ...upcomingMovies],
    [nowShowingMovies, upcomingMovies]
  );
  const selectedMovie = useMemo(
    () => movies.find(movie => movie.slug === movieSlug),
    [movieSlug, movies]
  );
  const movieListLoading = nowShowingQuery.loading || upcomingQuery.loading;
  const movieListError = nowShowingQuery.error || upcomingQuery.error;

  useEffect(() => {
    if (!movieSlug || !selectedMovie) {
      return undefined;
    }
    const controller = new AbortController();
    const today = vietnamDateKey();
    const from = selectedMovie.releaseDate && selectedMovie.releaseDate > today
      ? selectedMovie.releaseDate
      : today;

    const loadOptions = async () => {
      await Promise.resolve();
      if (controller.signal.aborted) return;
      setOptionLoading(true);
      setOptionError('');
      try {
        setOptions(await getBookingOptions(movieSlug, {
          from,
          to: addCalendarDays(from, 13),
          signal: controller.signal
        }));
      } catch (error) {
        if (error?.name !== 'CanceledError') {
          setOptions([]);
          setOptionError('Không thể tải lịch chiếu.');
        }
      } finally {
        if (!controller.signal.aborted) {
          setOptionLoading(false);
        }
      }
    };
    loadOptions();
    return () => controller.abort();
  }, [movieSlug, selectedMovie]);

  const cinemas = useMemo(() => [...new Map(options.map(option => [
    option.cinemaPublicId,
    { id: option.cinemaPublicId, name: option.cinemaName }
  ])).values()], [options]);
  const dates = useMemo(() => [...new Set(options
    .filter(option => option.cinemaPublicId === cinemaId)
    .map(option => option.serviceDate))].sort(), [options, cinemaId]);
  const showtimes = useMemo(() => options
    .filter(option => option.cinemaPublicId === cinemaId && option.serviceDate === serviceDate)
    .sort((a, b) => a.localStartTime.localeCompare(b.localStartTime)), [options, cinemaId, serviceDate]);

  const changeMovie = event => {
    setMovieSlug(event.target.value);
    setOptions([]);
    setOptionLoading(false);
    setCinemaId('');
    setServiceDate('');
    setShowtimeId('');
    setOptionError('');
  };
  const changeCinema = event => {
    setCinemaId(event.target.value);
    setServiceDate('');
    setShowtimeId('');
  };
  const changeDate = event => {
    setServiceDate(event.target.value);
    setShowtimeId('');
  };

  return (
    <section className="relative flex min-h-[85vh] items-end overflow-hidden bg-brand-dark px-6 pb-16 pt-24 md:px-12">
      <video
        autoPlay loop muted playsInline
        className="absolute inset-0 h-full w-full object-cover opacity-65"
        src="/video/hero_banner.mp4"
      />
      <div className="absolute inset-0 bg-gradient-to-t from-brand-dark via-brand-dark/30 to-black/30" />
      <div className="relative z-10 mx-auto w-full max-w-7xl">
        <p className="text-sm font-bold uppercase tracking-[.25em] text-brand-orange">LoraFilm</p>
        <h1 className="mt-3 max-w-3xl text-4xl font-black text-white md:text-6xl">Thế giới điện ảnh trong tầm tay</h1>
        <p className="mt-4 max-w-xl text-zinc-300">Chọn đúng phim, rạp, ngày phục vụ và suất chiếu đang mở bán.</p>

        <div className="mt-10 flex flex-col overflow-hidden rounded-2xl border border-white/10 bg-zinc-950/90 shadow-2xl backdrop-blur md:flex-row md:items-center">
          <SelectField label="Phim" value={movieSlug} onChange={changeMovie} disabled={movieListLoading}>
            <option className="bg-zinc-950 text-zinc-100" value="">
              {movieListLoading ? 'Đang tải phim…' : 'Chọn phim…'}
            </option>
            {nowShowingMovies.length > 0 && (
              <optgroup className="bg-zinc-950 text-brand-orange" label="Phim đang chiếu">
                {nowShowingMovies.map(movie => (
                  <option className="bg-zinc-950 text-zinc-100" key={movie.publicId || movie.slug} value={movie.slug}>
                    {movie.title}
                  </option>
                ))}
              </optgroup>
            )}
            {upcomingMovies.length > 0 && (
              <optgroup className="bg-zinc-950 text-amber-400" label="Phim sắp chiếu">
                {upcomingMovies.map(movie => (
                  <option className="bg-zinc-950 text-zinc-100" key={movie.publicId || movie.slug} value={movie.slug}>
                    {movie.title}
                  </option>
                ))}
              </optgroup>
            )}
          </SelectField>
          <SelectField label="Rạp" value={cinemaId} onChange={changeCinema} disabled={!movieSlug || optionLoading}>
            <option className="bg-zinc-950 text-zinc-100" value="">
              {optionLoading ? 'Đang tải rạp…' : options.length === 0 && movieSlug ? 'Chưa có rạp mở bán' : 'Chọn rạp…'}
            </option>
            {cinemas.map(cinema => <option className="bg-zinc-950 text-zinc-100" key={cinema.id} value={cinema.id}>{cinema.name}</option>)}
          </SelectField>
          <SelectField label="Ngày phục vụ" value={serviceDate} onChange={changeDate} disabled={!cinemaId}>
            <option className="bg-zinc-950 text-zinc-100" value="">Chọn ngày…</option>
            {dates.map(date => <option className="bg-zinc-950 text-zinc-100" key={date} value={date}>{formatServiceDate(date)}</option>)}
          </SelectField>
          <SelectField label="Suất chiếu" value={showtimeId} onChange={event => setShowtimeId(event.target.value)} disabled={!serviceDate}>
            <option className="bg-zinc-950 text-zinc-100" value="">Chọn suất…</option>
            {showtimes.map(showtime => (
              <option className="bg-zinc-950 text-zinc-100" key={showtime.showtimePublicId} value={showtime.showtimePublicId}>
                {formatLocalClock(showtime.localStartTime)} · {showtime.versionName || showtime.format}
              </option>
            ))}
          </SelectField>
          <button
            type="button"
            disabled={!showtimeId}
            onClick={() => navigate(seatSelectionPath(showtimeId))}
            className="m-3 rounded-full border border-transparent bg-brand-orange px-7 py-3 text-sm font-bold text-white shadow-lg shadow-brand-orange/20 transition-colors hover:bg-orange-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950 disabled:cursor-not-allowed disabled:border-brand-orange/30 disabled:bg-zinc-900 disabled:text-brand-orange/60 disabled:shadow-none"
          >
            Mua vé nhanh
          </button>
        </div>
        {movieListError && <p className="mt-3 text-sm text-red-300">{movieListError}</p>}
        {optionError && <p className="mt-3 text-sm text-red-300">{optionError}</p>}
      </div>
    </section>
  );
}
