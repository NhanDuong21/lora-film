import { useEffect, useRef, useState } from 'react';
import { useOutletContext, useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import useAdminShowtimes from '@/features/scheduling/admin/hooks/useAdminShowtimes';
import ShowtimeTable from '@/features/scheduling/admin/components/ShowtimeTable';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';

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
    batchId,
    setBatchId,
    source,
    setSource,
    currentPage,
    setCurrentPage,
    pageSize,
    totalPages,
    totalElements,
    fetchShowtimes
  } = useAdminShowtimes({ triggerToast });

  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const locationProcessed = useRef(false);
  const [isBatchActionLoading, setIsBatchActionLoading] = useState(false);

  useEffect(() => {
    if (!locationProcessed.current) {
      const urlBatchId = searchParams.get('batchId');
      const urlSource = searchParams.get('source');
      const urlStatus = searchParams.get('status');
      
      if (urlBatchId) setBatchId(urlBatchId);
      if (urlSource) setSource(urlSource);
      if (urlStatus) setStatus(urlStatus);
      
      if (location.state) {
        if (location.state.cinemaSlug) setCinemaSlug(location.state.cinemaSlug);
        if (location.state.status) setStatus(location.state.status);
        if (location.state.dateFrom) setDate(location.state.dateFrom);
        
        if (location.state.message) {
          triggerToast?.(location.state.message, 'success');
        }
        
        // Clear state to avoid infinite re-triggering if user navigates away and back
        window.history.replaceState({}, document.title);
      }
      
      locationProcessed.current = true;
    }
  }, [location.state, searchParams, setCinemaSlug, setStatus, setDate, setBatchId, setSource, triggerToast]);

  const shouldWait = !locationProcessed.current;

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

  const handleClearBatch = () => {
    searchParams.delete('batchId');
    searchParams.delete('source');
    setSearchParams(searchParams);
    setBatchId('');
    setSource('');
  };

  const handleTransitionBatch = async (targetStatus) => {
    if (!batchId) return;
    
    // confirm
    if (!window.confirm(`Bạn có chắc chắn muốn chuyển trạng thái toàn bộ đợt này sang ${targetStatus}?`)) return;

    setIsBatchActionLoading(true);
    try {
      const res = await adminShowtimeService.transitionBatchStatus(batchId, { status: targetStatus });
      if (res?.success) {
        triggerToast?.(`Chuyển trạng thái đợt thành công!`, 'success');
        fetchShowtimes();
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Lỗi chuyển trạng thái đợt';
      triggerToast?.(msg, 'error');
    } finally {
      setIsBatchActionLoading(false);
    }
  };

  const handleDeleteBatch = async () => {
    if (!batchId) return;
    
    // confirm
    if (!window.confirm(`Bạn có chắc chắn muốn XÓA toàn bộ suất chiếu DRAFT trong đợt này? Hành động không thể hoàn tác!`)) return;

    setIsBatchActionLoading(true);
    try {
      const res = await adminShowtimeService.deleteBatch(batchId);
      if (res?.success) {
        triggerToast?.(`Xóa đợt thành công!`, 'success');
        handleClearBatch();
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Lỗi xóa đợt';
      triggerToast?.(msg, 'error');
    } finally {
      setIsBatchActionLoading(false);
    }
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
      batchId={batchId}
      source={source}
      setBatchId={setBatchId}
      setSource={setSource}
      onOpenCreate={handleOpenCreate}
      onOpenAutoSchedule={handleOpenAutoSchedule}
      onViewDetail={handleViewDetail}
      fetchShowtimes={fetchShowtimes}
      onClearBatch={handleClearBatch}
      onTransitionBatch={handleTransitionBatch}
      onDeleteBatch={handleDeleteBatch}
      isBatchActionLoading={isBatchActionLoading}
    />
  );
};

export default AdminShowtimePage;
