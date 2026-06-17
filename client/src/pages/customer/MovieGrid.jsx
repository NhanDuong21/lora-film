import { useState, useMemo } from 'react';
import { Ticket, Play, X } from 'lucide-react';
import { useData } from '../../contexts/DataContext';

export default function MovieGrid({ onSelectMovie, onNavigate, activeTab: propActiveTab, onChangeActiveTab, onBuyTicket }) {
  const [localActiveTab, setLocalActiveTab] = useState('NOW_SHOWING');
  const activeTab = propActiveTab !== undefined ? propActiveTab : localActiveTab;
  const setActiveTab = onChangeActiveTab !== undefined ? onChangeActiveTab : setLocalActiveTab;
  const [activeTrailerUrl, setActiveTrailerUrl] = useState(null);
  const { movies } = useData();

  // Filter movies by the active status tab
  const filteredMovies = useMemo(() => {
    return movies.filter((movie) => {
      if (activeTab === 'NOW_SHOWING') {
        return movie.status === 'NOW_SHOWING' || movie.status === 'DANG_CHIEU' || movie.status === 'SHOWING';
      }
      if (activeTab === 'COMING_SOON') {
        return movie.status === 'COMING_SOON' || movie.status === 'SAP_CHIEU' || movie.status === 'UPCOMING';
      }
      return movie.status === activeTab;
    });
  }, [movies, activeTab]);

  // Paginated/Sliced subset - strictly display the first 8 movie items on the homepage
  const activeMovies = useMemo(() => {
    return filteredMovies.slice(0, 8);
  }, [filteredMovies]);

  const handleSeeMoreClick = () => {
    if (onNavigate) {
      onNavigate('discovery', { initialTab: activeTab });
    }
  };

  return (
    <section id="phim" className="relative px-6 md:px-12 py-16 bg-zinc-950">
      {/* Grid Header & Filters */}
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-8 border-b border-zinc-800/80 pb-4">
        <div className="flex items-center gap-8">
          <button
            onClick={() => {
              setActiveTab('NOW_SHOWING');
            }}
            className={`text-lg md:text-xl font-black tracking-wider uppercase pb-2 transition-all duration-300 relative ${
              activeTab === 'NOW_SHOWING'
                ? 'text-brand-coral border-b-2 border-brand-coral drop-shadow-[0_0_10px_rgba(216,129,116,0.4)]'
                : 'text-zinc-500 hover:text-zinc-300'
            }`}
          >
            Phim Đang Chiếu
          </button>
          <button
            onClick={() => {
              setActiveTab('COMING_SOON');
            }}
            className={`text-lg md:text-xl font-black tracking-wider uppercase pb-2 transition-all duration-300 relative ${
              activeTab === 'COMING_SOON'
                ? 'text-brand-coral border-b-2 border-brand-coral drop-shadow-[0_0_10px_rgba(216,129,116,0.4)]'
                : 'text-zinc-500 hover:text-zinc-300'
            }`}
          >
            Phim Sắp Chiếu
          </button>
        </div>
      </div>

      {/* Main Grid Workspace Framework */}
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 text-zinc-100 bg-zinc-950">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 px-6 md:px-12 py-10">
          {activeMovies.map((movie) => (
            <div
              key={movie.id}
              onClick={() => onSelectMovie && onSelectMovie(movie.id)}
              className="w-full flex flex-col group cursor-pointer overflow-visible"
            >
              {/* The Dynamic Colored Glow Framework / Pop-out Card */}
              <div className="group relative w-full aspect-[2/3] rounded-2xl bg-zinc-900 border border-zinc-800/80 transition-all duration-500 ease-out hover:translate-y-[-8px] hover:shadow-[0_35px_60px_-15px_rgba(245,158,11,0.25)] hover:border-amber-500/40 cursor-pointer overflow-visible">
                
                {/* Layer 1: The Background Frame */}
                <div className="z-10 absolute inset-0 rounded-2xl overflow-hidden">
                  <img 
                    src={movie.posterUrl} 
                    alt={movie.title}
                    className="w-full h-full object-cover rounded-2xl transition-transform duration-500 group-hover:scale-[1.02]" 
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = 'https://placehold.co/500x750?text=LoraFilm';
                    }}
                  />
                </div>
                
                {/* Layer 2: The Dark Cinema Gradients Interceptor */}
                <div className="z-20 absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/20 to-transparent opacity-90 rounded-2xl" />
                
                {/* Layer 3: The Extended Foreground Elements */}
                <div className="z-30 absolute bottom-0 left-0 w-full p-5 flex flex-col transform transition-transform duration-500 group-hover:scale-110 group-hover:translate-y-[-4px]">
                  {/* Age Rating Badge */}
                  <span className="text-[10px] font-mono font-black uppercase tracking-widest text-amber-400 bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded shadow-sm w-fit mb-2">
                    {movie.ageRating || 'T13'}
                  </span>
                  
                  {/* Text Title (Bypass single-line truncation locks, wraps beautifully) */}
                  <h3 className="text-sm md:text-base font-black text-white whitespace-normal break-words leading-tight drop-shadow-md mt-1 block">
                    {movie.title}
                  </h3>
                  
                  {/* Genre */}
                  <p className="text-[10px] text-zinc-400 mt-1 truncate">
                    {movie.genres ? movie.genres.join(', ') : movie.genre}
                  </p>
                </div>

                {/* Animated Poster Hover Overlay Architecture */}
                <div className="absolute inset-0 bg-black/75 flex flex-col items-center justify-center gap-3 opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-40 p-4 rounded-2xl">
                  {/* Nút "Mua Vé" */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      const defaultBooking = {
                        movieId: movie.id,
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
                      }
                    }}
                    className="w-full max-w-[160px] bg-orange-500 hover:bg-orange-600 text-white font-medium py-2 px-4 rounded-full flex items-center justify-center gap-2 transition-all text-sm shadow-md shadow-orange-500/10 cursor-pointer"
                  >
                    <Ticket className="w-4 h-4" />
                    Mua Vé
                  </button>

                  {/* Nút "Xem Trailer" */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      const trailerUrl = movie.trailerEmbedUrl || (movie.trailerId ? `https://www.youtube.com/embed/${movie.trailerId}` : '');
                      setActiveTrailerUrl(trailerUrl);
                    }}
                    className="w-full max-w-[160px] bg-transparent border border-white hover:border-amber-400 hover:text-amber-400 text-white font-medium py-2 px-4 rounded-full flex items-center justify-center gap-2 transition-all text-sm cursor-pointer"
                  >
                    <Play className="w-4 h-4 fill-current text-white" />
                    Xem Trailer
                  </button>
                </div>

              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Global Catalog Redirection trigger button */}
      {filteredMovies.length > 8 && (
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
