import { useCallback, useEffect, useRef, useState } from 'react';
import { useOutletContext, useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import useAdminShowtimes from '@/features/scheduling/admin/hooks/useAdminShowtimes';
import ShowtimeTable from '@/features/scheduling/admin/components/ShowtimeTable';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import {
  getBatchStatusReasonPresentation,
} from '@/features/scheduling/admin/utils/schedulingPresentation';

const canConfirmBatchTransition = summary => Boolean(
  summary?.actionAllowed
  && Number(summary.eligibleCount) > 0
  && (!summary.atomic || Number(summary.skippedCount) === 0)
);

const AdminShowtimePage = () => {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();

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
  } = useAdminShowtimes({
    triggerToast,
    initialFilters: {
      cinemaSlug: searchParams.get('cinemaSlug') || '',
      date: searchParams.get('date') || '',
      batchId: searchParams.get('batchId') || '',
      source: searchParams.get('source') || '',
      status: searchParams.get('status') || '',
    },
  });

  const locationStateProcessed = useRef(false);
  const [isBatchActionLoading, setIsBatchActionLoading] = useState(false);
  const [isBatchReadinessLoading, setIsBatchReadinessLoading] = useState(false);
  const [batchReadiness, setBatchReadiness] = useState(null);
  const [batchReadinessError, setBatchReadinessError] = useState('');
  const [batchActionDialog, setBatchActionDialog] = useState(null);
  const batchReadinessGenerationRef = useRef(0);

  useEffect(() => {
    setCinemaSlug(searchParams.get('cinemaSlug') || '');
    setDate(searchParams.get('date') || '');
    setBatchId(searchParams.get('batchId') || '');
    setSource(searchParams.get('source') || '');
    setStatus(searchParams.get('status') || '');
  }, [searchParams, setBatchId, setCinemaSlug, setDate, setSource, setStatus]);

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

  const handleClearBatch = () => {
    batchReadinessGenerationRef.current += 1;
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete('batchId');
    nextSearchParams.delete('source');
    setSearchParams(nextSearchParams, { replace: true });
    setBatchId('');
    setSource('');
    setBatchReadiness(null);
    setBatchReadinessError('');
    setBatchActionDialog(null);
  };

  const handleClearFilters = ({ preserveBatch = false } = {}) => {
    const nextSearchParams = new URLSearchParams(searchParams);
    if (!preserveBatch) {
      batchReadinessGenerationRef.current += 1;
      nextSearchParams.delete('batchId');
      nextSearchParams.delete('source');
    }
    nextSearchParams.delete('status');
    nextSearchParams.delete('cinemaSlug');
    nextSearchParams.delete('date');
    setSearchParams(nextSearchParams, { replace: true });
    if (!preserveBatch) {
      setBatchId('');
      setSource('');
      setBatchReadiness(null);
      setBatchReadinessError('');
      setBatchActionDialog(null);
    }
    setStatus('');
    setCinemaSlug('');
    setDate('');
  };

  const checkBatchReadiness = useCallback(async ({ showDialog = false, quiet = false } = {}) => {
    if (!batchId) return null;

    const requestGeneration = batchReadinessGenerationRef.current + 1;
    batchReadinessGenerationRef.current = requestGeneration;
    setIsBatchReadinessLoading(true);
    setBatchReadinessError('');
    try {
      const res = await adminShowtimeService.previewBatchStatus(batchId, 'OPEN_FOR_BOOKING');
      if (requestGeneration !== batchReadinessGenerationRef.current) return null;
      if (res?.success && res.data) {
        const normalizedSummary = { ...res.data, batchId: res.data.batchId || batchId };
        setBatchReadiness(normalizedSummary);
        if (showDialog) setBatchActionDialog({ phase: 'confirm', summary: normalizedSummary });
        return normalizedSummary;
      }
      throw new Error('Dữ liệu kiểm tra chưa đầy đủ.');
    } catch {
      if (requestGeneration !== batchReadinessGenerationRef.current) return null;
      const message = 'Không thể kiểm tra điều kiện mở bán lúc này. Vui lòng thử lại.';
      setBatchReadinessError(message);
      if (!quiet) triggerToast?.(message, 'error');
      return null;
    } finally {
      if (requestGeneration === batchReadinessGenerationRef.current) {
        setIsBatchReadinessLoading(false);
      }
    }
  }, [batchId, triggerToast]);

  useEffect(() => {
    if (!batchId) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- entering a batch intentionally starts its server-side readiness check.
    void checkBatchReadiness({ quiet: true });
  }, [batchId, checkBatchReadiness]);

  const handleOpenBatchDialog = () => {
    if (!batchReadiness || batchReadiness.batchId !== batchId) {
      void checkBatchReadiness({ showDialog: true });
      return;
    }
    setBatchActionDialog({ phase: 'confirm', summary: batchReadiness });
  };

  const confirmBatchTransition = async () => {
    const summary = batchActionDialog?.summary;
    if (!canConfirmBatchTransition(summary) || !batchId) return;
    setIsBatchActionLoading(true);
    try {
      const res = await adminShowtimeService.transitionBatchStatus(batchId, {
        status: summary.targetStatus,
      });
      if (res?.success && res.data) {
        setBatchActionDialog({ phase: 'result', summary: res.data });
        if (res.data.actionAllowed) {
          triggerToast?.(`Đã mở bán ${res.data.affectedCount} suất chiếu`, 'success');
          await fetchShowtimes();
          await checkBatchReadiness({ quiet: true });
        }
      }
    } catch {
      triggerToast?.('Không thể mở bán lịch; không có suất chiếu nào được thay đổi.', 'error');
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
      batchReadiness={batchReadiness?.batchId === batchId ? batchReadiness : null}
      batchReadinessError={batchReadinessError}
      isBatchReadinessLoading={isBatchReadinessLoading}
      onCheckBatch={() => checkBatchReadiness()}
      onOpenBatch={handleOpenBatchDialog}
      isBatchActionLoading={isBatchActionLoading}
    />
    {batchActionDialog && (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
        <div role="dialog" aria-modal="true" aria-labelledby="batch-action-title" className="w-full max-w-lg rounded-2xl border border-zinc-800 bg-zinc-900 p-6 text-white shadow-2xl">
          <h2 id="batch-action-title" className="text-lg font-black">
            {batchActionDialog.phase === 'confirm'
              ? `Bạn sắp mở bán ${batchActionDialog.summary.eligibleCount} suất chiếu`
              : 'Kết quả mở bán lịch chiếu'}
          </h2>
          <p className="mt-2 text-sm text-zinc-400">
            Sau khi xác nhận, khách có thể đặt vé cho các suất này. Hệ thống kiểm tra toàn bộ lịch, không chỉ các dòng đang hiển thị.
          </p>
          <dl className="mt-5 grid grid-cols-[1fr_auto] gap-x-4 gap-y-2 text-sm">
            <dt className="text-zinc-500">Tổng số suất</dt><dd className="font-bold">{batchActionDialog.summary.totalCount}</dd>
            <dt className="text-zinc-500">Đủ giá để mở bán</dt><dd className="font-bold text-emerald-300">{batchActionDialog.summary.eligibleCount}</dd>
            <dt className="text-zinc-500">Đã mở bán trước đó</dt><dd className="font-bold">{batchActionDialog.summary.alreadyTargetCount}</dd>
            <dt className="text-zinc-500">Chưa thể mở bán</dt><dd className="font-bold text-amber-300">{batchActionDialog.summary.skippedCount}</dd>
          </dl>
          {batchActionDialog.summary.reasonGroups?.length > 0 && (
            <div className="mt-5 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3">
              <p className="text-sm font-bold text-amber-200">Việc cần xử lý</p>
              <ul className="mt-2 space-y-1 text-xs text-amber-100">
                {batchActionDialog.summary.reasonGroups.map((group, index) => {
                  const presentation = getBatchStatusReasonPresentation(group.reasonCode);
                  return (
                    <li key={`${group.reasonCode || 'UNKNOWN'}-${index}`}>
                      {group.count} suất · {presentation.label}
                    </li>
                  );
                })}
              </ul>
            </div>
          )}
          {!batchActionDialog.summary.actionAllowed && (
            <div className="mt-4 text-sm font-bold text-amber-300">
              <p>Không thể mở bán một phần. Không có suất chiếu nào được thay đổi.</p>
              <p className="mt-1 font-medium text-amber-200/80">Lịch sẽ chỉ mở bán khi tất cả suất đều đủ điều kiện.</p>
            </div>
          )}
          <div className="mt-6 flex justify-end gap-3">
            <button type="button" disabled={isBatchActionLoading} onClick={() => setBatchActionDialog(null)} className="rounded-xl px-4 py-2 text-sm font-bold text-zinc-400 disabled:opacity-50">
              {batchActionDialog.phase === 'confirm' ? 'Hủy' : 'Đóng'}
            </button>
            {batchActionDialog.phase === 'confirm' && (
              <button type="button" disabled={!canConfirmBatchTransition(batchActionDialog.summary) || isBatchActionLoading} onClick={confirmBatchTransition} className="rounded-xl bg-emerald-500 px-4 py-2 text-sm font-black text-zinc-950 disabled:opacity-40">
                {isBatchActionLoading ? 'Đang mở bán…' : `Mở bán ${batchActionDialog.summary.eligibleCount} suất`}
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
