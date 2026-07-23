import { useMemo, useState } from 'react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  Calendar,
  CheckCircle2,
  Info,
  LayoutTemplate,
  List,
  Loader2,
  MapPin,
  RefreshCw,
  Save,
  Wand2,
} from 'lucide-react';
import AutoScheduleTimeline from '@/features/scheduling/admin/components/AutoScheduleTimeline';
import useAutoSchedulePreview from '@/features/scheduling/admin/hooks/useAutoSchedulePreview';
import {
  CANDIDATE_PAGE_SIZES,
  CANDIDATE_VIEWS,
  filterCandidatesByView,
  getCandidateViewCounts,
  getDefaultCandidateView,
  paginateCandidates,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewCandidates';
import {
  compareServiceDateKeys,
  formatCinemaDateTime,
  formatCinemaTime,
  formatPreviewDateKey,
  formatServiceDateKey,
  getCinemaDateKey,
  getServiceDateKey,
  resolveCinemaTimezone,
  UNKNOWN_SERVICE_DATE_KEY,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import {
  getCandidateApplyStateMeta,
  isCandidateSelectable,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewLifecycle';
import {
  buildQuickNonOverlappingSelection,
  buildSelectedItemsIndex,
  findSelectionBlock,
  getMalformedPreviewItems,
  SELECTION_BLOCK_TYPES,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewSelection';

const REJECTION_REASON_MAP = {
  SHOWTIME_OUTSIDE_OPERATING_HOURS: 'Ngoài giờ hoạt động của cụm rạp',
  SHOWTIME_OVERLAPS_EXISTING: 'Trùng với suất chiếu hiện có',
  MOVIE_NOT_ELIGIBLE: 'Phim chưa đủ điều kiện',
  AUDITORIUM_UNAVAILABLE: 'Phòng chiếu không khả dụng',
  NOT_ENOUGH_CLEANING_TIME: 'Không đủ thời gian dọn dẹp',
};

const CANDIDATE_VIEW_LABELS = {
  [CANDIDATE_VIEWS.RECOMMENDED]: 'Đề xuất',
  [CANDIDATE_VIEWS.REJECTED]: 'Bị từ chối / xung đột',
  [CANDIDATE_VIEWS.ALL]: 'Tất cả ứng viên',
  [CANDIDATE_VIEWS.CREATED]: 'Suất chiếu đã tạo',
};

const STATE_TONE_CLASSES = {
  blue: 'border-blue-500/30 bg-blue-500/10 text-blue-300',
  green: 'border-green-500/30 bg-green-500/10 text-green-300',
  red: 'border-red-500/30 bg-red-500/10 text-red-300',
  zinc: 'border-zinc-700 bg-zinc-800/70 text-zinc-300',
};

const LIFECYCLE_TONE_CLASSES = {
  GENERATING: 'border-blue-500/30 bg-blue-500/10 text-blue-300',
  PREVIEWED: 'border-green-500/30 bg-green-500/10 text-green-300',
  APPLYING: 'border-blue-500/30 bg-blue-500/10 text-blue-300',
  APPLIED: 'border-green-500/30 bg-green-500/10 text-green-300',
  FAILED: 'border-red-500/30 bg-red-500/10 text-red-300',
  EXPIRED: 'border-amber-500/30 bg-amber-500/10 text-amber-300',
  CANCELLED: 'border-zinc-700 bg-zinc-800/70 text-zinc-300',
};

const translateReason = reason => {
  if (!reason) return '';
  const normalized = reason.toUpperCase();
  const match = Object.entries(REJECTION_REASON_MAP)
    .find(([code]) => normalized.includes(code));
  return match?.[1] || reason;
};

const groupItemsForTimeline = items => {
  const groups = {};
  items.forEach(item => {
    const dateKey = getServiceDateKey(item.serviceDate);
    if (dateKey === UNKNOWN_SERVICE_DATE_KEY) return;
    const auditoriumKey = item.auditoriumName || item.auditoriumPublicId || 'Không xác định';
    groups[dateKey] ||= {};
    groups[dateKey][auditoriumKey] ||= [];
    groups[dateKey][auditoriumKey].push(item);
  });
  Object.values(groups).forEach(dateGroup => {
    Object.values(dateGroup).forEach(auditoriumItems => {
      auditoriumItems.sort((left, right) => new Date(left.startTime) - new Date(right.startTime));
    });
  });
  return groups;
};

const getEmptyStateMessage = view => {
  switch (view) {
    case CANDIDATE_VIEWS.RECOMMENDED:
      return 'Không có ứng viên được đề xuất trong bộ lọc hiện tại.';
    case CANDIDATE_VIEWS.REJECTED:
      return 'Không có ứng viên bị từ chối hoặc gặp xung đột.';
    case CANDIDATE_VIEWS.CREATED:
      return 'Không có suất chiếu nào được tạo từ bản xem trước này.';
    default:
      return 'Không có ứng viên phù hợp với bộ lọc hiện tại.';
  }
};

const AdminAutoSchedulePreviewPage = () => {
  const { id } = useParams();
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const handleSuccess = () => {
    navigate(`/admin/showtimes?source=AUTO&batchId=${id}&status=DRAFT`, {
      state: {
        message: `Đã tạo ${selectedItemIds.size} suất chiếu từ bản xem trước.`,
      },
    });
  };

  const {
    preview,
    items,
    selectedItemIds,
    isLoading,
    isRefreshing,
    isSnapshotUpdating,
    loadingProgress,
    snapshotError,
    capabilities,
    isApplying,
    isUpdatingSelection,
    handleToggleSelection,
    handleBulkSelection,
    handleApply,
    fetchPreview,
  } = useAutoSchedulePreview(id, { triggerToast, onSuccess: handleSuccess });

  const [filterAuditorium, setFilterAuditorium] = useState('');
  const [filterReason, setFilterReason] = useState('');
  const [filterDate, setFilterDate] = useState('');
  const [candidatePage, setCandidatePage] = useState(1);
  const [candidatePageSize, setCandidatePageSize] = useState(50);
  const [viewMode, setViewMode] = useState('timeline');
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [candidateViewChoice, setCandidateViewChoice] = useState({ key: null, view: null });

  const lifecycleKey = `${preview?.previewPublicId || id}:${capabilities?.effectiveStatus || 'UNKNOWN'}`;
  const defaultCandidateView = getDefaultCandidateView(capabilities);
  const candidateView = candidateViewChoice.key === lifecycleKey
    ? candidateViewChoice.view
    : defaultCandidateView;

  const timezoneResolution = useMemo(
    () => resolveCinemaTimezone(preview?.timezoneSnapshot),
    [preview?.timezoneSnapshot],
  );
  const effectiveTimezone = timezoneResolution.timezone;
  const malformedItems = useMemo(() => getMalformedPreviewItems(items), [items]);
  const malformedItemIds = useMemo(
    () => new Set(malformedItems.map(item => item.itemPublicId)),
    [malformedItems],
  );
  const selectedItemsIndex = useMemo(
    () => buildSelectedItemsIndex(items, selectedItemIds),
    [items, selectedItemIds],
  );
  const candidateViewCounts = useMemo(
    () => getCandidateViewCounts(items, selectedItemIds),
    [items, selectedItemIds],
  );
  const availableCandidateViews = capabilities.effectiveStatus === 'APPLIED'
    ? [CANDIDATE_VIEWS.RECOMMENDED, CANDIDATE_VIEWS.REJECTED, CANDIDATE_VIEWS.ALL, CANDIDATE_VIEWS.CREATED]
    : [CANDIDATE_VIEWS.RECOMMENDED, CANDIDATE_VIEWS.REJECTED, CANDIDATE_VIEWS.ALL];
  const rejectionReasons = useMemo(() => Array.from(new Set(
    items.filter(item => item.rejectionReason).map(item => item.rejectionReason),
  )), [items]);
  const uniqueAuditoriums = useMemo(() => Array.from(new Set(
    items.map(item => item.auditoriumName || item.auditoriumPublicId).filter(Boolean),
  )).sort(), [items]);
  const uniqueDates = useMemo(() => Array.from(new Set(
    items.map(item => getServiceDateKey(item.serviceDate)),
  )).sort(compareServiceDateKeys), [items]);

  const filteredCandidates = useMemo(() => filterCandidatesByView(
    items,
    candidateView,
    selectedItemIds,
  ).filter(item => {
    const auditoriumKey = item.auditoriumName || item.auditoriumPublicId;
    if (filterAuditorium && auditoriumKey !== filterAuditorium) return false;
    if (filterReason && item.rejectionReason !== filterReason) return false;
    if (filterDate && getServiceDateKey(item.serviceDate) !== filterDate) return false;
    return true;
  }), [
    candidateView,
    filterAuditorium,
    filterDate,
    filterReason,
    items,
    selectedItemIds,
  ]);
  const pagination = useMemo(
    () => paginateCandidates(filteredCandidates, candidatePage, candidatePageSize),
    [candidatePage, candidatePageSize, filteredCandidates],
  );
  const timelineGroups = useMemo(
    () => groupItemsForTimeline(pagination.items),
    [pagination.items],
  );
  const createdCount = candidateViewCounts[CANDIDATE_VIEWS.CREATED];

  const resetPageWith = setter => event => {
    setter(event.target.value);
    setCandidatePage(1);
  };

  const selectCandidateView = view => {
    setCandidateViewChoice({ key: lifecycleKey, view });
    setCandidatePage(1);
  };

  const handleQuickNonOverlappingSelection = () => {
    if (!capabilities.canSelect || items.length === 0) return;
    handleBulkSelection(buildQuickNonOverlappingSelection(items));
  };

  if (isLoading && !preview) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-zinc-950 p-8 text-zinc-300" role="status">
        <Loader2 className="mb-4 h-8 w-8 animate-spin text-brand-orange" aria-hidden="true" />
        <p className="font-semibold">Đang tải bản xem trước…</p>
        {loadingProgress.totalPages > 0 && (
          <p className="mt-2 text-sm text-zinc-500">
            {loadingProgress.loadedPages}/{loadingProgress.totalPages} trang · {loadingProgress.loadedItems}/{loadingProgress.totalItems} ứng viên
          </p>
        )}
      </div>
    );
  }

  if (!preview) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-zinc-950 p-8 text-center text-zinc-300">
        <AlertTriangle className="mb-4 h-10 w-10 text-red-400" aria-hidden="true" />
        <h1 className="text-xl font-bold text-white">Không thể tải bản xem trước</h1>
        <p className="mt-2 max-w-lg text-sm text-zinc-400">
          {snapshotError?.message || 'Bản xem trước không tồn tại hoặc hiện không khả dụng.'}
        </p>
        <div className="mt-6 flex gap-3">
          <button type="button" onClick={() => navigate(-1)} className="rounded-lg border border-zinc-700 px-4 py-2 text-sm">
            Quay lại
          </button>
          <button type="button" onClick={fetchPreview} className="rounded-lg bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950">
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-[400px] flex-1 flex-col bg-zinc-950 text-white">
      <header className="sticky top-0 z-20 flex flex-col gap-4 border-b border-zinc-800 bg-zinc-950/90 p-6 backdrop-blur-md md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-4">
          <button type="button" onClick={() => navigate(-1)} aria-label="Quay lại" className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white">
            <ArrowLeft className="h-5 w-5" aria-hidden="true" />
          </button>
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-xl font-black uppercase tracking-wider md:text-2xl">Bản xem trước lịch chiếu</h1>
              <span className={`rounded border px-2 py-1 text-[10px] font-black tracking-wider ${LIFECYCLE_TONE_CLASSES[capabilities.effectiveStatus] || LIFECYCLE_TONE_CLASSES.CANCELLED}`}>
                {capabilities.effectiveStatus || preview.status}
              </span>
            </div>
            <div className="mt-2 flex flex-wrap gap-4 text-sm text-zinc-400">
              <span className="flex items-center gap-1.5"><MapPin className="h-4 w-4" aria-hidden="true" />{preview.cinemaName}</span>
              <span className="flex items-center gap-1.5"><Calendar className="h-4 w-4" aria-hidden="true" />{formatPreviewDateKey(preview.scheduleFrom)} – {formatPreviewDateKey(preview.scheduleTo)}</span>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={fetchPreview}
            disabled={!capabilities.canRefresh}
            aria-label="Làm mới bản xem trước"
            className="rounded-xl border border-zinc-800 p-2.5 text-zinc-300 hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <RefreshCw className={`h-4 w-4 ${isSnapshotUpdating ? 'animate-spin' : ''}`} aria-hidden="true" />
          </button>
          {capabilities.isEditable && (
            <button
              type="button"
              onClick={handleQuickNonOverlappingSelection}
              disabled={!capabilities.canSelect}
              title="Chọn lại theo giờ bắt đầu sớm nhất và khoảng chiếm phòng; thao tác này có thể thay thế đề xuất tối ưu ban đầu."
              className="flex items-center gap-2 rounded-xl border border-blue-500/20 bg-blue-500/10 px-4 py-2.5 text-xs font-black uppercase tracking-wider text-blue-300 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Wand2 className="h-4 w-4" aria-hidden="true" /> Chọn nhanh không trùng
            </button>
          )}
          {capabilities.isEditable && (
            <button
              type="button"
              onClick={() => setShowApplyModal(true)}
              disabled={!capabilities.canApply}
              className="flex items-center gap-2 rounded-xl bg-brand-orange px-5 py-2.5 text-xs font-black uppercase tracking-wider text-zinc-950 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isApplying ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              Áp dụng ({selectedItemIds.size})
            </button>
          )}
        </div>
      </header>

      <main className="mx-auto w-full max-w-[1600px] space-y-6 p-6 md:p-8">
        <section className={`rounded-xl border p-4 ${LIFECYCLE_TONE_CLASSES[capabilities.effectiveStatus] || LIFECYCLE_TONE_CLASSES.CANCELLED}`} aria-live="polite">
          <div className="flex items-start gap-3">
            <Info className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />
            <div>
              <h2 className="font-bold">{capabilities.effectiveStatus}</h2>
              <p className="mt-1 text-sm opacity-90">{capabilities.lifecycleMessage}</p>
              {capabilities.failureReasonSafe && <p className="mt-1 text-sm">{capabilities.failureReasonSafe}</p>}
            </div>
          </div>
        </section>

        {(isRefreshing || isSnapshotUpdating) && (
          <section className="rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-blue-300" role="status">
            <div className="flex items-center gap-3">
              <Loader2 className="h-5 w-5 animate-spin" aria-hidden="true" />
              <div>
                <p className="font-bold">Đang làm mới ảnh chụp ứng viên</p>
                <p className="mt-1 text-sm">
                  {loadingProgress.loadedPages}/{loadingProgress.totalPages || '…'} trang · {loadingProgress.loadedItems}/{loadingProgress.totalItems || '…'} ứng viên. Dữ liệu hoàn chỉnh trước đó vẫn đang được hiển thị ở chế độ khóa.
                </p>
              </div>
            </div>
          </section>
        )}

        {snapshotError && (
          <section className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300" role="alert">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-start gap-3">
                <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />
                <div>
                  <h2 className="font-bold">Không thể công bố ảnh chụp mới</h2>
                  <p className="mt-1 text-sm">{snapshotError.message}</p>
                </div>
              </div>
              <button type="button" onClick={fetchPreview} disabled={!capabilities.canRefresh} className="rounded-lg border border-amber-500/40 px-3 py-2 text-sm font-bold disabled:opacity-50">
                Làm mới lại
              </button>
            </div>
          </section>
        )}

        {timezoneResolution.usedFallback && (
          <section className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300" role="alert">
            Múi giờ bản xem trước không hợp lệ; thời gian đang được hiển thị tạm thời theo UTC.
          </section>
        )}

        {malformedItems.length > 0 && (
          <section className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300" role="alert">
            {malformedItems.length} ứng viên thiếu khoảng chiếm phòng hợp lệ và không thể được chọn an toàn.
          </section>
        )}

        <section className="grid grid-cols-2 gap-4 md:grid-cols-4" aria-label="Tóm tắt bản xem trước">
          {[
            ['Tổng ứng viên', preview.totalCandidateCount ?? items.length, 'text-white'],
            ['Hợp lệ', preview.validCandidateCount ?? 0, 'text-green-300'],
            ['Bị từ chối', preview.rejectedCandidateCount ?? 0, 'text-red-300'],
            capabilities.effectiveStatus === 'APPLIED'
              ? ['Đã tạo', createdCount, 'text-green-300']
              : ['Hết hạn', formatCinemaDateTime(preview.expiresAt, effectiveTimezone), 'text-amber-300'],
          ].map(([label, value, tone]) => (
            <div key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4">
              <span className="block text-[10px] font-bold uppercase tracking-wider text-zinc-500">{label}</span>
              <span className={`mt-2 block font-black ${typeof value === 'number' ? 'text-2xl' : 'text-sm'} ${tone}`}>{value}</span>
            </div>
          ))}
        </section>

        <section className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex flex-wrap gap-2" role="tablist" aria-label="Nhóm ứng viên">
              {availableCandidateViews.map(view => (
                <button
                  key={view}
                  type="button"
                  role="tab"
                  aria-selected={candidateView === view}
                  onClick={() => selectCandidateView(view)}
                  className={`rounded-xl border px-4 py-2 text-sm font-bold ${candidateView === view ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 text-zinc-400 hover:text-white'}`}
                >
                  {CANDIDATE_VIEW_LABELS[view]} ({candidateViewCounts[view]})
                </button>
              ))}
            </div>
            <div className="flex items-center rounded-xl border border-zinc-800 bg-zinc-900 p-1">
              <button type="button" onClick={() => setViewMode('timeline')} aria-pressed={viewMode === 'timeline'} className={`flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold ${viewMode === 'timeline' ? 'bg-zinc-800 text-white' : 'text-zinc-500'}`}>
                <LayoutTemplate className="h-4 w-4" aria-hidden="true" /> Timeline
              </button>
              <button type="button" onClick={() => setViewMode('table')} aria-pressed={viewMode === 'table'} className={`flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold ${viewMode === 'table' ? 'bg-zinc-800 text-white' : 'text-zinc-500'}`}>
                <List className="h-4 w-4" aria-hidden="true" /> Danh sách
              </button>
            </div>
          </div>

          <div className="flex flex-wrap gap-2 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-3">
            <select aria-label="Lọc phòng chiếu" value={filterAuditorium} onChange={resetPageWith(setFilterAuditorium)} className="rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-300">
              <option value="">Tất cả phòng chiếu</option>
              {uniqueAuditoriums.map(auditorium => <option key={auditorium} value={auditorium}>{auditorium}</option>)}
            </select>
            <select aria-label="Lọc ngày vận hành" value={filterDate} onChange={resetPageWith(setFilterDate)} className="rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-300">
              <option value="">Tất cả ngày</option>
              {uniqueDates.map(date => <option key={date} value={date}>{formatServiceDateKey(date)}</option>)}
            </select>
            <select aria-label="Lọc lý do" value={filterReason} onChange={resetPageWith(setFilterReason)} className="rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-300">
              <option value="">Tất cả lý do</option>
              {rejectionReasons.map(reason => <option key={reason} value={reason}>{translateReason(reason)}</option>)}
            </select>
          </div>
        </section>

        {pagination.totalItems === 0 ? (
          <section className="rounded-2xl border border-dashed border-zinc-800 py-16 text-center text-zinc-500">
            {getEmptyStateMessage(candidateView)}
          </section>
        ) : viewMode === 'timeline' ? (
          <section className="space-y-3">
            {pagination.totalItems > pagination.items.length && (
              <p className="text-sm text-zinc-500">
                Timeline đang hiển thị trang hiện tại ({pagination.items.length}/{pagination.totalItems} ứng viên) để giới hạn số phần tử dựng.
              </p>
            )}
            <AutoScheduleTimeline
              groupedItems={timelineGroups}
              selectedItemIds={selectedItemIds}
              selectedItemsIndex={selectedItemsIndex}
              handleToggleSelection={handleToggleSelection}
              isSelectionBusy={isUpdatingSelection || isApplying || isSnapshotUpdating}
              canSelect={capabilities.canSelect}
              timezone={effectiveTimezone}
            />
          </section>
        ) : (
          <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/40">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[900px] text-left">
                <thead className="border-b border-zinc-800 bg-zinc-950/70 text-[10px] uppercase tracking-wider text-zinc-400">
                  <tr>
                    <th className="px-4 py-3">Chọn</th>
                    <th className="px-4 py-3">Ngày / thời gian</th>
                    <th className="px-4 py-3">Phòng</th>
                    <th className="px-4 py-3">Phim & định dạng</th>
                    <th className="px-4 py-3">Trạng thái áp dụng</th>
                    <th className="px-4 py-3">Chi tiết</th>
                  </tr>
                </thead>
                <tbody>
                  {pagination.items.map(item => {
                    const isSelected = selectedItemIds.has(item.itemPublicId);
                    const selectable = isCandidateSelectable(item, capabilities);
                    const selectionControlAvailable = capabilities.isEditable
                      && item.validationStatus === 'VALID'
                      && item.applyStatus === 'PENDING';
                    const selectionBlock = selectable && !isSelected
                      ? findSelectionBlock(item, selectedItemsIndex)
                      : null;
                    const malformed = malformedItemIds.has(item.itemPublicId)
                      || (selectionBlock && selectionBlock.type !== SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP);
                    const applyState = getCandidateApplyStateMeta(item.applyStatus);
                    const checkboxDisabled = !selectable
                      || isUpdatingSelection
                      || isApplying
                      || isSnapshotUpdating
                      || (!isSelected && Boolean(selectionBlock));

                    return (
                      <tr key={item.itemPublicId} data-testid="candidate-row" className="border-b border-zinc-800/70 text-sm last:border-b-0">
                        <td className="px-4 py-3">
                          {selectionControlAvailable ? (
                            <input
                              type="checkbox"
                              aria-label={`Chọn ${item.movieTitle} lúc ${formatCinemaTime(item.startTime, effectiveTimezone)}`}
                              checked={isSelected}
                              onChange={() => handleToggleSelection(item.itemPublicId, isSelected)}
                              disabled={checkboxDisabled}
                              className="h-4 w-4 rounded border-zinc-700 bg-zinc-900 text-brand-orange focus:ring-brand-orange disabled:cursor-not-allowed"
                            />
                          ) : <span aria-hidden="true" className="text-zinc-600">—</span>}
                        </td>
                        <td className="px-4 py-3">
                          <div className="font-bold text-zinc-200">{formatServiceDateKey(getServiceDateKey(item.serviceDate))}</div>
                          <div className="mt-1 text-xs text-zinc-400">
                            {formatCinemaTime(item.startTime, effectiveTimezone)}–{formatCinemaTime(item.endTime, effectiveTimezone)}
                            {getCinemaDateKey(item.startTime, effectiveTimezone) !== getCinemaDateKey(item.endTime, effectiveTimezone) && ' (+1 ngày)'}
                          </div>
                        </td>
                        <td className="px-4 py-3 text-zinc-300">{item.auditoriumName || item.auditoriumPublicId}</td>
                        <td className="px-4 py-3">
                          <div className="font-bold text-white">{item.movieTitle}</div>
                          <div className="mt-1 text-xs text-zinc-400">{[item.versionName, item.format, item.audioLanguage].filter(Boolean).join(' · ')}</div>
                        </td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex rounded border px-2 py-1 text-[10px] font-black uppercase tracking-wider ${STATE_TONE_CLASSES[applyState.tone]}`}>
                            {applyState.label}
                          </span>
                        </td>
                        <td className="max-w-sm whitespace-normal px-4 py-3 text-xs">
                          {item.validationStatus !== 'VALID' && <p className="text-red-300">{translateReason(item.rejectionReason) || 'Ứng viên không hợp lệ.'}</p>}
                          {selectionBlock?.type === SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP && <p className="text-red-300">Xung đột khoảng chiếm phòng với một ứng viên đã chọn.</p>}
                          {malformed && <p className="text-amber-300">Thiếu dữ liệu khoảng chiếm phòng.</p>}
                          {item.validationStatus === 'VALID' && !selectionBlock && !malformed && <p className="text-zinc-400">{applyState.description}</p>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        )}

        {pagination.totalItems > 0 && (
          <nav className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-zinc-800 bg-zinc-900/50 p-3" aria-label="Phân trang ứng viên">
            <label className="flex items-center gap-2 text-sm text-zinc-400">
              Số dòng
              <select
                aria-label="Số ứng viên mỗi trang"
                value={pagination.pageSize}
                onChange={event => {
                  setCandidatePageSize(Number(event.target.value));
                  setCandidatePage(1);
                }}
                className="rounded-lg border border-zinc-700 bg-zinc-950 px-2 py-1 text-zinc-200"
              >
                {CANDIDATE_PAGE_SIZES.map(size => <option key={size} value={size}>{size}</option>)}
              </select>
            </label>
            <span className="text-sm text-zinc-400">Trang {pagination.page}/{pagination.totalPages} · {pagination.totalItems} ứng viên</span>
            <div className="flex gap-2">
              <button type="button" onClick={() => setCandidatePage(pagination.page - 1)} disabled={pagination.page <= 1} className="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40">Trước</button>
              <button type="button" onClick={() => setCandidatePage(pagination.page + 1)} disabled={pagination.page >= pagination.totalPages} className="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40">Sau</button>
            </div>
          </nav>
        )}
      </main>

      {showApplyModal && capabilities.isEditable && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div role="dialog" aria-modal="true" aria-labelledby="apply-preview-title" className="w-full max-w-md overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 shadow-2xl">
            <div className="flex items-center gap-3 border-b border-zinc-800 p-6">
              <AlertCircle className="h-6 w-6 text-brand-orange" aria-hidden="true" />
              <h2 id="apply-preview-title" className="text-lg font-black uppercase tracking-wider">Xác nhận áp dụng lịch chiếu</h2>
            </div>
            <div className="space-y-4 p-6 text-zinc-300">
              <p>Bạn sắp áp dụng <strong className="text-white">{selectedItemIds.size} suất chiếu</strong> cho <strong className="text-white">{preview.cinemaName}</strong>.</p>
              <div className="rounded-xl border border-blue-500/20 bg-blue-500/10 p-3 text-sm text-blue-300">
                <CheckCircle2 className="mr-2 inline h-4 w-4" aria-hidden="true" />
                Hệ thống sẽ áp dụng theo chế độ {preview.applyMode}.
              </div>
            </div>
            <div className="flex justify-end gap-3 border-t border-zinc-800 bg-zinc-950/50 p-4">
              <button type="button" onClick={() => setShowApplyModal(false)} className="rounded-xl px-4 py-2 text-sm font-bold text-zinc-400 hover:bg-zinc-800">Hủy</button>
              <button
                type="button"
                disabled={!capabilities.canApply}
                onClick={() => {
                  setShowApplyModal(false);
                  handleApply();
                }}
                className="flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2 text-sm font-black text-zinc-950 disabled:opacity-50"
              >
                <Save className="h-4 w-4" aria-hidden="true" /> Xác nhận áp dụng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminAutoSchedulePreviewPage;
