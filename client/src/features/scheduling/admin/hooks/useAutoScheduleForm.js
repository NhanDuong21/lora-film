import { useState, useEffect, useCallback, useMemo } from 'react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminAutoScheduleService from '@/features/scheduling/admin/services/adminAutoScheduleService';

export default function useAutoScheduleForm({ triggerToast, onSuccess }) {
  // Step 1: Scope State
  const [selectedCinemaId, setSelectedCinemaId] = useState('');
  const [scheduleFrom, setScheduleFrom] = useState('');
  const [scheduleTo, setScheduleTo] = useState('');
  const [slotGranularityMinutes, setSlotGranularityMinutes] = useState(15);
  const [previewTtlMinutes, setPreviewTtlMinutes] = useState(60);

  // Step 2: Auditoriums State
  const [auditoriums, setAuditoriums] = useState([]);
  const [selectedAuditoriumIds, setSelectedAuditoriumIds] = useState([]);

  // Step 3: Movies & Versions State
  const [movies, setMovies] = useState([]);
  const [versionsByMovie, setVersionsByMovie] = useState({});
  const [selectedMovieVersionIds, setSelectedMovieVersionIds] = useState([]);

  // Data Loading State
  const [cinemas, setCinemas] = useState([]);
  const [isLoadingCinemas, setIsLoadingCinemas] = useState(false);
  const [isLoadingAuditoriums, setIsLoadingAuditoriums] = useState(false);
  const [isLoadingMovies, setIsLoadingMovies] = useState(false);
  const [loadingVersionsFor, setLoadingVersionsFor] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Validation
  const [errors, setErrors] = useState({});

  // 1. Initial Load: Cinemas and Movies
  useEffect(() => {
    const fetchInitial = async () => {
      try {
        setIsLoadingCinemas(true);
        setIsLoadingMovies(true);
        const [cinemaRes, movieRes] = await Promise.all([
          adminCinemaService.getCinemas({ size: 500, status: 'ACTIVE' }),
          adminMovieService.getMovies({ size: 1000 })
        ]);
        
        if (cinemaRes?.success) setCinemas(cinemaRes.data?.data || []);
        if (movieRes?.success) setMovies(movieRes.data?.data || []);
      } catch (err) {
        triggerToast?.('Không thể tải dữ liệu ban đầu', 'error');
      } finally {
        setIsLoadingCinemas(false);
        setIsLoadingMovies(false);
      }
    };
    fetchInitial();
  }, [triggerToast]);

  // 2. On Cinema Change: Load Auditoriums
  useEffect(() => {
    if (!selectedCinemaId) {
      setAuditoriums([]);
      setSelectedAuditoriumIds([]);
      return;
    }
    const fetchAuditoriums = async () => {
      setIsLoadingAuditoriums(true);
      try {
        const res = await adminCinemaService.getAdminCinemaDetail(selectedCinemaId);
        if (res?.success) {
          // Keep all auditoriums, we will disable inactive ones in UI
          const allAuds = res.data?.activeAuditoriums || [];
          setAuditoriums(allAuds);
          // Auto-select active ones by default
          setSelectedAuditoriumIds(allAuds.filter(a => a.status === 'ACTIVE').map(a => a.publicId));
        }
      } catch (err) {
        triggerToast?.('Không thể tải phòng chiếu', 'error');
      } finally {
        setIsLoadingAuditoriums(false);
      }
    };
    fetchAuditoriums();
  }, [selectedCinemaId, triggerToast]);

  // 3. Helper to fetch versions for a movie
  const toggleMovieExpansion = useCallback(async (movieId, isExpanded) => {
    if (!isExpanded || versionsByMovie[movieId]) return;

    setLoadingVersionsFor(prev => ({ ...prev, [movieId]: true }));
    try {
      const res = await adminMovieService.getMovieVersions(movieId);
      if (res?.success) {
        setVersionsByMovie(prev => ({ ...prev, [movieId]: res.data || [] }));
      }
    } catch (err) {
      triggerToast?.('Không thể tải định dạng phim', 'error');
    } finally {
      setLoadingVersionsFor(prev => ({ ...prev, [movieId]: false }));
    }
  }, [versionsByMovie, triggerToast]);

  const toggleAuditorium = useCallback((audId) => {
    setSelectedAuditoriumIds(prev => 
      prev.includes(audId) ? prev.filter(id => id !== audId) : [...prev, audId]
    );
  }, []);

  const toggleVersion = useCallback((versionId) => {
    setSelectedMovieVersionIds(prev => 
      prev.includes(versionId) ? prev.filter(id => id !== versionId) : [...prev, versionId]
    );
  }, []);

  const validate = useCallback(() => {
    const newErrors = {};
    if (!selectedCinemaId) newErrors.cinemaId = 'Vui lòng chọn cụm rạp';
    if (!scheduleFrom) newErrors.scheduleFrom = 'Vui lòng chọn ngày bắt đầu';
    if (!scheduleTo) newErrors.scheduleTo = 'Vui lòng chọn ngày kết thúc';
    if (scheduleFrom && scheduleTo && new Date(scheduleFrom) > new Date(scheduleTo)) {
      newErrors.scheduleTo = 'Ngày kết thúc không hợp lệ';
    }
    if (selectedAuditoriumIds.length === 0) newErrors.auditoriums = 'Vui lòng chọn ít nhất một phòng chiếu';
    if (selectedMovieVersionIds.length === 0) newErrors.versions = 'Vui lòng chọn ít nhất một định dạng phim';
    
    if (slotGranularityMinutes < 5 || slotGranularityMinutes > 60) newErrors.slotGranularityMinutes = 'Giá trị từ 5 đến 60';
    if (previewTtlMinutes < 5 || previewTtlMinutes > 120) newErrors.previewTtlMinutes = 'Giá trị từ 5 đến 120';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [selectedCinemaId, scheduleFrom, scheduleTo, selectedAuditoriumIds, selectedMovieVersionIds, slotGranularityMinutes, previewTtlMinutes]);

  const handleSubmit = useCallback(async () => {
    if (!validate()) return;
    setIsSubmitting(true);
    try {
      const payload = {
        cinemaPublicId: selectedCinemaId,
        scheduleFrom, // YYYY-MM-DD
        scheduleTo,   // YYYY-MM-DD
        movieVersionPublicIds: selectedMovieVersionIds,
        auditoriumPublicIds: selectedAuditoriumIds,
        slotGranularityMinutes: parseInt(slotGranularityMinutes),
        previewTtlMinutes: parseInt(previewTtlMinutes),
        idempotencyKey: crypto.randomUUID()
      };

      const res = await adminAutoScheduleService.generatePreview(payload);
      if (res?.success) {
        triggerToast?.('Tạo bản xem trước thành công', 'success');
        if (onSuccess) onSuccess(res.data?.previewPublicId);
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Lỗi khi tạo bản xem trước xếp lịch';
      triggerToast?.(msg, 'error');
    } finally {
      setIsSubmitting(false);
    }
  }, [
    selectedCinemaId, scheduleFrom, scheduleTo, selectedAuditoriumIds, selectedMovieVersionIds,
    slotGranularityMinutes, previewTtlMinutes, validate, triggerToast, onSuccess
  ]);

  const selectedCinema = useMemo(() => cinemas.find(c => c.publicId === selectedCinemaId), [cinemas, selectedCinemaId]);

  return {
    cinemas, movies, auditoriums, versionsByMovie,
    selectedCinemaId, setSelectedCinemaId, selectedCinema,
    scheduleFrom, setScheduleFrom,
    scheduleTo, setScheduleTo,
    slotGranularityMinutes, setSlotGranularityMinutes,
    previewTtlMinutes, setPreviewTtlMinutes,
    selectedAuditoriumIds, toggleAuditorium,
    selectedMovieVersionIds, toggleVersion,
    isLoadingCinemas, isLoadingAuditoriums, isLoadingMovies, loadingVersionsFor, isSubmitting,
    errors, setErrors,
    toggleMovieExpansion,
    handleSubmit, validate
  };
}
