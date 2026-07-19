import { useEffect } from 'react';
import { useOutletContext, useNavigate } from 'react-router-dom';
import useAdminShowtimes from '@/features/scheduling/admin/hooks/useAdminShowtimes';
import ShowtimeTable from '@/features/scheduling/admin/components/ShowtimeTable';

const AdminShowtimePage = () => {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const {
    showtimes,
    isLoading,
    cinemaSlug,
    setCinemaSlug,
    movieSlug,
    setMovieSlug,
    date,
    setDate,
    currentPage,
    setCurrentPage,
    pageSize,
    totalPages,
    totalElements,
    fetchShowtimes
  } = useAdminShowtimes({ triggerToast });

  useEffect(() => {
    fetchShowtimes();
  }, [fetchShowtimes]);

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
      isLoading={isLoading}
      cinemaSlug={cinemaSlug}
      setCinemaSlug={setCinemaSlug}
      movieSlug={movieSlug}
      setMovieSlug={setMovieSlug}
      date={date}
      setDate={setDate}
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
