import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { CheckCircle2, Loader2, XCircle } from 'lucide-react';
import ShowtimeTable from '@/features/scheduling/admin/components/ShowtimeTable';
import {
  getShowtimeTransitionActionPresentation,
  isExpiredDraftShowtime,
} from '@/features/scheduling/admin/utils/schedulingPresentation';
import { getOperationalTodayDateKey } from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import {
  buildShowtimeQueryCacheKey,
  invalidateShowtimeQueryCache,
  readShowtimeQueryCache,
  runShowtimeQueryOnce,
  writeShowtimeQueryCache,
} from '@/features/scheduling/admin/utils/showtimeQueryCache';
import { getUserAccountId } from '@/utils/authStorage';
import managerCinemaService from '../services/managerCinemaService';

const EMPTY_RESPONSE = {
  data: [],
  pageNo: 0,
  pageSize: 100,
  totalElements: 0,
  totalPages: 0,
  last: true,
};

const collectMovies = showtimes => {
  const values = new Map();
  showtimes.forEach(showtime => {
    const movie = showtime.movie;
    const key = movie?.publicId || movie?.slug || movie?.title;
    if (key && !values.has(key)) values.set(key, movie);
  });
  return Array.from(values.values()).sort((left, right) => (
    String(left?.title || '').localeCompare(String(right?.title || ''), 'vi')
  ));
};

const getPrimaryTransition = (showtime, now) => {
  if (showtime.status === 'DRAFT' && !isExpiredDraftShowtime(showtime, now)) {
    return 'OPEN_FOR_BOOKING';
  }
  if (showtime.status === 'OPEN_FOR_BOOKING') return 'CLOSED';
  if (showtime.status === 'CLOSED' && new Date(showtime.startTime).getTime() > now) {
    return 'OPEN_FOR_BOOKING';
  }
  if (showtime.status === 'CLOSED' && new Date(showtime.endTime).getTime() <= now) {
    return 'FINISHED';
  }
  return null;
};

const ManagerShowtimeActions = ({ showtime, now, actionId, onTransition, onCompleted }) => {
  const primaryTransition = getPrimaryTransition(showtime, now);
  const canCancel = !['CANCELLED', 'FINISHED'].includes(showtime.status);
  const isActing = actionId === showtime.showtimePublicId;

  const runTransition = async targetStatus => {
    const completed = await onTransition(showtime, targetStatus);
    if (completed) onCompleted?.();
  };

  if (!primaryTransition && !canCancel) {
    return <p className="text-center text-xs font-bold text-zinc-600">Suất chiếu không còn thao tác điều phối.</p>;
  }

  return (
    <div className="grid gap-2 sm:grid-cols-2">
      {primaryTransition && (
        <button type="button" disabled={isActing} onClick={() => runTransition(primaryTransition)} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 text-sm font-black text-zinc-950 disabled:opacity-40">
          {isActing ? <Loader2 className="h-4 w-4 animate-spin" /> : <CheckCircle2 className="h-4 w-4" />}
          {isActing
            ? 'Đang xử lý…'
            : showtime.status === 'CLOSED' && primaryTransition === 'OPEN_FOR_BOOKING'
              ? 'Mở bán lại'
              : getShowtimeTransitionActionPresentation(primaryTransition).label}
        </button>
      )}
      {canCancel && (
        <button type="button" disabled={isActing} onClick={() => runTransition('CANCELLED')} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-4 text-sm font-black text-red-200 disabled:opacity-40">
          <XCircle className="h-4 w-4" /> Hủy suất chiếu
        </button>
      )}
    </div>
  );
};

