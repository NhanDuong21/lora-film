// eslint-disable-next-line no-unused-vars
import React from 'react';
// eslint-disable-next-line no-unused-vars
import { Search, MapPin, Calendar, Clock, Plus, Zap, AlertCircle, RefreshCw, X, Play, Trash2 } from 'lucide-react';
import SkeletonTable from '@/components/common/SkeletonTable';
import SearchableSelect from '@/components/common/SearchableSelect';
import {
  formatShowtimeCinemaDate,
  formatShowtimeCinemaTime,
  resolveShowtimeCinemaTimezone,
} from '@/features/scheduling/admin/utils/showtimeCinemaDateTime';

export default function ShowtimeTable({
  showtimes,
  cinemas = [],
  movies = [],
  isLoading,
  isOptionsLoading,
  cinemaSlug,
  setCinemaSlug,
  movieSlug,
  setMovieSlug,
  date,
  setDate,
  status,
  setStatus,
  currentPage,
  setCurrentPage,
  totalPages,
  totalElements,
  batchId,
  source,
  onOpenCreate,
  onOpenAutoSchedule,
  onViewDetail,
  onClearBatch,
  onClearFilters,
  onTransitionBatch,
  onDeleteBatch,
  isBatchActionLoading
}) {

  const renderStatusBadge = (status) => {
    const config = {
      DRAFT: 'bg-zinc-800 text-zinc-400 border-zinc-700',
      OPEN_FOR_BOOKING: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
      CLOSED: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
      CANCELLED: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
      FINISHED: 'bg-zinc-800 text-zinc-500 border-zinc-700',
    };

    return (
      <span className={`px-2.5 py-1 text-[10px] font-black border rounded-full uppercase tracking-wider ${config[status] || config.DRAFT}`}>
        {status?.replace(/_/g, ' ') || 'UNKNOWN'}
      </span>
    );
  };

  const cinemaOptions = cinemas.map(c => ({
    value: c.slug,
    label: c.name,
    subtitle: c.address
  }));

  const movieOptions = movies.map(m => ({
    value: m.slug,
    label: m.title,
    subtitle: `${m.durationMinutes} phút • ${m.releaseDate || 'N/A'}`,
    badge: m.status?.replace('_', ' ')
  }));

  const statusOptions = [
    { value: 'DRAFT', label: 'DRAFT (Bản nháp)' },
    { value: 'OPEN_FOR_BOOKING', label: 'OPEN (Đang mở bán)' },
    { value: 'CLOSED', label: 'CLOSED (Đã đóng)' },
    { value: 'CANCELLED', label: 'CANCELLED (Đã hủy)' },
    { value: 'FINISHED', label: 'FINISHED (Đã chiếu xong)' }
  ];

  const handleClearFilters = () => {
    setCinemaSlug('');
    setMovieSlug('');
    setDate('');
    setStatus('');
    setCurrentPage(0);
    onClearFilters?.();
  };

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 animate-fade-in">
      {/* Title Header */}
      <div className="flex flex-col border-b border-zinc-800 pb-4">
        <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">QUẢN LÝ SUẤT CHIẾU</h1>
        <p className="text-zinc-500 text-sm mt-1">Theo dõi, sắp xếp và vận hành lịch chiếu</p>
      </div>

      {/* Batch Context Banner */}
      {batchId && (
        <div className="bg-blue-500/10 border border-blue-500/30 rounded-2xl p-4 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="bg-blue-500 text-zinc-950 px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider">
                {source || 'AUTO'} BATCH
              </span>
              <h3 className="text-blue-400 font-bold">Đang xem các suất chiếu thuộc đợt tự động</h3>
            </div>
            <p className="text-xs text-blue-300/70 mt-1 flex items-center gap-2">
              ID: <code className="bg-zinc-950 px-1.5 py-0.5 rounded text-blue-400 border border-blue-500/20">{batchId}</code>
            </p>
          </div>
          
          <div className="flex items-center gap-3">
            <button
              onClick={() => onTransitionBatch('OPEN_FOR_BOOKING')}
              disabled={isBatchActionLoading}
              className="bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-400 border border-emerald-500/30 font-black px-4 py-2 rounded-xl text-xs uppercase tracking-wider transition-all flex items-center gap-2 disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5" /> Mở bán toàn bộ
            </button>
            <button
              onClick={onDeleteBatch}
              disabled={isBatchActionLoading}
              className="bg-rose-500/20 hover:bg-rose-500/30 text-rose-400 border border-rose-500/30 font-black px-4 py-2 rounded-xl text-xs uppercase tracking-wider transition-all flex items-center gap-2 disabled:opacity-50"
            >
              <Trash2 className="w-3.5 h-3.5" /> Hủy đợt
            </button>
            <button
              onClick={onClearBatch}
              disabled={isBatchActionLoading}
              className="bg-zinc-800 hover:bg-zinc-700 text-zinc-300 px-3 py-2 rounded-xl transition-colors disabled:opacity-50"
              title="Thoát chế độ xem đợt"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Filter and search bar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-zinc-900/60 border border-zinc-800 p-4 rounded-2xl backdrop-blur-md">
        
        {/* Cinema Filter */}
        <div className="z-20">
          <SearchableSelect
            options={cinemaOptions}
            value={cinemaSlug}
            onChange={(val) => { setCinemaSlug(val); setCurrentPage(0); }}
            placeholder="Tất cả cụm rạp..."
            disabled={isOptionsLoading}
          />
        </div>

        {/* Movie Filter */}
        <div className="z-10">
          <SearchableSelect
            options={movieOptions}
            value={movieSlug}
            onChange={(val) => { setMovieSlug(val); setCurrentPage(0); }}
            placeholder="Tất cả phim..."
            disabled={isOptionsLoading}
          />
        </div>

        {/* Date Filter */}
        <div>
          <input
            type="date"
            value={date}
            onChange={(e) => { setDate(e.target.value); setCurrentPage(0); }}
            className="w-full bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2.5 px-3.5 text-xs transition-colors focus:outline-none"
          />
        </div>

        {/* Status Filter */}
        <div className="flex gap-2">
          <div className="flex-1 z-10">
            <SearchableSelect
              options={statusOptions}
              value={status}
              onChange={(val) => { setStatus(val); setCurrentPage(0); }}
              placeholder="Tất cả trạng thái..."
            />
          </div>
          <button
            onClick={handleClearFilters}
            aria-label="Xóa bộ lọc"
            className="p-2.5 rounded-xl border border-zinc-800 bg-zinc-950 hover:bg-zinc-800 text-zinc-400 hover:text-white transition-colors"
            title="Xóa bộ lọc"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Buttons */}
      <div className="flex justify-end gap-3">
        <button
          onClick={onOpenCreate}
          className="bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-orange/10 flex items-center gap-2 cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>TẠO THỦ CÔNG</span>
        </button>
        <button
          onClick={onOpenAutoSchedule}
          className="bg-blue-600 hover:bg-blue-500 text-white font-black px-5 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-blue-500/10 flex items-center gap-2 cursor-pointer"
        >
          <Zap className="w-4 h-4" />
          <span>XẾP LỊCH TỰ ĐỘNG</span>
        </button>
      </div>

      {/* Data Table */}
      {isLoading ? (
        <SkeletonTable rows={5} columns={7} />
      ) : (
        <div className="bg-zinc-950 border border-zinc-900 rounded-2xl overflow-hidden w-full shadow-2xl">
          <div className="overflow-x-auto max-h-[600px] custom-scrollbar">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead className="sticky top-0 z-10 bg-zinc-950 shadow-md">
                <tr className="bg-zinc-900/40 border-b border-zinc-900 text-[10px] font-black text-zinc-400 uppercase tracking-wider">
                  <th className="py-4 px-6">BẮT ĐẦU</th>
                  <th className="py-4 px-6">KẾT THÚC</th>
                  <th className="py-4 px-6 max-w-[200px]">PHIM</th>
                  <th className="py-4 px-6">PHIÊN BẢN</th>
                  <th className="py-4 px-6 max-w-[150px]">CỤM RẠP</th>
                  <th className="py-4 px-6">PHÒNG CHIẾU</th>
                  <th className="py-4 px-6">TRẠNG THÁI</th>
                  <th className="py-4 px-6 text-right">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {showtimes.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="py-16 text-center text-zinc-500 text-sm font-semibold">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <Calendar className="w-8 h-8 text-zinc-800" />
                        <span>Không tìm thấy suất chiếu nào.</span>
                      </div>
                    </td>
                  </tr>
                ) : (
                  showtimes.map((showtime) => {
                    const cinemaTimezone = showtime.cinema?.timezone;
                    const timezoneResolution = resolveShowtimeCinemaTimezone(cinemaTimezone);
                    return (
                    <tr key={showtime.showtimePublicId} className="border-b border-zinc-900/60 hover:bg-zinc-900/30 transition-colors group">
                      <td className="py-4 px-6">
                        <div className="flex flex-col gap-0.5">
                          <span className="text-sm font-bold text-zinc-200">
                            {formatShowtimeCinemaTime(showtime.startTime, cinemaTimezone)}
                          </span>
                          <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">
                            {formatShowtimeCinemaDate(showtime.startTime, cinemaTimezone)}
                          </span>
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex flex-col gap-0.5">
                          <span className="text-sm font-bold text-zinc-400">
                            {formatShowtimeCinemaTime(showtime.endTime, cinemaTimezone)}
                          </span>
                          <span className="text-[10px] text-zinc-600 font-bold uppercase tracking-wider">
                            {formatShowtimeCinemaDate(showtime.endTime, cinemaTimezone)}
                          </span>
                        </div>
                      </td>
                      <td className="py-4 px-6 max-w-[200px] truncate">
                        <div className="flex flex-col gap-0.5">
                          <span className="text-sm font-bold text-zinc-200 group-hover:text-amber-400 transition-colors truncate" title={showtime.movie?.title}>
                            {showtime.movie?.title || '—'}
                          </span>
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="flex flex-col gap-0.5">
                          <span className="text-xs font-semibold text-zinc-300">
                            {showtime.movieVersion?.versionName || '—'}
                          </span>
                          <span className="text-[10px] text-zinc-500 font-bold uppercase">
                            {showtime.movieVersion?.format} • {showtime.movieVersion?.audioLanguage}
                          </span>
                        </div>
                      </td>
                      <td className="py-4 px-6 max-w-[150px] truncate">
                        <span className="text-xs font-semibold text-zinc-300 truncate block" title={showtime.cinema?.name}>
                          {showtime.cinema?.name || '—'}
                        </span>
                        {timezoneResolution.usedFallback && (
                          <span className="mt-1 inline-flex rounded border border-amber-500/30 bg-amber-500/10 px-1.5 py-0.5 text-[9px] font-bold text-amber-300">
                            UTC dự phòng
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-6">
                        <span className="text-xs font-semibold text-zinc-400">
                          {showtime.auditorium?.name || '—'}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        {renderStatusBadge(showtime.status)}
                      </td>
                      <td className="py-4 px-6 text-right">
                        <button
                          onClick={() => onViewDetail(showtime.showtimePublicId)}
                          className="text-[10px] text-brand-orange hover:text-amber-400 font-black uppercase tracking-wider bg-brand-orange/5 hover:bg-brand-orange/10 border border-brand-orange/10 hover:border-brand-orange/20 px-2.5 py-1.5 rounded-lg cursor-pointer transition-colors"
                        >
                          Chi tiết
                        </button>
                      </td>
                    </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center px-6 py-4 bg-zinc-900/20 border-t border-zinc-900">
              <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider">
                Hiển thị {showtimes.length} / {totalElements} suất chiếu
              </span>
              <div className="flex gap-2">
                <button
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage(currentPage - 1)}
                  className="px-3 py-1.5 bg-zinc-900 border border-zinc-800 disabled:opacity-50 text-xs text-zinc-300 rounded-lg hover:text-white transition-colors cursor-pointer"
                >
                  Trước
                </button>
                <span className="px-3 py-1.5 text-xs text-zinc-400 font-bold bg-zinc-950 border border-zinc-900 rounded-lg">
                  {currentPage + 1} / {totalPages}
                </span>
                <button
                  disabled={currentPage === totalPages - 1}
                  onClick={() => setCurrentPage(currentPage + 1)}
                  className="px-3 py-1.5 bg-zinc-900 border border-zinc-800 disabled:opacity-50 text-xs text-zinc-300 rounded-lg hover:text-white transition-colors cursor-pointer"
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
