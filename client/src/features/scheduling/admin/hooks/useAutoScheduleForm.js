/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminAutoScheduleService from '@/features/scheduling/admin/services/adminAutoScheduleService';
import { getAutoScheduleError } from '@/features/scheduling/admin/utils/autoScheduleErrors';
import { buildAutoScheduleRequestFingerprint } from '@/features/scheduling/admin/utils/autoScheduleForm';
import { isSchedulableMovieStatus } from '@/features/scheduling/admin/utils/movieSchedulingEligibility';

const ALLOWED_PLANNING_DAYS = [1, 3, 7];
const createUuid = () => globalThis.crypto?.randomUUID?.()
  || `preview-${Date.now()}-${Math.random().toString(16).slice(2)}`;

const compact = values => (values?.length ? values : undefined);

const inferPlanningDays = draft => {
  if (ALLOWED_PLANNING_DAYS.includes(Number(draft?.planningDays))) return Number(draft.planningDays);
  if (!draft?.scheduleFrom || !draft?.scheduleTo) return 1;
  const from = Date.parse(`${draft.scheduleFrom}T00:00:00Z`);
  const to = Date.parse(`${draft.scheduleTo}T00:00:00Z`);
  const days = Math.round((to - from) / 86_400_000) + 1;
  return ALLOWED_PLANNING_DAYS.includes(days) ? days : 7;
};

