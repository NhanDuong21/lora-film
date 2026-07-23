/* eslint-disable react-hooks/set-state-in-effect */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminAutoScheduleService from '@/features/scheduling/admin/services/adminAutoScheduleService';
import { getAutoScheduleError } from '@/features/scheduling/admin/utils/autoScheduleErrors';
import {
  buildAutoScheduleRequestFingerprint,
  validateAutoScheduleDateRange,
} from '@/features/scheduling/admin/utils/autoScheduleForm';

const createUuid = () => globalThis.crypto?.randomUUID?.()
  || `preview-${Date.now()}-${Math.random().toString(16).slice(2)}`;

const mapFieldErrors = (fieldErrors = {}) => ({
  ...fieldErrors,
  cinemaId: fieldErrors.cinemaPublicId || fieldErrors.cinemaId,
  auditoriums: fieldErrors.auditoriumPublicIds || fieldErrors.auditoriums,
  versions: fieldErrors.movieVersionPublicIds || fieldErrors.versions,
});

export default function useAutoScheduleForm({ triggerToast, onSuccess }) {
  const [selectedCinemaId, setSelectedCinemaId] = useState('');
  const [scheduleFrom, setScheduleFrom] = useState('');
  const [scheduleTo, setScheduleTo] = useState('');
  const [slotGranularityMinutes, setSlotGranularityMinutes] = useState(15);
  const [previewTtlMinutes, setPreviewTtlMinutes] = useState(60);

  const [auditoriums, setAuditoriums] = useState([]);
  const [selectedAuditoriumIds, setSelectedAuditoriumIds] = useState([]);
  const [movies, setMovies] = useState([]);
  const [versionsByMovie, setVersionsByMovie] = useState({});
  const [selectedMovieVersionIds, setSelectedMovieVersionIds] = useState([]);
  const [selectionNotice, setSelectionNotice] = useState('');

  const [cinemas, setCinemas] = useState([]);
  const [isLoadingCinemas, setIsLoadingCinemas] = useState(false);
  const [isLoadingAuditoriums, setIsLoadingAuditoriums] = useState(false);
  const [isLoadingMovies, setIsLoadingMovies] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [movieLoadError, setMovieLoadError] = useState('');
  const [movieReloadSequence, setMovieReloadSequence] = useState(0);
  const [errors, setErrors] = useState({});

  const idempotencyRef = useRef({ fingerprint: '', key: '' });
  const movieRequestSequence = useRef(0);
  const auditoriumRequestSequence = useRef(0);

  const selectedCinema = useMemo(
    () => cinemas.find(cinema => cinema.publicId === selectedCinemaId),
    [cinemas, selectedCinemaId],
  );
  const dateRangeInfo = useMemo(() => validateAutoScheduleDateRange({
    scheduleFrom,
    scheduleTo,
    cinemaTimezone: selectedCinema?.timezone,
  }), [scheduleFrom, scheduleTo, selectedCinema?.timezone]);

  const selectedVersions = useMemo(() => {
    const selectedIds = new Set(selectedMovieVersionIds);
    return movies.flatMap(movie => (versionsByMovie[movie.publicId] || [])
      .filter(version => selectedIds.has(version.publicId))
      .map(version => ({
        ...version,
        moviePublicId: movie.publicId,
        movieTitle: movie.title,
      })));
  }, [movies, selectedMovieVersionIds, versionsByMovie]);

  useEffect(() => {
    const fetchInitial = async () => {
      setIsLoadingCinemas(true);
      try {
        const cinemaRes = await adminCinemaService.getCinemas({ size: 100, status: 'ACTIVE' });
        if (cinemaRes?.success) setCinemas(cinemaRes.data?.data || []);
      } catch {
        triggerToast?.('Không thể tải dữ liệu rạp', 'error');
      } finally {
        setIsLoadingCinemas(false);
      }
    };
    fetchInitial();
  }, [triggerToast]);

  useEffect(() => {
    const requestId = ++movieRequestSequence.current;
    const fetchMovies = async () => {
      setIsLoadingMovies(true);
      setMovieLoadError('');
      try {
        const params = {};
        if (scheduleFrom) params.fromDate = scheduleFrom;
        if (scheduleTo) params.toDate = scheduleTo;
        const movieRes = await adminAutoScheduleService.getEligibleMovies(params);
        if (requestId !== movieRequestSequence.current) return;
        if (!movieRes?.success) return;

        const moviesData = (movieRes.data || []).map(movie => ({
          ...movie,
          publicId: movie.moviePublicId,
          versions: movie.versions || [],
        }));
        const nextVersionsByMovie = {};
        const selectableVersionIds = new Set();
        moviesData.forEach(movie => {
          nextVersionsByMovie[movie.publicId] = movie.versions;
          if (movie.eligible) {
            movie.versions
              .filter(version => version.status === 'ACTIVE')
              .forEach(version => selectableVersionIds.add(version.publicId));
          }
        });

        setMovies(moviesData);
        setVersionsByMovie(nextVersionsByMovie);
        setSelectedMovieVersionIds(previous => {
          const next = previous.filter(versionId => selectableVersionIds.has(versionId));
          const removedCount = previous.length - next.length;
          if (removedCount > 0) {
            setSelectionNotice(`Đã bỏ ${removedCount} định dạng không còn đủ điều kiện trong khoảng ngày mới.`);
          }
          return next;
        });
      } catch {
        if (requestId !== movieRequestSequence.current) return;
        setMovieLoadError('Không thể xác minh điều kiện phim cho khoảng ngày đã chọn.');
        triggerToast?.('Không thể tải dữ liệu phim', 'error');
      } finally {
        if (requestId === movieRequestSequence.current) setIsLoadingMovies(false);
      }
    };
    fetchMovies();
  }, [movieReloadSequence, scheduleFrom, scheduleTo, triggerToast]);

  const retryMovies = useCallback(() => setMovieReloadSequence(previous => previous + 1), []);

  useEffect(() => {
    const requestId = ++auditoriumRequestSequence.current;
    setAuditoriums([]);
    setSelectedAuditoriumIds(previous => {
      if (previous.length > 0) {
        setSelectionNotice('Đã xóa lựa chọn phòng chiếu vì cụm rạp đã thay đổi.');
      }
      return [];
    });
    if (!selectedCinemaId) return;

    const fetchAuditoriums = async () => {
      setIsLoadingAuditoriums(true);
      try {
        const response = await adminCinemaService.getAdminCinemaDetail(selectedCinemaId);
        if (requestId === auditoriumRequestSequence.current && response?.success) {
          setAuditoriums(response.data?.activeAuditoriums || []);
        }
      } catch {
        if (requestId !== auditoriumRequestSequence.current) return;
        triggerToast?.('Không thể tải phòng chiếu', 'error');
      } finally {
        if (requestId === auditoriumRequestSequence.current) setIsLoadingAuditoriums(false);
      }
    };
    fetchAuditoriums();
  }, [selectedCinemaId, triggerToast]);

  const toggleMovieExpansion = useCallback(() => {}, []);

  const toggleAuditorium = useCallback(auditoriumId => {
    setSelectedAuditoriumIds(previous => previous.includes(auditoriumId)
      ? previous.filter(id => id !== auditoriumId)
      : [...previous, auditoriumId]);
  }, []);

  const selectAllActiveAuditoriums = useCallback(() => {
    setSelectedAuditoriumIds(auditoriums
      .filter(auditorium => auditorium.status === 'ACTIVE')
      .map(auditorium => auditorium.publicId));
  }, [auditoriums]);

  const clearAuditoriums = useCallback(() => setSelectedAuditoriumIds([]), []);

  const toggleVersion = useCallback(versionId => {
    setSelectedMovieVersionIds(previous => previous.includes(versionId)
      ? previous.filter(id => id !== versionId)
      : [...previous, versionId]);
  }, []);

  const readinessIssues = useMemo(() => {
    const issues = [];
    if (!selectedCinemaId) issues.push('Chọn cụm rạp.');
    Object.values(dateRangeInfo.errors || {}).forEach(message => issues.push(message));
    if (selectedAuditoriumIds.length === 0) issues.push('Chọn ít nhất một phòng chiếu.');
    if (selectedMovieVersionIds.length === 0) issues.push('Chọn ít nhất một định dạng phim.');
    if (movieLoadError) issues.push(movieLoadError);
    if (Number(slotGranularityMinutes) < 5 || Number(slotGranularityMinutes) > 60) {
      issues.push('Khoảng cách thử lịch phải từ 5 đến 60 phút.');
    }
    if (Number(previewTtlMinutes) < 5 || Number(previewTtlMinutes) > 120) {
      issues.push('Thời hạn bản xem trước phải từ 5 đến 120 phút.');
    }
    return [...new Set(issues)];
  }, [
    dateRangeInfo.errors,
    movieLoadError,
    previewTtlMinutes,
    selectedAuditoriumIds.length,
    selectedCinemaId,
    selectedMovieVersionIds.length,
    slotGranularityMinutes,
  ]);

  const isReady = readinessIssues.length === 0
    && !isLoadingAuditoriums
    && !isLoadingMovies
    && !isSubmitting;

  const validate = useCallback(() => {
    const newErrors = { ...dateRangeInfo.errors };
    if (!selectedCinemaId) newErrors.cinemaId = 'Vui lòng chọn cụm rạp';
    if (selectedAuditoriumIds.length === 0) newErrors.auditoriums = 'Vui lòng chọn ít nhất một phòng chiếu';
    if (selectedMovieVersionIds.length === 0) newErrors.versions = 'Vui lòng chọn ít nhất một định dạng phim';
    if (movieLoadError) newErrors.eligibility = movieLoadError;
    if (Number(slotGranularityMinutes) < 5 || Number(slotGranularityMinutes) > 60) {
      newErrors.slotGranularityMinutes = 'Giá trị từ 5 đến 60';
    }
    if (Number(previewTtlMinutes) < 5 || Number(previewTtlMinutes) > 120) {
      newErrors.previewTtlMinutes = 'Giá trị từ 5 đến 120';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [
    dateRangeInfo.errors,
    movieLoadError,
    previewTtlMinutes,
    selectedAuditoriumIds,
    selectedCinemaId,
    selectedMovieVersionIds,
    slotGranularityMinutes,
  ]);

  const handleSubmit = useCallback(async () => {
    if (!validate()) return false;
    const request = {
      cinemaPublicId: selectedCinemaId,
      scheduleFrom,
      scheduleTo,
      movieVersionPublicIds: selectedMovieVersionIds,
      auditoriumPublicIds: selectedAuditoriumIds,
      slotGranularityMinutes: Number.parseInt(slotGranularityMinutes, 10),
      previewTtlMinutes: Number.parseInt(previewTtlMinutes, 10),
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
      if (response?.success) {
        idempotencyRef.current = { fingerprint: '', key: '' };
        triggerToast?.('Tạo bản xem trước thành công', 'success');
        onSuccess?.(response.data?.previewPublicId);
        return true;
      }
      return false;
    } catch (error) {
      const normalized = getAutoScheduleError(error);
      setErrors(previous => ({ ...previous, ...mapFieldErrors(normalized.fieldErrors) }));
      triggerToast?.(normalized.message, 'error');
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, [
    onSuccess,
    previewTtlMinutes,
    scheduleFrom,
    scheduleTo,
    selectedAuditoriumIds,
    selectedCinemaId,
    selectedMovieVersionIds,
    slotGranularityMinutes,
    triggerToast,
    validate,
  ]);

  return {
    cinemas,
    movies,
    auditoriums,
    versionsByMovie,
    selectedCinemaId,
    setSelectedCinemaId,
    selectedCinema,
    scheduleFrom,
    setScheduleFrom,
    scheduleTo,
    setScheduleTo,
    slotGranularityMinutes,
    setSlotGranularityMinutes,
    previewTtlMinutes,
    setPreviewTtlMinutes,
    selectedAuditoriumIds,
    toggleAuditorium,
    selectAllActiveAuditoriums,
    clearAuditoriums,
    selectedMovieVersionIds,
    selectedVersions,
    toggleVersion,
    isLoadingCinemas,
    isLoadingAuditoriums,
    isLoadingMovies,
    isSubmitting,
    errors,
    setErrors,
    readinessIssues,
    isReady,
    selectionNotice,
    movieLoadError,
    retryMovies,
    dateRangeInfo,
    toggleMovieExpansion,
    handleSubmit,
    validate,
  };
}
