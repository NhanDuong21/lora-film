import { AlertTriangle, Calendar, CheckCircle2, ChevronRight, ClipboardCheck, Filter, LayoutList, Loader2, PanelsTopLeft, Play, Plus, RefreshCw, RotateCcw, Sparkles, X } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import SkeletonTable from '@/components/common/SkeletonTable';
import SearchableSelect from '@/components/common/SearchableSelect';
import OperationalShowtimeTimeline from '@/features/scheduling/admin/components/OperationalShowtimeTimeline';
import {
  formatShowtimeCinemaDate,
  formatShowtimeCinemaTime,
  resolveShowtimeCinemaTimezone,
} from '@/features/scheduling/admin/utils/showtimeCinemaDateTime';
import {
  getBatchStatusReasonPresentation,
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
  batchReadiness,
  batchReadinessError,
  isBatchReadinessLoading,
  onCheckBatch,
  onOpenBatch,
  isBatchActionLoading,
}) {
  const [viewMode, setViewMode] = useState(batchId ? 'LIST' : 'TIMELINE');
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
  const totalBatchCount = Number(batchReadiness?.totalCount ?? totalElements ?? 0);
  const readyBatchCount = Number(batchReadiness?.eligibleCount ?? 0);
  const blockedBatchCount = Number(batchReadiness?.skippedCount ?? 0);
  const openedBatchCount = Number(batchReadiness?.alreadyTargetCount ?? 0);
  const canOpenBatch = Boolean(
    batchReadiness?.actionAllowed
    && readyBatchCount > 0
    && (!batchReadiness?.atomic || blockedBatchCount === 0)
  );
  const batchFullyOpened = totalBatchCount > 0
    && openedBatchCount >= totalBatchCount
    && readyBatchCount === 0
    && blockedBatchCount === 0;

  const getBlockerAction = reasonCode => {
    const pricingCodes = new Set([
      'SHOWTIME_PRICE_MISSING',
      'PRICING_INCOMPLETE',
      'PRICE_POLICY_NOT_FOUND',
      'PRICING_AMBIGUOUS',
      'PRICE_POLICY_OVERLAP',
    ]);
    const cinemaCodes = new Set([
      'INVALID_CINEMA_TIMEZONE',
      'CINEMA_NOT_ACTIVE',
      'AUDITORIUM_NOT_ACTIVE',
      'CINEMA_OPERATING_HOURS_NOT_CONFIGURED',
      'SHOWTIME_OUTSIDE_OPERATING_HOURS',
      'SHOWTIME_OVERLAPS_CINEMA_CLOSURE',
      'SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE',
    ]);
    const cinema = showtimes[0]?.cinema;
    if (pricingCodes.has(reasonCode) && cinema?.publicId) {
      const returnTo = `/admin/showtimes?source=AUTO&batchId=${encodeURIComponent(batchId)}`;
      const params = new URLSearchParams({
        cinema: cinema.publicId,
        returnTo,
      });
      return { label: 'Thiết lập bảng giá', path: `/admin/pricing?${params.toString()}` };
    }
    if (cinemaCodes.has(reasonCode) && cinema?.publicId) {
      return { label: 'Mở cấu hình rạp', path: `/admin/cinemas/${encodeURIComponent(cinema.publicId)}` };
    }
    return { label: 'Xem danh sách suất', path: '' };
  };

  const clearFilters = () => {
    setCinemaSlug('');
    setMovieSlug('');
    setDate('');
    setStatus('');
    setCurrentPage(0);
    onClearFilters?.({ preserveBatch: Boolean(batchId) });
  };

  return (
    <div className="min-h-full space-y-6 bg-zinc-950 text-white animate-fade-in">
      <header className="flex flex-col gap-5 border-b border-zinc-800 pb-6 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.22em] text-brand-orange">{batchId ? 'Vận hành lịch chiếu' : 'Lịch chiếu & giá vé'}</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight">{batchId ? 'Chuẩn bị mở bán' : 'Lịch chiếu'}</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
            {batchId
              ? 'Kiểm tra điều kiện, xử lý các suất bị chặn và mở bán toàn bộ lịch khi đã sẵn sàng.'
              : 'Xem lịch, biết ngay việc nào cần xử lý và mở bán suất chiếu khi mọi thông tin đã sẵn sàng.'}
          </p>
        </div>
        {!batchId && <div className="flex flex-wrap gap-3">
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
        </div>}
      </header>

      {!batchId && <section className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-3" aria-label="Tóm tắt lịch chiếu">
        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 px-1 text-sm">
          <span><strong className="text-emerald-300">{formatCount(activeCount)}</strong> <span className="text-zinc-500">mở bán</span></span>
          <span><strong className="text-blue-300">{formatCount(draftCount)}</strong> <span className="text-zinc-500">đang soạn</span></span>
          <span className="text-xs text-zinc-600">{formatCount(totalElements)} suất theo bộ lọc</span>
        </div>
        <div className="flex rounded-xl border border-zinc-800 bg-zinc-950 p-1" role="group" aria-label="Chế độ xem lịch chiếu">
          <button type="button" aria-pressed={viewMode === 'TIMELINE'} onClick={() => setViewMode('TIMELINE')} className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-bold ${viewMode === 'TIMELINE' ? 'bg-zinc-700 text-white' : 'text-zinc-500'}`}><PanelsTopLeft className="h-4 w-4" />Sơ đồ</button>
          <button type="button" aria-pressed={viewMode === 'LIST'} onClick={() => setViewMode('LIST')} className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-bold ${viewMode === 'LIST' ? 'bg-zinc-700 text-white' : 'text-zinc-500'}`}><LayoutList className="h-4 w-4" />Danh sách</button>
        </div>
      </section>}

      {batchId && (
        <section className="space-y-5 rounded-2xl border border-blue-500/30 bg-blue-500/5 p-4 md:p-5" aria-labelledby="batch-readiness-title">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-md bg-blue-500 px-2 py-1 text-[10px] font-black uppercase tracking-wide text-zinc-950">Lịch tạo tự động</span>
                <h2 id="batch-readiness-title" className="text-lg font-black text-blue-100">Lịch {getPreviewShortCode(batchId)}</h2>
              </div>
              <p className="mt-2 text-sm text-blue-100/70">Hệ thống tự kiểm tra toàn bộ lịch. Chỉ khi không còn suất bị chặn, nút mở bán mới được bật.</p>
            </div>
            <details className="relative rounded-xl border border-blue-400/20 bg-zinc-950/30 px-3 py-2 text-sm text-blue-100">
              <summary className="cursor-pointer font-bold">Tùy chọn khác</summary>
              <div className="mt-3 flex min-w-52 flex-col gap-2 border-t border-blue-400/15 pt-3">
                <Link to={`/admin/showtime-schedules/${encodeURIComponent(batchId)}`} className="font-bold hover:text-white">Mở bản lịch gốc</Link>
                <Link
                  to={`/admin/showtime-schedules/${encodeURIComponent(batchId)}`}
                  state={{ autoScheduleAction: 'REPLACE' }}
                  className="inline-flex items-center gap-2 font-bold text-amber-200 hover:text-amber-100"
                >
                  <RotateCcw className="h-4 w-4" aria-hidden="true" />
                  Thay lịch
                </Link>
                <button type="button" onClick={onClearBatch} disabled={isBatchActionLoading} className="inline-flex items-center gap-2 text-left font-bold text-zinc-400 hover:text-white disabled:opacity-50"><X className="h-4 w-4" />Rời chế độ chuẩn bị mở bán</button>
              </div>
            </details>
          </div>

          <ol className="grid gap-3 md:grid-cols-3" aria-label="Các bước chuẩn bị mở bán">
            {[
              ['1', 'Kiểm tra điều kiện', isBatchReadinessLoading ? 'Đang thực hiện' : batchReadinessError ? 'Cần thử lại' : batchReadiness ? 'Đã hoàn tất' : 'Đang chờ'],
              ['2', 'Xử lý suất bị chặn', blockedBatchCount > 0 ? `Còn ${blockedBatchCount} suất` : batchReadiness ? 'Không còn vướng mắc' : 'Đang chờ kết quả'],
              ['3', 'Mở bán toàn bộ', batchFullyOpened ? 'Đã hoàn tất' : canOpenBatch ? 'Sẵn sàng xác nhận' : 'Chưa thể thực hiện'],
            ].map(([number, label, state], index) => (
              <li key={number} className={`rounded-xl border p-3 ${index === 0 && isBatchReadinessLoading ? 'border-blue-400/40 bg-blue-500/10' : index === 1 && blockedBatchCount > 0 ? 'border-amber-500/40 bg-amber-500/10' : index === 2 && (canOpenBatch || batchFullyOpened) ? 'border-emerald-500/40 bg-emerald-500/10' : 'border-zinc-800 bg-zinc-950/40'}`}>
                <div className="flex items-center gap-2"><span className="flex h-6 w-6 items-center justify-center rounded-full bg-zinc-800 text-xs font-black">{number}</span><span className="text-sm font-black">{label}</span></div>
                <p className="mt-2 text-xs text-zinc-400">{state}</p>
              </li>
            ))}
          </ol>

          <dl className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4" aria-label="Kết quả kiểm tra mở bán">
            {[
              ['Tổng số suất', totalBatchCount, 'text-white'],
              ['Sẵn sàng mở bán', readyBatchCount, 'text-emerald-300'],
              ['Bị chặn', blockedBatchCount, blockedBatchCount ? 'text-amber-300' : 'text-zinc-300'],
              ['Đang mở bán', openedBatchCount, 'text-blue-300'],
            ].map(([label, value, tone]) => (
              <div key={label} className="rounded-xl border border-zinc-800 bg-zinc-950/60 p-3"><dt className="text-xs font-bold text-zinc-500">{label}</dt><dd className={`mt-1 text-2xl font-black ${tone}`}>{isBatchReadinessLoading && !batchReadiness ? '—' : formatCount(value)}</dd></div>
            ))}
          </dl>

          <div className={`rounded-2xl border p-4 ${batchReadinessError ? 'border-red-500/30 bg-red-500/10' : blockedBatchCount > 0 ? 'border-amber-500/30 bg-amber-500/10' : batchReadiness ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-blue-500/30 bg-blue-500/10'}`} role="status">
            <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
              <div className="flex items-start gap-3">
                {isBatchReadinessLoading ? <Loader2 className="mt-0.5 h-5 w-5 shrink-0 animate-spin text-blue-300" /> : blockedBatchCount > 0 || batchReadinessError ? <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-300" /> : <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-300" />}
                <div>
                  <p className="font-black">{isBatchReadinessLoading ? 'Đang kiểm tra điều kiện mở bán…' : batchReadinessError || (blockedBatchCount > 0 ? `Còn ${blockedBatchCount} suất bị chặn` : batchFullyOpened ? 'Toàn bộ lịch đã mở bán' : batchReadiness ? `Toàn bộ ${readyBatchCount} suất đã sẵn sàng` : 'Đang chuẩn bị kiểm tra lịch')}</p>
                  <p className="mt-1 text-sm text-zinc-300/80">{batchReadinessError || (blockedBatchCount > 0 ? 'Hãy xử lý các mục bên dưới rồi kiểm tra lại. Lịch không mở bán một phần.' : batchFullyOpened ? 'Khách hàng đã có thể đặt vé cho các suất trong lịch này.' : batchReadiness ? 'Bạn có thể xác nhận mở bán toàn bộ lịch.' : 'Kết quả sẽ tự động hiển thị sau ít giây.')}</p>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                <button type="button" onClick={onCheckBatch} disabled={isBatchReadinessLoading || isBatchActionLoading} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-black text-zinc-200 hover:bg-zinc-800 disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${isBatchReadinessLoading ? 'animate-spin' : ''}`} />Kiểm tra lại</button>
                <button type="button" onClick={onOpenBatch} disabled={!canOpenBatch || isBatchReadinessLoading || isBatchActionLoading} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-emerald-500 px-5 text-sm font-black text-zinc-950 hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-40"><Play className="h-4 w-4" />{batchFullyOpened ? 'Đã mở bán toàn bộ' : `Mở bán ${formatCount(readyBatchCount)} suất`}</button>
              </div>
            </div>
          </div>

          {blockedBatchCount > 0 && batchReadiness?.reasonGroups?.length > 0 && (
            <section aria-labelledby="batch-blockers-title">
              <div className="flex items-center gap-2"><ClipboardCheck className="h-5 w-5 text-amber-300" /><h3 id="batch-blockers-title" className="font-black">Việc cần xử lý trước khi mở bán</h3></div>
              <div className="mt-3 grid gap-3 lg:grid-cols-2">
                {batchReadiness.reasonGroups.map((group, index) => {
                  const presentation = getBatchStatusReasonPresentation(group.reasonCode);
                  const blockerAction = getBlockerAction(group.reasonCode);
                  return (
                    <article key={`${group.reasonCode || 'khong-xac-dinh'}-${index}`} className="flex items-center justify-between gap-4 rounded-xl border border-amber-500/25 bg-zinc-950/50 p-4">
                      <div><p className="font-black text-amber-100">{formatCount(group.count)} suất</p><p className="mt-1 text-sm text-zinc-300">{presentation.label}</p></div>
                      {blockerAction.path ? <Link to={blockerAction.path} className="shrink-0 rounded-lg border border-amber-500/40 px-3 py-2 text-xs font-black text-amber-200 hover:bg-amber-500/10">{blockerAction.label}</Link> : <button type="button" onClick={() => setViewMode('LIST')} className="shrink-0 rounded-lg border border-amber-500/40 px-3 py-2 text-xs font-black text-amber-200 hover:bg-amber-500/10">{blockerAction.label}</button>}
                    </article>
                  );
                })}
              </div>
            </section>
          )}
        </section>
      )}

      {batchId && (
        <section className="flex flex-col gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-4 md:flex-row md:items-center md:justify-between" aria-label="Chọn cách rà soát lịch">
          <div><h2 className="font-black">Rà soát chi tiết</h2><p className="mt-1 text-xs text-zinc-500">Dùng danh sách để kiểm tra từng suất; dùng sơ đồ để kiểm chứng phân bổ phòng và thời gian.</p></div>
          <div className="flex rounded-xl border border-zinc-800 bg-zinc-950 p-1" role="group" aria-label="Chế độ rà soát lịch">
            <button type="button" aria-pressed={viewMode === 'LIST'} onClick={() => setViewMode('LIST')} className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-bold ${viewMode === 'LIST' ? 'bg-zinc-700 text-white' : 'text-zinc-500'}`}><LayoutList className="h-4 w-4" />Danh sách</button>
            <button type="button" aria-pressed={viewMode === 'TIMELINE'} onClick={() => setViewMode('TIMELINE')} className={`inline-flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-bold ${viewMode === 'TIMELINE' ? 'bg-zinc-700 text-white' : 'text-zinc-500'}`}><PanelsTopLeft className="h-4 w-4" />Sơ đồ phòng và thời gian</button>
          </div>
        </section>
      )}

      <details open={!batchId} className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4 md:p-5">
        {batchId && <summary className="cursor-pointer text-sm font-black text-zinc-300">Bộ lọc và tùy chọn hiển thị</summary>}
      <section className={batchId ? 'mt-4 border-t border-zinc-800 pt-4' : ''} aria-labelledby="showtime-filter-heading">
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
      </details>

      {!isLoading && showtimes.length > 0 && viewMode === 'TIMELINE' && (
        <>
          {Number(totalElements) > showtimes.length && (
            <div className="flex items-start gap-3 rounded-xl border border-amber-500/25 bg-amber-500/10 p-4 text-sm text-amber-100" role="note">
              <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              <p>
                Sơ đồ đang hiển thị {formatCount(showtimes.length)}/{formatCount(totalElements)} suất của trang dữ liệu này.
                Hãy chọn ngày hoặc rạp trong bộ lọc để rà soát chính xác hơn, hoặc dùng nút chuyển trang bên dưới.
              </p>
            </div>
          )}
          <OperationalShowtimeTimeline showtimes={showtimes} requestedDate={date} onViewDetail={onViewDetail} />
          {totalPages > 1 && (
            <nav className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-zinc-800 bg-zinc-900/50 p-3" aria-label="Phân trang sơ đồ lịch chiếu">
              <span className="text-xs text-zinc-500">Trang dữ liệu {currentPage + 1}/{totalPages} · lọc theo ngày và rạp để có góc nhìn chính xác hơn</span>
              <div className="flex gap-2">
                <button type="button" disabled={currentPage === 0} onClick={() => setCurrentPage(currentPage - 1)} className="min-h-9 rounded-lg border border-zinc-700 px-3 text-xs font-bold text-zinc-300 disabled:opacity-40">Trang trước</button>
                <button type="button" disabled={currentPage === totalPages - 1} onClick={() => setCurrentPage(currentPage + 1)} className="min-h-9 rounded-lg border border-zinc-700 px-3 text-xs font-bold text-zinc-300 disabled:opacity-40">Trang sau</button>
              </div>
            </nav>
          )}
        </>
      )}

      <section className={`${viewMode === 'TIMELINE' && showtimes.length > 0 && !isLoading ? 'hidden' : ''} overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/30`} aria-label="Danh sách suất chiếu">
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
