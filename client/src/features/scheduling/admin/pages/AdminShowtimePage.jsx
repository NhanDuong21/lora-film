import { useEffect, useRef } from 'react';
import { useOutletContext, useNavigate, useLocation } from 'react-router-dom';
import useAdminShowtimes from '@/features/scheduling/admin/hooks/useAdminShowtimes';
import ShowtimeTable from '@/features/scheduling/admin/components/ShowtimeTable';

const AdminShowtimePage = () => {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const {
    showtimes,
    cinemas,
    movies,
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
    pageSize,
    totalPages,
    totalElements,
    fetchShowtimes
  } = useAdminShowtimes({ triggerToast });

  const location = useLocation();
  const locationProcessed = useRef(false);

  useEffect(() => {
    if (location.state && !locationProcessed.current) {
      if (location.state.cinemaSlug) setCinemaSlug(location.state.cinemaSlug);
      if (location.state.status) setStatus(location.state.status);
      if (location.state.dateFrom) setDate(location.state.dateFrom);
      
      if (location.state.message) {
        triggerToast?.(location.state.message, 'success');
      }
      
      locationProcessed.current = true;
      // Clear state to avoid infinite re-triggering if user navigates away and back
      window.history.replaceState({}, document.title);
    }
  }, [location.state, setCinemaSlug, setStatus, setDate, triggerToast]);

  const shouldWait = location.state && !locationProcessed.current;

  useEffect(() => {
    if (!shouldWait) {
      fetchShowtimes();
    }
  }, [fetchShowtimes, shouldWait]);

  const handleOpenCreate = () => {
    navigate('/admin/showtimes/create');
  };

  const handleOpenAutoSchedule = () => {
    navigate('/admin/showtime-schedules/create');
  };

  const handleViewDetail = (showtimePublicId) => {
    navigate(`/admin/showtimes/${showtimePublicId}`);
  };

  return (
    <ShowtimeTable
      showtimes={showtimes}
      cinemas={cinemas}
      movies={movies}
      isLoading={isLoading}
      isOptionsLoading={isOptionsLoading}
      cinemaSlug={cinemaSlug}
      setCinemaSlug={setCinemaSlug}
      movieSlug={movieSlug}
      setMovieSlug={setMovieSlug}
      date={date}
      setDate={setDate}
      status={status}
      setStatus={setStatus}
      currentPage={currentPage}
      setCurrentPage={setCurrentPage}
      pageSize={pageSize}
      totalPages={totalPages}
      totalElements={totalElements}
      onOpenCreate={handleOpenCreate}
      onOpenAutoSchedule={handleOpenAutoSchedule}
      onViewDetail={handleViewDetail}
    />
  );
};

export default AdminShowtimePage;
