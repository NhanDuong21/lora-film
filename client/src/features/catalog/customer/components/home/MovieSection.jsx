import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Ticket, Play, X, ChevronLeft, ChevronRight, AlertCircle, RefreshCw } from 'lucide-react';

import { useMoviesQuery } from '@/hooks/useHomepageMovies';
import MovieSectionSkeleton from './MovieSectionSkeleton';
import {
  formatGenres,
  formatDuration,
  formatDate,
  getAgeRatingLabel,
  getYoutubeEmbedUrl
} from '@/utils/formatters';

const FALLBACK_POSTER = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='500' height='750' viewBox='0 0 500 750'><rect width='500' height='750' fill='%2318181b'/><text x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-weight='bold' font-size='24' fill='%2352525b'>LORA FILM</text><text x='50%25' y='55%25' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='14' fill='%233f3f46'>Không có ảnh bìa</text></svg>";

export default function MovieGrid({ onSelectMovie, onNavigate, activeTab: propActiveTab, onChangeActiveTab, onBuyTicket }) {
  const navigate = useNavigate();
  // Internal tab state as fallback
  const [localActiveTab, setLocalActiveTab] = useState('NOW_SHOWING');
  const activeTab = propActiveTab !== undefined ? propActiveTab : localActiveTab;
  const setActiveTab = onChangeActiveTab !== undefined ? onChangeActiveTab : setLocalActiveTab;

  const [activeTrailerUrl, setActiveTrailerUrl] = useState(null);

  // Setup queries for both tabs independently
  const nowShowingQuery = useMoviesQuery({
    status: 'NOW_SHOWING',
    sort: 'createdAt,desc',
    size: 8
  });

  const upcomingQuery = useMoviesQuery({
    status: 'UPCOMING',
    sort: 'createdAt,desc',
    size: 8
  });

  // Extract variables for the active tab
  const activeQuery = activeTab === 'NOW_SHOWING' ? nowShowingQuery : upcomingQuery;
  const {
    movies: activeMovies,
    loading,
    isRefreshing,
    error,
    page,
    setPage,
    totalPages,
    first,
    last,
    retry
  } = activeQuery;

  const handlePageChange = (newPage) => {
    if (newPage < 0 || newPage >= totalPages) return;
    setPage(newPage);
    
    // Smooth scroll to the movie section as requested
    const element = document.getElementById('phim');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleSeeMoreClick = () => {
    if (onNavigate) {
      onNavigate('discovery', { initialTab: activeTab });
    } else {
      navigate('/movies', { state: { initialTab: activeTab } });
    }
  };

  return (
    <section id="phim" className="relative px-6 md:px-12 py-16 bg-zinc-950 border-t border-zinc-900/60">
      {/* Grid Header & Filters */}
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-8 border-b border-zinc-800/80 pb-4">
        <div className="flex items-center gap-8">
          <button
            onClick={() => setActiveTab('NOW_SHOWING')}
            className={`text-lg md:text-xl font-black tracking-wider uppercase pb-2 transition-all duration-300 relative ${
              activeTab === 'NOW_SHOWING'
                ? 'text-brand-orange border-b-2 border-brand-orange drop-shadow-[0_0_10px_rgba(216,129,116,0.4)]'
                : 'text-zinc-500 hover:text-zinc-300'
            }`}
          >
            Phim Đang Chiếu
          </button>
          <button
            onClick={() => setActiveTab('UPCOMING')}
            className={`text-lg md:text-xl font-black tracking-wider uppercase pb-2 transition-all duration-300 relative ${
              activeTab === 'UPCOMING'
                ? 'text-brand-orange border-b-2 border-brand-orange drop-shadow-[0_0_10px_rgba(216,129,116,0.4)]'
                : 'text-zinc-500 hover:text-zinc-300'
            }`}
          >
            Phim Sắp Chiếu
          </button>
        </div>
      </div>

      {/* Main Content Workspace Framework */}
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 text-zinc-100 bg-zinc-950">
        
        {loading ? (
          <MovieSectionSkeleton />
        ) : error ? (
          /* Error State UI */
          <div className="flex flex-col items-center justify-center py-16 text-center bg-zinc-950 border border-red-950/40 rounded-2xl max-w-lg mx-auto px-6 space-y-4 shadow-xl">
            <div className="w-12 h-12 rounded-full bg-red-950/20 border border-red-500/20 flex items-center justify-center text-red-500">
              <AlertCircle className="w-6 h-6 animate-pulse" />
            </div>
            <h3 className="text-base font-bold text-zinc-200">Không thể tải danh sách phim.</h3>
            <p className="text-xs text-zinc-500 max-w-xs">
              Đã xảy ra lỗi khi kết nối với máy chủ. Vui lòng thử lại sau.
            </p>
            <button
              onClick={retry}
              className="flex items-center gap-2 bg-brand-orange hover:bg-orange-600 text-white font-medium py-2 px-6 rounded-full transition-all text-xs cursor-pointer shadow-md shadow-brand-orange/10"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              Thử lại
            </button>
          </div>
        ) : activeMovies.length === 0 ? (
          /* Empty State UI */
          <div className="flex flex-col items-center justify-center py-16 text-center bg-zinc-900/20 border border-zinc-900/60 rounded-2xl max-w-lg mx-auto px-6 space-y-4 shadow-xl">
            <div className="w-12 h-12 rounded-full bg-zinc-800/80 flex items-center justify-center text-zinc-500">
              <Ticket className="w-6 h-6" />
            </div>
            <p className="text-sm font-semibold text-zinc-400">
              {activeTab === 'NOW_SHOWING' ? 'Hiện chưa có phim đang chiếu.' : 'Hiện chưa có phim sắp chiếu.'}
            </p>
          </div>
        ) : (
          /* Movie Grid View */
          <>
            <div className={`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 px-6 md:px-12 py-10 transition-opacity duration-300 ${isRefreshing ? 'opacity-40 pointer-events-none' : ''}`}>
              {activeMovies.map((movie) => {
                const ratingMeta = getAgeRatingLabel(movie.ageRating);
                return (
                  <div
                    key={movie.publicId || movie.id}
                    onClick={() => {
                      const idToUse = movie.publicId || movie.id;
                      if (onSelectMovie) {
                        onSelectMovie(idToUse);
                      } else {
                        navigate(`/movies/${idToUse}`);
                      }
                    }}
                    className="w-full flex flex-col group cursor-pointer overflow-visible"
                  >
                    {/* The Dynamic Colored Glow Framework / Pop-out Card */}
                    <div className="group relative w-full aspect-[2/3] rounded-2xl bg-zinc-900 border border-zinc-800/80 transition-all duration-500 ease-out hover:translate-y-[-8px] hover:shadow-[0_35px_60px_-15px_rgba(245,158,11,0.25)] hover:border-amber-500/40 cursor-pointer overflow-visible">
                      
                      {/* Layer 1: The Background Frame */}
                      <div className="z-10 absolute inset-0 rounded-2xl overflow-hidden bg-zinc-900">
                        <img 
                          src={movie.primaryPoster || movie.posterUrl || FALLBACK_POSTER} 
                          alt={movie.title}
                          loading="lazy"
                          className="w-full h-full object-cover rounded-2xl transition-transform duration-500 group-hover:scale-[1.02]" 
                          onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = FALLBACK_POSTER;
                          }}
                        />
                      </div>
                      
                      {/* Layer 2: The Dark Cinema Gradients Interceptor */}
                      <div className="z-20 absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/20 to-transparent opacity-90 rounded-2xl" />
                      
                      {/* Layer 3: The Extended Foreground Elements */}
                      <div className="z-30 absolute bottom-0 left-0 w-full p-5 flex flex-col transform transition-transform duration-500 group-hover:scale-105 group-hover:translate-y-[-4px]">
                        {/* Age Rating Badge */}
                        <span className={`text-[10px] font-mono font-black uppercase tracking-widest border px-2 py-0.5 rounded shadow-sm w-fit mb-2 ${ratingMeta.bgClass}`}>
                          {ratingMeta.label}
                        </span>
                        
                        {/* Text Title */}
                        <h3 className="text-sm md:text-base font-black text-white whitespace-normal break-words leading-tight drop-shadow-md mt-1 block">
                          {movie.title}
                        </h3>
                        
                        {/* Genre */}
                        <p className="text-[10px] text-zinc-400 mt-1.5 truncate">
                          {formatGenres(movie.genres)}
                        </p>

                        {/* Duration & Release Date */}
                        <div className="flex items-center gap-2 mt-1 text-[9px] text-zinc-500 font-bold uppercase tracking-wider">
                          <span>{formatDuration(movie.durationMinutes)}</span>
                          <span>•</span>
                          <span>{formatDate(movie.releaseDate)}</span>
                        </div>
                      </div>

                      {/* Animated Poster Hover Overlay Architecture */}
                      <div className="absolute inset-0 bg-black/75 flex flex-col items-center justify-center gap-3 opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-40 p-4 rounded-2xl">
                        {/* Nút "Mua Vé" */}
                        {movie.status === 'NOW_SHOWING' && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              const defaultBooking = {
                                movieId: movie.publicId || movie.id,
                                movieTitle: movie.title,
                                cinema: 'Lora Nguyễn Du',
                                time: '19:30',
                                format: '2D DIGITAL',
                                date: new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
                                fullDate: new Date().toLocaleDateString('vi-VN'),
                                selectedSeats: []
                              };
                              if (onBuyTicket) {
                                onBuyTicket(defaultBooking);
                              } else if (onNavigate) {
                                onNavigate('seats', defaultBooking);
                              } else {
                                navigate('/booking', { state: { bookingPayload: defaultBooking } });
                              }
                            }}
                            className="w-full max-w-[160px] bg-brand-orange hover:bg-orange-600 text-white font-medium py-2 px-4 rounded-full flex items-center justify-center gap-2 transition-all text-sm shadow-md shadow-brand-orange/10 cursor-pointer"
                          >
                            <Ticket className="w-4 h-4" />
                            Mua Vé
                          </button>
                        )}

                        {/* Nút "Xem Trailer" */}
                        {movie.trailerUrl && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setActiveTrailerUrl(getYoutubeEmbedUrl(movie.trailerUrl));
                            }}
                            className="w-full max-w-[160px] bg-transparent border border-white hover:border-amber-400 hover:text-amber-400 text-white font-medium py-2 px-4 rounded-full flex items-center justify-center gap-2 transition-all text-sm cursor-pointer"
                          >
                            <Play className="w-4 h-4 fill-current text-white" />
                            Xem Trailer
                          </button>
                        )}
                      </div>

                    </div>
                  </div>
                );
              })}
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex justify-center items-center gap-2 mt-8">
                <button
                  onClick={() => handlePageChange(page - 1)}
                  disabled={first || isRefreshing}
                  className="p-2 rounded-full border border-zinc-800 bg-zinc-900 text-zinc-400 hover:text-white hover:border-zinc-700 disabled:opacity-40 disabled:cursor-not-allowed transition-all duration-300"
                  aria-label="Trang trước"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                
                <div className="flex items-center gap-1.5">
                  {Array.from({ length: totalPages }).map((_, idx) => (
                    <button
                      key={idx}
                      disabled={isRefreshing}
                      onClick={() => handlePageChange(idx)}
                      className={`w-8 h-8 rounded-full text-xs font-bold transition-all duration-300 ${
                        page === idx
                          ? "bg-brand-orange text-white shadow-md shadow-brand-orange/20"
                          : "border border-zinc-800 bg-zinc-900 text-zinc-400 hover:text-white hover:border-zinc-700"
                      } disabled:opacity-50 disabled:cursor-not-allowed`}
                    >
                      {idx + 1}
                    </button>
                  ))}
                </div>
                
                <button
                  onClick={() => handlePageChange(page + 1)}
                  disabled={last || isRefreshing}
                  className="p-2 rounded-full border border-zinc-800 bg-zinc-900 text-zinc-400 hover:text-white hover:border-zinc-700 disabled:opacity-40 disabled:cursor-not-allowed transition-all duration-300"
                  aria-label="Trang sau"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {/* Global Catalog Redirection trigger button */}
      {!loading && !error && activeMovies.length > 0 && totalPages > 1 && (
        <div className="flex justify-center mt-12">
          <button
            onClick={handleSeeMoreClick}
            className="bg-zinc-900 hover:bg-zinc-800 text-zinc-300 hover:text-white border border-zinc-800 font-bold px-8 py-3.5 rounded-full transition-all duration-300 transform hover:scale-105 shadow-md uppercase tracking-wider text-xs"
          >
            Xem thêm
          </button>
        </div>
      )}

      {/* Full-Screen Cinematic Lightbox Pop-up Component */}
      {activeTrailerUrl && (
        <div 
          className="fixed inset-0 bg-zinc-950/90 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-all animate-fade-in"
          onClick={() => setActiveTrailerUrl(null)}
        >
          {/* Player Chassis Box */}
          <div 
            className="relative w-full max-w-4xl aspect-video bg-black rounded-2xl overflow-hidden border border-zinc-800 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Dismissal Button Widget */}
            <button
              onClick={() => setActiveTrailerUrl(null)}
              className="absolute top-4 right-4 bg-zinc-900/80 text-zinc-400 hover:text-white p-2 rounded-full transition-colors z-20 cursor-pointer"
              aria-label="Close trailer"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Secure Video IFrame Instance */}
            <iframe
              src={`${activeTrailerUrl}?autoplay=1&rel=0&modestbranding=1`}
              title="LoraFilm Cinematic Trailer Player"
              className="w-full h-full border-0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
            ></iframe>
          </div>
        </div>
      )}
    </section>
  );
}
