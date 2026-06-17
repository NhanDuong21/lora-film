import { useState, useMemo, useCallback } from 'react';
import { Search, Edit3, Trash2, X, Upload, ArrowLeft, Check, Clock, Plus, Youtube } from 'lucide-react';

const GENRE_LIST = [
  'Anime', 'Bí ẩn', 'Cao bồi', 'Chiến tranh', 'Chính kịch', 'Gia đình', 'Giả tưởng', 'Giật gân',
  'Hoạt hình', 'Hài', 'Hành động', 'Học đường', 'Isekai', 'Khoa học viễn tưởng', 'Kinh dị', 'Lịch sử',
  'Phiêu lưu', 'Shounen', 'Siêu nhiên', 'Thể thao', 'Tài liệu', 'Tâm lý', 'Tình cảm', 'Tội phạm',
  'Viễn Tưởng', 'Võ thuật', 'Âm nhạc'
];

const getYouTubeEmbedId = (url) => {
  if (!url) return null;
  const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?\s*v=|\&v=)([^#\&\?]*).*/;
  const match = url.match(regExp);
  return (match && match[2].length === 11) ? match[2] : null;
};

export default function AdminMovieView({ movies, updateMoviesState, triggerToast }) {
  const [movieSearch, setMovieSearch] = useState('');
  const [movieStatusFilter, setMovieStatusFilter] = useState('ALL');
  const [movieModalOpen, setMovieModalOpen] = useState(false);
  const [editingMovie, setEditingMovie] = useState(null);
  
  const [movieForm, setMovieForm] = useState({
    title: '',
    synopsis: '',
    type: 'Single',
    status: 'SHOWING',
    duration: '120',
    episodesCount: 1,
    language: 'Tiếng Việt',
    directorName: '',
    actors: '',
    ageRating: 'P',
    posterUrl: '',
    backdrop_url: '',
    trailerUrl: '',
    trailer_url: '',
    country: 'Việt Nam',
    releaseDate: '2026-06-14'
  });

  const [selectedGenres, setSelectedGenres] = useState([]);

  // Memoized Filtered Movies Matrix to prevent redundant recalculation
  const filteredMovies = useMemo(() => {
    return movies.filter(m => {
      const title = m.title || '';
      const matchesSearch = title.toLowerCase().includes(movieSearch.toLowerCase());
      
      let matchesStatus = true;
      if (movieStatusFilter !== 'ALL') {
        const normalizedStatus = m.status === 'DANG_CHIEU' || m.status === 'NOW_SHOWING' || m.status === 'SHOWING'
          ? 'SHOWING'
          : m.status === 'SAP_CHIEU' || m.status === 'COMING_SOON' || m.status === 'UPCOMING'
            ? 'UPCOMING'
            : 'ENDED';
        matchesStatus = normalizedStatus === movieStatusFilter;
      }
      
      return matchesSearch && matchesStatus;
    });
  }, [movies, movieSearch, movieStatusFilter]);

  // useCallback Optimization for Modal & Form Triggers
  const handleOpenAddMovie = useCallback(() => {
    setEditingMovie(null);
    setMovieForm({
      title: '',
      synopsis: '',
      type: 'Single',
      status: 'SHOWING',
      duration: '120',
      episodesCount: 1,
      language: 'Tiếng Việt',
      directorName: '',
      actors: '',
      ageRating: 'P',
      posterUrl: '',
      backdrop_url: '',
      trailerUrl: '',
      trailer_url: '',
      country: 'Việt Nam',
      releaseDate: new Date().toISOString().split('T')[0]
    });
    setSelectedGenres([]);
    setMovieModalOpen(true);
  }, []);

  const handleOpenEditMovie = useCallback((movie) => {
    setEditingMovie(movie);
    // Make sure we resolve different status mappings correctly:
    const resolvedStatus = movie.status === 'NOW_SHOWING' || movie.status === 'DANG_CHIEU' || movie.status === 'SHOWING' 
      ? 'SHOWING' 
      : (movie.status === 'COMING_SOON' || movie.status === 'SAP_CHIEU' || movie.status === 'UPCOMING' ? 'UPCOMING' : movie.status || 'SHOWING');

    setMovieForm({
      title: movie.title || '',
      synopsis: movie.synopsis || '',
      type: movie.type || 'Single',
      status: resolvedStatus,
      duration: String(movie.duration || 120),
      episodesCount: movie.episodesCount || 1,
      language: movie.language || 'Tiếng Việt',
      directorName: movie.director?.name || (typeof movie.director === 'string' ? movie.director : ''),
      actors: movie.actor || '',
      ageRating: movie.ageRating || 'P',
      posterUrl: movie.posterUrl || '',
      backdrop_url: movie.backdrop_url || movie.backdropUrl || '',
      trailerUrl: movie.trailerUrl || movie.trailerEmbedUrl || '',
      trailer_url: movie.trailer_url || movie.trailerUrl || movie.trailerEmbedUrl || '',
      country: movie.country || 'Việt Nam',
      releaseDate: movie.releaseDate || (movie.releaseYear ? `${movie.releaseYear}-01-01` : new Date().toISOString().split('T')[0])
    });
    setSelectedGenres(movie.genres || (movie.genre ? movie.genre.split(',').map(g => g.trim()) : []));
    setMovieModalOpen(true);
  }, []);

  const handleGenreToggle = useCallback((genreName) => {
    setSelectedGenres(prev => {
      if (prev.includes(genreName)) {
        return prev.filter(g => g !== genreName);
      } else {
        return [...prev, genreName];
      }
    });
  }, []);

  const handleSaveMovie = useCallback((e) => {
    e.preventDefault();
    if (!movieForm.title || !movieForm.duration) {
      triggerToast('Vui lòng điền đầy đủ thông tin!', 'error');
      return;
    }

    const year = parseInt(movieForm.releaseDate.split('-')[0]) || 2026;
    const embedId = getYouTubeEmbedId(movieForm.trailer_url);

    if (editingMovie) {
      // On Edit: retain all properties via spread operator as specified
      const updatedMoviePayload = {
        ...editingMovie,
        title: movieForm.title,
        synopsis: movieForm.synopsis,
        type: movieForm.type,
        status: movieForm.status || editingMovie.status,
        duration: parseInt(movieForm.duration) || 120,
        episodesCount: parseInt(movieForm.episodesCount) || 1,
        language: movieForm.language,
        director: {
          ...(editingMovie.director || {}),
          name: movieForm.directorName,
          avatarUrl: editingMovie.director?.avatarUrl || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80'
        },
        actor: movieForm.actors,
        ageRating: movieForm.ageRating,
        posterUrl: movieForm.posterUrl || editingMovie.posterUrl || 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80',
        image: movieForm.posterUrl || editingMovie.image || 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80',
        backdrop_url: movieForm.backdrop_url,
        trailer_url: movieForm.trailer_url,
        trailerUrl: movieForm.trailer_url,
        trailerId: embedId || editingMovie.trailerId || '',
        trailerEmbedUrl: embedId ? `https://www.youtube.com/embed/${embedId}` : (editingMovie.trailerEmbedUrl || ''),
        country: movieForm.country,
        releaseDate: movieForm.releaseDate,
        releaseYear: year,
        genres: selectedGenres,
        genre: selectedGenres.join(', '),
        id: editingMovie.id // 🚨 CRITICAL: Never lose or change the primary key entity ID
      };

      const updated = movies.map(m => m.id === editingMovie.id ? updatedMoviePayload : m);
      updateMoviesState(updated);
      triggerToast('Cập nhật thông tin phim thành công!');
    } else {
      // On Create: construct a new movie object
      const newMoviePayload = {
        id: 'm_' + Date.now(),
        title: movieForm.title,
        synopsis: movieForm.synopsis,
        type: movieForm.type,
        status: movieForm.status || 'SHOWING',
        duration: parseInt(movieForm.duration) || 120,
        episodesCount: parseInt(movieForm.episodesCount) || 1,
        language: movieForm.language,
        director: {
          name: movieForm.directorName,
          avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80'
        },
        actor: movieForm.actors,
        ageRating: movieForm.ageRating,
        posterUrl: movieForm.posterUrl || 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80',
        image: movieForm.posterUrl || 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80',
        backdrop_url: movieForm.backdrop_url,
        trailer_url: movieForm.trailer_url,
        trailerUrl: movieForm.trailer_url,
        trailerId: embedId || '',
        trailerEmbedUrl: embedId ? `https://www.youtube.com/embed/${embedId}` : '',
        country: movieForm.country,
        releaseDate: movieForm.releaseDate,
        releaseYear: year,
        genres: selectedGenres,
        genre: selectedGenres.join(', '),
        rating: 4.5,
        actorIds: []
      };

      updateMoviesState([...movies, newMoviePayload]);
      triggerToast('Thêm phim mới thành công!');
    }
    setMovieModalOpen(false);
  }, [movieForm, selectedGenres, editingMovie, movies, updateMoviesState, triggerToast]);

  const handleDeleteMovie = useCallback((id) => {
    if (confirm('Bạn có chắc chắn muốn xóa phim này?')) {
      const updated = movies.filter(m => m.id !== id);
      updateMoviesState(updated);
      triggerToast('Đã xóa phim khỏi danh sách!');
    }
  }, [movies, updateMoviesState, triggerToast]);

  if (movieModalOpen) {
    return (
      <div className="w-full bg-brand-dark p-6 flex flex-col gap-6 animate-fade-in text-zinc-100">
        {/* Header Bar */}
        <div className="flex justify-between items-center border-b border-zinc-800/80 pb-4">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMovieModalOpen(false)}
              className="p-2 text-zinc-400 hover:text-white bg-brand-gray border border-zinc-800/80 rounded-xl transition-all cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <h2 className="text-lg font-black text-zinc-100 uppercase tracking-wider">
              {editingMovie ? 'Cập Nhật Thông Tin Phim' : 'Tạo Phim Mới'}
            </h2>
          </div>
        </div>

        <form onSubmit={handleSaveMovie} className="space-y-8">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            
            {/* LEFT COLUMN: CORE DATABASE FIELDS MATRIX (2/3 width) */}
            <div className="lg:col-span-2 space-y-6">
              <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-6 space-y-4">
                <h3 className="text-sm font-bold text-white uppercase tracking-wider border-b border-zinc-800 pb-2">
                  Thông Tin Cơ Bản
                </h3>
                
                {/* Tiêu đề phim */}
                <div className="space-y-1.5">
                  <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Tiêu đề phim</label>
                  <input
                    type="text"
                    value={movieForm.title}
                    onChange={(e) => setMovieForm({ ...movieForm, title: e.target.value })}
                    placeholder="Nhập tên phim..."
                    className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors"
                    required
                  />
                </div>

                {/* Mô tả phim */}
                <div className="space-y-1.5">
                  <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Mô tả phim</label>
                  <textarea
                    value={movieForm.synopsis}
                    onChange={(e) => setMovieForm({ ...movieForm, synopsis: e.target.value })}
                    rows={4}
                    placeholder="Tóm tắt nội dung..."
                    className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors resize-none"
                  />
                </div>

                {/* Row Grid Splitter 1 */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Loại phim</label>
                    <select
                      value={movieForm.type}
                      onChange={(e) => setMovieForm({ ...movieForm, type: e.target.value })}
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors cursor-pointer"
                    >
                      <option value="Single">Phim Lẻ (Single)</option>
                      <option value="Series">Phim Bộ (Series)</option>
                    </select>
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Trạng thái</label>
                    <select
                      value={movieForm.status}
                      onChange={(e) => setMovieForm({ ...movieForm, status: e.target.value })}
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors cursor-pointer"
                    >
                      <option value="UPCOMING">Sắp chiếu (UPCOMING)</option>
                      <option value="SHOWING">Đang chiếu (SHOWING)</option>
                      <option value="ENDED">Đã đóng suất (ENDED)</option>
                    </select>
                  </div>
                </div>

                {/* Row Grid Splitter 2 */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Thời lượng (Phút)</label>
                    <input
                      type="number"
                      min="0"
                      value={movieForm.duration}
                      onChange={(e) => setMovieForm({ ...movieForm, duration: e.target.value })}
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Tổng số tập</label>
                    <input
                      type="number"
                      min="1"
                      value={movieForm.episodesCount}
                      onChange={(e) => setMovieForm({ ...movieForm, episodesCount: e.target.value })}
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors"
                    />
                  </div>
                </div>

                {/* Row Grid Splitter 3 (Advanced Metadata) */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Ngôn ngữ</label>
                    <input
                      type="text"
                      value={movieForm.language}
                      onChange={(e) => setMovieForm({ ...movieForm, language: e.target.value })}
                      placeholder="Tiếng Anh, Vietsub..."
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Đạo diễn</label>
                    <input
                      type="text"
                      value={movieForm.directorName}
                      onChange={(e) => setMovieForm({ ...movieForm, directorName: e.target.value })}
                      placeholder="Tên đạo diễn..."
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors"
                    />
                  </div>
                </div>

                {/* Row Grid Splitter 4 (Extended Logs) */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Diễn viên</label>
                    <input
                      type="text"
                      value={movieForm.actors}
                      onChange={(e) => setMovieForm({ ...movieForm, actors: e.target.value })}
                      placeholder="Diễn viên 1, Diễn viên 2..."
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Độ tuổi</label>
                    <select
                      value={movieForm.ageRating}
                      onChange={(e) => setMovieForm({ ...movieForm, ageRating: e.target.value })}
                      className="w-full bg-brand-dark border border-zinc-800 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 focus:ring-0 transition-colors cursor-pointer"
                    >
                      <option value="P">P (Mọi lứa tuổi)</option>
                      <option value="T13">T13 (Từ 13 tuổi trở lên)</option>
                      <option value="T16">T16 (Từ 16 tuổi trở lên)</option>
                      <option value="T18">T18 (Từ 18 tuổi trở lên)</option>
                    </select>
                  </div>
                </div>

                {/* YouTube Trailer Field Row */}
                <div className="space-y-1.5 mt-4">
                  <label className="text-xs font-bold text-zinc-400 uppercase tracking-wider block mb-1.5">
                    ĐƯỜNG DẪN TRAILER (YOUTUBE)
                  </label>
                  <div className="relative flex items-center">
                    <Youtube className="absolute left-4 w-4 h-4 text-zinc-500 pointer-events-none" />
                    <input
                      type="text"
                      placeholder="Dán link video YouTube (Ví dụ: https://www.youtube.com/watch?v=...)"
                      value={movieForm.trailer_url || ''}
                      onChange={(e) => setMovieForm({ ...movieForm, trailer_url: e.target.value })}
                      className="w-full bg-brand-dark border border-zinc-800 focus:border-brand-coral/50 rounded-xl pl-11 pr-4 py-3 text-sm text-zinc-100 transition-colors placeholder-zinc-600 outline-none"
                    />
                  </div>
                  
                  {(() => {
                    const embedId = getYouTubeEmbedId(movieForm.trailer_url);
                    return embedId ? (
                      <div className="mt-3 aspect-video w-full max-w-xl rounded-xl overflow-hidden border border-brand-coral/20 bg-brand-dark">
                        <iframe 
                          src={`https://www.youtube.com/embed/${embedId}`} 
                          title="Live Trailer Preview" 
                          className="w-full h-full" 
                          allowFullScreen 
                        />
                      </div>
                    ) : (
                      <p className="text-[11px] text-zinc-500 italic mt-1.5">
                        * Nhập đường dẫn YouTube để kích hoạt tính năng xem thử video tự động.
                      </p>
                    );
                  })()}
                </div>
              </div>

              {/* Thể loại Pill Matrix */}
              <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-6">
                <h3 className="text-sm font-bold text-white uppercase tracking-wider border-b border-zinc-800 pb-2">
                  Thể loại (Chọn nhiều)
                </h3>
                <div className="flex flex-wrap gap-2.5 mt-4">
                  {GENRE_LIST.map((genreName) => {
                    const isSelected = selectedGenres.includes(genreName);
                    return (
                      <button
                        key={genreName}
                        type="button"
                        onClick={() => handleGenreToggle(genreName)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all duration-300 cursor-pointer ${
                          isSelected
                            ? 'bg-brand-coral text-zinc-950 font-bold border-brand-coral shadow-md shadow-brand-coral/10'
                            : 'bg-brand-dark border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:border-zinc-700'
                        }`}
                      >
                        {genreName}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* RIGHT COLUMN: ASSETS & RELATIONSHIP PILLS MATRIX (1/3 width) */}
            <div className="lg:col-span-1 space-y-6">
              
              {/* Poster Asset Upload Card */}
              <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-6 space-y-4">
                <h3 className="text-sm font-bold text-white uppercase tracking-wider border-b border-zinc-800 pb-2">
                  Poster phim
                </h3>
                
                <div className="space-y-3">
                  {movieForm.posterUrl ? (
                    <div className="aspect-[2/3] w-full rounded-2xl overflow-hidden border border-zinc-800 relative group">
                      <img src={movieForm.posterUrl} alt="Poster Preview" className="w-full h-full object-cover" />
                      <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                        <button
                          type="button"
                          onClick={() => setMovieForm({ ...movieForm, posterUrl: '' })}
                          className="bg-brand-coral hover:bg-opacity-90 text-zinc-950 font-bold text-xs py-1.5 px-3 rounded-lg transition-colors cursor-pointer"
                        >
                          Xóa ảnh
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="aspect-[2/3] w-full rounded-2xl bg-brand-dark border-2 border-dashed border-zinc-800 flex flex-col items-center justify-center gap-2 hover:border-brand-coral/40 transition-colors p-4 text-center">
                      <Upload className="w-8 h-8 text-zinc-500" />
                      <span className="text-[10px] text-zinc-400 font-bold">Kéo thả hoặc click để upload ảnh poster</span>
                      <span className="text-[9px] text-zinc-650">Tỷ lệ khuyên dùng 2:3 (Dạng ảnh đứng)</span>
                    </div>
                  )}

                  <input
                    type="text"
                    value={movieForm.posterUrl}
                    onChange={(e) => setMovieForm({ ...movieForm, posterUrl: e.target.value })}
                    placeholder="Đường dẫn ảnh poster (URL)..."
                    className="w-full bg-brand-dark border border-zinc-805 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 transition-colors"
                  />
                </div>
              </div>

              {/* Backdrop Asset Card */}
              <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-6 space-y-4">
                <h3 className="text-sm font-bold text-white uppercase tracking-wider border-b border-zinc-800 pb-2">
                  ẢNH BÌA / BACKDROP
                </h3>
                
                <div className="space-y-3">
                  {movieForm.backdrop_url ? (
                    <div className="aspect-[16/9] w-full rounded-2xl overflow-hidden border border-zinc-805 relative group">
                      <img src={movieForm.backdrop_url} alt="Backdrop Preview" className="w-full h-full object-cover" />
                      <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                        <button
                          type="button"
                          onClick={() => setMovieForm({ ...movieForm, backdrop_url: '' })}
                          className="bg-brand-coral hover:bg-opacity-90 text-zinc-950 font-bold text-xs py-1.5 px-3 rounded-lg transition-colors cursor-pointer"
                        >
                          Xóa ảnh
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="aspect-[16/9] w-full rounded-2xl bg-brand-dark border-2 border-dashed border-zinc-800 flex flex-col items-center justify-center gap-2 hover:border-brand-coral/40 transition-colors p-4 text-center">
                      <Upload className="w-8 h-8 text-zinc-500" />
                      <span className="text-[10px] text-zinc-400 font-bold">Kéo thả hoặc click để upload ảnh bìa</span>
                      <span className="text-[9px] text-zinc-650">Tỷ lệ khuyên dùng 16:9 (Dạng ảnh ngang)</span>
                    </div>
                  )}

                  <input 
                    type="text" 
                    placeholder="Đường dẫn ảnh bìa (Backdrop URL)..." 
                    value={movieForm.backdrop_url || ''} 
                    onChange={(e) => setMovieForm({ ...movieForm, backdrop_url: e.target.value })}
                    className="w-full bg-brand-dark border border-zinc-800 focus:border-brand-coral/50 rounded-xl px-4 py-3 text-sm text-zinc-100 placeholder-zinc-600 outline-none"
                  />
                </div>
              </div>

              {/* Relational Selectors Grid */}
              <div className="bg-brand-gray/60 border border-zinc-800/50 rounded-2xl p-6 space-y-4">
                <h3 className="text-sm font-bold text-white uppercase tracking-wider border-b border-zinc-800 pb-2">
                  Dữ Liệu Quốc Gia & Ngày Phát Hành
                </h3>

                <div className="space-y-3">
                  {/* Quốc gia */}
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Quốc gia</label>
                    <select
                      value={movieForm.country}
                      onChange={(e) => setMovieForm({ ...movieForm, country: e.target.value })}
                      className="w-full bg-brand-dark border border-zinc-850 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 transition-colors cursor-pointer"
                    >
                      <option value="Việt Nam">Việt Nam</option>
                      <option value="Mỹ">Mỹ</option>
                      <option value="Hàn Quốc">Hàn Quốc</option>
                      <option value="Nhật Bản">Nhật Bản</option>
                      <option value="Trung Quốc">Trung Quốc</option>
                      <option value="Anh">Anh</option>
                      <option value="Pháp">Pháp</option>
                    </select>
                  </div>

                  {/* Ngày phát hành */}
                  <div className="space-y-1.5">
                    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">Ngày phát hành</label>
                    <input
                      type="date"
                      value={movieForm.releaseDate}
                      onChange={(e) => setMovieForm({ ...movieForm, releaseDate: e.target.value })}
                      className="w-full bg-brand-dark border border-zinc-850 rounded-xl py-2.5 px-3 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral/40 transition-colors"
                    />
                  </div>
                </div>
              </div>

            </div>

          </div>

          {/* Action Trigger button bar at the bottom */}
          <div className="flex flex-col sm:flex-row gap-4 items-center justify-between border-t border-zinc-800/80 pt-6">
            <button
              type="button"
              onClick={() => setMovieModalOpen(false)}
              className="flex items-center justify-center gap-2 border border-zinc-800/80 hover:border-zinc-700 bg-brand-gray text-zinc-300 font-bold px-6 py-3 rounded-2xl text-xs transition-colors cursor-pointer w-full sm:w-auto"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Quay lại danh sách</span>
            </button>

            <button
              type="submit"
              className="bg-brand-coral hover:bg-opacity-90 text-zinc-950 font-black px-8 py-3.5 rounded-2xl text-xs transition-all shadow-xl tracking-wider uppercase flex items-center justify-center gap-2 w-full sm:w-auto cursor-pointer"
            >
              <Check className="w-4 h-4" />
              <span>{editingMovie ? 'CẬP NHẬT THÔNG TIN' : 'TẠO PHIM MỚI'}</span>
            </button>
          </div>
        </form>
      </div>
    );
  }

  return (
    <div className="space-y-6 bg-brand-dark min-h-screen text-zinc-100">
      {/* Search & Actions bar */}
      <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-brand-gray/60 border border-zinc-800/50 p-4 rounded-2xl backdrop-blur-md mb-6">
        <div className="flex flex-col sm:flex-row gap-4 w-full md:w-auto items-stretch sm:items-center">
          {/* Search Input */}
          <div className="relative w-full sm:w-80">
            <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
              <Search className="w-4 h-4" />
            </span>
            <input
              type="text"
              value={movieSearch}
              onChange={(e) => setMovieSearch(e.target.value)}
              placeholder="Tìm kiếm phim..."
              className="w-full bg-brand-dark border border-zinc-800 text-zinc-100 placeholder-zinc-500 focus:border-brand-coral/40 focus:ring-0 rounded-xl py-2.5 pl-9 pr-4 text-xs transition-colors"
            />
          </div>

          {/* Status Filter */}
          <select
            value={movieStatusFilter}
            onChange={(e) => setMovieStatusFilter(e.target.value)}
            className="bg-brand-dark border border-zinc-800 text-zinc-350 focus:outline-none focus:border-brand-coral/40 rounded-xl py-2.5 px-4 text-xs transition-colors cursor-pointer"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="SHOWING">Đang chiếu (SHOWING)</option>
            <option value="UPCOMING">Sắp chiếu (UPCOMING)</option>
            <option value="ENDED">Đã đóng (ENDED)</option>
          </select>
        </div>

        <button
          onClick={handleOpenAddMovie}
          className="bg-brand-coral hover:bg-opacity-90 text-zinc-950 font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-coral/10 flex items-center gap-2 cursor-pointer w-full sm:w-auto justify-center"
        >
          <Plus className="w-4 h-4" />
          <span>THÊM PHIM MỚI</span>
        </button>
      </div>

      {/* Cinematic Movie Grid */}
      {filteredMovies.length === 0 ? (
        <div className="bg-brand-gray/40 border border-zinc-800/40 rounded-2xl p-12 text-center text-zinc-500 font-semibold">
          Không tìm thấy phim nào phù hợp với bộ lọc hiện tại.
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-5">
          {filteredMovies.map((movie) => {
            // Normalize status for dynamic badge
            const normStatus = movie.status === 'DANG_CHIEU' || movie.status === 'NOW_SHOWING' || movie.status === 'SHOWING'
              ? 'SHOWING'
              : movie.status === 'SAP_CHIEU' || movie.status === 'COMING_SOON' || movie.status === 'UPCOMING'
                ? 'UPCOMING'
                : 'ENDED';

            const ageRating = movie.ageRating || movie.age_rating || 'P';

            // Get first 2 genres
            const genresToShow = Array.isArray(movie.genres) 
              ? movie.genres.slice(0, 2) 
              : (movie.genre ? movie.genre.split(',').map(g => g.trim()).slice(0, 2) : []);

            return (
              <div 
                key={movie.id} 
                className="bg-brand-gray/40 border border-zinc-800/40 rounded-2xl overflow-hidden flex flex-col group hover:border-brand-coral/30 transition-all duration-300 hover:-translate-y-1 transform-gpu shadow-xl relative"
              >
                {/* The Interactive Poster Wrapper */}
                <div className="relative aspect-[2/3] w-full overflow-hidden bg-brand-dark">
                  <img 
                    src={movie.posterUrl || 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80'} 
                    alt={movie.title} 
                    loading="lazy"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 linear transform-gpu" 
                  />

                  {/* Absolute Age Badge Layer */}
                  <div className="absolute top-3 left-3 z-10">
                    {ageRating === 'T18' ? (
                      <span className="px-2 py-0.5 text-[9px] font-black rounded-md bg-red-500/10 border border-red-500/20 text-red-400 uppercase">
                        {ageRating}
                      </span>
                    ) : ageRating === 'P' ? (
                      <span className="px-2 py-0.5 text-[9px] font-black rounded-md bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 uppercase">
                        {ageRating}
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 text-[9px] font-black rounded-md bg-amber-500/10 border border-amber-500/20 text-amber-400 uppercase">
                        {ageRating}
                      </span>
                    )}
                  </div>

                  {/* The Status Chip Overlay */}
                  <div className="absolute top-3 right-3 z-10">
                    {normStatus === 'SHOWING' && (
                      <span className="px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase bg-brand-coral/10 border border-brand-coral/20 text-brand-coral">
                        Đang chiếu
                      </span>
                    )}
                    {normStatus === 'UPCOMING' && (
                      <span className="px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase bg-brand-yellow/10 border border-brand-yellow/20 text-brand-yellow">
                        Sắp chiếu
                      </span>
                    )}
                    {normStatus === 'ENDED' && (
                      <span className="px-2.5 py-0.5 rounded-full text-[9px] font-black uppercase bg-zinc-800/60 text-zinc-400 border border-zinc-700">
                        Đã đóng
                      </span>
                    )}
                  </div>

                  {/* Invisible-to-Visible Action Overlay (Thao tác nhanh trên Card) */}
                  <div className="absolute inset-0 bg-gradient-to-t from-brand-dark via-brand-dark/45 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end justify-center pb-4 z-20">
                    <div className="flex gap-3 bg-brand-dark/95 backdrop-blur-md border border-zinc-800 px-4 py-2 rounded-xl shadow-2xl">
                      <button
                        onClick={() => handleOpenEditMovie(movie)}
                        className="p-1.5 text-zinc-400 hover:text-white bg-brand-gray hover:bg-brand-dark border border-zinc-800/80 rounded-lg transition-colors cursor-pointer"
                        title="Sửa phim"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleDeleteMovie(movie.id)}
                        className="p-1.5 text-brand-coral hover:text-brand-coral bg-brand-coral/10 border border-brand-coral/20 rounded-lg transition-colors cursor-pointer"
                        title="Xóa phim"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>

                {/* The Metadata Detail Block */}
                <div className="p-3.5 flex flex-col flex-1 gap-1.5 justify-between">
                  <div>
                    <h4 className="text-xs font-bold text-zinc-100 group-hover:text-brand-coral transition-colors line-clamp-1 mb-1">
                      {movie.title}
                    </h4>
                    
                    <div className="text-[11px] text-zinc-400 flex items-center gap-1 font-mono mb-2">
                      <Clock className="w-3.5 h-3.5 text-zinc-500 shrink-0" />
                      <span>{movie.duration} phút</span>
                    </div>
                  </div>

                  {/* Genres Row */}
                  {genresToShow.length > 0 && (
                    <div className="flex flex-wrap gap-1 text-[10px] text-zinc-500 font-medium">
                      {genresToShow.map((g, idx) => (
                        <span key={idx} className="bg-brand-dark border border-zinc-800/50 px-1.5 py-0.5 rounded-md">
                          {g}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
