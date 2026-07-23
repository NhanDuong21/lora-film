import { useCallback, useMemo, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  Calendar,
  CheckCircle2,
  Copy,
  Eye,
  ExternalLink,
  Info,
  Loader2,
  MapPin,
  RefreshCw,
  Save,
  Wand2,
  X,
} from 'lucide-react';
import AutoScheduleCandidateDrawer from '@/features/scheduling/admin/components/AutoScheduleCandidateDrawer';
import AutoScheduleTimeline from '@/features/scheduling/admin/components/AutoScheduleTimeline';
import useAutoSchedulePreview from '@/features/scheduling/admin/hooks/useAutoSchedulePreview';
import {
  CANDIDATE_PAGE_SIZES,
  CANDIDATE_VIEWS,
  getDefaultCandidateView,
  getCandidateMetrics,
  paginateCandidates,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewCandidates';
import {
  compareServiceDateKeys,
  formatCinemaDateTime,
  formatPreviewDateRange,
  formatServiceDateKey,
  resolveCinemaTimezone,
  UNKNOWN_SERVICE_DATE_KEY,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import {
  buildQuickNonOverlappingSelection,
  buildSelectedItemsIndex,
  findSelectionBlock,
  SELECTION_BLOCK_TYPES,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewSelection';
import {
  buildCandidateViewModels,
  getDefaultActiveServiceDate,
  getPrimaryTimelineCandidates,
  getRelevantAuditoriums,
  sortCandidateViewModels,
  TIMELINE_ZOOM_MODES,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewViewModel';
import {
  getApplyModePresentation,
  getCandidateValidationPresentation,
  getPreviewShortCode,
  getPreviewStatusPresentation,
  getScoreBreakdownRows,
  getShowtimeStatusPresentation,
} from '@/features/scheduling/admin/utils/schedulingPresentation';

const CANDIDATE_VIEW_LABELS = {
  [CANDIDATE_VIEWS.RECOMMENDED]: 'Đề xuất',
  [CANDIDATE_VIEWS.UNSELECTED_VALID]: 'Hợp lệ chưa chọn',
  [CANDIDATE_VIEWS.ISSUES]: 'Không hợp lệ / xung đột',
  [CANDIDATE_VIEWS.ALL]: 'Tất cả ứng viên',
  [CANDIDATE_VIEWS.CREATED]: 'Suất chiếu đã tạo',
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

const getViewModelsForTab = (viewModels, view) => {
  switch (view) {
    case CANDIDATE_VIEWS.RECOMMENDED:
      return viewModels.filter(candidate => candidate.selected);
    case CANDIDATE_VIEWS.UNSELECTED_VALID:
      return viewModels.filter(candidate => (
        candidate.validationStatus === 'VALID'
        && !candidate.selected
      ));
    case CANDIDATE_VIEWS.ISSUES:
      return viewModels.filter(candidate => (
        candidate.validationStatus !== 'VALID'
        || candidate.applyStatus === 'CONFLICT'
        || candidate.applyStatus === 'FAILED'
      ));
    case CANDIDATE_VIEWS.CREATED:
      return viewModels.filter(candidate => candidate.applyStatus === 'CREATED');
    default:
      return viewModels;
  }
};

const getEmptyStateMessage = view => {
  if (view === CANDIDATE_VIEWS.RECOMMENDED) return 'Không có ứng viên được đề xuất trong bộ lọc hiện tại.';
  if (view === CANDIDATE_VIEWS.UNSELECTED_VALID) return 'Không có ứng viên hợp lệ chưa được chọn.';
  if (view === CANDIDATE_VIEWS.ISSUES) return 'Không có ứng viên không hợp lệ hoặc gặp xung đột.';
  if (view === CANDIDATE_VIEWS.CREATED) return 'Không có suất chiếu nào được tạo từ bản xem trước này.';
  return 'Không có ứng viên phù hợp với bộ lọc hiện tại.';
};

const getStatusTone = candidate => {
  if (candidate.applyStatus === 'CREATED') return 'border-green-500/30 bg-green-500/10 text-green-300';
  if (candidate.applyStatus === 'CONFLICT' || candidate.applyStatus === 'FAILED') return 'border-red-500/30 bg-red-500/10 text-red-300';
  if (candidate.validationStatus !== 'VALID') return 'border-red-500/30 bg-red-500/10 text-red-300';
  if (candidate.applyStatus === 'SKIPPED') return 'border-zinc-700 bg-zinc-800 text-zinc-300';
  return 'border-blue-500/30 bg-blue-500/10 text-blue-300';
};

const AdminAutoSchedulePreviewPage = () => {
  const { id } = useParams();
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const handleSuccess = result => {
    navigate(`/admin/showtimes?source=AUTO&batchId=${encodeURIComponent(id)}`, {
      state: {
        message: `Đã tạo ${result?.createdShowtimeCount ?? 0} suất chiếu; ${result?.skippedItemCount ?? 0} ứng viên không được chọn.`,
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
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [candidateViewChoice, setCandidateViewChoice] = useState({ key: null, view: null });
  const [activeDateChoice, setActiveDateChoice] = useState({ key: null, date: null });
  const [diagnosticChoice, setDiagnosticChoice] = useState({ key: null, id: null });
  const [drawerCandidateId, setDrawerCandidateId] = useState(null);
  const [drawerReturnFocusElement, setDrawerReturnFocusElement] = useState(null);
  const [zoomMode, setZoomMode] = useState(TIMELINE_ZOOM_MODES.FIT);

  const lifecycleKey = `${preview?.previewPublicId || id}:${capabilities?.effectiveStatus || 'UNKNOWN'}`;
  const previewKey = preview?.previewPublicId || id;
  const defaultCandidateView = getDefaultCandidateView(capabilities);
  const candidateView = candidateViewChoice.key === lifecycleKey
    ? candidateViewChoice.view
    : defaultCandidateView;
  const timezoneResolution = useMemo(
    () => resolveCinemaTimezone(preview?.timezoneSnapshot),
    [preview?.timezoneSnapshot],
  );
  const effectiveTimezone = timezoneResolution.timezone;
  const viewModels = useMemo(() => buildCandidateViewModels(items, {
    selectedItemIds,
    timezone: effectiveTimezone,
  }), [effectiveTimezone, items, selectedItemIds]);
  const sortedViewModels = useMemo(() => sortCandidateViewModels(viewModels), [viewModels]);
  const candidateById = useMemo(
    () => new Map(viewModels.map(candidate => [candidate.id, candidate])),
    [viewModels],
  );
  const serviceDates = useMemo(() => Array.from(new Set(
    viewModels
      .map(candidate => candidate.serviceDate)
      .filter(date => date !== UNKNOWN_SERVICE_DATE_KEY),
  )).sort(compareServiceDateKeys), [viewModels]);
  const defaultActiveDate = useMemo(
    () => getDefaultActiveServiceDate(viewModels),
    [viewModels],
  );
  const requestedActiveDate = activeDateChoice.key === previewKey ? activeDateChoice.date : null;
  const activeServiceDate = serviceDates.includes(requestedActiveDate)
    ? requestedActiveDate
    : defaultActiveDate;
  const diagnosticCandidate = diagnosticChoice.key === previewKey
    ? candidateById.get(diagnosticChoice.id) || null
    : null;
  const timelineCandidates = useMemo(() => getPrimaryTimelineCandidates(
    viewModels,
    activeServiceDate,
    diagnosticCandidate,
  ), [activeServiceDate, diagnosticCandidate, viewModels]);
  const relevantAuditoriums = useMemo(
    () => getRelevantAuditoriums(viewModels, activeServiceDate),
    [activeServiceDate, viewModels],
  );
  const selectedItemsIndex = useMemo(
    () => buildSelectedItemsIndex(items, selectedItemIds),
    [items, selectedItemIds],
  );
  const diagnosticConflict = useMemo(() => {
    if (!diagnosticCandidate) return null;
    const block = findSelectionBlock(diagnosticCandidate.raw, selectedItemsIndex);
    return block?.type === SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP ? block : null;
  }, [diagnosticCandidate, selectedItemsIndex]);

  const candidateMetrics = useMemo(
    () => getCandidateMetrics(items, selectedItemIds),
    [items, selectedItemIds],
  );
  const selectedCandidates = useMemo(
    () => items.filter(item => selectedItemIds.has(item.itemPublicId)),
    [items, selectedItemIds],
  );
  const selectedRoomCount = useMemo(() => new Set(
    selectedCandidates.map(item => item.auditoriumPublicId || item.auditoriumName),
  ).size, [selectedCandidates]);
  const selectedIssueCount = useMemo(() => selectedCandidates.filter(item => (
    item.validationStatus === 'REJECTED'
    || item.applyStatus === 'CONFLICT'
    || item.applyStatus === 'FAILED'
  )).length, [selectedCandidates]);
  const viewCounts = useMemo(() => ({
    [CANDIDATE_VIEWS.RECOMMENDED]: candidateMetrics.selectedRecommendations,
    [CANDIDATE_VIEWS.UNSELECTED_VALID]: candidateMetrics.validUnselected,
    [CANDIDATE_VIEWS.ISSUES]: candidateMetrics.issueCandidates,
    [CANDIDATE_VIEWS.ALL]: candidateMetrics.totalGenerated,
    [CANDIDATE_VIEWS.CREATED]: candidateMetrics.createdShowtimes,
  }), [candidateMetrics]);
  const availableCandidateViews = capabilities.effectiveStatus === 'APPLIED'
    ? [CANDIDATE_VIEWS.RECOMMENDED, CANDIDATE_VIEWS.UNSELECTED_VALID, CANDIDATE_VIEWS.ISSUES, CANDIDATE_VIEWS.ALL, CANDIDATE_VIEWS.CREATED]
    : [CANDIDATE_VIEWS.RECOMMENDED, CANDIDATE_VIEWS.UNSELECTED_VALID, CANDIDATE_VIEWS.ISSUES, CANDIDATE_VIEWS.ALL];
  const uniqueAuditoriums = useMemo(() => Array.from(new Set(
    viewModels.map(candidate => candidate.auditoriumName),
  )).sort(), [viewModels]);
  const uniqueReasons = useMemo(() => Array.from(new Set(
    viewModels.map(candidate => candidate.conciseReason).filter(Boolean),
  )).sort(), [viewModels]);
  const filteredCandidates = useMemo(() => getViewModelsForTab(
    sortedViewModels,
    candidateView,
  ).filter(candidate => {
    if (filterAuditorium && candidate.auditoriumName !== filterAuditorium) return false;
    if (filterReason && candidate.conciseReason !== filterReason) return false;
    if (filterDate && candidate.serviceDate !== filterDate) return false;
    return true;
  }), [candidateView, filterAuditorium, filterDate, filterReason, sortedViewModels]);
  const pagination = useMemo(
    () => paginateCandidates(filteredCandidates, candidatePage, candidatePageSize),
    [candidatePage, candidatePageSize, filteredCandidates],
  );
  const drawerBaseCandidate = candidateById.get(drawerCandidateId) || null;
  const drawerCandidate = useMemo(() => drawerBaseCandidate
    ? { ...drawerBaseCandidate, diagnostic: diagnosticCandidate?.id === drawerBaseCandidate.id }
    : null, [diagnosticCandidate?.id, drawerBaseCandidate]);
  const drawerSelectionBlock = useMemo(() => {
    if (!drawerBaseCandidate || drawerBaseCandidate.selected) return null;
    return findSelectionBlock(drawerBaseCandidate.raw, selectedItemsIndex);
  }, [drawerBaseCandidate, selectedItemsIndex]);
  const drawerSelectionBlockedMessage = drawerSelectionBlock?.type === SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP
    ? 'Ứng viên này xung đột khoảng chiếm phòng với một đề xuất đã chọn.'
    : drawerSelectionBlock
      ? 'Ứng viên này thiếu dữ liệu cần thiết để lựa chọn an toàn.'
      : '';

  const openDrawer = useCallback((candidate, triggerElement) => {
    setDrawerReturnFocusElement(triggerElement);
    setDrawerCandidateId(candidate.id);
  }, []);
  const closeDrawer = useCallback(() => setDrawerCandidateId(null), []);

  const selectActiveDate = date => {
    setActiveDateChoice({ key: previewKey, date });
    setDiagnosticChoice({ key: previewKey, id: null });
  };
  const showDiagnosticOnTimeline = candidate => {
    if (!candidate.timelineEligible) return;
    setDiagnosticChoice({ key: previewKey, id: candidate.id });
    setActiveDateChoice({ key: previewKey, date: candidate.serviceDate });
  };
  const clearDiagnostic = () => setDiagnosticChoice({ key: previewKey, id: null });
  const copyPreviewId = async () => {
    const value = preview?.previewPublicId || id;
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      triggerToast?.('Đã sao chép UUID bản xem trước', 'success');
    } catch {
      triggerToast?.('Không thể sao chép UUID bản xem trước', 'error');
    }
  };
  const selectCandidateView = view => {
    setCandidateViewChoice({ key: lifecycleKey, view });
    setCandidatePage(1);
  };
  const resetPageWith = setter => event => {
    setter(event.target.value);
    setCandidatePage(1);
  };
  const handleQuickNonOverlappingSelection = () => {
    if (capabilities.canSelect && items.length > 0) {
      handleBulkSelection(buildQuickNonOverlappingSelection(items));
    }
  };

  if (isLoading && !preview) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-zinc-950 p-8 text-zinc-300" role="status">
        <Loader2 className="mb-4 h-8 w-8 animate-spin text-brand-orange" aria-hidden="true" />
        <p className="font-semibold">Đang tải bản xem trước…</p>
        {loadingProgress.totalPages > 0 && <p className="mt-2 text-sm text-zinc-500">{loadingProgress.loadedPages}/{loadingProgress.totalPages} trang · {loadingProgress.loadedItems}/{loadingProgress.totalItems} ứng viên</p>}
      </div>
    );
  }

  if (!preview) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-zinc-950 p-8 text-center text-zinc-300">
        <AlertTriangle className="mb-4 h-10 w-10 text-red-400" aria-hidden="true" />
        <h1 className="text-xl font-bold text-white">Không thể tải bản xem trước</h1>
        <p className="mt-2 max-w-lg text-sm text-zinc-400">{snapshotError?.message || 'Bản xem trước không tồn tại hoặc hiện không khả dụng.'}</p>
        <button type="button" onClick={fetchPreview} className="mt-5 rounded-lg bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950">Thử lại</button>
      </div>
    );
  }

  return (
    <div className="flex min-h-[400px] flex-1 flex-col bg-zinc-950 text-white">
      <header className="sticky top-0 z-20 flex flex-col gap-4 border-b border-zinc-800 bg-zinc-950/90 p-5 backdrop-blur-md md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-4">
          <button type="button" onClick={() => navigate(-1)} aria-label="Quay lại" className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white"><ArrowLeft className="h-5 w-5" /></button>
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-xl font-black uppercase tracking-wider md:text-2xl">Rà soát lịch chiếu</h1>
              <span className={`rounded border px-2 py-1 text-[10px] font-black tracking-wider ${LIFECYCLE_TONE_CLASSES[capabilities.effectiveStatus] || LIFECYCLE_TONE_CLASSES.CANCELLED}`}>{getPreviewStatusPresentation(capabilities.effectiveStatus || preview.status).label}</span>
            </div>
            <div className="mt-2 flex flex-wrap gap-4 text-sm text-zinc-400">
              <span className="flex items-center gap-1.5"><MapPin className="h-4 w-4" />{preview.cinemaName}</span>
              <span className="flex items-center gap-1.5"><Calendar className="h-4 w-4" />{formatPreviewDateRange(preview.scheduleFrom, preview.scheduleTo)}</span>
              <span>Tạo lúc {formatCinemaDateTime(preview.generatedAt, effectiveTimezone)}</span>
              <span>Mã rút gọn: <strong className="font-mono text-zinc-300">{getPreviewShortCode(preview.previewPublicId || id)}</strong></span>
            </div>
          </div>
        </div>
        <div className="flex flex-wrap gap-3">
          {capabilities.effectiveStatus === 'APPLIED' && (
            <Link
              to={`/admin/showtimes?source=AUTO&batchId=${encodeURIComponent(preview.previewPublicId || id)}`}
              className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-violet-500/30 bg-violet-500/10 px-4 py-2.5 text-xs font-black uppercase text-violet-200 hover:bg-violet-500/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
            >
              <ExternalLink className="h-4 w-4" aria-hidden="true" /> Xem các suất chiếu đã tạo
            </Link>
          )}
          <button type="button" onClick={fetchPreview} disabled={!capabilities.canRefresh} aria-label="Làm mới bản xem trước" className="rounded-xl border border-zinc-800 p-2.5 text-zinc-300 disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${isSnapshotUpdating ? 'animate-spin' : ''}`} /></button>
          {capabilities.isEditable && (
            <button
              type="button"
              onClick={handleQuickNonOverlappingSelection}
              disabled={!capabilities.canSelect}
              aria-label={isUpdatingSelection ? 'Đang tự chọn lịch không xung đột' : 'Tự chọn lịch không xung đột'}
              title={!capabilities.canSelect && !isUpdatingSelection ? 'Bản xem trước hiện không cho phép tự chọn lịch không xung đột.' : undefined}
              className="flex items-center gap-2 rounded-xl border border-blue-500/20 bg-blue-500/10 px-4 py-2.5 text-xs font-black uppercase text-blue-300 disabled:opacity-50"
            >
              {isUpdatingSelection ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> : <Wand2 className="h-4 w-4" aria-hidden="true" />}
              {isUpdatingSelection ? 'Đang tự chọn lịch không xung đột…' : 'Tự chọn lịch không xung đột'}
            </button>
          )}
          {capabilities.isEditable && (
            <button type="button" onClick={() => setShowApplyModal(true)} disabled={!capabilities.canApply} className="flex items-center gap-2 rounded-xl bg-brand-orange px-5 py-2.5 text-xs font-black uppercase text-zinc-950 disabled:opacity-50"><Save className="h-4 w-4" />Áp dụng ({selectedItemIds.size})</button>
          )}
        </div>
      </header>

      <main className="mx-auto w-full max-w-[1700px] space-y-6 p-5 md:p-8">
        <section className={`rounded-xl border p-4 ${LIFECYCLE_TONE_CLASSES[capabilities.effectiveStatus] || LIFECYCLE_TONE_CLASSES.CANCELLED}`} aria-live="polite">
          <div className="flex gap-3"><Info className="mt-0.5 h-5 w-5 shrink-0" /><div className="min-w-0"><h2 className="font-bold">{getPreviewStatusPresentation(capabilities.effectiveStatus).label}</h2><p className="mt-1 text-sm opacity-90">{capabilities.lifecycleMessage}</p><details className="mt-2 text-xs opacity-80"><summary className="cursor-pointer font-bold">Thông tin kỹ thuật</summary><div className="mt-2 space-y-1 break-all font-mono"><p>status: {capabilities.effectiveStatus || preview.status}</p><p>previewPublicId: {preview.previewPublicId || id}</p><p>expiresAt: {preview.expiresAt || '—'}</p><button type="button" onClick={copyPreviewId} className="inline-flex items-center gap-1 rounded border border-current/30 px-2 py-1 font-sans font-bold"><Copy className="h-3 w-3" />Sao chép UUID</button></div></details></div></div>
        </section>

        {(isRefreshing || isSnapshotUpdating) && (
          <section className="rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-blue-300" role="status">
            <p className="font-bold">Đang làm mới ảnh chụp ứng viên</p>
            <p className="mt-1 text-sm">{loadingProgress.loadedPages}/{loadingProgress.totalPages || '…'} trang · {loadingProgress.loadedItems}/{loadingProgress.totalItems || '…'} ứng viên. Lựa chọn đang bị khóa.</p>
          </section>
        )}
        {snapshotError && <section className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300" role="alert"><strong>Không thể công bố ảnh chụp mới.</strong> {snapshotError.message}</section>}
        {timezoneResolution.usedFallback && <section className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300" role="alert">Múi giờ không hợp lệ; thời gian tạm hiển thị theo UTC.</section>}

        <section className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-7" aria-label="Tóm tắt ứng viên">
          {[
            ['Tổng đã tạo', preview.totalCandidateCount ?? candidateMetrics.totalGenerated],
            ['Đề xuất đã chọn', candidateMetrics.selectedRecommendations],
            ['Hợp lệ chưa chọn', candidateMetrics.validUnselected],
            ['Không hợp lệ', candidateMetrics.rejectedCandidates],
            ['Xung đột / thất bại', candidateMetrics.applyConflictsFailures],
            ['Đã tạo suất chiếu', candidateMetrics.createdShowtimes],
            ['Không được chọn', candidateMetrics.skippedCandidates],
          ].map(([label, value]) => <div key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4"><span className="block text-[10px] font-bold uppercase text-zinc-500">{label}</span><span className={`${typeof value === 'number' ? 'text-2xl' : 'text-sm'} mt-2 block font-black text-white`}>{value}</span></div>)}
        </section>
        <p className="rounded-xl border border-zinc-800 bg-zinc-900/40 p-3 text-xs leading-relaxed text-zinc-400">
          Một ứng viên là một phương án ghép phiên bản phim, phòng chiếu, thời điểm bắt đầu, thời lượng phim và thời gian dọn phòng. Ứng viên chỉ trở thành suất chiếu vận hành khi có trạng thái “Đã tạo suất chiếu”.
        </p>

        {capabilities.effectiveStatus === 'APPLIED' && (
          <section className="grid gap-4 rounded-2xl border border-violet-500/30 bg-violet-500/10 p-4 text-violet-100 sm:grid-cols-2" aria-label="Kết quả áp dụng">
            <div>
              <p className="text-[10px] font-black uppercase tracking-wider text-violet-300">Đã áp dụng lúc</p>
              <p className="mt-1 font-bold">{formatCinemaDateTime(preview.appliedAt, effectiveTimezone)}</p>
            </div>
            <div>
              <p className="text-[10px] font-black uppercase tracking-wider text-violet-300">Kết quả vận hành</p>
              <p className="mt-1 font-bold">{candidateMetrics.createdShowtimes} suất chiếu đã tạo</p>
            </div>
          </section>
        )}

        <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-900/30 p-4 md:p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Ngày vận hành đang rà soát</p>
              <div className="mt-2 flex flex-wrap gap-2" role="group" aria-label="Chọn ngày vận hành">
                {serviceDates.map(date => <button key={date} type="button" aria-pressed={activeServiceDate === date} onClick={() => selectActiveDate(date)} className={`rounded-xl border px-3 py-2 text-sm font-bold ${activeServiceDate === date ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 text-zinc-400'}`}>{formatServiceDateKey(date)}</button>)}
              </div>
            </div>
            {diagnosticCandidate && (
              <button type="button" onClick={clearDiagnostic} className="flex items-center gap-2 rounded-xl border border-dashed border-blue-400/60 bg-blue-500/10 px-3 py-2 text-sm font-bold text-blue-200"><X className="h-4 w-4" />Bỏ phủ chẩn đoán: {diagnosticCandidate.movieTitle}</button>
            )}
          </div>
          {diagnosticConflict && (
            <p className="rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-200" role="status">
              Ứng viên chẩn đoán xung đột khoảng chiếm phòng với một đề xuất đã chọn trong toàn bộ ảnh chụp.
            </p>
          )}
          {activeServiceDate ? (
            <AutoScheduleTimeline
              serviceDate={activeServiceDate}
              candidates={timelineCandidates}
              auditoriums={relevantAuditoriums}
              zoomMode={zoomMode}
              onZoomChange={setZoomMode}
              onOpenDetails={openDrawer}
            />
          ) : <div className="py-12 text-center text-zinc-500">Không có ngày vận hành hợp lệ để dựng timeline.</div>}
          <p className="text-xs text-zinc-500" data-testid="timeline-boundary-evidence">Timeline: {timelineCandidates.filter(candidate => !candidate.diagnostic).length} đề xuất đã chọn + {diagnosticCandidate ? 1 : 0} phủ chẩn đoán; dữ liệu đầy đủ {items.length} ứng viên vẫn được giữ cho kiểm tra xung đột.</p>
        </section>

        <section className="space-y-4">
          <p className="rounded-xl border border-zinc-800 bg-zinc-900/50 p-3 text-xs leading-relaxed text-zinc-400">
            Điểm ưu tiên là tổng các thành phần cơ bản, khung giờ cao điểm/thấp điểm, đầu ca, mức phù hợp phòng và độ liền mạch; điểm cao hơn tốt hơn, còn ứng viên không hợp lệ nhận 0 điểm. Hạng toàn cục sắp ứng viên hợp lệ trước, rồi theo điểm giảm dần và các tiêu chí thời gian/phòng/phim ổn định. Hạng chỉ là thứ tự hiển thị, không phải thứ tự chọn: S3 tối đa hóa tổng điểm của một tập không trùng nhau, nên một ứng viên hạng thấp hơn vẫn có thể được chọn.
          </p>
          <div className="flex flex-wrap gap-2" role="tablist" aria-label="Nhóm ứng viên">
            {availableCandidateViews.map(view => <button key={view} type="button" role="tab" aria-selected={candidateView === view} onClick={() => selectCandidateView(view)} className={`rounded-xl border px-4 py-2 text-sm font-bold ${candidateView === view ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 text-zinc-400'}`}>{CANDIDATE_VIEW_LABELS[view]} ({viewCounts[view]})</button>)}
          </div>
          <div className="flex flex-wrap gap-2 rounded-xl border border-zinc-800 bg-zinc-900/50 p-3">
            <select aria-label="Lọc phòng chiếu" value={filterAuditorium} onChange={resetPageWith(setFilterAuditorium)} className="rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs"><option value="">Tất cả phòng</option>{uniqueAuditoriums.map(value => <option key={value} value={value}>{value}</option>)}</select>
            <select aria-label="Lọc ngày vận hành" value={filterDate} onChange={resetPageWith(setFilterDate)} className="rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs"><option value="">Tất cả ngày</option>{serviceDates.map(value => <option key={value} value={value}>{formatServiceDateKey(value)}</option>)}</select>
            <select aria-label="Lọc lý do" value={filterReason} onChange={resetPageWith(setFilterReason)} className="rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs"><option value="">Tất cả lý do</option>{uniqueReasons.map(value => <option key={value} value={value}>{value}</option>)}</select>
          </div>
        </section>

        {pagination.totalItems === 0 ? (
          <section className="rounded-2xl border border-dashed border-zinc-800 py-14 text-center text-zinc-500">{getEmptyStateMessage(candidateView)}</section>
        ) : (
          <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/40">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1050px] text-left">
                <thead className="border-b border-zinc-800 bg-zinc-950/70 text-[10px] uppercase tracking-wider text-zinc-400"><tr><th className="px-4 py-3">Ngày / giờ</th><th className="px-4 py-3">Phòng</th><th className="px-4 py-3">Phim / phiên bản</th><th className="px-4 py-3"><span title="Điểm ưu tiên do chiến lược hiện tại tính; điểm cao hơn tốt hơn.">Điểm ưu tiên</span> / <span title="Hạng trong toàn bộ ứng viên của bản xem trước; đây là thứ tự hiển thị, không phải thứ tự quyết định lựa chọn.">Hạng toàn cục</span></th><th className="px-4 py-3">Kiểm tra / áp dụng</th><th className="px-4 py-3">Lựa chọn / hành động</th></tr></thead>
                <tbody>
                  {pagination.items.map(candidate => {
                    const selectionBlock = !candidate.selected ? findSelectionBlock(candidate.raw, selectedItemsIndex) : null;
                    const selectionAvailable = capabilities.isEditable && candidate.validationStatus === 'VALID' && candidate.applyStatus === 'PENDING';
                    const selectionDisabled = !capabilities.canSelect || Boolean(selectionBlock);
                    const showDiagnosticAction = (candidateView === CANDIDATE_VIEWS.ISSUES || candidateView === CANDIDATE_VIEWS.ALL) && candidate.timelineEligible;
                    const validationPresentation = getCandidateValidationPresentation(candidate.validationStatus);
                    const scoreBreakdownRows = getScoreBreakdownRows(candidate.technicalDetails.scoreBreakdown);
                    return (
                      <tr key={candidate.id} data-testid="candidate-row" className="border-b border-zinc-800/70 align-top text-sm last:border-b-0">
                        <td className="px-4 py-3"><strong className="text-zinc-200">{formatServiceDateKey(candidate.serviceDate)}</strong><div className="mt-1 text-xs text-zinc-400">{candidate.startTimeDisplay}–{candidate.endTimeDisplay}</div></td>
                        <td className="px-4 py-3 text-zinc-300">{candidate.auditoriumName}</td>
                        <td className="px-4 py-3"><strong className="text-white">{candidate.movieTitle}</strong><div className="mt-1 text-xs text-zinc-400">{candidate.versionName}</div></td>
                        <td className="px-4 py-3 text-zinc-300"><span className="font-bold text-white">{candidate.score ?? '—'}</span> / <span>{candidate.rank ?? '—'}</span></td>
                        <td className="px-4 py-3"><span className={`inline-flex rounded border px-2 py-1 text-[10px] font-black uppercase ${getStatusTone(candidate)}`}>{validationPresentation.label} · {candidate.applyState.label}</span>{candidate.conciseReason && <div className="mt-2 max-w-xs text-xs text-zinc-400">{candidate.conciseReason}</div>}</td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap items-center gap-2">
                            {selectionAvailable ? <label className="flex items-center gap-2 text-xs font-bold text-zinc-300"><input type="checkbox" checked={candidate.selected} disabled={selectionDisabled} onChange={() => handleToggleSelection(candidate.id, candidate.selected)} aria-label={`Chọn ${candidate.movieTitle} lúc ${candidate.startTimeDisplay}`} />{candidate.selected ? 'Đã chọn' : 'Chưa chọn'}</label> : <span className="text-xs text-zinc-500">{candidate.selected ? 'Đã chọn' : 'Không thể chọn'}</span>}
                            {showDiagnosticAction && <button type="button" onClick={() => showDiagnosticOnTimeline(candidate)} className="flex items-center gap-1 rounded-lg border border-blue-500/30 px-2 py-1.5 text-xs font-bold text-blue-300"><Eye className="h-3.5 w-3.5" />Xem trên timeline</button>}
                            {candidate.createdShowtimePath && (
                              <Link
                                to={candidate.createdShowtimePath}
                                className="inline-flex items-center gap-1 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-2 py-1.5 text-xs font-bold text-emerald-300"
                                aria-label={`Mở suất chiếu ${candidate.createdShowtimePublicId}`}
                              >
                                <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" /> Suất chiếu
                              </Link>
                            )}
                            <button type="button" onClick={event => openDrawer(candidate, event.currentTarget)} className="rounded-lg border border-zinc-700 px-2 py-1.5 text-xs font-bold text-zinc-300">Mở chi tiết</button>
                          </div>
                          <details className="mt-3 text-xs text-zinc-400"><summary className="cursor-pointer font-bold">Dữ liệu mở rộng</summary><dl className="mt-2 space-y-1 break-all"><div className="font-mono">ID: {candidate.id}</div><div className="font-mono">validationStatus: {candidate.validationStatus}</div><div className="font-mono">applyStatus: {candidate.applyStatus}</div><div className="font-mono">startTime: {candidate.technicalDetails.startTime || '—'}</div><div className="font-mono">endTime: {candidate.technicalDetails.endTime || '—'}</div><div className="font-mono">occupancyEndTime: {candidate.technicalDetails.occupancyEndTime || '—'}</div><div className="font-mono">codes: {[candidate.technicalDetails.rejectionCode, candidate.technicalDetails.applyErrorCode].filter(Boolean).join(' / ') || '—'}</div>{scoreBreakdownRows.length > 0 && <div className="mt-2 rounded border border-zinc-800 p-2"><dt className="font-bold text-zinc-300">Thành phần điểm</dt>{scoreBreakdownRows.map(row => <dd key={row.key} className="mt-1 flex justify-between gap-3"><span>{row.label}</span><span className="font-mono">{row.value}</span></dd>)}</div>}<div className="font-mono">scoreBreakdown(raw): {candidate.technicalDetails.scoreBreakdown ? JSON.stringify(candidate.technicalDetails.scoreBreakdown) : '—'}</div></dl></details>
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
            <label className="flex items-center gap-2 text-sm text-zinc-400">Số dòng<select aria-label="Số ứng viên mỗi trang" value={pagination.pageSize} onChange={event => { setCandidatePageSize(Number(event.target.value)); setCandidatePage(1); }} className="rounded-lg border border-zinc-700 bg-zinc-950 px-2 py-1 text-zinc-200">{CANDIDATE_PAGE_SIZES.map(size => <option key={size} value={size}>{size}</option>)}</select></label>
            <span className="text-sm text-zinc-400">Trang {pagination.page}/{pagination.totalPages} · {pagination.totalItems} ứng viên</span>
            <div className="flex gap-2"><button type="button" onClick={() => setCandidatePage(pagination.page - 1)} disabled={pagination.page <= 1} className="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40">Trước</button><button type="button" onClick={() => setCandidatePage(pagination.page + 1)} disabled={pagination.page >= pagination.totalPages} className="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40">Sau</button></div>
          </nav>
        )}
      </main>

      <AutoScheduleCandidateDrawer
        candidate={drawerCandidate}
        timezone={effectiveTimezone}
        capabilities={capabilities}
        selectionBlockedMessage={drawerSelectionBlockedMessage}
        onToggleSelection={handleToggleSelection}
        canInspectOnTimeline={Boolean(
          drawerCandidate?.timelineEligible
          && (candidateView === CANDIDATE_VIEWS.ISSUES || candidateView === CANDIDATE_VIEWS.ALL)
        )}
        onShowDiagnostic={() => showDiagnosticOnTimeline(drawerCandidate)}
        onClearDiagnostic={clearDiagnostic}
        onClose={closeDrawer}
        returnFocusElement={drawerReturnFocusElement}
      />

      {showApplyModal && capabilities.isEditable && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/70 p-4">
          <div role="dialog" aria-modal="true" aria-labelledby="apply-preview-title" className="w-full max-w-lg rounded-2xl border border-zinc-800 bg-zinc-900 p-6">
            <div className="flex items-center gap-3">
              <AlertCircle className="h-6 w-6 text-brand-orange" />
              <h2 id="apply-preview-title" className="text-lg font-black">Xác nhận áp dụng lịch chiếu</h2>
            </div>
            <dl className="mt-5 grid grid-cols-[1fr_auto] gap-x-4 gap-y-2 text-sm">
              <dt className="text-zinc-500">Cụm rạp</dt><dd className="text-right font-bold text-white">{preview.cinemaName}</dd>
              <dt className="text-zinc-500">Khoảng ngày vận hành</dt><dd className="text-right font-bold text-white">{formatPreviewDateRange(preview.scheduleFrom, preview.scheduleTo)}</dd>
              <dt className="text-zinc-500">Phòng đã chọn</dt><dd className="text-right font-bold text-white">{selectedRoomCount}</dd>
              <dt className="text-zinc-500">Ứng viên đã chọn</dt><dd className="text-right font-bold text-white">{selectedItemIds.size}</dd>
              <dt className="text-zinc-500">Dự kiến tạo suất chiếu</dt><dd className="text-right font-bold text-emerald-300">{selectedItemIds.size}</dd>
              <dt className="text-zinc-500">Trạng thái ban đầu</dt><dd className="text-right font-bold text-white">{getShowtimeStatusPresentation('DRAFT').label}</dd>
              <dt className="text-zinc-500">Chế độ áp dụng</dt><dd className="text-right font-bold text-white">{getApplyModePresentation(preview.applyMode).label}</dd>
              <dt className="text-zinc-500">Không hợp lệ / xung đột toàn bản</dt><dd className="text-right font-bold text-amber-300">{candidateMetrics.issueCandidates}</dd>
              <dt className="text-zinc-500">Không hợp lệ / xung đột đã chọn</dt><dd className="text-right font-bold text-amber-300">{selectedIssueCount}</dd>
            </dl>
            <div className="mt-5 space-y-2 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs leading-relaxed text-amber-200">
              <p>Sau khi áp dụng thành công, lựa chọn trở thành chỉ đọc.</p>
              <p>Nếu lỗi truyền tải xảy ra, hộp thoại vẫn mở và lần thử lại dùng cùng khóa an toàn để không tạo trùng.</p>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button type="button" disabled={isApplying} onClick={() => setShowApplyModal(false)} className="rounded-xl px-4 py-2 text-zinc-400 disabled:opacity-50">Hủy</button>
              <button
                type="button"
                disabled={!capabilities.canApply || isApplying}
                onClick={() => handleApply()}
                className="flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2 font-black text-zinc-950 disabled:opacity-50"
              >
                {isApplying ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                {isApplying ? 'Đang áp dụng…' : 'Xác nhận'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminAutoSchedulePreviewPage;
