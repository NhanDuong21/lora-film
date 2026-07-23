import { useEffect, useRef, useState } from 'react';
import { useOutletContext, useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import useAdminShowtimes from '@/features/scheduling/admin/hooks/useAdminShowtimes';
import ShowtimeTable from '@/features/scheduling/admin/components/ShowtimeTable';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import {
  getBatchStatusReasonPresentation,
  getShowtimeStatusPresentation,
} from '@/features/scheduling/admin/utils/schedulingPresentation';

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
    totalPages,
    totalElements,
    fetchShowtimes
  } = useAdminShowtimes({ triggerToast });

  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const locationStateProcessed = useRef(false);
  const isReady = useRef(false);
  const [isBatchActionLoading, setIsBatchActionLoading] = useState(false);
  const [batchActionDialog, setBatchActionDialog] = useState(null);

  useEffect(() => {
    setBatchId(searchParams.get('batchId') || '');
    setSource(searchParams.get('source') || '');
    setStatus(searchParams.get('status') || '');
    isReady.current = true;
  }, [searchParams, setBatchId, setSource, setStatus]);

  useEffect(() => {
    if (!locationStateProcessed.current && location.state) {
      if (location.state.cinemaSlug) setCinemaSlug(location.state.cinemaSlug);
      if (location.state.status) setStatus(location.state.status);
      if (location.state.dateFrom) setDate(location.state.dateFrom);

      if (location.state.message) {
        triggerToast?.(location.state.message, 'success');
      }

      window.history.replaceState({}, document.title);
    }
    locationStateProcessed.current = true;
  }, [location.state, setCinemaSlug, setStatus, setDate, triggerToast]);

  useEffect(() => {
    if (isReady.current) {
      fetchShowtimes();
    }
  }, [fetchShowtimes, isReady]);

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
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete('batchId');
    nextSearchParams.delete('source');
    setSearchParams(nextSearchParams, { replace: true });
    setBatchId('');
    setSource('');
  };

  const handleClearFilters = () => {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete('batchId');
    nextSearchParams.delete('source');
    nextSearchParams.delete('status');
    setSearchParams(nextSearchParams, { replace: true });
    setBatchId('');
    setSource('');
    setStatus('');
  };

  const handleTransitionBatch = async (targetStatus) => {
    if (!batchId) return;

    setIsBatchActionLoading(true);
    try {
      const res = await adminShowtimeService.previewBatchStatus(batchId, targetStatus);
      if (res?.success && res.data) {
        setBatchActionDialog({ phase: 'confirm', summary: res.data });
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Không thể kiểm tra điều kiện mở bán của đợt';
      triggerToast?.(msg, 'error');
    } finally {
      setIsBatchActionLoading(false);
    }
  };

  const confirmBatchTransition = async () => {
    const summary = batchActionDialog?.summary;
    if (!summary?.actionAllowed || !batchId) return;
    setIsBatchActionLoading(true);
    try {
      const res = await adminShowtimeService.transitionBatchStatus(batchId, {
        status: summary.targetStatus,
      });
      if (res?.success && res.data) {
        setBatchActionDialog({ phase: 'result', summary: res.data });
        if (res.data.actionAllowed) {
          triggerToast?.(`Đã mở bán ${res.data.affectedCount} suất chiếu`, 'success');
          fetchShowtimes();
        }
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Không thể mở bán đợt; không có suất chiếu nào được thay đổi';
      triggerToast?.(msg, 'error');
    } finally {
      setIsBatchActionLoading(false);
    }
  };

  return (
    <>
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
      totalPages={totalPages}
      totalElements={totalElements}
      batchId={batchId}
      source={source}
      onOpenCreate={handleOpenCreate}
      onOpenAutoSchedule={handleOpenAutoSchedule}
      onViewDetail={handleViewDetail}
      fetchShowtimes={fetchShowtimes}
      onClearBatch={handleClearBatch}
      onClearFilters={handleClearFilters}
      onTransitionBatch={handleTransitionBatch}
      isBatchActionLoading={isBatchActionLoading}
    />
    {batchActionDialog && (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
        <div role="dialog" aria-modal="true" aria-labelledby="batch-action-title" className="w-full max-w-lg rounded-2xl border border-zinc-800 bg-zinc-900 p-6 text-white shadow-2xl">
          <h2 id="batch-action-title" className="text-lg font-black">
            {batchActionDialog.phase === 'confirm' ? 'Xác nhận mở bán toàn bộ' : 'Kết quả mở bán toàn bộ'}
          </h2>
          <p className="mt-2 text-sm text-zinc-400">
            Trạng thái đích: {getShowtimeStatusPresentation(batchActionDialog.summary.targetStatus).label}. Kết quả dựa trên toàn bộ đợt, không phụ thuộc trang đang xem.
          </p>
          <dl className="mt-5 grid grid-cols-[1fr_auto] gap-x-4 gap-y-2 text-sm">
            <dt className="text-zinc-500">Tổng suất chiếu</dt><dd className="font-bold">{batchActionDialog.summary.totalCount}</dd>
            <dt className="text-zinc-500">Đủ điều kiện</dt><dd className="font-bold text-emerald-300">{batchActionDialog.summary.eligibleCount}</dd>
            <dt className="text-zinc-500">Đã ở trạng thái đích</dt><dd className="font-bold">{batchActionDialog.summary.alreadyTargetCount}</dd>
            <dt className="text-zinc-500">Bị chặn</dt><dd className="font-bold text-amber-300">{batchActionDialog.summary.skippedCount}</dd>
            <dt className="text-zinc-500">Đã thay đổi</dt><dd className="font-bold text-emerald-300">{batchActionDialog.summary.affectedCount}</dd>
            <dt className="text-zinc-500">Thực thi nguyên tử</dt><dd className="font-bold">{batchActionDialog.summary.atomic ? 'Có' : 'Không'}</dd>
          </dl>
          {batchActionDialog.summary.reasonGroups?.length > 0 && (
            <div className="mt-5 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3">
              <p className="text-sm font-bold text-amber-200">Lý do chặn</p>
              <ul className="mt-2 space-y-1 text-xs text-amber-100">
                {batchActionDialog.summary.reasonGroups.map(group => (
                  <li key={group.reasonCode}>
                    {group.count} suất · {getBatchStatusReasonPresentation(group.reasonCode).label}
                    <details className="mt-1 text-[10px] text-amber-100/70">
                      <summary className="cursor-pointer">Thông tin kỹ thuật</summary>
                      <span className="font-mono">{group.reasonCode}: {group.reason}</span>
                    </details>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {!batchActionDialog.summary.actionAllowed && (
            <p className="mt-4 text-sm font-bold text-amber-300">Không thể mở bán một phần. Không có suất chiếu nào được thay đổi.</p>
          )}
          <div className="mt-6 flex justify-end gap-3">
            <button type="button" disabled={isBatchActionLoading} onClick={() => setBatchActionDialog(null)} className="rounded-xl px-4 py-2 text-sm font-bold text-zinc-400 disabled:opacity-50">
              {batchActionDialog.phase === 'confirm' ? 'Hủy' : 'Đóng'}
            </button>
            {batchActionDialog.phase === 'confirm' && (
              <button type="button" disabled={!batchActionDialog.summary.actionAllowed || isBatchActionLoading} onClick={confirmBatchTransition} className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-zinc-950 disabled:opacity-40">
                {isBatchActionLoading ? 'Đang mở bán…' : 'Mở bán toàn bộ'}
              </button>
            )}
          </div>
        </div>
      </div>
    )}
    </>
  );
};

export default AdminShowtimePage;
