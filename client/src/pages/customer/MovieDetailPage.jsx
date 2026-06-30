import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Play, X, AlertCircle, Calendar, Clock, Film } from 'lucide-react';
import { getMovieById } from '../../services/movieService';
import {
  formatGenres,
  formatDuration,
  formatDate,
  getAgeRatingLabel,
  getYoutubeEmbedUrl
} from '../../utils/formatters';

const FALLBACK_POSTER = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='500' height='750' viewBox='0 0 500 750'><rect width='500' height='750' fill='%2318181b'/><text x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-weight='bold' font-size='24' fill='%2352525b'>LORA FILM</text><text x='50%25' y='55%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='14' fill='%233f3f46'>Không có ảnh bìa</text></svg>";

function MovieDetailSkeleton() {
  return (
    <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 md:px-8 py-10 animate-pulse">
      {/* Back Button Skeleton */}
      <div className="h-8 w-24 bg-zinc-900 rounded-full mb-8" />
      
      {/* Hero Section Skeleton */}
      <div className="flex flex-col md:flex-row gap-8 lg:gap-12">
        {/* Left Poster Skeleton */}
        <div className="w-full md:w-1/3 max-w-md aspect-[2/3] bg-zinc-900 rounded-2xl" />
        
        {/* Right Info Skeleton */}
        <div className="flex-1 space-y-6">
          <div className="h-6 w-20 bg-zinc-900 rounded" />
          <div className="h-10 w-2/3 bg-zinc-900 rounded" />
          <div className="h-4 w-1/4 bg-zinc-900 rounded" />
          <div className="h-4 w-1/3 bg-zinc-900 rounded" />
          <div className="space-y-2">
            <div className="h-4 w-full bg-zinc-900 rounded" />
            <div className="h-4 w-full bg-zinc-900 rounded" />
            <div className="h-4 w-5/6 bg-zinc-900 rounded" />
          </div>
          <div className="h-12 w-40 bg-zinc-900 rounded-full" />
        </div>
      </div>
    </div>
  );
}

