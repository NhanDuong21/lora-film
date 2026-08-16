import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  ArrowRight,
  Play,
  RefreshCw,
  Ticket,
  X,
} from 'lucide-react';

import { useMoviesQuery } from '@/features/catalog/customer/hooks/useHomepageMovies';
import MovieSectionSkeleton from './MovieSectionSkeleton';
import {
  formatDate,
  formatDuration,
  getAgeRatingLabel,
  getYoutubeEmbedUrl,
} from '@/utils/formatters';

const FALLBACK_POSTER = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='500' height='750' viewBox='0 0 500 750'><rect width='500' height='750' fill='%2318181b'/><text x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-weight='bold' font-size='24' fill='%2352525b'>LORA FILM</text><text x='50%25' y='55%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='14' fill='%233f3f46'>Không có ảnh bìa</text></svg>";

const formatPrice = value => value == null
  ? null
  : `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Number(value))}đ`;

const compactGenres = genres => {
  if (!Array.isArray(genres) || genres.length === 0) return 'Đang cập nhật thể loại';
  return genres
    .map(genre => (typeof genre === 'string' ? genre : genre?.genreName || genre?.name))
    .filter(Boolean)
    .slice(0, 2)
    .map(value => value.replace(/^Phim\s+/i, ''))
    .join(', ');
};

