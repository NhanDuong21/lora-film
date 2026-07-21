import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import { ArrowLeft, Play, X, AlertCircle, Calendar, Clock, Film, Star } from 'lucide-react';
import { getMovieById, getShowtimes } from '@/features/catalog/customer/services/movieService';
import {
  formatGenres,
  formatDuration,
  formatDate,
  getAgeRatingLabel,
  getYoutubeEmbedUrl
} from '@/utils/formatters';

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
  const [selectedDateIndex, setSelectedDateIndex] = useState(0);

  const [showtimes, setShowtimes] = useState([]);
  const [loadingShowtimes, setLoadingShowtimes] = useState(false);

  // Generate 5 dates starting from today
  const dates = useMemo(() => {
    const list = [];
    const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    for (let i = 0; i < 5; i++) {
      const d = new Date();
      d.setDate(d.getDate() + i);
      
      const label = i === 0 ? 'Hôm nay' : i === 1 ? 'Ngày mai' : dayNames[d.getDay()];
      const dd = String(d.getDate()).padStart(2, '0');
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const yyyy = d.getFullYear();
      
      list.push({
        label,
        dateStr: `${dd}/${mm}`,
        dateQuery: `${yyyy}-${mm}-${dd}`
      });
    }
    return list;
  }, []);

  const activeDate = dates[selectedDateIndex];

  const fetchMovieDetail = useCallback(async () => {
    if (!movieId) return;
    
    setLoading(true);
    setError(null);
    try {
      const data = await getMovieById(movieId);
      if (!data) {
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

  const fetchShowtimesData = useCallback(async () => {
    if (!movie || !activeDate) return;
    setLoadingShowtimes(true);
    try {
      const showtimeData = await getShowtimes({
        movieSlug: movie.slug,
        date: activeDate.dateQuery
      });
      setShowtimes(showtimeData.data || showtimeData.content || []);
    } catch (err) {
      console.error("Failed to load showtimes:", err);
      setShowtimes([]);
    } finally {
      setLoadingShowtimes(false);
    }
  }, [movie, activeDate]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMovieDetail();
  }, [fetchMovieDetail]);

  useEffect(() => {
    if (movie) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      fetchShowtimesData();
    }
  }, [movie, activeDate, fetchShowtimesData]);

  // Set document title once movie details are loaded
  useEffect(() => {
    if (movie && movie.title) {
      document.title = `${movie.title} - LoraFilm`;
    } else {
      document.title = "LoraFilm - Chi tiết phim";
    }
  }, [movie]);

  const handleBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate('/movies');
    }
  };

  const handleGoHome = () => {
    navigate('/');
  };

  // Group showtimes by cinema, then by movie version format name
  const showtimesByCinema = useMemo(() => {
    const grouped = {};
    showtimes.forEach(st => {
      const cinemaName = st.cinema?.name || 'Rạp Liên Kết';
      if (!grouped[cinemaName]) {
        grouped[cinemaName] = {};
      }
      
      const formatLabel = st.movieVersion?.versionName || st.movieVersion?.format || '2D Digital';
      if (!grouped[cinemaName][formatLabel]) {
        grouped[cinemaName][formatLabel] = [];
      }
      
      grouped[cinemaName][formatLabel].push(st);
    });

    // Sort times chronologically for each group
    Object.keys(grouped).forEach(cinema => {
      Object.keys(grouped[cinema]).forEach(format => {
        grouped[cinema][format].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
      });
    });

    return grouped;
  }, [showtimes]);

  const posterUrl = useMemo(() => {
    if (!movie) return FALLBACK_POSTER;
    return movie.primaryPoster || (movie.media && movie.media.find(m => m.mediaType === 'POSTER')?.url) || FALLBACK_POSTER;
  }, [movie]);

  const trailerUrl = useMemo(() => {
    if (!movie) return null;
    return movie.media && movie.media.find(m => m.mediaType === 'TRAILER')?.url;
  }, [movie]);

  const directorNames = useMemo(() => {
    if (!movie || !movie.directors) return 'Đang cập nhật';
    return movie.directors.map(d => d.fullName).join(', ');
  }, [movie]);

  const actorNames = useMemo(() => {
    if (!movie || !movie.actors) return 'Đang cập nhật';
    return movie.actors.map(a => a.fullName).join(', ');
  }, [movie]);

  const ageRatingLabelMeta = useMemo(() => {
    if (!movie) return { label: 'P', bgClass: 'bg-emerald-500 text-black', description: '' };
    return getAgeRatingLabel(movie.ageRating);
  }, [movie]);

  const formattedStartTime = (timeString) => {
    const d = new Date(timeString);
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
  };

  return (
    <div className="flex flex-col min-h-screen bg-brand-dark text-zinc-100 selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
      {/* Cinematic Ambient Backdrop */}
      {movie && (
        <div className="absolute top-0 left-0 w-full h-[520px] md:h-[600px] overflow-hidden z-0 pointer-events-none select-none">
          <img 
            src={posterUrl} 
            alt="" 
            className="w-full h-full object-cover filter blur-2xl scale-110 opacity-20 transform-gpu will-change-transform"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-brand-dark via-brand-dark/75 to-transparent" />
          <div className="absolute inset-0 bg-brand-dark/50" />
        </div>
      )}

      <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-7xl mx-auto w-full relative z-10">
        
        {loading ? (
          <MovieDetailSkeleton />
        ) : error ? (
          /* Error State Panel */
          <div className="w-full">
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
                    className="bg-brand-orange hover:bg-opacity-90 text-white font-bold px-5 py-2 rounded-full text-xs transition-all duration-300 cursor-pointer"
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
            <div className="pb-6 border-b border-zinc-900/40 mb-8 flex justify-between items-center">
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
              <div className="w-52 md:w-64 aspect-[2/3] shrink-0 self-center md:self-start">
                <div className="w-full h-full rounded-2xl overflow-hidden bg-zinc-900 border border-zinc-850 shadow-[0_25px_50px_-12px_rgba(0,0,0,0.8)] relative group">
                  <img
                    src={posterUrl}
                    alt={movie.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = FALLBACK_POSTER;
                    }}
                  />
                  {trailerUrl && (
                    <button
                      onClick={() => setActiveTrailerUrl(getYoutubeEmbedUrl(trailerUrl))}
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
                  <span
                    className={`text-xs font-mono font-black uppercase tracking-widest border px-3 py-1 rounded shadow-sm ${ageRatingLabelMeta.bgClass}`}
                    title={ageRatingLabelMeta.description}
                  >
                    {ageRatingLabelMeta.label}
                  </span>
                  
                  <span className="text-xs text-zinc-400 font-semibold bg-zinc-900/80 px-3 py-1 rounded border border-zinc-800">
                    {formatGenres(movie.genres)}
                  </span>
                </div>

                {/* Movie Title */}
                <h1 className="text-3xl md:text-5xl font-black text-white tracking-tight uppercase leading-tight mb-4 drop-shadow-[0_4px_12px_rgba(0,0,0,0.9)]">
                  {movie.title}
                </h1>

                {/* Subtitle */}
                {movie.originalTitle && (
                  <h2 className="text-zinc-400 text-sm font-semibold mb-6 italic">
                    {movie.originalTitle}
                  </h2>
                )}

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
                  <p className="text-sm leading-relaxed text-zinc-300 font-normal max-w-3xl whitespace-pre-line drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
                    {movie.synopsis || "Nội dung phim đang được cập nhật."}
                  </p>
                </div>

                {/* Production Metadata */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pb-8 mb-8 border-b border-zinc-900/60 text-sm max-w-3xl">
                  <div>
                    <span className="text-xs font-black uppercase tracking-widest text-zinc-500 block mb-1">Đạo diễn</span>
                    <span className="text-zinc-300 font-semibold">{directorNames}</span>
                  </div>
                  <div>
                    <span className="text-xs font-black uppercase tracking-widest text-zinc-500 block mb-1">Diễn viên</span>
                    <span className="text-zinc-300 font-semibold">{actorNames}</span>
                  </div>
                </div>

                {/* Main Action Buttons */}
                <div className="flex flex-wrap gap-4">
                  {trailerUrl && (
                    <button
                      onClick={() => setActiveTrailerUrl(getYoutubeEmbedUrl(trailerUrl))}
                      className="group flex items-center gap-2 bg-transparent border border-white hover:border-brand-orange hover:text-brand-orange text-white font-bold px-6 py-3.5 rounded-full transition-all duration-300 cursor-pointer text-xs uppercase tracking-wider"
                    >
                      <Play className="w-4 h-4 fill-current group-hover:text-brand-orange" />
                      Xem Trailer
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Showtime Selection System */}
            <div className="w-full pt-8 border-t border-zinc-900/40">
              <div className="bg-zinc-900/50 border border-zinc-800/80 rounded-3xl p-6 md:p-8 backdrop-blur-sm">
                <h2 className="text-xl font-black uppercase tracking-wider text-white mb-6">
                  Lịch Chiếu & Đặt Vé
                </h2>

                {/* 5-Day Horizontal Calendar Tab Selector */}
                <div className="flex gap-2 overflow-x-auto pb-4 scrollbar-thin scrollbar-thumb-zinc-800 mb-8">
                  {dates.map((item, idx) => (
                    <button
                      key={idx}
                      onClick={() => setSelectedDateIndex(idx)}
                      className={`flex flex-col items-center justify-center px-5 py-3 rounded-2xl min-w-[90px] border transition-all duration-300 shrink-0 cursor-pointer ${
                        selectedDateIndex === idx
                          ? 'bg-brand-orange border-brand-orange text-white font-bold scale-105 shadow-lg shadow-brand-orange/20'
                          : 'bg-zinc-900 border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:border-zinc-700'
                      }`}
                    >
                      <span className="text-[10px] uppercase tracking-wider mb-1">{item.label}</span>
                      <span className="text-sm font-black">{item.dateStr}</span>
                    </button>
                  ))}
                </div>

                {/* Accordions Grouped by Cinema */}
                {loadingShowtimes ? (
                  <div className="flex justify-center items-center py-12">
                    <div className="w-8 h-8 border-2 border-brand-orange border-t-transparent rounded-full animate-spin"></div>
                  </div>
                ) : Object.keys(showtimesByCinema).length > 0 ? (
                  <div className="space-y-6">
                    {Object.keys(showtimesByCinema).map((cinemaName, cIdx) => (
                      <div
                        key={cIdx}
                        className="bg-zinc-950/40 border border-zinc-850 rounded-2xl p-5 md:p-6"
                      >
                        <h3 className="text-sm md:text-base font-bold text-white mb-4 border-l-2 border-brand-orange pl-3 uppercase tracking-wider">
                          {cinemaName}
                        </h3>

                        <div className="space-y-4">
                          {Object.keys(showtimesByCinema[cinemaName]).map((formatLabel, fIdx) => (
                            <div key={fIdx} className="flex flex-col sm:flex-row sm:items-center gap-4 py-2 border-b border-zinc-900/60 last:border-0">
                              <span className="text-[10px] font-black text-brand-orange uppercase tracking-widest shrink-0 w-24">
                                {formatLabel}
                              </span>
                              
                              <div className="flex flex-wrap gap-3">
                                {showtimesByCinema[cinemaName][formatLabel].map((st) => (
                                  <button
                                    key={st.showtimePublicId}
                                    onClick={() => navigate(`/seat-selection?showtimeId=${st.showtimePublicId}`)}
                                    className="bg-zinc-900 hover:bg-brand-orange text-zinc-300 hover:text-white border border-zinc-800 hover:border-brand-orange text-xs md:text-sm font-semibold py-2px px-4 py-2 rounded-xl transition-all duration-300 cursor-pointer"
                                  >
                                    {formattedStartTime(st.startTime)}
                                  </button>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center p-12 text-center bg-zinc-950/20 border border-zinc-900 rounded-2xl max-w-xl mx-auto my-4 space-y-3">
                    <div className="w-10 h-10 rounded-full bg-zinc-900 flex items-center justify-center text-zinc-500">
                      <Film className="w-5 h-5" />
                    </div>
                    <h3 className="text-sm font-bold text-zinc-300">Không có suất chiếu.</h3>
                    <p className="text-xs text-zinc-500">
                      Vui lòng chọn ngày chiếu khác để tìm kiếm vé phim.
                    </p>
                  </div>
                )}
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