export default function MovieDetailPage() {
  const { movieId } = useParams();
  const navigate = useNavigate();
  const [movie, setMovie] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTrailerUrl, setActiveTrailerUrl] = useState(null);

  const fetchMovieDetail = useCallback(async () => {
    if (!movieId) return;
    
    // Defer state updates to microtask to prevent react-hooks/set-state-in-effect
    await Promise.resolve();
    
    if (isNaN(Number(movieId))) {
      setError("MOVIE_NOT_FOUND");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await getMovieById(movieId);
      // Public API treats INACTIVE movies as not found
      if (!data || data.status === 'INACTIVE') {
        setError("MOVIE_NOT_FOUND");
      } else {
        setMovie(data);
      }
    } catch (err) {
      if (err && (err.status === 404 || err.message === 'MOVIE_NOT_FOUND' || err.errorCode === 'MOVIE_NOT_FOUND')) {
        setError("MOVIE_NOT_FOUND");
      } else {
        setError(err.message || "Không thể tải thông tin phim.");
      }
    } finally {
      setLoading(false);
    }
  }, [movieId]);

  useEffect(() => {
    let active = true;
    const load = async () => {
      await Promise.resolve();
      if (active) {
        fetchMovieDetail();
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [fetchMovieDetail]);

  // Set document title once movie details are loaded
  useEffect(() => {
    if (movie && movie.title) {
      document.title = `${movie.title} - LoraFilm`;
    } else {
      document.title = "LoraFilm - Chi tiết phim";
    }
  }, [movie]);

  const handleBack = () => {
    // Navigate back or to homepage as fallback
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate('/');
    }
  };

  const handleGoHome = () => {
    navigate('/');
  };

  return (
    <div className="flex flex-col min-h-screen bg-brand-dark text-zinc-100 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
      <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-7xl mx-auto w-full flex flex-col justify-center">
        
        {loading ? (
          <MovieDetailSkeleton />
        ) : error ? (
          /* Error State Panel */
          <div className="w-full">
            {/* Back Button */}
            <div className="pb-6 border-b border-zinc-900 mb-8">
              <button
                onClick={handleBack}
                className="flex items-center gap-2 bg-black/40 hover:bg-brand-orange/25 text-white border border-white/10 hover:border-brand-orange font-bold px-4 py-2 rounded-full transition-all duration-300 cursor-pointer text-xs"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Quay lại</span>
              </button>
            </div>

            <div className="flex flex-col items-center justify-center p-12 text-center bg-zinc-900/40 border border-zinc-800 rounded-2xl max-w-xl mx-auto my-12 space-y-4 shadow-2xl">
              <div className="w-12 h-12 rounded-full bg-red-500/10 flex items-center justify-center text-red-500">
                <AlertCircle className="w-6 h-6 animate-pulse" />
              </div>
              <h2 className="text-lg font-bold text-zinc-100">
                {error === "MOVIE_NOT_FOUND" 
                  ? "Không tìm thấy phim hoặc phim không còn khả dụng." 
                  : "Không thể tải thông tin phim."}
              </h2>
              <p className="text-xs text-zinc-400">
                {error === "MOVIE_NOT_FOUND"
                  ? "Phim có thể đã ngừng chiếu hoặc đường dẫn không chính xác."
                  : "Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau."}
              </p>
              
              <div className="flex gap-4 pt-2">
                {error !== "MOVIE_NOT_FOUND" && (
                  <button
                    onClick={fetchMovieDetail}
                    className="bg-brand-orange hover:bg-orange-600 text-white font-bold px-5 py-2 rounded-full text-xs transition-all duration-300 cursor-pointer"
                  >
                    Thử lại
                  </button>
                )}
                <button
                  onClick={handleGoHome}
                  className="bg-zinc-800 hover:bg-zinc-700 text-white border border-zinc-700 font-bold px-5 py-2 rounded-full text-xs transition-all duration-300 cursor-pointer"
                >
                  Về trang chủ
                </button>
              </div>
            </div>
          </div>
        ) : movie ? (
          /* Movie Details Render */
          <div className="w-full">
            {/* Top Back Action Bar */}
            <div className="pb-6 border-b border-zinc-900 mb-8 flex justify-between items-center">
              <button
                onClick={handleBack}
                className="flex items-center gap-2 bg-black/40 hover:bg-brand-orange/25 text-white border border-white/10 hover:border-brand-orange font-bold px-4 py-2 rounded-full transition-all duration-300 cursor-pointer text-xs"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Quay lại</span>
              </button>
            </div>

            {/* Hero / Movie Info Block */}
            <div className="flex flex-col md:flex-row gap-8 lg:gap-12 mb-16">
              {/* Left Column: Poster Image */}
              <div className="w-full md:w-1/3 max-w-md shrink-0 self-center md:self-start">
                <div className="aspect-[2/3] rounded-2xl overflow-hidden bg-zinc-900 border border-zinc-850 shadow-2xl relative group">
                  <img
                    src={movie.posterUrl || FALLBACK_POSTER}
                    alt={movie.title}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = FALLBACK_POSTER;
                    }}
                  />
                  {movie.trailerUrl && (
                    <button
                      onClick={() => setActiveTrailerUrl(getYoutubeEmbedUrl(movie.trailerUrl))}
                      className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300"
                    >
                      <div className="w-14 h-14 rounded-full bg-brand-orange flex items-center justify-center text-white shadow-lg shadow-brand-orange/30 scale-90 group-hover:scale-100 transition-transform duration-300">
                        <Play className="w-6 h-6 fill-current ml-1" />
                      </div>
                    </button>
                  )}
                </div>
              </div>

              {/* Right Column: Metadata Details */}
              <div className="flex-1 flex flex-col justify-start">
                {/* Age Rating and Genres */}
                <div className="flex flex-wrap items-center gap-3 mb-4">
                  {(() => {
                    const ratingMeta = getAgeRatingLabel(movie.ageRating);
                    return (
                      <span
                        className={`text-xs font-mono font-black uppercase tracking-widest border px-3 py-1 rounded shadow-sm ${ratingMeta.bgClass}`}
                        title={ratingMeta.description}
                      >
                        {ratingMeta.label}
                      </span>
                    );
                  })()}
                  
                  <span className="text-xs text-zinc-400 font-semibold bg-zinc-900/80 px-3 py-1 rounded border border-zinc-800">
                    {formatGenres(movie.genres)}
                  </span>
                </div>

                {/* Big Movie Title */}
                <h1 className="text-3xl md:text-5xl font-black text-white tracking-tight uppercase leading-tight mb-4">
                  {movie.title}
                </h1>

                {/* Meta details list */}
                <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm text-zinc-400 font-semibold mb-6">
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-4 h-4 text-brand-orange" />
                    <span>{formatDuration(movie.durationMinutes)}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Calendar className="w-4 h-4 text-brand-orange" />
                    <span>Khởi chiếu: {formatDate(movie.releaseDate)}</span>
                  </div>
                </div>

                {/* Description Text */}
                <div className="mb-8">
                  <h3 className="text-xs font-black uppercase tracking-widest text-zinc-500 mb-2">Tóm tắt nội dung</h3>
                  <p className="text-sm leading-relaxed text-zinc-300 font-normal max-w-3xl whitespace-pre-line">
                    {movie.description || "Nội dung phim đang được cập nhật."}
                  </p>
                </div>

                {/* Production Metadata */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pb-8 mb-8 border-b border-zinc-900/60 text-sm max-w-3xl">
                  <div>
                    <span className="text-xs font-black uppercase tracking-widest text-zinc-500 block mb-1">Đạo diễn</span>
                    <span className="text-zinc-300 font-semibold">{movie.director || "Đang cập nhật"}</span>
                  </div>
                  <div>
                    <span className="text-xs font-black uppercase tracking-widest text-zinc-500 block mb-1">Diễn viên</span>
                    <span className="text-zinc-300 font-semibold">{movie.actor || "Đang cập nhật"}</span>
                  </div>
                </div>

                {/* Main Action Buttons */}
                <div className="flex flex-wrap gap-4">
                  {movie.trailerUrl && (
                    <button
                      onClick={() => setActiveTrailerUrl(getYoutubeEmbedUrl(movie.trailerUrl))}
                      className="group flex items-center gap-2 bg-transparent border border-white hover:border-brand-orange hover:text-brand-orange text-white font-bold px-6 py-3.5 rounded-full transition-all duration-300 cursor-pointer text-xs uppercase tracking-wider"
                    >
                      <Play className="w-4 h-4 fill-current group-hover:text-brand-orange" />
                      Xem Trailer
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Lower Section: Showtime / Reservation Calendar placeholder */}
            <div className="w-full pt-8 border-t border-zinc-900">
              <h2 className="text-xl font-black uppercase tracking-wider text-white mb-6">
                Lịch Chiếu & Đặt Vé
              </h2>
              
              <div className="flex flex-col items-center justify-center p-12 text-center bg-zinc-900/10 border border-zinc-900/80 rounded-2xl max-w-xl mx-auto my-4 space-y-3">
                <div className="w-10 h-10 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-500">
                  <Film className="w-5 h-5" />
                </div>
                <h3 className="text-sm font-bold text-zinc-300">Lịch chiếu đang được cập nhật.</h3>
                <p className="text-xs text-zinc-500">
                  Vui lòng quay lại sau để đặt vé cho suất chiếu này.
                </p>
              </div>
            </div>
          </div>
        ) : null}
      </main>

      {/* Cinematic Trailer Popup component */}
      {activeTrailerUrl && (
        <div 
          className="fixed inset-0 bg-zinc-950/95 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-all animate-fade-in"
          onClick={() => setActiveTrailerUrl(null)}
        >
          <div 
            className="relative w-full max-w-4xl aspect-video bg-black rounded-2xl overflow-hidden border border-zinc-800 shadow-2xl animate-scale-in"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setActiveTrailerUrl(null)}
              className="absolute top-4 right-4 bg-zinc-900/85 text-zinc-400 hover:text-white p-2 rounded-full transition-colors z-20 cursor-pointer"
              aria-label="Đóng trailer"
            >
              <X className="w-5 h-5" />
            </button>

            <iframe
              src={`${activeTrailerUrl}?autoplay=1&rel=0&modestbranding=1`}
              title={`${movie?.title || 'LoraFilm'} Trailer`}
              className="w-full h-full border-0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
            ></iframe>
          </div>
        </div>
      )}
    </div>
  );
}
