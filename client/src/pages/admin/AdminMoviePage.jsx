import { useState, useEffect, useCallback, useMemo } from 'react';
import { Search, Pencil, Trash2, Plus, LayoutList, Image as ImageIcon } from 'lucide-react';
import { getMovies } from '../../services/movieService';
import SkeletonTable from '../../components/common/SkeletonTable';
import { useOutletContext } from 'react-router-dom';

export default function AdminMoviePage() {
  const { triggerToast } = useOutletContext() || {};
  const [movies, setMovies] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  const fetchMovies = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await getMovies({ page: 0, size: 50 }); // Fetch first 50 for admin list
      let movieList = [];
      if (Array.isArray(data?.content)) {
        movieList = data.content;
      } else if (Array.isArray(data)) {
        movieList = data;
      }
      setMovies(movieList);
    } catch {
      if (triggerToast) triggerToast('Lỗi khi tải danh sách phim', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [triggerToast]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchMovies();
  }, [fetchMovies]);

  const filteredMovies = useMemo(() => {
    return movies.filter(m => 
      (m.title || '').toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [movies, searchTerm]);

  const handleOpenAdd = useCallback(() => {
    if (triggerToast) triggerToast('Chức năng thêm phim mới đang được nâng cấp cùng backend API', 'error');
  }, [triggerToast]);

  const handleAction = useCallback(() => {
    if (triggerToast) triggerToast('Chức năng chỉnh sửa đang được nâng cấp', 'error');
  }, [triggerToast]);

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const d = new Date(dateString);
    if (isNaN(d)) return dateString;
    return d.toLocaleDateString('vi-VN');
  };

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 animate-fade-in" data-testid="admin-movie-page">
      <div className="flex flex-col border-b border-zinc-800 pb-4">
        <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">DANH SÁCH BỘ PHIM</h1>
      </div>
      
      <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-brand-gray/60 border border-zinc-800/50 p-4 rounded-2xl backdrop-blur-md">
        <div className="relative w-full sm:w-80">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
            <Search className="w-4 h-4" />
          </span>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm kiếm tên phim..."
            className="w-full bg-brand-dark border border-zinc-800 text-zinc-100 placeholder-zinc-500 focus:border-brand-coral/40 focus:ring-0 rounded-xl py-2.5 pl-9 pr-4 text-xs transition-colors"
          />
        </div>

        <button
          onClick={handleOpenAdd}
          className="bg-brand-coral hover:bg-opacity-90 text-zinc-950 font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-coral/10 flex items-center gap-2 cursor-pointer w-full sm:w-auto justify-center"
        >
          <Plus className="w-4 h-4" />
          <span>THÊM PHIM</span>
        </button>
      </div>

      {isLoading ? (
        <SkeletonTable rows={6} columns={7} />
      ) : (
        <div className="bg-neutral-950 border border-neutral-800 rounded-2xl overflow-hidden w-full shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead>
                <tr className="bg-neutral-900/50 border-b border-neutral-800 text-[10px] font-black text-neutral-400 uppercase tracking-wider">
                  <th className="py-4 px-6 w-16 text-center">STT</th>
                  <th className="py-4 px-6 w-24 text-center">ẢNH POSTER</th>
                  <th className="py-4 px-6">TÊN PHIM</th>
                  <th className="py-4 px-6 w-32 text-center">THỜI LƯỢNG</th>
                  <th className="py-4 px-6 w-40 text-center">NGÀY KHỞI CHIẾU</th>
                  <th className="py-4 px-6 w-32 text-center">TRẠNG THÁI</th>
                  <th className="py-4 px-6 w-32 text-right">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {filteredMovies.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="py-12 text-center text-neutral-500 text-sm font-semibold">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <LayoutList className="w-8 h-8 text-neutral-700" />
                        <span>Không tìm thấy phim nào.</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredMovies.map((movie, index) => (
                    <tr key={movie.id} className="border-b border-neutral-800/50 hover:bg-neutral-900/50 transition-colors group">
                      <td className="py-4 px-6 text-center">
                        <span className="text-xs font-black text-neutral-400">{(index + 1).toString().padStart(2, '0')}</span>
                      </td>
                      <td className="py-4 px-6 text-center">
                        <div className="w-10 h-14 bg-neutral-800 rounded flex items-center justify-center overflow-hidden mx-auto shadow-md">
                          {movie.posterUrl ? (
                            <img 
                              src={movie.posterUrl} 
                              alt={movie.title} 
                              className="w-full h-full object-cover"
                              onError={(e) => {
                                e.target.onerror = null;
                                e.target.style.display = 'none';
                                e.target.nextSibling.style.display = 'flex';
                              }}
                            />
                          ) : null}
                          <div className={`w-full h-full flex flex-col items-center justify-center text-neutral-600 bg-neutral-800 ${movie.posterUrl ? 'hidden' : 'flex'}`}>
                            <ImageIcon className="w-4 h-4" />
                          </div>
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex flex-col">
                          <span className="text-sm font-bold text-zinc-200 group-hover:text-amber-400 transition-colors truncate max-w-[200px] lg:max-w-xs">
                            {movie.title}
                          </span>
                          {movie.ageRating && (
                            <span className="text-[10px] font-bold text-neutral-500 uppercase mt-0.5">
                              {movie.ageRating}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="py-4 px-6 text-center">
                        <span className="text-xs font-medium text-zinc-300">
                          {movie.durationMinutes ? `${movie.durationMinutes} phút` : 'N/A'}
                        </span>
                      </td>
                      <td className="py-4 px-6 text-center">
                        <span className="text-xs font-medium text-zinc-300">
                          {formatDate(movie.releaseDate)}
                        </span>
                      </td>
                      <td className="py-4 px-6 text-center">
                        {movie.status === 'NOW_SHOWING' && (
                          <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px] font-black px-2.5 py-1 rounded-md uppercase tracking-wider">Đang Chiếu</span>
                        )}
                        {movie.status === 'UPCOMING' && (
                          <span className="bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[10px] font-black px-2.5 py-1 rounded-md uppercase tracking-wider">Sắp Chiếu</span>
                        )}
                        {movie.status === 'ENDED' && (
                          <span className="bg-neutral-500/10 text-neutral-400 border border-neutral-500/20 text-[10px] font-black px-2.5 py-1 rounded-md uppercase tracking-wider">Ngừng Chiếu</span>
                        )}
                        {!['NOW_SHOWING', 'UPCOMING', 'ENDED'].includes(movie.status) && (
                          <span className="bg-neutral-800 text-neutral-400 border border-neutral-700 text-[10px] font-black px-2.5 py-1 rounded-md uppercase tracking-wider">{movie.status || 'N/A'}</span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-right">
                        <div className="flex items-center justify-end gap-2 transition-opacity">
                          <button
                            onClick={handleAction}
                            className="p-2 text-neutral-400 hover:text-amber-500 hover:bg-amber-500/10 border border-transparent hover:border-amber-500/20 rounded-lg transition-all cursor-pointer"
                            title="Sửa phim (Đang nâng cấp)"
                          >
                            <Pencil className="w-4 h-4" />
                          </button>
                          <button
                            onClick={handleAction}
                            className="p-2 text-neutral-400 hover:text-red-500 hover:bg-red-500/10 border border-transparent hover:border-red-500/20 rounded-lg transition-all cursor-pointer"
                            title="Xóa phim (Đang nâng cấp)"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