export default function ManagerShowtimesPage() {
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const defaultOperationalDate = getOperationalTodayDateKey(selectedCinema?.timezone);
  const [movieSlug, setMovieSlug] = useState('');
  const [date, setDate] = useState(defaultOperationalDate);
  const [status, setStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(100);
  const [movies, setMovies] = useState([]);
  const [actionId, setActionId] = useState('');
  const [now, setNow] = useState(() => Date.now());
  const [state, setState] = useState({ loading: true, refreshing: false, error: '', response: EMPTY_RESPONSE });
  const requestGenerationRef = useRef(0);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 60_000);
    return () => window.clearInterval(timer);
  }, []);

  const fetchShowtimes = useCallback(async ({ force = false } = {}) => {
    if (!selectedCinemaId) return;
    const requestGeneration = ++requestGenerationRef.current;
    const params = {
      cinemaPublicId: selectedCinemaId,
      movieSlug: movieSlug || undefined,
      date: date || undefined,
      status: status || undefined,
      page: currentPage,
      size: pageSize,
    };
    const accountScope = getUserAccountId() || 'current-session';
    const cacheScope = `manager-showtimes:${accountScope}:`;
    const cacheKey = buildShowtimeQueryCacheKey(cacheScope, params);
    const cached = readShowtimeQueryCache(cacheKey);

    if (cached && !force && cached.isFresh) {
      setState({ loading: false, refreshing: false, error: '', response: cached.response });
      if (!movieSlug) setMovies(collectMovies(cached.response?.data || []));
      return cached.response;
    }

    setState(current => cached
      ? { loading: false, refreshing: true, error: '', response: cached.response }
      : { ...current, loading: true, refreshing: false, error: '' });
    try {
      const response = await runShowtimeQueryOnce(
        cacheKey,
        () => managerCinemaService.getShowtimes(params),
      );
      writeShowtimeQueryCache(cacheKey, response || EMPTY_RESPONSE);
      if (requestGeneration !== requestGenerationRef.current) return;
      setState({ loading: false, refreshing: false, error: '', response: response || EMPTY_RESPONSE });
      if (!movieSlug) setMovies(collectMovies(response?.data || []));
      return response;
    } catch (error) {
      if (requestGeneration !== requestGenerationRef.current) return;
      setState(current => ({
        ...current,
        loading: false,
        refreshing: false,
        error: error?.message || 'Không thể tải lịch chiếu.',
      }));
    }
  }, [currentPage, date, movieSlug, pageSize, selectedCinemaId, status]);

  useEffect(() => {
    void fetchShowtimes();
  }, [fetchShowtimes]);

  const transition = async (showtime, targetStatus) => {
    const reason = targetStatus === 'CANCELLED'
      ? window.prompt('Nhập lý do hủy suất chiếu. Khách đã đặt vé sẽ được đưa vào quy trình hoàn tiền:')
      : null;
    if (targetStatus === 'CANCELLED' && !reason?.trim()) return false;

    setActionId(showtime.showtimePublicId);
    setState(current => ({ ...current, error: '' }));
    try {
      const updated = await managerCinemaService.transitionShowtimeStatus(
        showtime.showtimePublicId,
        targetStatus,
        reason,
      );
      invalidateShowtimeQueryCache(`manager-showtimes:${getUserAccountId() || 'current-session'}:`);
      setState(current => {
        const remainsInFilter = !status || updated.status === status;
        const currentData = current.response.data || [];
        return {
          ...current,
          response: {
            ...current.response,
            data: remainsInFilter
              ? currentData.map(item => item.showtimePublicId === updated.showtimePublicId ? updated : item)
              : currentData.filter(item => item.showtimePublicId !== updated.showtimePublicId),
            totalElements: remainsInFilter
              ? current.response.totalElements
              : Math.max(0, Number(current.response.totalElements || 0) - 1),
          },
        };
      });
      return true;
    } catch (error) {
      setState(current => ({
        ...current,
        error: error?.message || 'Không thể cập nhật trạng thái suất chiếu.',
      }));
      return false;
    } finally {
      setActionId('');
    }
  };

  const showtimes = state.response.data || [];
  const cinemaSlug = selectedCinema?.slug || selectedCinema?.publicId || '';
  const cinemas = useMemo(() => selectedCinema ? [selectedCinema] : [], [selectedCinema]);

  if (cinemaState.loading) {
    return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp…</p>;
  }
  if (!selectedCinema) {
    return <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center"><h1 className="text-xl font-black">Chưa có rạp để xem lịch chiếu</h1><p className="mt-2 text-sm text-amber-100/70">Quản trị viên cần phân công rạp cho tài khoản này trước.</p></div>;
  }

  return (
    <div className="space-y-4">
      {state.error && <div role="alert" className="rounded-xl border border-red-500/25 bg-red-500/10 px-4 py-3 text-sm font-bold text-red-200">{state.error}</div>}
      <ShowtimeTable
        showtimes={showtimes}
        cinemas={cinemas}
        movies={movies}
        isLoading={state.loading}
        isRefreshing={state.refreshing}
        isOptionsLoading={state.loading}
        cinemaSlug={cinemaSlug}
        setCinemaSlug={() => {}}
        movieSlug={movieSlug}
        setMovieSlug={setMovieSlug}
        date={date}
        setDate={setDate}
        status={status}
        setStatus={setStatus}
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
        pageSize={pageSize}
        setPageSize={setPageSize}
        totalPages={state.response.totalPages || 0}
        totalElements={state.response.totalElements || 0}
        batchId=""
        onClearFilters={() => {}}
        headerEyebrow="Điều phối tại rạp"
        headerDescription={`Theo dõi và điều phối lịch chiếu tại ${selectedCinema.name}. Dữ liệu được giới hạn theo rạp do quản trị viên phân công.`}
        showCreateActions={false}
        cinemaFilterLocked
        defaultDate={defaultOperationalDate}
        quickDrawerProps={{
          showPricing: false,
          seatControlApi: managerCinemaService,
          renderActions: (showtime, { close }) => (
            <ManagerShowtimeActions
              showtime={showtime}
              now={now}
              actionId={actionId}
              onTransition={transition}
              onCompleted={close}
            />
          ),
        }}
      />
    </div>
  );
}