export default function MovieSection({
  onSelectMovie,
  onNavigate,
  activeTab: propActiveTab,
  onChangeActiveTab,
  onBuyTicket,
}) {
  const navigate = useNavigate();
  const [localActiveTab, setLocalActiveTab] = useState('NOW_SHOWING');
  const [activeTrailerUrl, setActiveTrailerUrl] = useState(null);
  const activeTab = propActiveTab ?? localActiveTab;
  const setActiveTab = onChangeActiveTab ?? setLocalActiveTab;

  const nowShowingQuery = useMoviesQuery({
    status: 'NOW_SHOWING',
    sort: 'createdAt,desc',
    size: 8,
  });
  const upcomingQuery = useMoviesQuery({
    status: 'UPCOMING',
    sort: 'createdAt,desc',
    size: 8,
  });
  const activeQuery = activeTab === 'NOW_SHOWING' ? nowShowingQuery : upcomingQuery;
  const {
    movies,
    loading,
    isRefreshing,
    error,
    retry,
  } = activeQuery;

  const openMovie = movie => {
    const identifier = movie.slug || movie.publicId;
    if (onSelectMovie) onSelectMovie(identifier);
    else navigate(`/movies/${identifier}`);
  };

  const openShowtimes = movie => {
    const identifier = movie.slug || movie.publicId;
    if (onBuyTicket) onBuyTicket(identifier);
    else if (onNavigate) onNavigate('movie-detail', { movieId: identifier });
    else navigate(`/movies/${identifier}`);
  };

  return (
    <section id="phim" className="relative border-t border-zinc-900/60 bg-zinc-950 px-4 py-16 sm:px-6 lg:px-8">
      <div className="mx-auto w-full max-w-7xl">
        <header className="flex flex-col gap-5 border-b border-zinc-800/80 pb-5 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-[11px] font-black uppercase tracking-[0.24em] text-brand-orange">
              Khám phá điện ảnh
            </p>
            <div className="mt-3 flex flex-wrap items-center gap-x-7 gap-y-3">
              <button
                type="button"
                onClick={() => setActiveTab('NOW_SHOWING')}
                className={`relative pb-2 text-lg font-black uppercase tracking-wide transition-colors md:text-xl ${
                  activeTab === 'NOW_SHOWING'
                    ? 'text-white after:absolute after:inset-x-0 after:-bottom-px after:h-0.5 after:bg-brand-orange'
                    : 'text-zinc-500 hover:text-zinc-300'
                }`}
              >
                Phim đang chiếu
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('UPCOMING')}
                className={`relative pb-2 text-lg font-black uppercase tracking-wide transition-colors md:text-xl ${
                  activeTab === 'UPCOMING'
                    ? 'text-white after:absolute after:inset-x-0 after:-bottom-px after:h-0.5 after:bg-brand-orange'
                    : 'text-zinc-500 hover:text-zinc-300'
                }`}
              >
                Phim sắp chiếu
              </button>
            </div>
          </div>
          <Link
            to={`/movies?status=${activeTab}`}
            className="inline-flex items-center gap-2 text-sm font-bold text-zinc-400 transition-colors hover:text-brand-orange"
          >
            Xem tất cả phim <ArrowRight className="h-4 w-4" />
          </Link>
        </header>

        {loading ? (
          <MovieSectionSkeleton />
        ) : error ? (
          <div className="mx-auto mt-10 flex max-w-lg flex-col items-center justify-center space-y-4 rounded-2xl border border-red-950/40 bg-zinc-950 px-6 py-16 text-center shadow-xl">
            <div className="flex h-12 w-12 items-center justify-center rounded-full border border-red-500/20 bg-red-950/20 text-red-500">
              <AlertCircle className="h-6 w-6" />
            </div>
            <h3 className="text-base font-bold text-zinc-200">Không thể tải danh sách phim</h3>
            <p className="max-w-xs text-xs text-zinc-500">Vui lòng kiểm tra kết nối và thử lại.</p>
            <button
              type="button"
              onClick={retry}
              className="flex items-center gap-2 rounded-full bg-brand-orange px-6 py-2 text-xs font-bold text-white transition-colors hover:bg-orange-600"
            >
              <RefreshCw className="h-3.5 w-3.5" /> Thử lại
            </button>
          </div>
        ) : movies.length === 0 ? (
          <div className="mx-auto mt-10 flex min-h-64 max-w-lg flex-col items-center justify-center rounded-2xl border border-zinc-900/60 bg-zinc-900/20 px-6 text-center">
            <Ticket className="h-9 w-9 text-zinc-700" />
            <p className="mt-3 text-sm font-semibold text-zinc-400">
              {activeTab === 'NOW_SHOWING'
                ? 'Hiện chưa có phim đang chiếu.'
                : 'Hiện chưa có phim sắp chiếu.'}
            </p>
          </div>
        ) : (
          <div className={`grid grid-cols-1 gap-7 py-10 sm:grid-cols-2 lg:grid-cols-4 ${isRefreshing ? 'pointer-events-none opacity-50' : ''}`}>
            {movies.map(movie => {
              const rating = getAgeRatingLabel(movie.ageRating);
              const canBook = ['NOW_SHOWING', 'UPCOMING'].includes(movie.status)
                && movie.bookable === true;
              const genres = compactGenres(movie.genres);
              const metadata = [
                rating.label,
                movie.durationMinutes ? formatDuration(movie.durationMinutes) : null,
                genres,
              ].filter(Boolean).join(' · ');

              return (
                <article
                  key={movie.publicId || movie.id}
                  className="group relative aspect-[2/3] overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 shadow-lg transition-all duration-300 hover:-translate-y-1 hover:border-brand-orange/50 hover:shadow-[0_24px_55px_-22px_rgba(255,122,26,0.48)]"
                >
                  <img
                    src={movie.primaryPoster || movie.posterUrl || FALLBACK_POSTER}
                    alt={movie.title}
                    loading="lazy"
                    className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.04]"
                    onError={event => {
                      event.currentTarget.onerror = null;
                      event.currentTarget.src = FALLBACK_POSTER;
                    }}
                  />
                  <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_top,rgba(0,0,0,0.99)_0%,rgba(0,0,0,0.94)_33%,rgba(0,0,0,0.66)_52%,rgba(0,0,0,0.12)_76%,transparent_100%)]" />

                  <button
                    type="button"
                    aria-label={`Xem chi tiết phim ${movie.title}`}
                    onClick={() => openMovie(movie)}
                    className="absolute inset-0 z-10 rounded-2xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-orange"
                  />

                  <span className={`pointer-events-none absolute left-4 top-4 z-20 rounded-md border px-2.5 py-1 text-xs font-black tracking-wider shadow-lg backdrop-blur ${rating.bgClass}`}>
                    {rating.label}
                  </span>

                  {movie.trailerUrl && (
                    <button
                      type="button"
                      aria-label={`Xem trailer phim ${movie.title}`}
                      onClick={() => setActiveTrailerUrl(getYoutubeEmbedUrl(movie.trailerUrl))}
                      className="absolute right-4 top-4 z-30 flex h-9 w-9 items-center justify-center rounded-full border border-white/25 bg-black/65 text-white opacity-0 backdrop-blur transition-all hover:border-brand-orange hover:text-brand-orange group-hover:opacity-100 focus-visible:opacity-100"
                    >
                      <Play className="h-4 w-4 fill-current" />
                    </button>
                  )}

                  <div className="pointer-events-none absolute inset-x-0 bottom-0 z-20 p-5">
                    <h3 className="line-clamp-2 min-h-11 text-base font-black leading-snug text-white drop-shadow-lg md:text-lg">
                      {movie.title}
                    </h3>
                    <p className="mt-2 line-clamp-2 min-h-10 text-[13px] font-semibold leading-5 text-zinc-300">
                      {metadata}
                    </p>
                    {movie.status === 'UPCOMING' && movie.releaseDate && (
                      <p className="mt-1 text-xs font-medium text-zinc-400">
                        Khởi chiếu {formatDate(movie.releaseDate)}
                      </p>
                    )}
                    {movie.priceFrom != null && (
                      <p className="mt-2 text-sm font-black text-brand-orange">
                        Từ {formatPrice(movie.priceFrom)}
                      </p>
                    )}
                    <div className="pointer-events-auto mt-4">
                      <button
                        type="button"
                        disabled={!canBook}
                        onClick={() => openShowtimes(movie)}
                        className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-white shadow-lg shadow-orange-950/30 transition-colors hover:bg-orange-600 disabled:cursor-not-allowed disabled:bg-zinc-800/90 disabled:text-zinc-500"
                      >
                        <Ticket className="h-4 w-4" />
                        {canBook
                          ? (movie.status === 'UPCOMING' ? 'Xem lịch chiếu' : 'Chọn suất')
                          : (movie.status === 'UPCOMING' ? 'Chưa mở lịch' : 'Tạm hết lịch')}
                      </button>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}

        {!loading && !error && movies.length > 0 && (
          <div className="flex justify-center border-t border-zinc-900 pt-7">
            <Link
              to={`/movies?status=${activeTab}`}
              className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-zinc-700 px-5 text-sm font-black text-zinc-200 transition-colors hover:border-brand-orange hover:text-brand-orange"
            >
              Xem tất cả {activeTab === 'NOW_SHOWING' ? 'phim đang chiếu' : 'phim sắp chiếu'}
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        )}
      </div>

      {activeTrailerUrl && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/90 p-4 backdrop-blur-sm"
          onClick={() => setActiveTrailerUrl(null)}
        >
          <div
            className="relative aspect-video w-full max-w-4xl overflow-hidden rounded-2xl border border-zinc-800 bg-black shadow-2xl"
            onClick={event => event.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setActiveTrailerUrl(null)}
              className="absolute right-4 top-4 z-20 rounded-full bg-zinc-900/80 p-2 text-zinc-400 transition-colors hover:text-white"
              aria-label="Đóng đoạn giới thiệu phim"
            >
              <X className="h-5 w-5" />
            </button>
            <iframe
              src={`${activeTrailerUrl}?autoplay=1&rel=0&modestbranding=1`}
              title="Trình phát đoạn giới thiệu phim LoraFilm"
              className="h-full w-full border-0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
            />
          </div>
        </div>
      )}
    </section>
  );
}
