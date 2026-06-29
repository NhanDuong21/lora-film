import { useState, useEffect, useMemo } from 'react';
import { 
  Play, RefreshCw, AlertCircle 
} from 'lucide-react';
import TrailerModal from '../../components/common/TrailerModal';
import { getMovies, getGenres } from '../../services/movieService';
import { getYoutubeEmbedUrl } from '../../utils/formatters';



const STATUS_LIST = [
  { label: 'Tất cả trạng thái', value: 'ALL' },
  { label: 'Phim Đang Chiếu', value: 'NOW_SHOWING' },
  { label: 'Phim Sắp Chiếu', value: 'COMING_SOON' }
];

const SORT_LIST = [
  { label: 'Mới Nhất', value: 'releaseDate,desc' },
  { label: 'Cũ Nhất', value: 'releaseDate,asc' },
  { label: 'Tên A-Z', value: 'title,asc' }
];

export default function MovieDiscoveryView({ initialTab = 'ALL' }) {
  const [movies, setMovies] = useState([]);
  const [genres, setGenres] = useState([{ label: 'Tất cả thể loại', value: 'ALL' }]);
  const [loading, setLoading] = useState(true);

  const [selectedGenre, setSelectedGenre] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState(initialTab);
  const [selectedSort, setSelectedSort] = useState('releaseDate,desc');
  
  const [activeTrailerUrl, setActiveTrailerUrl] = useState(null);

  useEffect(() => {
    const fetchMovies = async () => {
      setLoading(true);
      try {
        const [moviesData, genresData] = await Promise.all([
          getMovies({ size: 100 }), // Fetch a large batch for client-side filter
          getGenres()
        ]);
        if (moviesData) {
          const movieList = Array.isArray(moviesData) ? moviesData : (moviesData.content || []);
          setMovies(movieList);
        }
        if (genresData && Array.isArray(genresData)) {
          const formattedGenres = genresData.map(g => ({
            label: g.genreName,
            value: g.id
          }));
          setGenres([{ label: 'Tất cả thể loại', value: 'ALL' }, ...formattedGenres]);
        }
      } catch (error) {
        console.error("Failed to fetch data:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchMovies();
  }, []);

  const filteredAndSortedMovies = useMemo(() => {
    let result = [...movies];

    if (selectedGenre !== 'ALL') {
      result = result.filter(m => 
        m.genres && m.genres.some(g => g.id && g.id.toString() === selectedGenre.toString())
      );
    }

    if (selectedStatus !== 'ALL') {
      result = result.filter(m => {
        if (selectedStatus === 'NOW_SHOWING') {
          return m.status === 'NOW_SHOWING' || m.status === 'SHOWING';
        }
        if (selectedStatus === 'COMING_SOON') {
          return m.status === 'COMING_SOON' || m.status === 'UPCOMING';
        }
        return m.status === selectedStatus;
      });
    }

    if (selectedSort) {
      const [field, direction] = selectedSort.split(',');
      result.sort((a, b) => {
        if (field === 'releaseDate') {
          const dateA = new Date(a.releaseDate).getTime();
          const dateB = new Date(b.releaseDate).getTime();
          return direction === 'asc' ? dateA - dateB : dateB - dateA;
        }
        if (field === 'title') {
          return direction === 'asc' ? a.title.localeCompare(b.title) : b.title.localeCompare(a.title);
        }
        return 0;
      });
    }

    return result;
  }, [movies, selectedGenre, selectedStatus, selectedSort]);

  const handleResetFilters = () => {
    setSelectedGenre('ALL');
    setSelectedStatus('ALL');
    setSelectedSort('releaseDate,desc');
  };

  const handleTrailerOpen = (e, trailerUrl) => {
    e.stopPropagation();
    setActiveTrailerUrl(getYoutubeEmbedUrl(trailerUrl));
  };

  return (
    <div className="bg-brand-dark text-zinc-100 min-h-screen py-8 px-4 md:px-8">
      <div className="max-w-7xl mx-auto space-y-8">
        
        {/* Header Breadcrumbs block */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-zinc-900">
          <div>
            <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">Khám Phá Điện Ảnh</h1>
            <p className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mt-1">Lọc danh sách phim đang chiếu & sắp chiếu theo nhu cầu</p>
          </div>
        </div>

        {/* 1. UPPER HORIZONTAL BAR: Multi-Criteria Filter Widget */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-5 flex flex-wrap gap-4 items-center justify-between shadow-2xl">
          
          <div className="flex flex-wrap gap-3 flex-grow lg:flex-nowrap">
            {/* Thể loại Select */}
            <div className="flex flex-col gap-1.5 flex-grow sm:flex-grow-0">
              <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider pl-1">Thể Loại</label>
              <select
                value={selectedGenre}
                onChange={(e) => setSelectedGenre(e.target.value)}
                className="bg-zinc-950 border border-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold rounded-xl py-2.5 px-3 focus:border-brand-orange focus:outline-none transition-colors"
              >
                {genres.map(g => (
                  <option key={g.value} value={g.value}>{g.label}</option>
                ))}
              </select>
            </div>

            {/* Đang chiếu / Sắp chiếu Select */}
            <div className="flex flex-col gap-1.5 flex-grow sm:flex-grow-0">
              <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider pl-1">Trạng Thái</label>
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="bg-zinc-950 border border-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold rounded-xl py-2.5 px-3 focus:border-brand-orange focus:outline-none transition-colors"
              >
                {STATUS_LIST.map(s => (
                  <option key={s.value} value={s.value}>{s.label}</option>
                ))}
              </select>
            </div>

            {/* Sắp xếp Select */}
            <div className="flex flex-col gap-1.5 flex-grow sm:flex-grow-0">
              <label className="text-[9px] text-zinc-500 font-black uppercase tracking-wider pl-1">Sắp Xếp Theo</label>
              <select
                value={selectedSort}
                onChange={(e) => setSelectedSort(e.target.value)}
                className="bg-zinc-950 border border-zinc-800 hover:border-zinc-700 text-zinc-200 text-xs font-semibold rounded-xl py-2.5 px-3 focus:border-brand-orange focus:outline-none transition-colors"
              >
                {SORT_LIST.map(s => (
                  <option key={s.value} value={s.value}>{s.label}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Reset Filters action */}
          <button
            onClick={handleResetFilters}
            className="flex items-center gap-1.5 bg-zinc-950 hover:bg-zinc-800 text-zinc-400 hover:text-white border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-bold transition-all focus:outline-none shrink-0"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Đặt lại bộ lọc</span>
          </button>

        </div>

        {/* ASYMMETRIC SPLIT SCREEN VIEW GRID */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* LEFT PANEL: Filtered Movie Row List (2/3 Width) */}
          <div className="lg:col-span-2 space-y-6">
            
            {loading ? (
                <div className="text-center py-10 text-zinc-500">Đang tải dữ liệu phim...</div>
            ) : filteredAndSortedMovies.length === 0 ? (
              <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-12 text-center space-y-3">
                <AlertCircle className="w-12 h-12 text-zinc-650 mx-auto" />
                <h3 className="text-sm font-black uppercase tracking-wider text-white">Không Tìm Thấy Phim Phù Hợp</h3>
                <p className="text-zinc-500 text-xs max-w-sm mx-auto">
                  Không có tác phẩm nào khớp với tiêu chuẩn tìm kiếm của bạn. Hãy thử thay đổi bộ lọc hoặc đặt lại bộ lọc.
                </p>
                <button
                  onClick={handleResetFilters}
                  className="bg-brand-orange hover:bg-opacity-95 text-white font-bold py-2.5 px-6 rounded-xl text-xs uppercase tracking-wider transition-colors inline-block mt-2 focus:outline-none"
                >
                  Đặt lại bộ lọc
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {filteredAndSortedMovies.map((movie) => {
                  return (
                    <div 
                      key={movie.id}
                      className="bg-zinc-900 border border-zinc-800 hover:border-zinc-700/80 rounded-3xl p-4 flex gap-4 md:gap-6 shadow-xl transition-all duration-300 relative group overflow-hidden"
                    >
                      {/* Image Block (Left Aspect Ratio container) */}
                      <div className="w-28 sm:w-36 shrink-0 aspect-[2/3] rounded-2xl overflow-hidden bg-zinc-950 border border-zinc-850 relative">
                        <img 
                          src={movie.posterUrl || movie.image} 
                          alt={movie.title}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                        />
                        
                        {/* Hover dual-button overlay on desktop */}
                        <div className="absolute inset-0 bg-black/75 opacity-0 group-hover:opacity-100 flex flex-col items-center justify-center gap-3 transition-opacity duration-300 hidden md:flex p-2 z-10">
                          {movie.trailerUrl && (
                            <button
                                onClick={(e) => handleTrailerOpen(e, movie.trailerUrl)}
                                className="w-full bg-zinc-900 border border-zinc-700 hover:bg-zinc-800 text-white font-bold py-2 rounded-xl text-[10px] uppercase tracking-wider transition-colors flex items-center justify-center gap-1"
                            >
                                <Play className="w-3 h-3 fill-white" />
                                <span>Trailer</span>
                            </button>
                          )}
                        </div>
                      </div>

                      {/* Right Details Block */}
                      <div className="flex-grow flex flex-col justify-between py-1">
                        <div className="space-y-2">
                          
                          {/* Title + Like Pill Header */}
                          <div className="flex items-start justify-between gap-4">
                            <h3 className="font-black text-sm md:text-base text-white leading-snug group-hover:text-brand-orange transition-colors">
                              {movie.title}
                            </h3>
                          </div>

                          {/* Meta Tags */}
                          <div className="flex flex-wrap gap-2 items-center">
                            <span className="text-[9px] font-black uppercase tracking-wider bg-brand-orange/10 text-brand-orange border border-brand-orange/20 px-2 py-0.5 rounded">
                              {movie.ageRating || 'P'}
                            </span>
                            <span className="text-[10px] text-zinc-400 font-bold">
                              {movie.durationMinutes || 120} phút
                            </span>
                            <span className="text-[10px] text-zinc-500 font-semibold">
                              {movie.releaseDate ? new Date(movie.releaseDate).getFullYear() : ''}
                            </span>
                          </div>

                          {/* Short Description */}
                          <p className="text-xs text-zinc-400 leading-relaxed max-w-xl line-clamp-2 md:line-clamp-3">
                            {movie.description}
                          </p>

                        </div>

                        {/* Interactive actions for mobile/tablet */}
                        <div className="flex gap-2 mt-4 md:hidden">
                          {movie.trailerUrl && (
                            <button
                                onClick={(e) => handleTrailerOpen(e, movie.trailerUrl)}
                                className="bg-zinc-950 border border-zinc-800 hover:bg-zinc-800 text-white font-bold px-4 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-colors flex items-center gap-1"
                            >
                                <Play className="w-3.5 h-3.5 fill-white" />
                                <span>Trailer</span>
                            </button>
                          )}
                        </div>

                      </div>
                    </div>
                  );
                })}
              </div>
            )}

          </div>

          {/* RIGHT PANEL: Sticky Booking & Promotion Sidebar (1/3 Width) */}
          <div className="space-y-6 lg:sticky lg:top-24 h-fit">
            
            <div className="bg-zinc-900 border border-zinc-850 rounded-2xl p-5 space-y-4 shadow-2xl">
              <div className="border-b border-zinc-800 pb-2 flex justify-between items-center">
                <span className="text-white text-[10px] font-black uppercase tracking-wider">Phim Nổi Bật</span>
                <span className="text-[8px] font-black uppercase tracking-widest text-brand-yellow animate-pulse">Hot Now</span>
              </div>

              <div className="space-y-4">
                {movies.slice(0, 3).map((movie) => (
                  <div 
                    key={movie.id}
                    className="flex gap-3 hover:bg-white/5 p-1.5 rounded-xl transition-colors cursor-pointer group"
                  >
                    <div className="w-12 h-18 rounded-lg overflow-hidden shrink-0 bg-zinc-950 border border-zinc-800">
                      <img src={movie.posterUrl || movie.image} alt={movie.title} className="w-full h-full object-cover" />
                    </div>

                    <div className="space-y-1.5 flex flex-col justify-center">
                      <h4 className="text-xs font-extrabold text-zinc-200 group-hover:text-brand-orange transition-colors line-clamp-1">
                        {movie.title}
                      </h4>
                      <div className="flex items-center gap-2">
                        <span className="text-[8px] font-black uppercase bg-zinc-950 border border-zinc-800 text-brand-yellow px-1.5 py-0.5 rounded">
                          {movie.ageRating || 'P'}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

          </div>

        </div>

      </div>

      {activeTrailerUrl && (
        <TrailerModal
          isOpen={!!activeTrailerUrl}
          onClose={() => setActiveTrailerUrl(null)}
          trailerUrl={activeTrailerUrl}
        />
      )}
    </div>
  );
}
