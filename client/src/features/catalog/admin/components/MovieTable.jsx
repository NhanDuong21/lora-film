// eslint-disable-next-line no-unused-vars
import React from 'react';
import { Search, Plus, LayoutList, Image as ImageIcon, Pencil, Trash2 } from 'lucide-react';
import SkeletonTable from '@/components/common/SkeletonTable';
import { LazyImage } from '@/components/common/ui/uiKit';
import { formatDate } from '@/utils/movieHelpers';
import { ADMIN_MOVIE_STATUS_TABS, getStatusConfig } from '@/features/catalog/admin/config/movieStatusConfig';
import { getMovieListWarnings } from '@/features/catalog/admin/utils/movieListWarnings';
import { AlertTriangle } from 'lucide-react';

export default function MovieTable({
  movies = [],
  isLoading = false,
  currentPage = 0,
  setCurrentPage,
  pageSize = 10,
  setPageSize,
  statusFilter = '',
  setStatusFilter,
  searchTerm = '',
  setSearchTerm,
  totalElements = 0,
  totalPages = 0,
  onOpenAdd,
  onOpenDetail,
  onOpenEdit,
  onDelete,
}) {
  console.log('[MovieDebug] MovieTable props length:', movies?.length);
  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto bg-zinc-950 text-white space-y-6 animate-fade-in" data-testid="admin-movie-page">
      <div className="border-b border-zinc-800 pb-4">
        <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider">QUẢN LÝ PHIM</h1>
        <p className="text-sm text-zinc-400 mt-2">
          Theo dõi, xem xét và quản lý các phim được hệ thống tự động đồng bộ từ TMDB.
        </p>
      </div>

      {/* Quick Tabs */}
      <div className="flex flex-wrap gap-2 mb-2">
        {ADMIN_MOVIE_STATUS_TABS.map((tab) => {
          const isActive = (statusFilter || 'ALL') === tab.value;
          return (
            <button
              key={tab.value}
              onClick={() => {
                setStatusFilter(tab.value === 'ALL' ? '' : tab.value);
                setCurrentPage(0);
              }}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                isActive 
                  ? 'bg-amber-500/20 text-amber-500 border border-amber-500/30' 
                  : 'bg-zinc-900/60 text-zinc-400 border border-transparent hover:bg-zinc-800'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Filters */}
      <div className="flex flex-col lg:flex-row gap-4 justify-between items-center bg-zinc-900/60 border border-zinc-800/50 p-4 rounded-2xl">
        <div className="relative w-full lg:w-80">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500 pointer-events-none" />
          <input
            type="text"
            value={searchTerm}
            onChange={e => { setSearchTerm(e.target.value); setCurrentPage(0); }}
            placeholder="Tìm kiếm tên phim..."
            className="w-full bg-[#050506] border border-zinc-800 text-zinc-100 placeholder-zinc-500 focus:border-[#ff7a1a]/40 rounded-xl py-2.5 pl-9 pr-4 text-xs outline-none transition-colors"
          />
        </div>
        <div className="flex flex-col sm:flex-row items-center gap-3 w-full lg:w-auto">
          <select
            value={pageSize}
            onChange={e => { setPageSize(Number(e.target.value)); setCurrentPage(0); }}
            className="w-full sm:w-32 bg-[#050506] border border-zinc-800 text-zinc-100 focus:border-[#ff7a1a]/40 rounded-xl py-2.5 px-4 text-xs outline-none cursor-pointer"
          >
            {[5, 10, 20, 50].map(n => <option key={n} value={n}>{n}/trang</option>)}
          </select>
          <button
            type="button"
            onClick={onOpenAdd}
            className="bg-zinc-800 hover:bg-zinc-700 text-zinc-200 border border-zinc-700 font-bold px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all flex items-center gap-2 cursor-pointer w-full sm:w-auto justify-center"
          >
            <Plus className="w-4 h-4" />TẠO PHIM THỦ CÔNG
          </button>
        </div>
      </div>

      {/* Table */}
      {isLoading ? (
        <SkeletonTable rows={pageSize} columns={7} />
      ) : (
        <div className="bg-neutral-950 border border-neutral-800 rounded-2xl overflow-hidden shadow-xl shrink-0">
          <div className="w-full overflow-x-auto">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead className="sticky top-0 z-10 bg-neutral-900">
                <tr className="bg-neutral-900/50 border-b border-neutral-800 text-[10px] font-black text-neutral-400 uppercase tracking-wider">
                  <th className="py-4 px-5 w-12 text-center">STT</th>
                  <th className="py-4 px-5 w-16 text-center">POSTER</th>
                  <th className="py-4 px-5">TÊN PHIM</th>
                  <th className="py-4 px-5 w-40">DỮ LIỆU</th>
                  <th className="py-4 px-5 w-32 text-center">THỜI LƯỢNG</th>
                  <th className="py-4 px-5 w-36 text-center">KHỞI CHIẾU</th>
                  <th className="py-4 px-5 w-32 text-center">TRẠNG THÁI</th>
                  <th className="py-4 px-5 w-28 text-right">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {movies.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="py-16 text-center text-neutral-500">
                      <div className="flex flex-col items-center gap-2">
                        <LayoutList className="w-10 h-10 text-neutral-700" />
                        <span className="text-sm">Không tìm thấy phim nào.</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  movies.map((movie, idx) => {
                    const statusCfg = getStatusConfig(movie.status);
                    const warnings = getMovieListWarnings(movie);
                    return (
                    <tr key={movie.publicId} className="border-b border-neutral-800/50 hover:bg-neutral-900/50 transition-colors">
                      <td className="py-4 px-5 text-center">
                        <span className="text-xs font-black text-neutral-500">{(currentPage * pageSize + idx + 1).toString().padStart(2, '0')}</span>
                      </td>
                      <td className="py-4 px-5">
                        <div className="w-9 h-13 bg-neutral-800 rounded overflow-hidden mx-auto">
                          {movie.primaryPoster ? (
                            <LazyImage
                              src={movie.primaryPoster}
                              alt={movie.title}
                              containerClassName="w-full h-full border-none rounded-none bg-transparent"
                              className="object-cover"
                            />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-neutral-700">
                              <ImageIcon className="w-4 h-4" />
                            </div>
                          )}
                        </div>
                      </td>
                      <td className="py-4 px-5">
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => onOpenDetail(movie)}
                            className="text-sm font-bold text-zinc-200 hover:text-amber-400 transition-colors text-left truncate max-w-[220px] block cursor-pointer"
                          >
                            {movie.title || <span className="italic text-zinc-500">Chưa có tên</span>}
                          </button>
                          {movie.source === 'TMDB' && (
                            <span className="text-[9px] bg-blue-500/20 text-blue-400 border border-blue-500/30 px-1.5 py-0.5 rounded uppercase font-bold" title={`Đồng bộ từ TMDB (${movie.tmdbId})\nCập nhật: ${formatDate(movie.tmdbLastUpdated)}`}>TMDB</span>
                          )}
                          {movie.source === 'MANUAL' && (
                            <span className="text-[9px] bg-zinc-700/50 text-zinc-400 border border-zinc-600/50 px-1.5 py-0.5 rounded uppercase font-bold" title="Tạo thủ công">Thủ công</span>
                          )}
                        </div>
                        <div className="flex items-center gap-2 mt-1">
                          {movie.ageRating && <span className="text-[10px] font-bold text-neutral-500 uppercase">[{movie.ageRating}]</span>}
                          {movie.readiness?.classification === 'READY' ? (
                             <span className="text-[9px] text-green-500 font-medium">● Sẵn sàng</span>
                          ) : (
                             <span className="text-[9px] text-red-500 font-medium">● Thiếu thông tin</span>
                          )}
                        </div>
                        {warnings.length > 0 && (
                          <div className="flex items-center gap-1 mt-1 cursor-help group relative">
                            <AlertTriangle className="w-3 h-3 text-yellow-500" />
                            <span className="text-[10px] text-yellow-500 truncate max-w-[150px]">
                              {warnings[0].label} {warnings.length > 1 && `· Còn ${warnings.length - 1} cảnh báo`}
                            </span>
                            {/* Tooltip for full warnings */}
                            <div className="absolute left-0 bottom-full mb-2 hidden group-hover:block bg-zinc-900 border border-zinc-700 text-zinc-300 text-[10px] p-2 rounded shadow-xl whitespace-normal min-w-[200px] z-50">
                              <ul className="list-disc pl-4 space-y-1">
                                {warnings.map((w, i) => (
                                  <li key={i}>{w.label}</li>
                                ))}
                              </ul>
                            </div>
                          </div>
                        )}
                      </td>
                      <td className="py-4 px-5">
                        <div className="flex flex-col gap-1 text-[10px] text-zinc-400">
                          <div className="flex justify-between items-center bg-zinc-900/50 px-2 py-0.5 rounded">
                            <span>Phiên bản (Active):</span>
                            <span className="font-bold text-zinc-200">{movie.activeVersionCount || 0}</span>
                          </div>
                          <div className="flex justify-between items-center bg-zinc-900/50 px-2 py-0.5 rounded">
                            <span>Hình ảnh (Media):</span>
                            <span className="font-bold text-zinc-200">{movie.mediaCount || 0}</span>
                          </div>
                          <div className="flex justify-between items-center bg-zinc-900/50 px-2 py-0.5 rounded">
                            <span>Lịch chiếu:</span>
                            <span className="font-bold text-zinc-200">{movie.showtimeCount || 0}</span>
                          </div>
                        </div>
                      </td>
                      <td className="py-4 px-5 text-center">
                        <span className="text-xs text-zinc-300">{movie.durationMinutes ? `${movie.durationMinutes} phút` : 'N/A'}</span>
                      </td>
                      <td className="py-4 px-5 text-center">
                        <span className="text-xs text-zinc-300">{formatDate(movie.releaseDate)}</span>
                      </td>
                      <td className="py-4 px-5 text-center">
                        <span className={`text-[10px] font-black px-2.5 py-1 rounded-md border uppercase tracking-wider ${statusCfg.colorClass}`}>
                          {statusCfg.label}
                        </span>
                      </td>
                      <td className="py-4 px-5 text-right">
                        <div className="flex justify-end items-center gap-2">
                          <button
                            type="button"
                            onClick={() => onOpenDetail(movie)}
                            className="bg-zinc-800 hover:bg-zinc-700 text-zinc-200 border border-zinc-700 font-bold px-3 py-1.5 rounded-lg text-[10px] uppercase tracking-wider transition-all cursor-pointer"
                          >
                            {movie.status === 'DRAFT' ? 'Xem xét' : 'Xem chi tiết'}
                          </button>
                          
                          <button
                            type="button"
                            onClick={() => onOpenEdit(movie)}
                            className="p-1.5 text-neutral-400 hover:text-amber-500 hover:bg-amber-500/10 rounded-lg transition-all cursor-pointer"
                            title="Chỉnh sửa metadata"
                          >
                            <Pencil className="w-4 h-4" />
                          </button>
                          
                          <button
                            type="button"
                            disabled={movie.status !== 'ENDED' && movie.status !== 'DRAFT'}
                            onClick={() => onDelete(movie.publicId, movie.title)}
                            className="p-1.5 text-neutral-400 hover:text-red-500 hover:bg-red-500/10 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-neutral-400 disabled:cursor-not-allowed rounded-lg transition-all cursor-pointer"
                            title={movie.status === 'ENDED' || movie.status === 'DRAFT' ? "Xóa" : "Chỉ có thể xóa phim ở trạng thái Nháp hoặc Ngừng chiếu"}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalElements > 0 && (
            <div className="flex flex-col sm:flex-row items-center justify-between p-4 border-t border-neutral-800 bg-neutral-900/30 gap-4">
              <span className="text-xs text-neutral-400">
                Hiển thị {currentPage * pageSize + 1}–{Math.min((currentPage + 1) * pageSize, totalElements)} / {totalElements} phim
              </span>
              <div className="flex items-center gap-1">
                <button
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage(p => p - 1)}
                  className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-neutral-800 bg-neutral-900 text-neutral-400 hover:text-white hover:bg-neutral-800 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  Trước
                </button>
                <div className="flex items-center gap-1 px-1">
                  {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
                    let page;
                    if (totalPages <= 7) page = i;
                    else if (currentPage < 4) page = i;
                    else if (currentPage > totalPages - 5) page = totalPages - 7 + i;
                    else page = currentPage - 3 + i;
                    return (
                      <button
                        key={page}
                        onClick={() => setCurrentPage(page)}
                        className={`w-7 h-7 flex items-center justify-center text-xs font-bold rounded-lg transition-colors ${currentPage === page ? 'bg-amber-500 text-black' : 'text-neutral-400 hover:bg-neutral-800 hover:text-white'}`}
                      >
                        {page + 1}
                      </button>
                    );
                  })}
                </div>
                <button
                  disabled={currentPage >= totalPages - 1 || totalPages === 0}
                  onClick={() => setCurrentPage(p => p + 1)}
                  className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-neutral-800 bg-neutral-900 text-neutral-400 hover:text-white hover:bg-neutral-800 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  Sau
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
