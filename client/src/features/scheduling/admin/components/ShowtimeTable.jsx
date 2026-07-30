import { Calendar, CheckCircle2, ChevronRight, Filter, Play, Plus, RefreshCw, Sparkles, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import SkeletonTable from '@/components/common/SkeletonTable';
import SearchableSelect from '@/components/common/SearchableSelect';
import {
  formatShowtimeCinemaDate,
  formatShowtimeCinemaTime,
  resolveShowtimeCinemaTimezone,
} from '@/features/scheduling/admin/utils/showtimeCinemaDateTime';
import {
  getPreviewShortCode,
  getShowtimeStatusPresentation,
} from '@/features/scheduling/admin/utils/schedulingPresentation';

const statusStyles = {
  DRAFT: 'border-zinc-700 bg-zinc-800/70 text-zinc-300',
  OPEN_FOR_BOOKING: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300',
  CLOSED: 'border-amber-500/30 bg-amber-500/10 text-amber-300',
  CANCELLED: 'border-rose-500/30 bg-rose-500/10 text-rose-300',
  FINISHED: 'border-zinc-700 bg-zinc-800/70 text-zinc-500',
};

const statusDescriptions = {
  DRAFT: 'Đang soạn, chưa bán vé',
  OPEN_FOR_BOOKING: 'Khách có thể đặt vé',
  CLOSED: 'Đã đóng bán',
  CANCELLED: 'Đã hủy',
  FINISHED: 'Đã chiếu',
};

const StatusBadge = ({ status }) => (
  <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-bold ${statusStyles[status] || statusStyles.DRAFT}`}>
    {status === 'OPEN_FOR_BOOKING' && <CheckCircle2 className="h-3.5 w-3.5" aria-hidden="true" />}
    {getShowtimeStatusPresentation(status).label}
  </span>
);

const formatCount = value => new Intl.NumberFormat('vi-VN').format(Number(value || 0));

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
  onOpenCreate,
  onOpenAutoSchedule,
  onViewDetail,
  onClearBatch,
  onClearFilters,
  onTransitionBatch,
  isBatchActionLoading,
}) {
  const cinemaOptions = cinemas.map(cinema => ({
    value: cinema.slug,
    label: cinema.name,
    subtitle: cinema.address,
  }));

  const movieOptions = movies.map(movie => ({
    value: movie.slug,
    label: movie.title,
    subtitle: `${movie.durationMinutes || '—'} phút`,
  }));

  const statusOptions = ['DRAFT', 'OPEN_FOR_BOOKING', 'CLOSED', 'CANCELLED', 'FINISHED']
    .map(value => ({ value, label: getShowtimeStatusPresentation(value).label }));

  const activeCount = showtimes.filter(item => item.status === 'OPEN_FOR_BOOKING').length;
  const draftCount = showtimes.filter(item => item.status === 'DRAFT').length;
  const needsActionCount = showtimes.filter(item => ['DRAFT', 'CLOSED'].includes(item.status)).length;

  const clearFilters = () => {
    setCinemaSlug('');
    setMovieSlug('');
    setDate('');
    setStatus('');
    setCurrentPage(0);
    onClearFilters?.();
  };

  return (
    <div className="min-h-full space-y-6 bg-zinc-950 text-white animate-fade-in">
      <header className="flex flex-col gap-5 border-b border-zinc-800 pb-6 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.22em] text-brand-orange">Lịch chiếu & giá vé</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight">Lịch chiếu</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
            Xem lịch, biết ngay việc nào cần xử lý và mở bán suất chiếu khi mọi thông tin đã sẵn sàng.
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            type="button"
            onClick={onOpenCreate}
            className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-zinc-950 shadow-lg shadow-brand-orange/10 transition-colors hover:bg-amber-400"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            Thêm suất chiếu
          </button>
          <button
            type="button"
            onClick={onOpenAutoSchedule}
            className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-blue-500 px-4 text-sm font-black text-white shadow-lg shadow-blue-500/10 transition-colors hover:bg-blue-400"
          >
            <Sparkles className="h-4 w-4" aria-hidden="true" />
            Tạo lịch tuần
          </button>
        </div>
      </header>

      <section className="grid gap-3 sm:grid-cols-3" aria-label="Tóm tắt lịch chiếu">
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-4">
          <p className="text-xs font-bold text-zinc-500">Mở bán trên trang này</p>
          <p className="mt-2 text-2xl font-black text-emerald-300">{formatCount(activeCount)}</p>
          <p className="mt-1 text-xs text-zinc-500">suất trong danh sách hiện tại</p>
        </div>
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-4">
          <p className="text-xs font-bold text-zinc-500">Đang soạn</p>
          <p className="mt-2 text-2xl font-black text-blue-300">{formatCount(draftCount)}</p>
          <p className="mt-1 text-xs text-zinc-500">chưa mở bán cho khách</p>
        </div>
        <div className={`rounded-2xl border p-4 ${needsActionCount ? 'border-amber-500/30 bg-amber-500/10' : 'border-zinc-800 bg-zinc-900/70'}`}>
          <p className="text-xs font-bold text-zinc-500">Cần kiểm tra</p>
          <p className={`mt-2 text-2xl font-black ${needsActionCount ? 'text-amber-300' : 'text-zinc-200'}`}>{formatCount(needsActionCount)}</p>
          <p className="mt-1 text-xs text-zinc-500">suất cần bạn xem lại</p>
        </div>
      </section>

      {batchId && (
        <section className="rounded-2xl border border-blue-500/30 bg-blue-500/10 p-4 md:p-5" aria-label="Lịch đang soạn">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-md bg-blue-500 px-2 py-1 text-[10px] font-black uppercase tracking-wide text-zinc-950">Lịch tạo tự động</span>
                <h2 className="text-base font-black text-blue-100">Bạn đang xem các suất trong cùng một lịch đang soạn</h2>
              </div>
              <p className="mt-2 text-sm text-blue-100/75">
                Hãy kiểm tra giá và lịch trước khi mở bán. Bạn có thể rời chế độ xem này bất cứ lúc nào.
              </p>
              <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-blue-200/70">
                <span>Mã lịch: <strong className="text-blue-200">{getPreviewShortCode(batchId)}</strong></span>
                <Link to={`/admin/showtime-schedules/${encodeURIComponent(batchId)}`} className="font-bold text-blue-200 underline decoration-blue-400/40 underline-offset-4">
                  Mở bản lịch gốc
                </Link>
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => onTransitionBatch('OPEN_FOR_BOOKING')}
                disabled={isBatchActionLoading}
                className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-emerald-500 px-4 text-sm font-black text-zinc-950 transition-colors hover:bg-emerald-400 disabled:opacity-50"
              >
                <Play className="h-4 w-4" aria-hidden="true" />
                Kiểm tra để mở bán
              </button>
              <div>
                <button
                  type="button"
                  disabled
                  title="Chưa thể xác minh an toàn đặt vé"
                  className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-rose-500/30 px-4 text-sm font-bold text-rose-300 opacity-60"
                >
                  Hủy cả bản lịch
                </button>
                <span className="mt-1 block text-[10px] font-semibold text-rose-300/80">Chưa thể xác minh an toàn đặt vé</span>
              </div>
              <button
                type="button"
                onClick={onClearBatch}
                disabled={isBatchActionLoading}
                className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-blue-400/20 px-3 text-sm font-bold text-blue-100 hover:bg-blue-400/10 disabled:opacity-50"
              >
                <X className="h-4 w-4" aria-hidden="true" />
                Thoát
              </button>
            </div>
          </div>
        </section>
      )}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4 md:p-5" aria-labelledby="showtime-filter-heading">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 id="showtime-filter-heading" className="flex items-center gap-2 text-base font-black">
              <Filter className="h-4 w-4 text-brand-orange" aria-hidden="true" />
              Tìm lịch chiếu
            </h2>
            <p className="mt-1 text-xs text-zinc-500">Lọc theo rạp, phim, ngày hoặc tình trạng mở bán.</p>
          </div>
          <button type="button" onClick={clearFilters} className="inline-flex min-h-9 items-center gap-2 rounded-lg border border-zinc-700 px-3 text-xs font-bold text-zinc-300 hover:bg-zinc-800">
            <RefreshCw className="h-3.5 w-3.5" aria-hidden="true" />
            Xóa bộ lọc
          </button>
        </div>
        <div className="grid gap-3 lg:grid-cols-4">
          <label className="space-y-1.5 text-xs font-bold text-zinc-400">
            Rạp
            <SearchableSelect options={cinemaOptions} value={cinemaSlug} onChange={value => { setCinemaSlug(value); setCurrentPage(0); }} placeholder="Tất cả rạp" disabled={isOptionsLoading} />
          </label>
          <label className="space-y-1.5 text-xs font-bold text-zinc-400">
            Phim
            <SearchableSelect options={movieOptions} value={movieSlug} onChange={value => { setMovieSlug(value); setCurrentPage(0); }} placeholder="Tất cả phim" disabled={isOptionsLoading} />
          </label>
          <label className="space-y-1.5 text-xs font-bold text-zinc-400">
            Ngày chiếu
            <input type="date" value={date} onChange={event => { setDate(event.target.value); setCurrentPage(0); }} className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200 outline-none focus:border-brand-orange" />
          </label>
          <label className="space-y-1.5 text-xs font-bold text-zinc-400">
            Tình trạng
            <SearchableSelect options={statusOptions} value={status} onChange={value => { setStatus(value); setCurrentPage(0); }} placeholder="Tất cả tình trạng" />
          </label>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/30" aria-label="Danh sách suất chiếu">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-800 px-4 py-4 md:px-5">
          <div>
            <h2 className="text-base font-black">Danh sách suất chiếu</h2>
            <p className="mt-1 text-xs text-zinc-500">Chọn “Chi tiết” để xem giá vé và thay đổi trạng thái.</p>
          </div>
          <span className="text-xs font-bold text-zinc-500">{formatCount(totalElements)} suất</span>
        </div>
        {isLoading ? (
          <SkeletonTable rows={5} columns={6} />
        ) : showtimes.length === 0 ? (
          <div className="flex flex-col items-center gap-3 px-6 py-16 text-center">
            <Calendar className="h-10 w-10 text-zinc-700" aria-hidden="true" />
            <h3 className="text-base font-black text-zinc-200">Chưa có suất chiếu phù hợp</h3>
            <p className="max-w-sm text-sm text-zinc-500">Thử đổi bộ lọc hoặc tạo lịch chiếu mới cho rạp.</p>
            <button type="button" onClick={onOpenAutoSchedule} className="mt-2 inline-flex min-h-10 items-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-zinc-950">
              <Sparkles className="h-4 w-4" aria-hidden="true" />
              Tạo lịch tuần
            </button>
          </div>
        ) : (
          <div className="divide-y divide-zinc-800">
            {showtimes.map(showtime => {
              const timezone = showtime.cinema?.timezone;
              const timezoneResolution = resolveShowtimeCinemaTimezone(timezone);
              return (
                <article key={showtime.showtimePublicId} className="grid gap-4 px-4 py-4 transition-colors hover:bg-zinc-900/70 md:grid-cols-[130px_1fr_180px_150px_auto] md:items-center md:px-5">
                  <div>
                    <p className="flex items-baseline gap-2 text-lg font-black text-zinc-100">
                      <span>{formatShowtimeCinemaTime(showtime.startTime, timezone)}</span>
                      <span className="text-sm font-bold text-zinc-500">–</span>
                      <span className="text-sm font-bold text-zinc-400">{formatShowtimeCinemaTime(showtime.endTime, timezone)}</span>
                    </p>
                    <p className="mt-1 text-xs font-semibold text-zinc-500">
                      <span>{formatShowtimeCinemaDate(showtime.startTime, timezone)}</span>
                      <span className="sr-only">{formatShowtimeCinemaDate(showtime.endTime, timezone)}</span>
                    </p>
                  </div>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-black text-zinc-100">{showtime.movie?.title || 'Chưa có tên phim'}</p>
                    <p className="mt-1 text-xs text-zinc-500">
                      {showtime.movieVersion?.versionName || 'Chưa có phiên bản'}
                      {showtime.movieVersion?.format ? ` · ${showtime.movieVersion.format}` : ''}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm font-bold text-zinc-300">{showtime.cinema?.name || 'Chưa có rạp'}</p>
                    <p className="mt-1 text-xs text-zinc-500">{showtime.auditorium?.name || 'Chưa có phòng'}</p>
                    {timezoneResolution.usedFallback && <span className="mt-1 inline-flex text-[10px] font-bold text-amber-300">Giờ hiển thị tạm thời</span>}
                  </div>
                  <div>
                    <StatusBadge status={showtime.status} />
                    <p className="mt-1 text-[11px] text-zinc-500">{statusDescriptions[showtime.status]}</p>
                  </div>
                  <button type="button" onClick={() => onViewDetail(showtime.showtimePublicId)} className="inline-flex min-h-10 items-center justify-center gap-1 rounded-xl border border-brand-orange/30 px-3 text-xs font-black text-brand-orange hover:bg-brand-orange/10">
                    Chi tiết
                    <ChevronRight className="h-4 w-4" aria-hidden="true" />
                  </button>
                </article>
              );
            })}
          </div>
        )}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-zinc-800 px-4 py-4 md:px-5">
            <span className="text-xs text-zinc-500">Trang {currentPage + 1} / {totalPages}</span>
            <div className="flex gap-2">
              <button type="button" disabled={currentPage === 0} onClick={() => setCurrentPage(currentPage - 1)} className="min-h-9 rounded-lg border border-zinc-700 px-3 text-xs font-bold text-zinc-300 disabled:opacity-40">Trước</button>
              <button type="button" disabled={currentPage === totalPages - 1} onClick={() => setCurrentPage(currentPage + 1)} className="min-h-9 rounded-lg border border-zinc-700 px-3 text-xs font-bold text-zinc-300 disabled:opacity-40">Sau</button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
