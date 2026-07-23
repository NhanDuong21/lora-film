import {
  AlertTriangle,
  CalendarClock,
  ChevronLeft,
  ChevronRight,
  Eye,
  ExternalLink,
  RefreshCw,
  RotateCcw,
  Sparkles,
} from 'lucide-react';
import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import SearchableSelect from '@/components/common/SearchableSelect';
import SkeletonTable from '@/components/common/SkeletonTable';
import useAutoScheduleHistory from '@/features/scheduling/admin/hooks/useAutoScheduleHistory';
import {
  AUTO_SCHEDULE_HISTORY_STATUSES,
  AUTO_SCHEDULE_HISTORY_STRATEGIES,
  dateTimeLocalToInstant,
  hasAutoScheduleHistoryFilters,
  instantToDateTimeLocal,
} from '@/features/scheduling/admin/utils/autoScheduleHistoryQuery';
import {
  formatCinemaDateTime,
  formatPreviewDateKey,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';

const HISTORY_STATUS_CONFIG = Object.freeze({
  GENERATING: { label: 'Đang tạo', className: 'border-blue-500/30 bg-blue-500/10 text-blue-300', progress: true },
  PREVIEWED: { label: 'Sẵn sàng', className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' },
  APPLYING: { label: 'Đang áp dụng', className: 'border-blue-500/30 bg-blue-500/10 text-blue-300', progress: true },
  APPLIED: { label: 'Đã áp dụng', className: 'border-violet-500/30 bg-violet-500/10 text-violet-300' },
  EXPIRED: { label: 'Đã hết hạn', className: 'border-zinc-700 bg-zinc-800/60 text-zinc-400' },
  FAILED: { label: 'Thất bại', className: 'border-red-500/30 bg-red-500/10 text-red-300' },
  CANCELLED: { label: 'Đã hủy', className: 'border-orange-500/30 bg-orange-500/10 text-orange-300' },
});

const STATUS_FILTER_LABELS = Object.freeze({
  GENERATING: 'Đang tạo',
  PREVIEWED: 'Sẵn sàng (đã lưu)',
  APPLYING: 'Đang áp dụng',
  APPLIED: 'Đã áp dụng',
  EXPIRED: 'Đã hết hạn (đã lưu)',
  FAILED: 'Thất bại',
  CANCELLED: 'Đã hủy',
});

const SORT_OPTIONS = [
  ['createdAt,desc', 'Mới tạo gần đây'],
  ['createdAt,asc', 'Cũ nhất trước'],
  ['scheduleFrom,asc', 'Ngày chiếu tăng dần'],
  ['scheduleFrom,desc', 'Ngày chiếu giảm dần'],
  ['cinemaName,asc', 'Tên cụm rạp A–Z'],
  ['status,asc', 'Trạng thái A–Z'],
  ['totalCandidateCount,desc', 'Nhiều ứng viên nhất'],
  ['selectedCandidateCount,desc', 'Nhiều lựa chọn nhất'],
  ['appliedAt,desc', 'Áp dụng gần đây'],
];

const controlClassName = 'w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2.5 text-xs text-zinc-300 outline-none transition-colors focus:border-amber-500/50';

function StatusBadge({ status }) {
  const config = HISTORY_STATUS_CONFIG[status] || {
    label: status || 'Không xác định',
    className: 'border-zinc-700 bg-zinc-800 text-zinc-400',
  };
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${config.className}`}>
      {config.progress && <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-current" />}
      {config.label}
    </span>
  );
}

function HistoryFilters({ history }) {
  const {
    query,
    cinemas,
    isCinemaLoading,
    cinemaError,
    rangeError,
    commitQuery,
    resetFilters,
    fetchCinemas,
  } = history;
  const deviceTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'múi giờ thiết bị';
  const cinemaOptions = useMemo(() => cinemas.map(cinema => ({
    value: cinema.publicId,
    label: cinema.name,
    subtitle: [cinema.district, cinema.city].filter(Boolean).join(', '),
  })), [cinemas]);

  return (
    <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <label className="space-y-1.5 text-xs font-bold text-zinc-400">
          Cụm rạp
          <SearchableSelect
            options={cinemaOptions}
            value={query.cinemaPublicId}
            onChange={value => commitQuery({ cinemaPublicId: value })}
            placeholder="Tất cả cụm rạp"
            disabled={isCinemaLoading}
          />
        </label>
        <label className="space-y-1.5 text-xs font-bold text-zinc-400">
          Trạng thái đã lưu
          <select
            aria-label="Trạng thái đã lưu"
            value={query.status}
            onChange={event => commitQuery({ status: event.target.value })}
            className={controlClassName}
          >
            <option value="">Tất cả trạng thái</option>
            {AUTO_SCHEDULE_HISTORY_STATUSES.map(status => (
              <option key={status} value={status}>{STATUS_FILTER_LABELS[status]}</option>
            ))}
          </select>
        </label>
        <label className="space-y-1.5 text-xs font-bold text-zinc-400">
          Phiên bản chiến lược
          <select
            aria-label="Phiên bản chiến lược"
            value={query.strategyVersion}
            onChange={event => commitQuery({ strategyVersion: event.target.value })}
            className={controlClassName}
          >
            <option value="">Tất cả phiên bản</option>
            {AUTO_SCHEDULE_HISTORY_STRATEGIES.map(strategy => (
              <option key={strategy} value={strategy}>{strategy}</option>
            ))}
          </select>
        </label>
        <label className="space-y-1.5 text-xs font-bold text-zinc-400">
          Sắp xếp
          <select
            aria-label="Sắp xếp"
            value={query.sort}
            onChange={event => commitQuery({ sort: event.target.value })}
            className={controlClassName}
          >
            {SORT_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <label className="space-y-1.5 text-xs font-bold text-zinc-400">
          Lịch chiếu từ ngày
          <input
            aria-label="Lịch chiếu từ ngày"
            type="date"
            value={query.scheduleFrom}
            onChange={event => commitQuery({ scheduleFrom: event.target.value })}
            className={controlClassName}
          />
        </label>
        <label className="space-y-1.5 text-xs font-bold text-zinc-400">
          Lịch chiếu đến ngày
          <input
            aria-label="Lịch chiếu đến ngày"
            type="date"
            value={query.scheduleTo}
            onChange={event => commitQuery({ scheduleTo: event.target.value })}
            className={controlClassName}
          />
        </label>
        <label className="space-y-1.5 text-xs font-bold text-zinc-400">
          Tạo từ ({deviceTimezone})
          <input
            aria-label="Tạo từ"
            type="datetime-local"
            value={instantToDateTimeLocal(query.createdFrom)}
            onChange={event => commitQuery({ createdFrom: dateTimeLocalToInstant(event.target.value) })}
            className={controlClassName}
          />
        </label>
        <label className="space-y-1.5 text-xs font-bold text-zinc-400">
          Tạo đến, không gồm mốc này ({deviceTimezone})
          <input
            aria-label="Tạo đến"
            type="datetime-local"
            value={instantToDateTimeLocal(query.createdTo)}
            onChange={event => commitQuery({ createdTo: dateTimeLocalToInstant(event.target.value) })}
            className={controlClassName}
          />
        </label>
      </div>

      <div className="mt-4 flex flex-col gap-3 border-t border-zinc-800 pt-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-h-5 text-xs">
          {rangeError && <p className="text-red-400">{rangeError}</p>}
          {!rangeError && cinemaError && (
            <p className="text-amber-400">
              Không tải được bộ chọn cụm rạp.{' '}
              <button type="button" onClick={fetchCinemas} className="font-bold underline">Thử lại</button>
            </p>
          )}
          {!rangeError && !cinemaError && (
            <p className="text-zinc-500">Ngày lịch chiếu được hiểu theo lịch địa phương của cụm rạp.</p>
          )}
        </div>
        <button
          type="button"
          onClick={resetFilters}
          className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-300 hover:bg-zinc-800"
        >
          <RotateCcw className="h-3.5 w-3.5" /> Xóa bộ lọc
        </button>
      </div>
    </section>
  );
}

function HistoryTable({ history, onOpenDetail, onViewCreatedShowtimes, onCreate }) {
  const {
    previews,
    query,
    isInitialLoading,
    isRefreshing,
    error,
    rangeError,
    totalElements,
    totalPages,
    commitQuery,
    resetFilters,
    fetchHistory,
  } = history;
  const filtered = hasAutoScheduleHistoryFilters(query);

  if (isInitialLoading) return <SkeletonTable rows={query.size} columns={10} />;

  if (error && previews.length === 0) {
    return (
      <div className="flex min-h-64 flex-col items-center justify-center gap-3 rounded-2xl border border-red-900/40 bg-red-950/20 p-8 text-center text-red-300">
        <AlertTriangle className="h-8 w-8" />
        <p className="text-sm">Không thể tải lịch sử bản xem trước xếp lịch: {error}</p>
        <button type="button" onClick={fetchHistory} className="inline-flex items-center gap-2 rounded-lg border border-red-800 px-3 py-2 text-xs font-bold hover:bg-red-900/30">
          <RefreshCw className="h-3.5 w-3.5" /> Thử lại
        </button>
      </div>
    );
  }

  return (
    <section className="relative overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-950 shadow-xl">
      {isRefreshing && (
        <div className="absolute inset-x-0 top-0 z-30 flex items-center justify-center gap-2 bg-amber-500/90 py-1 text-[10px] font-black uppercase tracking-wider text-zinc-950">
          <RefreshCw className="h-3 w-3 animate-spin" /> Đang cập nhật
        </div>
      )}
      {error && previews.length > 0 && (
        <div className="flex items-center justify-between border-b border-red-900/40 bg-red-950/20 px-4 py-2 text-xs text-red-300">
          <span>Không thể làm mới lịch sử bản xem trước: {error}</span>
          <button type="button" onClick={fetchHistory} className="font-bold underline">Thử lại</button>
        </div>
      )}
      {rangeError && previews.length > 0 && (
        <div className="border-b border-amber-900/40 bg-amber-950/20 px-4 py-2 text-xs text-amber-300">
          Đang giữ kết quả trước đó cho đến khi khoảng lọc hợp lệ.
        </div>
      )}

      <div className="max-h-[620px] overflow-auto custom-scrollbar">
        <table className="w-full min-w-[960px] text-left" data-layout="laptop-five-groups">
          <thead className="sticky top-0 z-20 bg-zinc-900">
            <tr className="border-b border-zinc-800 text-[10px] font-black uppercase tracking-wider text-zinc-400">
              <th className="px-4 py-4">Bản xem trước / Cụm rạp</th>
              <th className="px-4 py-4">Lịch chiếu / Múi giờ</th>
              <th className="px-4 py-4">Vòng đời / Thời điểm</th>
              <th className="px-4 py-4">Ứng viên / Đã tạo</th>
              <th className="px-4 py-4 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {previews.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-16 text-center text-zinc-500">
                  <div className="flex flex-col items-center gap-3">
                    <CalendarClock className="h-10 w-10 text-zinc-700" />
                    <p className="text-sm">
                      {filtered ? 'Không có bản xem trước phù hợp với bộ lọc.' : 'Chưa có lịch sử bản xem trước xếp lịch.'}
                    </p>
                    <button
                      type="button"
                      onClick={filtered ? resetFilters : onCreate}
                      className="rounded-xl bg-amber-500 px-4 py-2 text-xs font-black text-zinc-950 hover:bg-amber-400"
                    >
                      {filtered ? 'Xóa bộ lọc' : 'Tạo bản xem trước'}
                    </button>
                  </div>
                </td>
              </tr>
            ) : previews.map(preview => {
              const actionLabel = preview.editable ? 'Mở / chỉnh sửa' : 'Xem chi tiết';
              return (
                <tr key={preview.previewPublicId} className="border-b border-zinc-800/60 align-top transition-colors hover:bg-zinc-900/50">
                  <td className="max-w-[230px] px-4 py-4">
                    <button
                      type="button"
                      onClick={() => onOpenDetail(preview.previewPublicId)}
                      className="block max-w-[220px] truncate text-left font-mono text-xs font-bold text-amber-400 hover:text-amber-300"
                      title={preview.previewPublicId}
                    >
                      {preview.previewPublicId}
                    </button>
                    <p className="mt-1 max-w-[230px] truncate text-xs font-semibold text-zinc-300" title={preview.cinemaName}>{preview.cinemaName}</p>
                    <p className="mt-2 font-mono text-[10px] text-zinc-500">{preview.strategyVersion}</p>
                  </td>
                  <td className="px-4 py-4 text-xs text-zinc-300">
                    <p className="font-semibold">{formatPreviewDateKey(preview.scheduleFrom)} – {formatPreviewDateKey(preview.scheduleTo)}</p>
                    <p className="mt-1 text-[10px] text-zinc-500">{preview.timezoneSnapshot}</p>
                    <p className="mt-1 text-[10px] text-zinc-600">{preview.applyMode}</p>
                  </td>
                  <td className="max-w-[250px] px-4 py-4">
                    <StatusBadge status={preview.displayStatus} />
                    {preview.applicable && <p className="mt-2 text-[10px] font-bold text-emerald-400">Có thể áp dụng trong chi tiết</p>}
                    {preview.failureReasonSafe && <p className="mt-2 max-w-[220px] whitespace-normal text-[10px] text-red-300">{preview.failureReasonSafe}</p>}
                    <div className="mt-2 text-[10px] text-zinc-500">
                      <p>Tạo {formatCinemaDateTime(preview.createdAt, preview.timezoneSnapshot)}</p>
                      {preview.displayStatus === 'APPLIED' ? (
                        <p className="mt-1 font-bold text-violet-300">Đã áp dụng lúc {formatCinemaDateTime(preview.appliedAt, preview.timezoneSnapshot)}</p>
                      ) : (
                        <p className="mt-1">Hết hạn {formatCinemaDateTime(preview.expiresAt, preview.timezoneSnapshot)}</p>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-4 text-xs text-zinc-300">
                    <p><strong className="text-zinc-100">{preview.totalCandidateCount}</strong> ứng viên</p>
                    <p className="mt-1 text-[10px] text-zinc-500"><span className="text-emerald-400">{preview.validCandidateCount} hợp lệ</span> · <span className="text-red-400">{preview.rejectedCandidateCount} loại</span></p>
                    <p className="mt-1 text-[10px]"><span className="font-bold text-amber-400">{preview.selectedCandidateCount}</span> đã chọn</p>
                    {preview.displayStatus === 'APPLIED' && (
                      <p className="mt-2 font-bold text-violet-300">{preview.appliedShowtimeCount ?? 0} suất đã tạo</p>
                    )}
                  </td>
                  <td className="px-4 py-4 text-right">
                    <div className="flex flex-col items-end gap-2">
                      {preview.displayStatus === 'APPLIED' && (
                        <button
                          type="button"
                          onClick={() => onViewCreatedShowtimes(preview.previewPublicId)}
                          className="inline-flex min-h-10 items-center gap-1.5 rounded-lg border border-violet-500/30 bg-violet-500/10 px-3 py-2 text-[10px] font-black uppercase tracking-wider text-violet-300 hover:bg-violet-500/20"
                        >
                          <ExternalLink className="h-3.5 w-3.5" /> Xem các suất chiếu đã tạo
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => onOpenDetail(preview.previewPublicId)}
                        className="inline-flex min-h-10 items-center gap-1.5 rounded-lg border border-amber-500/20 bg-amber-500/5 px-3 py-2 text-[10px] font-black uppercase tracking-wider text-amber-400 hover:bg-amber-500/10"
                      >
                        <Eye className="h-3.5 w-3.5" /> {actionLabel}
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {totalElements > 0 && (
        <footer className="flex flex-col gap-3 border-t border-zinc-800 bg-zinc-900/40 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3 text-xs text-zinc-500">
            <span>Hiển thị {query.page * query.size + 1}–{Math.min((query.page + 1) * query.size, totalElements)} / {totalElements}</span>
            <select
              aria-label="Số dòng mỗi trang"
              value={query.size}
              onChange={event => commitQuery({ size: Number(event.target.value) })}
              className="rounded-lg border border-zinc-800 bg-zinc-950 px-2 py-1.5 text-xs text-zinc-300"
            >
              {[10, 20, 50].map(size => <option key={size} value={size}>{size} dòng</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              aria-label="Trang trước"
              disabled={query.page === 0}
              onClick={() => commitQuery({ page: query.page - 1 }, { resetPage: false })}
              className="rounded-lg border border-zinc-800 p-2 text-zinc-300 hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="min-w-24 text-center text-xs font-bold text-zinc-400">Trang {query.page + 1} / {Math.max(totalPages, 1)}</span>
            <button
              type="button"
              aria-label="Trang sau"
              disabled={query.page >= totalPages - 1}
              onClick={() => commitQuery({ page: query.page + 1 }, { resetPage: false })}
              className="rounded-lg border border-zinc-800 p-2 text-zinc-300 hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </footer>
      )}
    </section>
  );
}

export default function AdminAutoScheduleHistoryPage() {
  const navigate = useNavigate();
  const history = useAutoScheduleHistory();

  return (
    <div className="flex min-h-[400px] flex-col space-y-6 bg-zinc-950 text-white animate-fade-in">
      <header className="flex flex-col gap-4 border-b border-zinc-800 pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-xl font-black uppercase tracking-wider text-white md:text-2xl">Lịch sử bản xem trước xếp lịch</h1>
          <p className="mt-1 text-sm text-zinc-500">Tra cứu các bản xem trước đã tạo mà không thay đổi trạng thái lịch.</p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={history.fetchHistory}
            disabled={history.isRefreshing || Boolean(history.rangeError)}
            className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-bold text-zinc-300 hover:bg-zinc-800 disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${history.isRefreshing ? 'animate-spin' : ''}`} /> Làm mới
          </button>
          <button
            type="button"
            onClick={() => navigate('/admin/showtime-schedules/create')}
            className="inline-flex items-center gap-2 rounded-xl bg-amber-500 px-4 py-2.5 text-xs font-black uppercase tracking-wider text-zinc-950 hover:bg-amber-400"
          >
            <Sparkles className="h-4 w-4" /> Tạo lịch tự động
          </button>
        </div>
      </header>

      <HistoryFilters history={history} />
      <HistoryTable
        history={history}
        onCreate={() => navigate('/admin/showtime-schedules/create')}
        onOpenDetail={previewPublicId => navigate(`/admin/showtime-schedules/${previewPublicId}`)}
        onViewCreatedShowtimes={previewPublicId => navigate(`/admin/showtimes?source=AUTO&batchId=${encodeURIComponent(previewPublicId)}`)}
      />
    </div>
  );
}