export default function useAutoScheduleForm({ triggerToast, onSuccess, initialDraft = {} }) {
  const [cinemas, setCinemas] = useState([]);
  const [selectedCinemaId, setSelectedCinemaId] = useState(initialDraft.cinemaPublicId || '');
  const [planningDays, setPlanningDays] = useState(inferPlanningDays(initialDraft));
  const [slotGranularityMinutes, setSlotGranularityMinutes] = useState(
    initialDraft.slotGranularityMinutes || 15,
  );
  const [previewTtlMinutes, setPreviewTtlMinutes] = useState(60);
  const [includeAuditoriumIds, setIncludeAuditoriumIds] = useState(
    initialDraft.auditoriumPublicIds || [],
  );
  const [excludeAuditoriumIds, setExcludeAuditoriumIds] = useState([]);
  const [includeMovieVersionIds, setIncludeMovieVersionIds] = useState(
    initialDraft.movieVersionPublicIds || [],
  );
  const [excludeMovieVersionIds, setExcludeMovieVersionIds] = useState([]);
  const [auditoriums, setAuditoriums] = useState([]);
  const [movies, setMovies] = useState([]);
  const [preflight, setPreflight] = useState(null);
  const [preflightError, setPreflightError] = useState('');
  const [isLoadingCinemas, setIsLoadingCinemas] = useState(false);
  const [isLoadingScope, setIsLoadingScope] = useState(false);
  const [isCheckingPreflight, setIsCheckingPreflight] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState({});
  const idempotencyRef = useRef({ fingerprint: '', key: '' });
  const preflightSequence = useRef(0);

  const selectedCinema = useMemo(
    () => cinemas.find(cinema => cinema.publicId === selectedCinemaId),
    [cinemas, selectedCinemaId],
  );

  const preflightRequest = useMemo(() => ({
    cinemaPublicId: selectedCinemaId,
    planningDays,
    includeMovieVersionPublicIds: compact(includeMovieVersionIds),
    includeAuditoriumPublicIds: compact(includeAuditoriumIds),
    excludeMovieVersionPublicIds: compact(excludeMovieVersionIds),
    excludeAuditoriumPublicIds: compact(excludeAuditoriumIds),
  }), [
    excludeAuditoriumIds,
    excludeMovieVersionIds,
    includeAuditoriumIds,
    includeMovieVersionIds,
    planningDays,
    selectedCinemaId,
  ]);

  useEffect(() => {
    let active = true;
    setIsLoadingCinemas(true);
    adminCinemaService.getCinemas({ size: 100, status: 'ACTIVE' })
      .then(response => {
        if (active && response?.success) setCinemas(response.data?.data || []);
      })
      .catch(() => triggerToast?.('Không thể tải danh sách rạp', 'error'))
      .finally(() => { if (active) setIsLoadingCinemas(false); });
    return () => { active = false; };
  }, [triggerToast]);

  useEffect(() => {
    setAuditoriums([]);
    if (!selectedCinemaId) return undefined;
    let active = true;
    setIsLoadingScope(true);
    adminCinemaService.getAdminCinemaDetail(selectedCinemaId)
      .then(response => {
        if (!active || !response?.success) return;
        setAuditoriums((response.data?.activeAuditoriums || [])
          .filter(auditorium => auditorium.status === 'ACTIVE'));
      })
      .catch(() => triggerToast?.('Không thể tải phòng chiếu cho phần nâng cao', 'error'))
      .finally(() => { if (active) setIsLoadingScope(false); });
    return () => { active = false; };
  }, [selectedCinemaId, triggerToast]);

  const runPreflight = useCallback(async ({ silent = false } = {}) => {
    if (!selectedCinemaId) {
      setPreflight(null);
      return null;
    }
    const sequence = ++preflightSequence.current;
    setIsCheckingPreflight(true);
    setPreflightError('');
    try {
      const response = await adminAutoScheduleService.preflight(preflightRequest);
      if (sequence !== preflightSequence.current) return null;
      if (!response?.success || !response.data) throw new Error('Preflight không trả về dữ liệu hợp lệ.');
      setPreflight(response.data);
      return response.data;
    } catch (error) {
      if (sequence !== preflightSequence.current) return null;
      const normalized = getAutoScheduleError(error);
      setPreflight(null);
      setPreflightError(normalized.message || 'Không thể kiểm tra điều kiện tạo lịch.');
      if (!silent) triggerToast?.(normalized.message, 'error');
      return null;
    } finally {
      if (sequence === preflightSequence.current) setIsCheckingPreflight(false);
    }
  }, [preflightRequest, selectedCinemaId, triggerToast]);

  useEffect(() => {
    setPreflight(null);
    if (!selectedCinemaId) return undefined;
    const timer = setTimeout(() => { void runPreflight({ silent: true }); }, 250);
    return () => clearTimeout(timer);
  }, [runPreflight, selectedCinemaId]);

  useEffect(() => {
    if (!preflight?.planningFrom || !preflight?.planningTo) return undefined;
    let active = true;
    adminAutoScheduleService.getEligibleMovies({
      fromDate: preflight.planningFrom,
      toDate: preflight.planningTo,
    }).then(response => {
      if (!active || !response?.success) return;
      setMovies((response.data || [])
        .filter(movie => isSchedulableMovieStatus(movie.status))
        .map(movie => ({ ...movie, publicId: movie.moviePublicId })));
    }).catch(() => {
      if (active) setMovies([]);
    }).finally(() => { if (active) setIsLoadingScope(false); });
    return () => { active = false; };
  }, [preflight?.planningFrom, preflight?.planningTo]);

  const setPlanningPreset = useCallback(days => {
    if (ALLOWED_PLANNING_DAYS.includes(Number(days))) setPlanningDays(Number(days));
  }, []);

  const setScopeChoice = useCallback((kind, id, checked) => {
    const isInclude = kind === 'include';
    const isAuditorium = id.startsWith('auditorium:');
    const publicId = id.slice(id.indexOf(':') + 1);
    const setter = isAuditorium
      ? (isInclude ? setIncludeAuditoriumIds : setExcludeAuditoriumIds)
      : (isInclude ? setIncludeMovieVersionIds : setExcludeMovieVersionIds);
    const oppositeSetter = isAuditorium
      ? (isInclude ? setExcludeAuditoriumIds : setIncludeAuditoriumIds)
      : (isInclude ? setExcludeMovieVersionIds : setIncludeMovieVersionIds);
    setter(previous => checked
      ? Array.from(new Set([...previous, publicId]))
      : previous.filter(value => value !== publicId));
    if (checked) oppositeSetter(previous => previous.filter(value => value !== publicId));
  }, []);

  const readinessIssues = useMemo(() => {
    const issues = [];
    if (!selectedCinemaId) issues.push('Chọn một rạp để lập lịch.');
    if (preflightError) issues.push(preflightError);
    (preflight?.blockers || []).forEach(blocker => issues.push(blocker.message));
    if (Number(slotGranularityMinutes) < 5 || Number(slotGranularityMinutes) > 60) {
      issues.push('Khoảng cách thử lịch phải từ 5 đến 60 phút.');
    }
    if (Number(previewTtlMinutes) < 5 || Number(previewTtlMinutes) > 120) {
      issues.push('Thời gian giữ preview phải từ 5 đến 120 phút.');
    }
    return [...new Set(issues)];
  }, [preflight?.blockers, preflightError, previewTtlMinutes, selectedCinemaId, slotGranularityMinutes]);

  const isReady = Boolean(preflight?.canGenerate)
    && readinessIssues.length === 0
    && !isCheckingPreflight
    && !isSubmitting;

  const validate = useCallback((canGenerate = preflight?.canGenerate) => {
    const next = {};
    if (!selectedCinemaId) next.cinemaId = 'Vui lòng chọn rạp';
    if (!canGenerate) next.preflight = 'Preflight chưa xác nhận phạm vi này có thể tạo lịch.';
    if (Number(slotGranularityMinutes) < 5 || Number(slotGranularityMinutes) > 60) {
      next.slotGranularityMinutes = 'Giá trị từ 5 đến 60';
    }
    if (Number(previewTtlMinutes) < 5 || Number(previewTtlMinutes) > 120) {
      next.previewTtlMinutes = 'Giá trị từ 5 đến 120';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  }, [preflight?.canGenerate, previewTtlMinutes, selectedCinemaId, slotGranularityMinutes]);

  const handleSubmit = useCallback(async () => {
    const latestPreflight = await runPreflight({ silent: true });
    if (!latestPreflight?.canGenerate || !validate(latestPreflight.canGenerate)) return false;
    const request = {
      cinemaPublicId: selectedCinemaId,
      planningDays,
      ...(includeMovieVersionIds.length ? { movieVersionPublicIds: includeMovieVersionIds } : {}),
      ...(includeAuditoriumIds.length ? { auditoriumPublicIds: includeAuditoriumIds } : {}),
      ...(excludeMovieVersionIds.length ? { excludeMovieVersionPublicIds: excludeMovieVersionIds } : {}),
      ...(excludeAuditoriumIds.length ? { excludeAuditoriumPublicIds: excludeAuditoriumIds } : {}),
      slotGranularityMinutes: Number(slotGranularityMinutes),
      previewTtlMinutes: Number(previewTtlMinutes),
    };
    const fingerprint = buildAutoScheduleRequestFingerprint(request);
    if (idempotencyRef.current.fingerprint !== fingerprint) {
      idempotencyRef.current = { fingerprint, key: createUuid() };
    }
    setIsSubmitting(true);
    try {
      const response = await adminAutoScheduleService.generatePreview({
        ...request,
        idempotencyKey: idempotencyRef.current.key,
      });
      if (!response?.success) return false;
      idempotencyRef.current = { fingerprint: '', key: '' };
      triggerToast?.('Đã tạo lịch tối ưu để kiểm tra', 'success');
      onSuccess?.(response.data?.previewPublicId);
      return true;
    } catch (error) {
      const normalized = getAutoScheduleError(error);
      setErrors(previous => ({ ...previous, ...normalized.fieldErrors }));
      triggerToast?.(normalized.message, 'error');
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, [
    excludeAuditoriumIds, excludeMovieVersionIds, includeAuditoriumIds,
    includeMovieVersionIds, onSuccess, planningDays, previewTtlMinutes,
    runPreflight, selectedCinemaId, slotGranularityMinutes, triggerToast, validate,
  ]);

  return {
    cinemas,
    selectedCinemaId,
    setSelectedCinemaId,
    selectedCinema,
    planningDays,
    setPlanningPreset,
    slotGranularityMinutes,
    setSlotGranularityMinutes,
    previewTtlMinutes,
    setPreviewTtlMinutes,
    auditoriums,
    movies,
    includeAuditoriumIds,
    excludeAuditoriumIds,
    includeMovieVersionIds,
    excludeMovieVersionIds,
    setScopeChoice,
    preflight,
    preflightError,
    isLoadingCinemas,
    isLoadingScope,
    isCheckingPreflight,
    isSubmitting,
    errors,
    readinessIssues,
    isReady,
    runPreflight,
    handleSubmit,
    validate,
  };
}
