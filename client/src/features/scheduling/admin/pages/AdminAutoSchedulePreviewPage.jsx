import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useOutletContext, useParams } from 'react-router-dom';
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
  RotateCcw,
  Save,
  X,
} from 'lucide-react';
import AutoScheduleCandidateDrawer from '@/features/scheduling/admin/components/AutoScheduleCandidateDrawer';
import AutoScheduleTimeline from '@/features/scheduling/admin/components/AutoScheduleTimeline';
import useAutoSchedulePreview from '@/features/scheduling/admin/hooks/useAutoSchedulePreview';
import useExistingShowtimeSummary from '@/features/scheduling/admin/hooks/useExistingShowtimeSummary';
import adminAutoScheduleService from '@/features/scheduling/admin/services/adminAutoScheduleService';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import {
  CANDIDATE_PAGE_SIZES,
  CANDIDATE_VIEWS,
  getDefaultCandidateView,
  getCandidateMetrics,
  getMovieDistribution,
  getMovieDistributionSummary,
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
  buildSelectedItemsIndex,
  findSelectionBlock,
  SELECTION_BLOCK_TYPES,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewSelection';
import { getDailyOperationalSummaries } from '@/features/scheduling/admin/utils/autoScheduleOperationalInsights';
import { buildAutoScheduleRecreateDraft } from '@/features/scheduling/admin/utils/autoScheduleRecreateDraft';
import {
  buildCandidateViewModels,
  getDefaultActiveServiceDate,
  getPrimaryTimelineCandidates,
  getRelevantAuditoriums,
  sortCandidateViewModels,
  TIMELINE_ZOOM_MODES,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewViewModel';
import {
  getCandidateValidationPresentation,
  getPreviewShortCode,
  getPreviewStatusPresentation,
  getScoreBreakdownRows,
  getShowtimeStatusPresentation,
} from '@/features/scheduling/admin/utils/schedulingPresentation';

const CANDIDATE_VIEW_LABELS = {
  [CANDIDATE_VIEWS.RECOMMENDED]: 'Hệ thống đã chọn',
  [CANDIDATE_VIEWS.UNSELECTED_VALID]: 'Phương án thay thế',
  [CANDIDATE_VIEWS.ISSUES]: 'Cần kiểm tra',
  [CANDIDATE_VIEWS.ALL]: 'Tất cả phương án',
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
  if (view === CANDIDATE_VIEWS.RECOMMENDED) return 'Không có suất được đề xuất trong bộ lọc hiện tại.';
  if (view === CANDIDATE_VIEWS.UNSELECTED_VALID) return 'Không có suất hợp lệ nào đang chờ chọn.';
  if (view === CANDIDATE_VIEWS.ISSUES) return 'Không có suất lỗi hoặc trùng lịch.';
  if (view === CANDIDATE_VIEWS.CREATED) return 'Không có suất chiếu nào được tạo từ bản lịch này.';
  return 'Không có suất đề xuất phù hợp với bộ lọc hiện tại.';
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
  const location = useLocation();

  const handleSuccess = result => {
    navigate(`/admin/showtimes?source=AUTO&batchId=${encodeURIComponent(id)}`, {
      state: {
        message: `Đã tạo ${result?.createdShowtimeCount ?? 0} suất chiếu; ${result?.skippedItemCount ?? 0} đề xuất không được chọn.`,
      },
    });
  };

  const {
    preview,
    items,
    selectedItemIds,
    expectedVersion,
    isLoading,
    isRefreshing,
    isSnapshotUpdating,
    loadingProgress,
    snapshotError,
    pricingPreflight,
    pricingPreflightError,
    isCheckingPricing,
    capabilities,
    isApplying,
    handleToggleSelection,
    handleApply,
    checkPricingReadiness,
    fetchPreview,
  } = useAutoSchedulePreview(id, { triggerToast, onSuccess: handleSuccess });
  const existingSchedule = useExistingShowtimeSummary({
    cinemaSlug: preview?.cinemaSlug,
    scheduleFrom: preview?.scheduleFrom,
    scheduleTo: preview?.scheduleTo,
    excludeBatchId: preview?.previewPublicId || id,
  });

  const [filterAuditorium, setFilterAuditorium] = useState('');
  const [filterMovie, setFilterMovie] = useState('');
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
  const [distributionDate, setDistributionDate] = useState('');
  const [scheduleActionDialog, setScheduleActionDialog] = useState(null);
  const [isScheduleActionLoading, setIsScheduleActionLoading] = useState(false);
  const requestedScheduleActionRef = useRef(location.state?.autoScheduleAction || '');

  const prepareReplacement = useCallback(async () => {
    setScheduleActionDialog({ type: 'REPLACE', phase: 'checking', summary: null });
    setIsScheduleActionLoading(true);
    try {
      const response = await adminShowtimeService.previewBatchStatus(id, 'CANCELLED');
      if (!response?.success || !response.data) {
        throw new Error('Không nhận được kết quả kiểm tra an toàn.');
      }
      setScheduleActionDialog({
        type: 'REPLACE',
        phase: response.data.actionAllowed ? 'confirm' : 'blocked',
        summary: response.data,
      });
    } catch (error) {
      setScheduleActionDialog({
        type: 'REPLACE',
        phase: 'error',
        summary: null,
        message: error?.response?.data?.message || error?.message || 'Không thể kiểm tra điều kiện thay lịch.',
      });
    } finally {
      setIsScheduleActionLoading(false);
    }
  }, [id]);

  const openScheduleAction = useCallback((type) => {
    if (type === 'DISCARD') {
      setScheduleActionDialog({ type: 'DISCARD', phase: 'confirm', summary: null });
      return;
    }
    prepareReplacement();
  }, [prepareReplacement]);

  useEffect(() => {
    if (!preview || !requestedScheduleActionRef.current) return;
    const requestedAction = requestedScheduleActionRef.current;
    requestedScheduleActionRef.current = '';
    if (requestedAction === 'DISCARD' && preview.status === 'PREVIEWED') {
      openScheduleAction('DISCARD');
    }
    if (requestedAction === 'REPLACE' && preview.status === 'APPLIED') {
      openScheduleAction('REPLACE');
    }
  }, [openScheduleAction, preview]);

  const confirmScheduleAction = async () => {
    if (!scheduleActionDialog || scheduleActionDialog.phase !== 'confirm') return;
    const draft = buildAutoScheduleRecreateDraft(preview, items);
    setIsScheduleActionLoading(true);
    try {
      if (scheduleActionDialog.type === 'DISCARD') {
        const response = await adminAutoScheduleService.cancelPreview(id, { expectedVersion });
        if (!response?.success) throw new Error('Không thể bỏ bản đề xuất.');
      } else {
        const response = await adminShowtimeService.transitionBatchStatus(id, {
          status: 'CANCELLED',
          reason: 'Thay thế bằng một lịch tự động mới',
        });
        if (!response?.success || !response.data?.actionAllowed) {
          setScheduleActionDialog({
            type: 'REPLACE',
            phase: 'blocked',
            summary: response?.data || scheduleActionDialog.summary,
          });
          return;
        }
      }

      triggerToast?.(
        scheduleActionDialog.type === 'DISCARD'
          ? 'Đã bỏ bản đề xuất cũ. Bạn có thể điều chỉnh và tạo lại.'
          : 'Đã hủy toàn bộ suất nháp cũ. Bạn có thể tạo lịch thay thế.',
        'success',
      );
      navigate('/admin/showtime-schedules/create', {
        state: {
          autoScheduleRecreate: {
            draft,
            sourcePreviewId: preview.previewPublicId || id,
            sourceShortCode: getPreviewShortCode(preview.previewPublicId || id),
          },
        },
      });
    } catch (error) {
      setScheduleActionDialog(previous => ({
        ...previous,
        phase: 'error',
        message: error?.response?.data?.message || error?.message || 'Không thể chuẩn bị tạo lại lịch.',
      }));
    } finally {
      setIsScheduleActionLoading(false);
    }
  };

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
  const allMovieDistribution = useMemo(
    () => getMovieDistribution(viewModels),
    [viewModels],
  );
  const movieDistribution = useMemo(
    () => getMovieDistribution(viewModels, distributionDate),
    [distributionDate, viewModels],
  );
  const movieDistributionSummary = useMemo(
    () => getMovieDistributionSummary(movieDistribution),
    [movieDistribution],
  );
  const overallMovieDistributionSummary = useMemo(
    () => getMovieDistributionSummary(allMovieDistribution),
    [allMovieDistribution],
  );
  const dailyOperationalSummaries = useMemo(
    () => getDailyOperationalSummaries(viewModels),
    [viewModels],
  );
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
  const selectedMovieCount = useMemo(() => new Set(
    selectedCandidates.map(item => item.moviePublicId || item.movieVersionPublicId || item.movieTitle),
  ).size, [selectedCandidates]);
  const selectedIssueCount = useMemo(() => selectedCandidates.filter(item => (
    item.validationStatus === 'REJECTED'
    || item.applyStatus === 'CONFLICT'
    || item.applyStatus === 'FAILED'
  )).length, [selectedCandidates]);
  const pricingReady = Boolean(pricingPreflight?.complete);
  const pricingNeedsAttention = Boolean(pricingPreflight && !pricingPreflight.complete);
  const pricingHasAmbiguity = Boolean(pricingPreflight?.reasonGroups?.some(
    group => group.reasonCode === 'PRICING_AMBIGUOUS',
  ));
  const pricingActionLabel = pricingHasAmbiguity
    ? 'Sửa bảng giá bị trùng'
    : 'Thiết lập bảng giá cho rạp này';
  const pricingRepairPath = useMemo(() => {
    if (!preview?.cinemaPublicId) return '/admin/pricing';
    const params = new URLSearchParams({
      cinema: preview.cinemaPublicId,
      effectiveDate: preview.scheduleFrom || '',
      effectiveFrom: preview.scheduleFrom || '',
      effectiveTo: preview.scheduleTo || '',
      returnTo: location.pathname,
    });
    return `/admin/pricing?${params.toString()}`;
  }, [location.pathname, preview?.cinemaPublicId, preview?.scheduleFrom, preview?.scheduleTo]);
  const readinessToneClass = capabilities.effectiveStatus === 'PREVIEWED'
    ? pricingNeedsAttention
      ? 'border-red-500/30 bg-red-500/10 text-red-200'
      : pricingReady
        ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200'
        : 'border-blue-500/30 bg-blue-500/10 text-blue-200'
    : (LIFECYCLE_TONE_CLASSES[capabilities.effectiveStatus] || LIFECYCLE_TONE_CLASSES.CANCELLED);
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
  const uniqueMovies = useMemo(() => allMovieDistribution
    .map(movie => ({ key: movie.movieKey, title: movie.movieTitle }))
    .sort((left, right) => left.title.localeCompare(right.title, 'vi')), [allMovieDistribution]);
  const uniqueReasons = useMemo(() => Array.from(new Set(
    viewModels.map(candidate => candidate.conciseReason).filter(Boolean),
  )).sort(), [viewModels]);
  const filteredCandidates = useMemo(() => getViewModelsForTab(
    sortedViewModels,
    candidateView,
  ).filter(candidate => {
    if (filterAuditorium && candidate.auditoriumName !== filterAuditorium) return false;
    if (filterMovie && candidate.movieKey !== filterMovie) return false;
    if (filterReason && candidate.conciseReason !== filterReason) return false;
    if (filterDate && candidate.serviceDate !== filterDate) return false;
    return true;
  }), [candidateView, filterAuditorium, filterDate, filterMovie, filterReason, sortedViewModels]);
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
    ? 'Suất đề xuất này bị trùng khoảng sử dụng phòng với một suất đã chọn.'
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
      triggerToast?.('Đã sao chép mã kỹ thuật của bản lịch', 'success');
    } catch {
      triggerToast?.('Không thể sao chép mã kỹ thuật của bản lịch', 'error');
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
  if (isLoading && !preview) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-zinc-950 p-8 text-zinc-300" role="status">
        <Loader2 className="mb-4 h-8 w-8 animate-spin text-brand-orange" aria-hidden="true" />
        <p className="font-semibold">Đang tải lịch đang soạn…</p>
        {loadingProgress.totalPages > 0 && <p className="mt-2 text-sm text-zinc-500">{loadingProgress.loadedPages}/{loadingProgress.totalPages} trang · {loadingProgress.loadedItems}/{loadingProgress.totalItems} suất đề xuất</p>}
      </div>
    );
  }

  if (!preview) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center bg-zinc-950 p-8 text-center text-zinc-300">
        <AlertTriangle className="mb-4 h-10 w-10 text-red-400" aria-hidden="true" />
        <h1 className="text-xl font-bold text-white">Không thể tải lịch đang soạn</h1>
        <p className="mt-2 max-w-lg text-sm text-zinc-400">{snapshotError?.message || 'Bản lịch không tồn tại hoặc hiện không khả dụng.'}</p>
        <button type="button" onClick={fetchPreview} className="mt-5 rounded-lg bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950">Thử lại</button>
      </div>
    );
  }

  return (
    <div className="flex min-h-[400px] flex-1 flex-col bg-zinc-950 text-white">
      <header className="flex flex-col gap-4 border-b border-zinc-800 bg-zinc-950 p-5 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-4">
          <button type="button" onClick={() => navigate(-1)} aria-label="Quay lại" className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white"><ArrowLeft className="h-5 w-5" /></button>
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-xl font-black tracking-tight md:text-2xl">Kiểm tra lịch đề xuất</h1>
              <span className={`rounded border px-2 py-1 text-[10px] font-black tracking-wider ${LIFECYCLE_TONE_CLASSES[capabilities.effectiveStatus] || LIFECYCLE_TONE_CLASSES.CANCELLED}`}>{getPreviewStatusPresentation(capabilities.effectiveStatus || preview.status).label}</span>
              <span className="rounded border border-zinc-700 bg-zinc-900 px-2 py-1 text-[10px] font-bold text-zinc-400">Bản lịch tự động</span>
            </div>
            <div className="mt-2 flex flex-wrap gap-4 text-sm text-zinc-400">
              <span className="flex items-center gap-1.5"><MapPin className="h-4 w-4" />{preview.cinemaName}</span>
              <span className="flex items-center gap-1.5"><Calendar className="h-4 w-4" />{formatPreviewDateRange(preview.scheduleFrom, preview.scheduleTo)}</span>
              <span>Tạo lúc {formatCinemaDateTime(preview.generatedAt, effectiveTimezone)}</span>
              <span>Mã lịch: <strong className="text-zinc-300">{getPreviewShortCode(preview.previewPublicId || id)}</strong></span>
            </div>
          </div>
        </div>
        <div className="flex flex-wrap gap-3">
          {capabilities.effectiveStatus === 'PREVIEWED' && (
            <button
              type="button"
              onClick={() => openScheduleAction('DISCARD')}
              disabled={isSnapshotUpdating || isScheduleActionLoading}
              className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-2.5 text-xs font-black uppercase text-amber-200 hover:bg-amber-500/20 disabled:opacity-50"
            >
              <RotateCcw className="h-4 w-4" aria-hidden="true" /> Bỏ bản đề xuất &amp; tạo lại
            </button>
          )}
          {capabilities.effectiveStatus === 'APPLIED' && (
            <>
              <button
                type="button"
                onClick={() => openScheduleAction('REPLACE')}
                disabled={isScheduleActionLoading}
                className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-2.5 text-xs font-black uppercase text-amber-200 hover:bg-amber-500/20 disabled:opacity-50"
              >
                {isScheduleActionLoading ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" /> : <RotateCcw className="h-4 w-4" aria-hidden="true" />}
                Thay lịch
              </button>
              <Link
                to={`/admin/showtimes?source=AUTO&batchId=${encodeURIComponent(preview.previewPublicId || id)}`}
                className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-violet-500/30 bg-violet-500/10 px-4 py-2.5 text-xs font-black uppercase text-violet-200 hover:bg-violet-500/20 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
              >
                <ExternalLink className="h-4 w-4" aria-hidden="true" /> Xem các suất chiếu đã tạo
              </Link>
            </>
          )}
          <button type="button" onClick={fetchPreview} disabled={!capabilities.canRefresh} aria-label="Làm mới bản lịch" className="rounded-xl border border-zinc-800 p-2.5 text-zinc-300 disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${isSnapshotUpdating ? 'animate-spin' : ''}`} /></button>
        </div>
      </header>

      <main className="mx-auto w-full max-w-[1700px] space-y-6 p-5 md:p-8">
        <section className={`rounded-xl border p-4 ${readinessToneClass}`} aria-live="polite">
          <div className="flex gap-3"><Info className="mt-0.5 h-5 w-5 shrink-0" /><div className="min-w-0"><h2 className="font-bold">{capabilities.effectiveStatus === 'PREVIEWED' ? (pricingNeedsAttention ? 'Chưa thể tạo suất chiếu' : pricingReady ? 'Sẵn sàng tạo suất chiếu' : 'Đang kiểm tra điều kiện tạo suất') : getPreviewStatusPresentation(capabilities.effectiveStatus).label}</h2><p className="mt-1 text-sm opacity-90">{capabilities.effectiveStatus === 'PREVIEWED' ? (pricingNeedsAttention ? 'Lịch không bị trùng, nhưng bảng giá chưa đủ. Hãy xử lý mục Giá vé bên dưới.' : pricingReady ? 'Lịch và bảng giá đã sẵn sàng. Bạn có thể tạo các suất chiếu ở trạng thái đang soạn.' : 'Hệ thống đang kiểm tra bảng giá cho toàn bộ suất đã chọn.') : capabilities.lifecycleMessage}</p><details className="mt-2 text-xs opacity-80"><summary className="cursor-pointer font-bold">Thông tin kỹ thuật</summary><div className="mt-2 space-y-1 break-all font-mono"><p>status: {capabilities.effectiveStatus || preview.status}</p><p>previewPublicId: {preview.previewPublicId || id}</p><p>expiresAt: {preview.expiresAt || '—'}</p><button type="button" onClick={copyPreviewId} className="inline-flex items-center gap-1 rounded border border-current/30 px-2 py-1 font-sans font-bold"><Copy className="h-3 w-3" />Sao chép UUID</button></div></details></div></div>
        </section>

        {(isRefreshing || isSnapshotUpdating) && (
          <section className="rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-blue-300" role="status">
            <p className="font-bold">Đang cập nhật các suất chiếu đề xuất</p>
            <p className="mt-1 text-sm">{loadingProgress.loadedPages}/{loadingProgress.totalPages || '…'} trang · {loadingProgress.loadedItems}/{loadingProgress.totalItems || '…'} suất đề xuất. Lựa chọn tạm thời bị khóa.</p>
          </section>
        )}
        {snapshotError && <section className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300" role="alert"><strong>Không thể cập nhật lịch mới nhất.</strong> {snapshotError.message}</section>}
        {isCheckingPricing && capabilities.isEditable && (
          <section className="flex items-start gap-3 rounded-xl border border-blue-500/30 bg-blue-500/10 p-4 text-blue-100" role="status">
            <Loader2 className="mt-0.5 h-5 w-5 shrink-0 animate-spin" aria-hidden="true" />
            <div><h2 className="font-black">Đang kiểm tra bảng giá</h2><p className="mt-1 text-sm text-blue-200/80">Hệ thống đang kiểm tra từng phòng, loại ghế và ngày chiếu đã chọn.</p></div>
          </section>
        )}
        {pricingPreflight && (
          <section className={`rounded-xl border p-4 ${pricingPreflight.complete ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200' : 'border-red-500/30 bg-red-500/10 text-red-100'}`} role={pricingPreflight.complete ? 'status' : 'alert'}>
            <h2 className="font-black">
              {pricingPreflight.complete
                ? `Giá vé đã sẵn sàng cho ${pricingPreflight.totalCandidateCount} suất`
                : `Chưa thể tạo ${pricingPreflight.totalCandidateCount} suất chiếu`}
            </h2>
            {!pricingPreflight.complete && <p className="mt-1 text-sm">Chỉ {pricingPreflight.completeCandidateCount}/{pricingPreflight.totalCandidateCount} suất đã đủ giá. Không suất nào sẽ được tạo cho đến khi bạn sửa xong.</p>}
            {pricingPreflight.reasonGroups?.map(group => {
              const displayMessage = group.displayMessage
                || 'Không xác định — xem chi tiết kỹ thuật';
              return (
                <div key={group.reasonCode} className="mt-3 rounded-lg border border-current/20 p-3 text-sm">
                  <p className="font-bold">{group.count} suất · {displayMessage}</p>
                  {group.affectedDates?.length > 0 && <p className="mt-1">Ngày: {group.affectedDates.map(formatServiceDateKey).join(', ')}</p>}
                  {group.auditoriums?.length > 0 && <p className="mt-1">Phòng: {group.auditoriums.map(room => room.name).join(', ')}</p>}
                  {group.seatTypes?.length > 0 && <p className="mt-1">Loại ghế: {group.seatTypes.map(seat => seat.name).join(', ')}</p>}
                  <details className="mt-2 text-xs opacity-70"><summary className="cursor-pointer">Chi tiết kỹ thuật</summary><p className="mt-1 font-mono">{group.reasonCode}</p></details>
                </div>
              );
            })}
            {!pricingPreflight.complete && (
              <div className="mt-4 flex flex-wrap gap-3">
                <Link to={pricingRepairPath} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-red-200 px-4 text-sm font-black text-red-950 hover:bg-white">
                  <ExternalLink className="h-4 w-4" aria-hidden="true" /> {pricingActionLabel}
                </Link>
                <button type="button" onClick={() => checkPricingReadiness({ force: true })} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-red-200/30 px-4 text-sm font-bold text-red-100 hover:bg-red-500/10">
                  <RefreshCw className="h-4 w-4" aria-hidden="true" /> Kiểm tra lại giá
                </button>
              </div>
            )}
          </section>
        )}
        {pricingPreflightError && capabilities.isEditable && (
          <section className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-100" role="alert">
            <h2 className="font-black">Chưa kiểm tra được bảng giá</h2>
            <p className="mt-1 text-sm">{pricingPreflightError}</p>
            <button type="button" onClick={() => checkPricingReadiness({ force: true })} className="mt-3 inline-flex min-h-10 items-center gap-2 rounded-xl border border-amber-300/30 px-4 text-sm font-bold text-amber-100 hover:bg-amber-500/10"><RefreshCw className="h-4 w-4" />Thử kiểm tra lại</button>
          </section>
        )}
        {timezoneResolution.usedFallback && <section className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-amber-300" role="alert"><p>Cấu hình giờ của rạp đang bị lỗi; thời gian tạm hiển thị theo giờ chuẩn hệ thống.</p><Link to={`/admin/cinemas/${encodeURIComponent(preview.cinemaPublicId)}`} className="mt-3 inline-flex min-h-10 items-center gap-2 rounded-xl border border-amber-400/30 px-4 text-sm font-black hover:bg-amber-500/10"><ExternalLink className="h-4 w-4" />Sửa cấu hình rạp</Link></section>}

        <section className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5" aria-label="Tóm tắt lịch đề xuất">
          {[
            [capabilities.effectiveStatus === 'APPLIED' ? 'Suất chiếu đã tạo' : 'Suất được đề xuất', capabilities.effectiveStatus === 'APPLIED' ? candidateMetrics.createdShowtimes : candidateMetrics.selectedRecommendations],
            ['Phim có suất đề xuất', `${overallMovieDistributionSummary.representedMovieCount}/${allMovieDistribution.length}`],
            ['Phòng được sử dụng', selectedRoomCount],
            ['Suất cần kiểm tra', candidateMetrics.issueCandidates],
            ['Tình trạng giá', isCheckingPricing ? 'Đang kiểm tra' : pricingReady ? 'Đã đủ' : pricingNeedsAttention ? 'Cần xử lý' : 'Chưa kiểm tra'],
          ].map(([label, value]) => <div key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4"><span className="block text-[10px] font-bold uppercase text-zinc-500">{label}</span><span className={`${typeof value === 'number' ? 'text-2xl' : 'text-sm'} mt-2 block font-black text-white`}>{value}</span></div>)}
        </section>

        <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-900/30 p-4 md:p-5" aria-labelledby="proposal-timeline-title">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Lịch đề xuất</p>
              <h2 id="proposal-timeline-title" className="mt-1 text-xl font-black text-zinc-100">Phòng chiếu × thời gian</h2>
              <p className="mt-1 text-sm text-zinc-500">Rà soát lịch hệ thống đã chọn; bấm vào một suất để xem lý do và điều chỉnh.</p>
            </div>
            {diagnosticCandidate && (
              <button type="button" onClick={clearDiagnostic} className="flex items-center gap-2 rounded-xl border border-dashed border-blue-400/60 bg-blue-500/10 px-3 py-2 text-sm font-bold text-blue-200"><X className="h-4 w-4" />Bỏ đánh dấu: {diagnosticCandidate.movieTitle}</button>
            )}
          </div>
          <div>
            <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Ngày vận hành đang rà soát</p>
            <div className="mt-2 flex flex-wrap gap-2" role="group" aria-label="Chọn ngày vận hành">
              {serviceDates.map(date => <button key={date} type="button" aria-pressed={activeServiceDate === date} onClick={() => selectActiveDate(date)} className={`rounded-xl border px-3 py-2 text-sm font-bold ${activeServiceDate === date ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 text-zinc-400'}`}>{formatServiceDateKey(date)}</button>)}
            </div>
          </div>
          {diagnosticConflict && (
            <p className="rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-200" role="status">
              Suất đang kiểm tra bị trùng khoảng sử dụng phòng với một suất đã chọn trong toàn bộ lịch.
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
          ) : <div className="py-12 text-center text-zinc-500">Không có ngày vận hành hợp lệ để hiển thị sơ đồ.</div>}
          <p className="text-xs text-zinc-500" data-testid="timeline-boundary-evidence">Đang hiển thị {timelineCandidates.filter(candidate => !candidate.diagnostic).length} suất hệ thống đã chọn{diagnosticCandidate ? ' và 1 suất đang kiểm tra' : ''}. Hệ thống vẫn kiểm tra xung đột trên toàn bộ {items.length} phương án.</p>
        </section>

        <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-4 md:p-5" aria-labelledby="movie-distribution-title">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 id="movie-distribution-title" className="text-base font-black uppercase text-white">Phân bổ suất đề xuất</h2>
              <p className="mt-1 text-sm text-zinc-400">
                Đây là phần hệ thống đề xuất thêm vào các khung còn trống; lịch hiện có không bị di chuyển hoặc thay thế.
              </p>
            </div>
            <span className={`rounded-full border px-3 py-1 text-xs font-bold ${
              movieDistributionSummary.hasCoverageGap || movieDistributionSummary.hasBlockedMovies || movieDistributionSummary.isHighlyConcentrated
                ? 'border-amber-500/30 bg-amber-500/10 text-amber-200'
                : 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200'
            }`}>
              {movieDistributionSummary.representedMovieCount}/{movieDistribution.length} phim có suất đề xuất
            </span>
          </div>

          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4" role="group" aria-label="Xem kết quả theo ngày">
            <button
              type="button"
              aria-pressed={!distributionDate}
              onClick={() => setDistributionDate('')}
              className={`rounded-xl border p-3 text-left ${!distributionDate ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-950/50'}`}
            >
              <span className="block text-xs font-black text-white">Toàn bộ khoảng ngày</span>
              <span className="mt-1 block text-xs text-zinc-500">{existingSchedule.totalExisting} suất hiện có · {candidateMetrics.selectedRecommendations} suất đề xuất thêm</span>
            </button>
            {dailyOperationalSummaries.map(day => {
              const existingCount = existingSchedule.countsByDate[day.serviceDate] || 0;
              return (
              <button
                key={day.serviceDate}
                type="button"
                aria-pressed={distributionDate === day.serviceDate}
                onClick={() => setDistributionDate(day.serviceDate)}
                className={`rounded-xl border p-3 text-left ${distributionDate === day.serviceDate ? 'border-brand-orange bg-brand-orange/10' : day.state === 'NO_VALID_OPTIONS' ? 'border-amber-500/30 bg-amber-500/5' : 'border-zinc-800 bg-zinc-950/50'}`}
              >
                <span className="block text-xs font-black text-white">{formatServiceDateKey(day.serviceDate)}</span>
                <span className={`mt-1 block text-xs ${day.state === 'NO_VALID_OPTIONS' ? 'text-amber-300' : 'text-zinc-500'}`}>{day.label}</span>
                <span className="mt-1 block text-[11px] text-zinc-500">{existingCount} suất hiện có · {day.scheduledCount} suất đề xuất thêm</span>
                <span className="mt-1 block text-[11px] text-zinc-600">{day.validCount}/{day.generatedCount} phương án hợp lệ</span>
              </button>
              );
            })}
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-blue-500/20 bg-blue-500/5 p-3 text-sm text-blue-100">
            <p>
              <strong>Phạm vi đang xem:</strong> {distributionDate ? formatServiceDateKey(distributionDate) : 'Toàn bộ khoảng ngày'} · {' '}
              {distributionDate ? existingSchedule.countsByDate[distributionDate] || 0 : existingSchedule.totalExisting} suất hiện có + {' '}
              {distributionDate ? dailyOperationalSummaries.find(day => day.serviceDate === distributionDate)?.scheduledCount || 0 : candidateMetrics.selectedRecommendations} suất đề xuất thêm.
            </p>
            <Link
              to={`/admin/showtimes?${new URLSearchParams({
                ...(preview.cinemaSlug ? { cinemaSlug: preview.cinemaSlug } : {}),
                date: distributionDate || preview.scheduleFrom,
              }).toString()}`}
              className="inline-flex items-center gap-1 font-bold text-blue-300 hover:text-blue-200"
            >
              <ExternalLink className="h-4 w-4" aria-hidden="true" /> Xem lịch chiếu hiện có
            </Link>
          </div>
          {existingSchedule.isLoading && <p className="text-xs text-zinc-500" role="status">Đang đối chiếu lịch chiếu hiện có…</p>}
          {existingSchedule.error && (
            <p className="text-xs text-amber-300">Chưa tải được số suất hiện có. Các lý do loại trong bản đề xuất vẫn được giữ nguyên để kiểm tra.</p>
          )}

          {(movieDistributionSummary.hasCoverageGap || movieDistributionSummary.isHighlyConcentrated) && (
            <div className="flex gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-sm text-amber-100" role="alert">
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-300" aria-hidden="true" />
              <div>
                <p className="font-bold">Lịch đang phân bổ chưa cân bằng.</p>
                {movieDistributionSummary.hasCoverageGap && (
                  <p className="mt-1">
                    {movieDistributionSummary.uncoveredMovies.map(movie => movie.movieTitle).join(', ')} có phương án hợp lệ nhưng chưa có suất nào được chọn.
                  </p>
                )}
                {movieDistributionSummary.isHighlyConcentrated && (
                  <p className="mt-1">
                    {movieDistributionSummary.dominantMovieTitle} đang chiếm {movieDistributionSummary.dominantSharePercent}% lịch được chọn.
                  </p>
                )}
              </div>
            </div>
          )}

          {movieDistributionSummary.hasBlockedMovies && (
            <div className="flex gap-3 rounded-xl border border-blue-500/30 bg-blue-500/10 p-3 text-sm text-blue-100">
              <Info className="mt-0.5 h-5 w-5 shrink-0 text-blue-300" aria-hidden="true" />
              <div>
                <p className="font-bold">Có phim 0 suất vì không còn phương án hợp lệ, không phải do bị thuật toán bỏ quên.</p>
                <p className="mt-1">
                  {movieDistributionSummary.blockedMovies.map(movie => `${movie.movieTitle}: ${movie.operationalState.label}`).join(' · ')}
                </p>
              </div>
            </div>
          )}

          <div className="grid gap-3 lg:grid-cols-2">
            {movieDistribution.map(movie => (
              <article
                key={movie.movieKey}
                className={`rounded-xl border p-3 text-left transition-colors ${
                  filterMovie === movie.movieKey
                    ? 'border-brand-orange bg-brand-orange/10'
                    : movie.hasCoverageGap || movie.validCount === 0
                      ? 'border-amber-500/30 bg-amber-500/5'
                      : 'border-zinc-800 bg-zinc-950/50 hover:border-zinc-700'
                }`}
              >
                <button
                  type="button"
                  onClick={() => {
                    setFilterMovie(current => current === movie.movieKey ? '' : movie.movieKey);
                    setFilterDate(distributionDate);
                    setCandidatePage(1);
                  }}
                  aria-pressed={filterMovie === movie.movieKey}
                  className="block w-full text-left"
                >
                <div className="flex items-center justify-between gap-3">
                  <span className="min-w-0 truncate text-sm font-bold text-white">{movie.movieTitle}</span>
                  <span className={`shrink-0 text-sm font-black ${movie.hasCoverageGap || movie.validCount === 0 ? 'text-amber-300' : 'text-white'}`}>
                    {movie.scheduledCount} suất · {movie.sharePercent}%
                  </span>
                </div>
                <div className="mt-2 h-2 overflow-hidden rounded-full bg-zinc-800">
                  <span
                    className="block h-full rounded-full"
                    style={{
                      width: `${Math.max(movie.sharePercent, movie.scheduledCount > 0 ? 2 : 0)}%`,
                      backgroundColor: movie.palette?.solid || '#f97316',
                    }}
                  />
                </div>
                <p className="mt-2 text-xs text-zinc-500">
                  {movie.validCount} phương án hợp lệ / {movie.generatedCount} phương án đã xét
                </p>
                </button>
                <div className={`mt-3 rounded-lg border p-2.5 text-xs ${movie.operationalState.tone === 'success' ? 'border-emerald-500/20 bg-emerald-500/5 text-emerald-200' : 'border-amber-500/20 bg-amber-500/5 text-amber-100'}`}>
                  <p className="font-black">{movie.operationalState.label}</p>
                  <p className="mt-1 leading-5 opacity-80">{movie.operationalState.explanation}</p>
                  {movie.scheduledCount === 0 && (
                    <button
                      type="button"
                      onClick={() => {
                        setFilterMovie(movie.movieKey);
                        setFilterDate(distributionDate);
                        selectCandidateView(movie.validCount > 0 ? CANDIDATE_VIEWS.UNSELECTED_VALID : CANDIDATE_VIEWS.ISSUES);
                      }}
                      className="mt-2 font-black text-brand-orange hover:text-orange-300"
                    >
                      {movie.operationalState.actionLabel || 'Xem chi tiết nguyên nhân'} →
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        </section>

        {capabilities.effectiveStatus === 'APPLIED' && (
          <section className="grid gap-4 rounded-2xl border border-violet-500/30 bg-violet-500/10 p-4 text-violet-100 sm:grid-cols-2" aria-label="Kết quả tạo suất chiếu">
            <div>
              <p className="text-[10px] font-black uppercase tracking-wider text-violet-300">Đã tạo suất chiếu lúc</p>
              <p className="mt-1 font-bold">{formatCinemaDateTime(preview.appliedAt, effectiveTimezone)}</p>
            </div>
            <div>
              <p className="text-[10px] font-black uppercase tracking-wider text-violet-300">Kết quả vận hành</p>
              <p className="mt-1 font-bold">{candidateMetrics.createdShowtimes} suất chiếu đã tạo</p>
            </div>
          </section>
        )}

        <section className="space-y-4">
          <details className="rounded-xl border border-zinc-800 bg-zinc-900/50 p-3 text-xs leading-relaxed text-zinc-400">
            <summary className="cursor-pointer font-bold text-zinc-300">Thông tin nâng cao về cách hệ thống xếp lịch</summary>
            <p className="mt-2">
              Điểm ưu tiên tổng hợp khung giờ, mức phù hợp phòng và độ liền mạch; điểm cao hơn tốt hơn. Thứ tự rà soát không phải thứ tự quyết định lựa chọn.
              {preview.strategyVersion === 'BALANCED_V1_S5'
                ? ' Chiến lược S5 cân bằng số suất giữa các phim theo từng ngày vận hành bằng cách dựng lại chuỗi giờ chiếu và thực hiện các thay thế không xung đột. Phương án mới phải giữ tối thiểu 90% chất lượng trung bình và 90% thời gian sử dụng phòng, vì vậy một số chênh lệch nhỏ vẫn có thể được giữ lại.'
                : preview.strategyVersion === 'BALANCED_V1_S4'
                  ? ' Chiến lược S4 chỉ bảo đảm độ phủ tối thiểu theo từng ngày, chưa giới hạn tỷ lệ của một phim; vì vậy lịch S4 vẫn có thể bị dồn suất.'
                  : ' Đây là bản lịch sử dùng chiến lược cũ: hệ thống tối đa hóa tổng điểm của tập không trùng nhau nên có thể dồn nhiều suất vào cùng một phim.'}
            </p>
          </details>
          <div className="flex flex-wrap gap-2" role="tablist" aria-label="Nhóm suất đề xuất">
            {availableCandidateViews.map(view => <button key={view} type="button" role="tab" aria-selected={candidateView === view} onClick={() => selectCandidateView(view)} className={`rounded-xl border px-4 py-2 text-sm font-bold ${candidateView === view ? 'border-brand-orange bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 text-zinc-400'}`}>{CANDIDATE_VIEW_LABELS[view]} ({viewCounts[view]})</button>)}
          </div>
          <div className="flex flex-wrap gap-2 rounded-xl border border-zinc-800 bg-zinc-900/50 p-3">
            <select aria-label="Lọc phim" value={filterMovie} onChange={resetPageWith(setFilterMovie)} className="rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs"><option value="">Tất cả phim</option>{uniqueMovies.map(movie => <option key={movie.key} value={movie.key}>{movie.title}</option>)}</select>
            <select aria-label="Lọc phòng chiếu" value={filterAuditorium} onChange={resetPageWith(setFilterAuditorium)} className="rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs"><option value="">Tất cả phòng</option>{uniqueAuditoriums.map(value => <option key={value} value={value}>{value}</option>)}</select>
            <select aria-label="Lọc ngày vận hành" value={filterDate} onChange={resetPageWith(setFilterDate)} className="rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs"><option value="">Tất cả ngày</option>{serviceDates.map(value => <option key={value} value={value}>{formatServiceDateKey(value)}</option>)}</select>
            <select aria-label="Lọc lý do" value={filterReason} onChange={resetPageWith(setFilterReason)} className="rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs"><option value="">Tất cả lý do</option>{uniqueReasons.map(value => <option key={value} value={value}>{value}</option>)}</select>
          </div>
        </section>

        {pagination.totalItems === 0 ? (
          <section className="rounded-2xl border border-dashed border-zinc-800 py-14 text-center text-zinc-500">{getEmptyStateMessage(candidateView)}</section>
        ) : (
          <section className="grid gap-3 xl:grid-cols-2" aria-label="Hàng đợi rà soát suất đề xuất">
            {pagination.items.map(candidate => {
              const selectionBlock = !candidate.selected ? findSelectionBlock(candidate.raw, selectedItemsIndex) : null;
              const selectionAvailable = capabilities.isEditable && candidate.validationStatus === 'VALID' && candidate.applyStatus === 'PENDING';
              const selectionDisabled = !capabilities.canSelect || Boolean(selectionBlock);
              const showDiagnosticAction = (candidateView === CANDIDATE_VIEWS.ISSUES || candidateView === CANDIDATE_VIEWS.ALL) && candidate.timelineEligible;
              const validationPresentation = getCandidateValidationPresentation(candidate.validationStatus);
              const scoreBreakdownRows = getScoreBreakdownRows(candidate.technicalDetails.scoreBreakdown);
              return (
                <article key={candidate.id} data-testid="candidate-row" className={`rounded-2xl border p-4 ${candidate.validationStatus !== 'VALID' ? 'border-red-500/30 bg-red-500/5' : candidate.selected ? 'border-blue-500/30 bg-blue-500/5' : 'border-zinc-800 bg-zinc-900/40'}`}>
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2 text-xs font-bold text-zinc-400">
                        <span className="text-white">{formatServiceDateKey(candidate.serviceDate)}</span>
                        <span>{candidate.startTimeDisplay}–{candidate.endTimeDisplay}</span>
                        <span>·</span>
                        <span>{candidate.auditoriumName}</span>
                      </div>
                      <h3 className="mt-2 truncate text-base font-black text-white">{candidate.movieTitle}</h3>
                      <p className="mt-1 text-xs text-zinc-500">{candidate.versionName}</p>
                    </div>
                    <div className="shrink-0 text-right">
                      <span className="block text-[10px] font-bold uppercase text-zinc-600">Ưu tiên</span>
                      <span className="text-xl font-black text-white">{candidate.score ?? '—'}</span>
                    </div>
                  </div>
                  <div className="mt-4 flex flex-wrap items-center gap-2">
                    <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase ${getStatusTone(candidate)}`}>{validationPresentation.label} · {candidate.applyState.label}</span>
                    {candidate.conciseReason && <span className="text-xs text-zinc-400">{candidate.conciseReason}</span>}
                  </div>
                  <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-zinc-800 pt-3">
                    {selectionAvailable ? <label className="mr-auto flex items-center gap-2 text-xs font-bold text-zinc-300"><input type="checkbox" checked={candidate.selected} disabled={selectionDisabled} onChange={() => handleToggleSelection(candidate.id, candidate.selected)} aria-label={`Chọn ${candidate.movieTitle} lúc ${candidate.startTimeDisplay}`} />{candidate.selected ? 'Đã chọn' : 'Chưa chọn'}</label> : <span className="mr-auto text-xs text-zinc-500">{candidate.selected ? 'Đã chọn' : 'Không thể chọn'}</span>}
                    {showDiagnosticAction && <button type="button" onClick={() => showDiagnosticOnTimeline(candidate)} className="flex items-center gap-1 rounded-lg border border-blue-500/30 px-2 py-1.5 text-xs font-bold text-blue-300"><Eye className="h-3.5 w-3.5" />Xem trên sơ đồ</button>}
                    {candidate.createdShowtimePath && <Link to={candidate.createdShowtimePath} className="inline-flex items-center gap-1 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-2 py-1.5 text-xs font-bold text-emerald-300" aria-label={`Mở suất chiếu ${candidate.createdShowtimePublicId}`}><ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />Suất chiếu</Link>}
                    <button type="button" onClick={event => openDrawer(candidate, event.currentTarget)} className="rounded-lg border border-zinc-700 px-2 py-1.5 text-xs font-bold text-zinc-300 hover:bg-zinc-800">Kiểm tra chi tiết</button>
                  </div>
                  <details className="mt-3 text-xs text-zinc-400"><summary className="cursor-pointer font-bold">Thông tin nâng cao</summary><dl className="mt-2 space-y-1 break-all"><div>Thứ tự rà soát: {candidate.rank ?? '—'}</div><div className="font-mono">ID: {candidate.id}</div><div className="font-mono">validationStatus: {candidate.validationStatus}</div><div className="font-mono">applyStatus: {candidate.applyStatus}</div><div className="font-mono">startTime: {candidate.technicalDetails.startTime || '—'}</div><div className="font-mono">endTime: {candidate.technicalDetails.endTime || '—'}</div><div className="font-mono">occupancyEndTime: {candidate.technicalDetails.occupancyEndTime || '—'}</div><div className="font-mono">codes: {[candidate.technicalDetails.rejectionCode, candidate.technicalDetails.applyErrorCode].filter(Boolean).join(' / ') || '—'}</div>{scoreBreakdownRows.length > 0 && <div className="mt-2 rounded border border-zinc-800 p-2"><dt className="font-bold text-zinc-300">Thành phần điểm</dt>{scoreBreakdownRows.map(row => <dd key={row.key} className="mt-1 flex justify-between gap-3"><span>{row.label}</span><span className="font-mono">{row.value}</span></dd>)}</div>}<div className="font-mono">scoreBreakdown(raw): {candidate.technicalDetails.scoreBreakdown ? JSON.stringify(candidate.technicalDetails.scoreBreakdown) : '—'}</div></dl></details>
                </article>
              );
            })}
          </section>
        )}

        {pagination.totalItems > 0 && (
          <nav className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-zinc-800 bg-zinc-900/50 p-3" aria-label="Phân trang suất đề xuất">
            <label className="flex items-center gap-2 text-sm text-zinc-400">Số dòng<select aria-label="Số suất mỗi trang" value={pagination.pageSize} onChange={event => { setCandidatePageSize(Number(event.target.value)); setCandidatePage(1); }} className="rounded-lg border border-zinc-700 bg-zinc-950 px-2 py-1 text-zinc-200">{CANDIDATE_PAGE_SIZES.map(size => <option key={size} value={size}>{size}</option>)}</select></label>
            <span className="text-sm text-zinc-400">Trang {pagination.page}/{pagination.totalPages} · {pagination.totalItems} suất</span>
            <div className="flex gap-2"><button type="button" onClick={() => setCandidatePage(pagination.page - 1)} disabled={pagination.page <= 1} className="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40">Trước</button><button type="button" onClick={() => setCandidatePage(pagination.page + 1)} disabled={pagination.page >= pagination.totalPages} className="rounded-lg border border-zinc-700 px-3 py-1.5 text-sm disabled:opacity-40">Sau</button></div>
          </nav>
        )}

        {capabilities.isEditable && (
          <section className="sticky bottom-4 z-30 flex flex-col gap-3 rounded-2xl border border-brand-orange/30 bg-zinc-950/95 p-4 shadow-2xl shadow-black/50 backdrop-blur md:flex-row md:items-center md:justify-between" aria-label="Tiến độ duyệt lịch">
            <div>
              <p className="text-sm font-black text-white">Đã chọn {selectedItemIds.size} suất · Lịch {candidateMetrics.issueCandidates > 0 ? `còn ${candidateMetrics.issueCandidates} lỗi` : 'không bị trùng'}</p>
              <p className={`mt-1 text-xs ${pricingNeedsAttention ? 'font-bold text-red-300' : pricingReady ? 'font-bold text-emerald-300' : 'text-zinc-500'}`}>{pricingNeedsAttention ? 'Bảng giá chưa sẵn sàng — cần xử lý trước khi tạo suất.' : pricingReady ? 'Bảng giá đã đủ — có thể tạo suất chiếu.' : 'Đang kiểm tra bảng giá cho các suất đã chọn.'}</p>
            </div>
            {pricingNeedsAttention ? (
              <Link to={pricingRepairPath} className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-red-200 px-5 text-sm font-black text-red-950 hover:bg-white"><ExternalLink className="h-4 w-4" />{pricingActionLabel}</Link>
            ) : (
              <button type="button" onClick={() => setShowApplyModal(true)} disabled={!capabilities.canApply || !pricingReady || isCheckingPricing} className="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-zinc-950 disabled:cursor-not-allowed disabled:opacity-40">
                {isCheckingPricing ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                {isCheckingPricing ? 'Đang kiểm tra giá…' : `Tạo ${selectedItemIds.size} suất chiếu`}
              </button>
            )}
          </section>
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

      {scheduleActionDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4">
          <div role="dialog" aria-modal="true" aria-labelledby="schedule-action-title" className="w-full max-w-lg rounded-2xl border border-zinc-800 bg-zinc-900 p-6 shadow-2xl">
            <div className="flex items-start gap-3">
              {scheduleActionDialog.phase === 'checking'
                ? <Loader2 className="mt-0.5 h-6 w-6 shrink-0 animate-spin text-blue-300" aria-hidden="true" />
                : <RotateCcw className={`mt-0.5 h-6 w-6 shrink-0 ${scheduleActionDialog.phase === 'blocked' || scheduleActionDialog.phase === 'error' ? 'text-red-300' : 'text-amber-300'}`} aria-hidden="true" />}
              <div>
                <h2 id="schedule-action-title" className="text-lg font-black">
                  {scheduleActionDialog.phase === 'error'
                    ? 'Không thể chuẩn bị tạo lại lịch'
                    : scheduleActionDialog.type === 'DISCARD'
                      ? 'Bỏ bản đề xuất và tạo lại?'
                      : scheduleActionDialog.phase === 'checking'
                        ? 'Đang kiểm tra lịch cũ…'
                        : scheduleActionDialog.phase === 'blocked'
                          ? 'Không thể thay lịch an toàn'
                          : 'Hủy suất nháp cũ và tạo lại?'}
                </h2>
                <p className="mt-2 text-sm leading-6 text-zinc-400">
                  {scheduleActionDialog.type === 'DISCARD' && scheduleActionDialog.phase === 'confirm'
                    && 'Bản này chưa tạo suất chiếu thật nên không chiếm phòng và không gây chồng lịch. Hệ thống chỉ đánh dấu bỏ bản đề xuất rồi điền lại cấu hình cũ.'}
                  {scheduleActionDialog.type === 'REPLACE' && scheduleActionDialog.phase === 'checking'
                    && 'Hệ thống đang kiểm tra toàn bộ suất trong bản lịch. Chưa có suất nào bị thay đổi.'}
                  {scheduleActionDialog.type === 'REPLACE' && scheduleActionDialog.phase === 'confirm'
                    && `Cả ${scheduleActionDialog.summary?.totalCount || 0} suất vẫn đang soạn và chưa từng mở bán. Hệ thống sẽ hủy tất cả cùng lúc rồi điền lại cấu hình cũ.`}
                  {scheduleActionDialog.type === 'REPLACE' && scheduleActionDialog.phase === 'blocked'
                    && 'Có ít nhất một suất không còn ở trạng thái đang soạn hoặc không thuộc lịch tự động. Để tránh ảnh hưởng vé và vận hành, hệ thống không hủy bất kỳ suất nào.'}
                  {scheduleActionDialog.phase === 'error' && scheduleActionDialog.message}
                </p>
              </div>
            </div>

            {scheduleActionDialog.type === 'REPLACE' && scheduleActionDialog.summary && (
              <dl className="mt-5 grid grid-cols-[1fr_auto] gap-x-4 gap-y-2 rounded-xl border border-zinc-800 bg-zinc-950/70 p-4 text-sm">
                <dt className="text-zinc-500">Tổng suất trong lịch</dt>
                <dd className="text-right font-bold text-zinc-100">{scheduleActionDialog.summary.totalCount || 0}</dd>
                <dt className="text-zinc-500">Suất vẫn đang soạn</dt>
                <dd className="text-right font-bold text-emerald-300">{scheduleActionDialog.summary.eligibleCount || 0}</dd>
                <dt className="text-zinc-500">Đang chặn thay lịch</dt>
                <dd className="text-right font-bold text-red-300">{scheduleActionDialog.summary.skippedCount || 0}</dd>
              </dl>
            )}

            {scheduleActionDialog.phase === 'blocked' && scheduleActionDialog.summary?.reasonGroups?.length > 0 && (
              <div className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-100">
                {scheduleActionDialog.summary.reasonGroups.map(group => (
                  <p key={group.reasonCode} className="mt-1 first:mt-0">
                    <strong>{group.count} suất:</strong>{' '}
                    {group.reasonCode === 'SHOWTIME_BATCH_REPLACEMENT_REQUIRES_AUTO_DRAFT'
                      ? 'đã mở bán, đã đóng/hủy, hoặc không thuộc bản lịch tự động này.'
                      : (group.reason || 'không đủ điều kiện thay lịch.')}
                  </p>
                ))}
              </div>
            )}

            {scheduleActionDialog.phase === 'confirm' && (
              <div className="mt-4 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs leading-5 text-amber-100">
                Lưu ý: sau khi xác nhận, lịch cũ sẽ được đánh dấu đã hủy ngay. Nếu bạn rời trang tạo mới, lịch cũ vẫn giữ trạng thái đã hủy.
              </div>
            )}

            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <button
                type="button"
                disabled={isScheduleActionLoading}
                onClick={() => setScheduleActionDialog(null)}
                className="rounded-xl px-4 py-2 text-sm font-bold text-zinc-400 hover:bg-zinc-800 disabled:opacity-50"
              >
                {scheduleActionDialog.phase === 'confirm'
                  ? (scheduleActionDialog.type === 'DISCARD' ? 'Giữ bản hiện tại' : 'Giữ lịch cũ')
                  : 'Đóng'}
              </button>
              {scheduleActionDialog.phase === 'error' && scheduleActionDialog.type === 'REPLACE' && (
                <button type="button" onClick={prepareReplacement} className="rounded-xl border border-zinc-700 px-4 py-2 text-sm font-bold text-zinc-200 hover:bg-zinc-800">
                  Thử kiểm tra lại
                </button>
              )}
              {scheduleActionDialog.phase === 'blocked' && (
                <Link to={`/admin/showtimes?source=AUTO&batchId=${encodeURIComponent(preview.previewPublicId || id)}`} className="rounded-xl border border-violet-500/30 bg-violet-500/10 px-4 py-2 text-sm font-bold text-violet-200 hover:bg-violet-500/20">
                  Xem suất đang chặn
                </Link>
              )}
              {scheduleActionDialog.phase === 'confirm' && (
                <button
                  type="button"
                  disabled={isScheduleActionLoading}
                  onClick={confirmScheduleAction}
                  className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2 text-sm font-black text-zinc-950 disabled:opacity-50"
                >
                  {isScheduleActionLoading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
                  {scheduleActionDialog.type === 'DISCARD' ? 'Bỏ bản cũ và tạo lại' : 'Hủy toàn bộ suất cũ và tạo lại'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {showApplyModal && capabilities.isEditable && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/70 p-4">
          <div role="dialog" aria-modal="true" aria-labelledby="apply-preview-title" className="w-full max-w-lg rounded-2xl border border-zinc-800 bg-zinc-900 p-6">
            <div className="flex items-center gap-3">
              <AlertCircle className="h-6 w-6 text-brand-orange" />
              <h2 id="apply-preview-title" className="text-lg font-black">Tạo {selectedItemIds.size} suất chiếu từ lịch này?</h2>
            </div>
            <dl className="mt-5 grid grid-cols-[1fr_auto] gap-x-4 gap-y-2 text-sm">
              <dt className="text-zinc-500">Rạp</dt><dd className="text-right font-bold text-white">{preview.cinemaName}</dd>
              <dt className="text-zinc-500">Khoảng ngày</dt><dd className="text-right font-bold text-white">{formatPreviewDateRange(preview.scheduleFrom, preview.scheduleTo)}</dd>
              <dt className="text-zinc-500">Phòng đã chọn</dt><dd className="text-right font-bold text-white">{selectedRoomCount}</dd>
              <dt className="text-zinc-500">Suất sẽ được tạo</dt><dd className="text-right font-bold text-emerald-300">{selectedItemIds.size}</dd>
              <dt className="text-zinc-500">Trạng thái ban đầu</dt><dd className="text-right font-bold text-white">{getShowtimeStatusPresentation('DRAFT').label}</dd>
              <dt className="text-zinc-500">Suất lỗi trong toàn bộ đề xuất</dt><dd className="text-right font-bold text-amber-300">{candidateMetrics.issueCandidates}</dd>
              <dt className="text-zinc-500">Suất lỗi trong phần đã chọn</dt><dd className="text-right font-bold text-amber-300">{selectedIssueCount}</dd>
            </dl>
            {selectedMovieCount === 1 && selectedItemIds.size > 0 && (
              <div className="mt-4 flex gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs leading-relaxed text-amber-100" role="alert">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-300" aria-hidden="true" />
                <p><strong>Lịch chỉ có một phim.</strong> Hệ thống sẽ lấp các khung trống bằng phim này nên kết quả có thể dồn suất. Quay lại bước chọn phim nếu bạn muốn thuật toán cân bằng giữa nhiều phim.</p>
              </div>
            )}
            <div className="mt-5 space-y-2 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs leading-relaxed text-amber-200">
              <p><strong>Giá vé đã được kiểm tra.</strong> Hệ thống sẽ kiểm tra lần cuối khi bạn xác nhận để tránh dữ liệu vừa thay đổi.</p>
              <p>Nếu có suất thiếu giá hoặc bị trùng lịch, không suất nào được tạo để tránh lịch dở dang.</p>
              <p>Các suất mới chỉ ở trạng thái “Đang soạn”; khách chưa thể đặt vé cho đến khi bạn mở bán.</p>
              <details>
                <summary className="cursor-pointer font-bold">Thông tin nâng cao</summary>
                <p className="mt-2">Nếu lỗi kết nối xảy ra, lần thử lại sẽ dùng cùng khóa an toàn để không tạo trùng suất.</p>
              </details>
            </div>
            {pricingPreflight && !pricingPreflight.complete && (
              <div className="mt-4 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-xs text-red-100">
                <p className="font-black">Chưa thể tạo suất chiếu: {pricingPreflight.completeCandidateCount}/{pricingPreflight.totalCandidateCount} suất đã đủ giá.</p>
                {pricingPreflight.reasonGroups?.map(group => (
                  <p key={group.reasonCode} className="mt-1">
                    {group.count} suất · {group.displayMessage || 'Chưa xác định — xem thông tin kỹ thuật'}
                  </p>
                ))}
                <Link to={pricingRepairPath} onClick={() => setShowApplyModal(false)} className="mt-3 inline-flex min-h-9 items-center gap-2 rounded-lg bg-red-200 px-3 font-black text-red-950 hover:bg-white"><ExternalLink className="h-3.5 w-3.5" />{pricingActionLabel}</Link>
              </div>
            )}
            <div className="mt-6 flex justify-end gap-3">
              <button type="button" disabled={isApplying} onClick={() => setShowApplyModal(false)} className="rounded-xl px-4 py-2 text-zinc-400 disabled:opacity-50">Hủy</button>
              <button
                type="button"
                disabled={!capabilities.canApply || isApplying || !pricingReady}
                onClick={() => handleApply()}
                className="flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2 font-black text-zinc-950 disabled:opacity-50"
              >
                {isApplying ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
                {isApplying ? 'Đang kiểm tra và tạo suất…' : `Tạo ${selectedItemIds.size} suất chiếu`}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminAutoSchedulePreviewPage;
