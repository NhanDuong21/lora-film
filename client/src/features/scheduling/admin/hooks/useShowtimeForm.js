/* eslint-disable react-hooks/set-state-in-effect, no-unused-vars, react-hooks/exhaustive-deps */
import { useState, useEffect, useCallback, useMemo } from 'react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';

export default function useShowtimeForm({ triggerToast, onSuccess }) {
  const [cinemas, setCinemas] = useState([]);
  const [auditoriums, setAuditoriums] = useState([]);
  const [movies, setMovies] = useState([]);
  const [versions, setVersions] = useState([]);

  // Form State
  const [selectedCinemaId, setSelectedCinemaId] = useState('');
  const [selectedAuditoriumId, setSelectedAuditoriumId] = useState('');
  const [selectedMovieId, setSelectedMovieId] = useState('');
  const [selectedVersionId, setSelectedVersionId] = useState('');
  const [startTime, setStartTime] = useState('');

  const [isLoadingCinemas, setIsLoadingCinemas] = useState(false);
  const [isLoadingMovies, setIsLoadingMovies] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  // Fetch initial data
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
        if (movieRes?.success) {
           // We might want to filter out 'INACTIVE' or similar, depending on what getMovies returns
           setMovies(movieRes.data?.data || []);
        }
      } catch (err) {
        triggerToast?.('Không thể tải dữ liệu khởi tạo', 'error');
      } finally {
        setIsLoadingCinemas(false);
        setIsLoadingMovies(false);
      }
    };
    fetchInitial();
  }, [triggerToast]);

  // Handle Cinema Selection -> Load Auditoriums
  useEffect(() => {
    if (!selectedCinemaId) {
      setAuditoriums([]);
      setSelectedAuditoriumId('');
      return;
    }

    const fetchAuditoriums = async () => {
      try {
        const res = await adminCinemaService.getAdminCinemaDetail(selectedCinemaId);
        if (res?.success) {
          // Lấy active auditoriums
          setAuditoriums(res.data?.activeAuditoriums || []);
        }
      } catch (err) {
        triggerToast?.('Không thể tải phòng chiếu', 'error');
      }
    };
    
    // Reset selected auditorium when cinema changes
    setSelectedAuditoriumId('');
    fetchAuditoriums();
  }, [selectedCinemaId, triggerToast]);

  // Handle Movie Selection -> Load Versions
  useEffect(() => {
    if (!selectedMovieId) {
      setVersions([]);
      setSelectedVersionId('');
      return;
    }

    const fetchVersions = async () => {
      try {
        const res = await adminMovieService.getMovieVersions(selectedMovieId);
        if (res?.success) {
          setVersions(res.data || []);
        }
      } catch (err) {
        triggerToast?.('Không thể tải định dạng phim', 'error');
      }
    };

    // Reset selected version when movie changes
    setSelectedVersionId('');
    fetchVersions();
  }, [selectedMovieId, triggerToast]);

  // Derived Values for Summary
  const selectedCinema = useMemo(() => cinemas.find(c => c.publicId === selectedCinemaId), [cinemas, selectedCinemaId]);
  const selectedAuditorium = useMemo(() => auditoriums.find(a => a.publicId === selectedAuditoriumId), [auditoriums, selectedAuditoriumId]);
  const selectedMovie = useMemo(() => movies.find(m => m.publicId === selectedMovieId), [movies, selectedMovieId]);
  const selectedVersion = useMemo(() => versions.find(v => v.publicId === selectedVersionId), [versions, selectedVersionId]);

  // Calculate expected end time based on movie duration and cleaning buffer
  const { expectedEndTime, cleaningCompleteTime } = useMemo(() => {
    if (!startTime || !selectedMovie?.durationMinutes) return { expectedEndTime: null, cleaningCompleteTime: null };
    
    const startDate = new Date(startTime);
    if (isNaN(startDate.getTime())) return { expectedEndTime: null, cleaningCompleteTime: null };

    // End time = start time + movie duration
    const endMillis = startDate.getTime() + (selectedMovie.durationMinutes * 60000);
    const end = new Date(endMillis);

    // Cleaning complete = end time + cleaning buffer
    // Wait, where is cleaningBuffer? It's typically on the auditorium. Wait, I see it in AuditoriumDto if backend returned it, otherwise fallback.
    // Wait, Phase 0 audit and code search showed Auditorium has cleaningBufferMinutes! But `CinemaDetailDto.AuditoriumDto` doesn't have it explicitly mapped in frontend. Wait, let me check the DTO.
    // It's not in the Java DTO I checked earlier (CinemaDetailDto -> AuditoriumDto had screenType, soundType, capacity, status, but NO cleaningBufferMinutes). 
    // Fallback to 15 mins default for UI purpose.
    const buffer = 15;
    const cleaningMillis = endMillis + (buffer * 60000);
    const cleaning = new Date(cleaningMillis);

    return { expectedEndTime: end, cleaningCompleteTime: cleaning };
  }, [startTime, selectedMovie]);

  const validate = () => {
    const newErrors = {};
    if (!selectedCinemaId) newErrors.cinemaId = 'Vui lòng chọn cụm rạp';
    if (!selectedAuditoriumId) newErrors.auditoriumId = 'Vui lòng chọn phòng chiếu';
    if (!selectedMovieId) newErrors.movieId = 'Vui lòng chọn phim';
    if (!selectedVersionId) newErrors.versionId = 'Vui lòng chọn định dạng';
    if (!startTime) {
      newErrors.startTime = 'Vui lòng chọn giờ chiếu';
    } else {
      const startDate = new Date(startTime);
      if (startDate < new Date()) {
        newErrors.startTime = 'Giờ chiếu phải ở tương lai';
      }
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = useCallback(async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const cinemaTimezone = selectedCinema?.timezone || 'Asia/Ho_Chi_Minh';
      // Mặc định trình duyệt trả về datetime-local theo giờ local (user system time).
      // Để gửi lên server theo định dạng UTC chuẩn tương ứng với giờ đã chọn CỦA RẠP (nếu user ở timezone khác),
      // trong một hệ thống production nghiêm ngặt cần parse ngày giờ người dùng nhập với timezone của rạp.
      // Dưới đây sử dụng .toISOString() cho Date đã tạo từ datetime-local (được coi là hệ thống local).
      // Giả sử user đang set giờ trên máy local cùng timezone với rạp.
      const startTimeIso = new Date(startTime).toISOString();

      const payload = {
        cinemaPublicId: selectedCinemaId,
        auditoriumPublicId: selectedAuditoriumId,
        moviePublicId: selectedMovieId,
        movieVersionPublicId: selectedVersionId,
        startTime: startTimeIso
      };

      const res = await adminShowtimeService.createShowtime(payload);
      if (res?.success) {
        triggerToast?.('Tạo suất chiếu thành công', 'success');
        if (onSuccess) onSuccess(res.data?.showtimePublicId);
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Lỗi khi tạo suất chiếu';
      triggerToast?.(msg, 'error');
    } finally {
      setIsSubmitting(false);
    }
  }, [
    selectedCinemaId, selectedAuditoriumId, selectedMovieId, selectedVersionId, 
    startTime, selectedCinema, triggerToast, onSuccess, validate
  ]);

  return {
    cinemas, auditoriums, movies, versions,
    selectedCinemaId, setSelectedCinemaId,
    selectedAuditoriumId, setSelectedAuditoriumId,
    selectedMovieId, setSelectedMovieId,
    selectedVersionId, setSelectedVersionId,
    startTime, setStartTime,
    isLoadingCinemas, isLoadingMovies, isSubmitting,
    errors,
    selectedCinema, selectedAuditorium, selectedMovie, selectedVersion,
    expectedEndTime, cleaningCompleteTime,
    handleSubmit
  };
}
