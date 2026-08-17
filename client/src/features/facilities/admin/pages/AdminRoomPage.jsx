import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Armchair,
  CalendarClock,
  ChevronLeft,
  ChevronRight,
  Clock3,
  CirclePause,
  DoorOpen,
  ExternalLink,
  PlusCircle,
  Search,
  Settings2,
} from 'lucide-react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { EmptyState, ErrorState, LoadingState } from '@/components/common/ui/uiKit';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import {
  formatCinemaTime,
  formatServiceDateKey,
  getCinemaDateKey,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import {
  getAuditoriumReadiness,
  getAuditoriumStatus,
  SCREEN_TYPE_LABELS,
  SOUND_TYPE_LABELS,
} from '@/features/facilities/admin/utils/facilityPresentation';
import {
  addDaysToDateKey,
  getNextShowtimeForAuditorium,
  getShowtimeDateKeys,
  getShowtimeState,
} from '@/features/facilities/admin/utils/roomShowtimePresentation';

const PAGE_SIZE = 8;

export default function AdminRoomPage() {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const navigate = useNavigate();
  const [cinemas, setCinemas] = useState([]);
  const [selectedCinemaId, setSelectedCinemaId] = useState('');
  const [rooms, setRooms] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [isMutating, setIsMutating] = useState(false);
  const [showtimes, setShowtimes] = useState([]);
  const [isShowtimesLoading, setIsShowtimesLoading] = useState(false);
  const [showtimesError, setShowtimesError] = useState(null);
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const fetchCinemas = async () => {
      try {
        const response = await adminCinemaService.getCinemas({ page: 0, size: 100 });
        const list = response?.success && Array.isArray(response.data?.data)
          ? response.data.data
          : [];
        setCinemas(list);
        if (list.length > 0) setSelectedCinemaId(list[0].publicId);
      } catch (requestError) {
        setError(
          requestError.response?.data?.message
            || requestError.message
            || 'Không thể tải danh sách cụm rạp',
        );
      }
    };
    fetchCinemas();
  }, []);

  const fetchRooms = useCallback(async () => {
    if (!selectedCinemaId) {
      setRooms([]);
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const response = await adminCinemaService.getAdminCinemaDetail(selectedCinemaId);
      setRooms(
        response?.success && Array.isArray(response.data?.activeAuditoriums)
          ? response.data.activeAuditoriums
          : [],
      );
    } catch (requestError) {
      setError(
        requestError.response?.data?.message
          || requestError.message
          || 'Không thể tải danh sách phòng chiếu',
      );
    } finally {
      setIsLoading(false);
    }
  }, [selectedCinemaId]);

  useEffect(() => {
    // The effect synchronizes the selected cinema with its remote room list.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchRooms();
  }, [fetchRooms]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 60_000);
    return () => window.clearInterval(timer);
  }, []);

  const selectedCinema = useMemo(
    () => cinemas.find(cinema => cinema.publicId === selectedCinemaId),
    [cinemas, selectedCinemaId],
  );

  useEffect(() => {
    let isCurrentRequest = true;

    const fetchUpcomingShowtimes = async () => {
      if (!selectedCinema?.slug) {
        setShowtimes([]);
        setShowtimesError(null);
        setIsShowtimesLoading(false);
        return;
      }

      setIsShowtimesLoading(true);
      setShowtimesError(null);
      const dateKeys = getShowtimeDateKeys(new Date(), selectedCinema.timezone, 7);

      try {
        const responses = await Promise.all(
          dateKeys.map(date => adminShowtimeService.getShowtimes({
            cinemaSlug: selectedCinema.slug,
            date,
            page: 0,
            size: 100,
          })),
        );
        if (!isCurrentRequest) return;

        const rows = responses.flatMap(response => (
          response?.success && Array.isArray(response.data?.data) ? response.data.data : []
        ));
        setShowtimes(rows);
      } catch (requestError) {
        if (!isCurrentRequest) return;
        setShowtimes([]);
        setShowtimesError(
          requestError.response?.data?.message
            || requestError.message
            || 'Không thể tải lịch chiếu',
        );
      } finally {
        if (isCurrentRequest) setIsShowtimesLoading(false);
      }
    };

    fetchUpcomingShowtimes();
    return () => {
      isCurrentRequest = false;
    };
  }, [selectedCinema]);

  const filteredRooms = useMemo(() => {
    const keyword = searchTerm.trim().toLocaleLowerCase('vi');
    return rooms.filter((room) => {
      const roomName = room.name || room.auditoriumName || '';
      const matchesKeyword = !keyword || roomName.toLocaleLowerCase('vi').includes(keyword);
      const matchesStatus = statusFilter === 'ALL' || room.status === statusFilter;
      return matchesKeyword && matchesStatus;
    });
  }, [rooms, searchTerm, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredRooms.length / PAGE_SIZE));
  const visibleRooms = filteredRooms.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const roomSummary = useMemo(() => ({
    ready: filteredRooms.filter(room => getAuditoriumReadiness(room).canServe).length,
    seats: filteredRooms.reduce((total, room) => total + Number(room.capacity || 0), 0),
  }), [filteredRooms]);

  const handlePauseRoom = async (room) => {
    const roomName = room.name || room.auditoriumName || 'phòng chiếu';
    const cleaningBufferMinutes = Number(room.cleaningBufferMinutes);
    if (
      room.cleaningBufferMinutes == null
      || !Number.isFinite(cleaningBufferMinutes)
      || cleaningBufferMinutes < 0
    ) {
      triggerToast?.(
        `Thiếu dữ liệu thời gian dọn phòng của ${roomName}. Vui lòng tải lại trang trước khi thay đổi trạng thái.`,
        'error',
      );
      return;
    }
    const confirmed = await triggerConfirm?.({
      title: `Tạm ngừng ${roomName}?`,
      message:
        'Phòng sẽ không tiếp tục tham gia xếp lịch hoặc bán vé. Hệ thống hiện chưa trả số suất chiếu và đơn đặt vé bị ảnh hưởng; hãy kiểm tra Lịch vận hành và Đơn đặt vé trước khi xác nhận.',
      confirmLabel: 'Tạm ngừng phòng',
      cancelLabel: 'Quay lại kiểm tra',
      tone: 'danger',
    });
    if (!confirmed) return;

    setIsMutating(true);
    try {
      const response = await adminRoomService.updateAuditorium(room.publicId, {
        name: roomName,
        screenType: room.screenType || 'STANDARD',
        soundType: room.soundType || 'STANDARD',
        capacity: Number(room.capacity || 1),
        cleaningBufferMinutes,
        status: 'INACTIVE',
      });
      if (response?.success) {
        triggerToast?.(`Đã tạm ngừng ${roomName}`);
        await fetchRooms();
      }
    } catch (requestError) {
      triggerToast?.(
        requestError.response?.data?.message
          || requestError.message
          || 'Không thể tạm ngừng phòng chiếu',
        'error',
      );
    } finally {
      setIsMutating(false);
    }
  };

  return (
    <div className="flex min-h-[400px] flex-1 flex-col space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <header className="flex flex-col gap-4 border-b border-zinc-900 pb-5 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-xl font-black uppercase tracking-wide text-zinc-50 md:text-2xl">
            Trung tâm phòng chiếu
          </h1>
          <p className="mt-1 text-xs text-zinc-400">
            Kiểm tra mức độ sẵn sàng, cấu hình ghế và trạng thái vận hành của từng phòng.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            if (!selectedCinemaId) {
              triggerToast?.('Vui lòng chọn cụm rạp trước', 'error');
              return;
            }
            navigate(`/admin/rooms/create?cinemaId=${selectedCinemaId}`);
          }}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 py-3 text-xs font-black text-white shadow-lg shadow-brand-orange/10"
        >
          <PlusCircle className="h-4 w-4" />
          Tạo phòng chiếu
        </button>
      </header>

      <section className="grid gap-4 rounded-2xl border border-zinc-900 bg-zinc-900/20 p-5 md:grid-cols-4">
        <label className="space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
          Cụm rạp
          <select
            value={selectedCinemaId}
            onChange={(event) => {
              setSelectedCinemaId(event.target.value);
              setPage(0);
            }}
            className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm font-semibold normal-case text-zinc-200 outline-none focus:border-brand-orange"
          >
            {cinemas.map((cinema) => (
              <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>
            ))}
            {cinemas.length === 0 && <option value="">Chưa có cụm rạp</option>}
          </select>
        </label>
        <label className="space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
          Trạng thái vận hành
          <select
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setPage(0);
            }}
            className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm font-semibold normal-case text-zinc-200 outline-none focus:border-brand-orange"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Sẵn sàng phục vụ</option>
            <option value="DRAFT">Đang thiết lập</option>
            <option value="MAINTENANCE">Đang bảo trì</option>
            <option value="INACTIVE">Tạm ngừng</option>
          </select>
        </label>
        <label className="space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500 md:col-span-2">
          Tìm phòng
          <div className="relative">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
            <input
              type="search"
              placeholder="Nhập tên phòng chiếu..."
              value={searchTerm}
              onChange={(event) => {
                setSearchTerm(event.target.value);
                setPage(0);
              }}
              className="w-full rounded-xl border border-zinc-800 bg-zinc-950 py-3 pl-11 pr-4 text-sm font-semibold normal-case text-zinc-200 outline-none focus:border-brand-orange"
            />
          </div>
        </label>
      </section>

      <section>
        {isLoading && <LoadingState message="Đang tải danh sách phòng chiếu..." />}
        {!isLoading && error && <ErrorState message={error} onRetry={fetchRooms} />}
        {!isLoading && !error && filteredRooms.length === 0 && (
          <EmptyState
            message="Không tìm thấy phòng chiếu phù hợp"
            actionLabel="Tạo phòng chiếu"
            onAction={() => navigate(`/admin/rooms/create?cinemaId=${selectedCinemaId}`)}
          />
        )}
        {!isLoading && !error && visibleRooms.length > 0 && (
          <>
            <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h2 className="text-sm font-black uppercase tracking-wide text-zinc-200">
                  Danh sách phòng chiếu
                </h2>
                <p className="mt-1 text-xs text-zinc-500">
                  Chọn một phòng để quản lý thông tin, sơ đồ ghế và trạng thái vận hành.
                </p>
              </div>
              <div className="flex flex-wrap gap-2 text-[11px] font-bold">
                <span className="rounded-full border border-zinc-800 bg-zinc-900/60 px-3 py-1.5 text-zinc-300">
                  {filteredRooms.length} phòng
                </span>
                <span className="rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1.5 text-emerald-300">
                  {roomSummary.ready} sẵn sàng
                </span>
                <span className="rounded-full border border-sky-500/20 bg-sky-500/10 px-3 py-1.5 text-sky-300">
                  {roomSummary.seats} ghế
                </span>
              </div>
            </div>

            <div className="grid gap-4 xl:grid-cols-2 2xl:grid-cols-3">
              {visibleRooms.map((room) => {
                const roomName = room.name || room.auditoriumName || 'Phòng chiếu';
                const status = getAuditoriumStatus(room.status);
                const readiness = getAuditoriumReadiness(room);
                const nextShowtime = getNextShowtimeForAuditorium(showtimes, room.publicId, now);
                const showtimeState = nextShowtime ? getShowtimeState(nextShowtime, now) : null;
                const showtimeTimezone = nextShowtime?.cinema?.timezone || selectedCinema?.timezone;
                const todayKey = getCinemaDateKey(now, showtimeTimezone);
                const showtimeDateKey = nextShowtime?.serviceDate || getCinemaDateKey(
                  nextShowtime?.startTime,
                  showtimeTimezone,
                );
                const showtimeDateLabel = showtimeDateKey === todayKey
                  ? 'Hôm nay'
                  : showtimeDateKey === addDaysToDateKey(todayKey, 1)
                    ? 'Ngày mai'
                    : formatServiceDateKey(showtimeDateKey);
                return (
                  <article
                    key={room.publicId}
                    aria-labelledby={`room-${room.publicId}-title`}
                    className="group flex min-h-full flex-col overflow-hidden rounded-3xl border border-zinc-800/80 bg-zinc-900/30 transition-all hover:-translate-y-0.5 hover:border-zinc-700 hover:bg-zinc-900/50 hover:shadow-2xl hover:shadow-black/20"
                  >
                    <header className="flex items-start justify-between gap-3 border-b border-zinc-800/80 p-5">
                      <div className="flex min-w-0 items-center gap-3">
                        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border border-brand-orange/25 bg-brand-orange/10 transition-colors group-hover:bg-brand-orange/15">
                          <DoorOpen className="h-5 w-5 text-brand-orange" />
                        </div>
                        <div className="min-w-0">
                          <h3 id={`room-${room.publicId}-title`} className="truncate font-black text-white" title={roomName}>{roomName}</h3>
                          <p className="mt-1 truncate text-xs text-zinc-500">
                            {SCREEN_TYPE_LABELS[room.screenType] || 'Chưa rõ màn hình'}
                            {' · '}
                            {SOUND_TYPE_LABELS[room.soundType] || 'Chưa rõ âm thanh'}
                          </p>
                        </div>
                      </div>
                      <span className={`shrink-0 rounded-lg border px-2.5 py-1.5 text-[10px] font-black ${status.className}`}>
                        {status.label}
                      </span>
                    </header>

                    <div className="flex flex-1 flex-col gap-4 p-5">
                      <div className="grid grid-cols-2 gap-3">
                        <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
                          <div className="flex items-center gap-2 text-zinc-500">
                            <Armchair className="h-4 w-4" />
                            <p className="text-[10px] font-black uppercase tracking-widest">Sơ đồ ghế</p>
                          </div>
                          <p className={`mt-2 text-lg font-black ${
                            readiness.hasSeatLayout ? 'text-emerald-300' : 'text-amber-300'
                          }`}>
                            {readiness.hasSeatLayout ? `${Number(room.capacity || 0)} ghế` : 'Chưa có'}
                          </p>
                          <p className="mt-1 text-[11px] text-zinc-500">
                            {readiness.hasSeatLayout ? 'Đã thiết lập sơ đồ' : 'Cần hoàn thiện cấu hình'}
                          </p>
                        </div>
                        <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
                          <div className="flex items-center gap-2 text-zinc-500">
                            <Clock3 className="h-4 w-4" />
                            <p className="text-[10px] font-black uppercase tracking-widest">Dọn phòng</p>
                          </div>
                          <p className="mt-2 text-lg font-black text-zinc-100">
                            {room.cleaningBufferMinutes != null
                              ? `${Number(room.cleaningBufferMinutes)} phút`
                              : 'Chưa đặt'}
                          </p>
                          <p className="mt-1 text-[11px] text-zinc-500">Khoảng đệm giữa hai suất</p>
                        </div>
                      </div>

                      <section className="flex min-h-48 flex-1 flex-col rounded-2xl border border-zinc-800 bg-black/20 p-4" aria-label={`Suất chiếu kế tiếp của ${roomName}`}>
                        <div className="flex items-center justify-between gap-3">
                          <div className="flex items-center gap-2 text-zinc-400">
                            <CalendarClock className="h-4 w-4 text-brand-orange" />
                            <h4 className="text-[10px] font-black uppercase tracking-widest">Suất chiếu kế tiếp</h4>
                          </div>
                          {nextShowtime && !isShowtimesLoading && !showtimesError && (
                            <span className="text-[11px] font-bold text-zinc-500">{showtimeDateLabel}</span>
                          )}
                        </div>
                      {isShowtimesLoading ? (
                        <div className="flex flex-1 items-center justify-center">
                          <p className="inline-flex items-center gap-2 text-xs text-zinc-500">
                          <Clock3 className="h-3.5 w-3.5 animate-pulse" />
                          Đang cập nhật lịch...
                          </p>
                        </div>
                      ) : showtimesError ? (
                        <div className="flex flex-1 items-center"><p className="text-xs leading-5 text-rose-300">
                          Không thể tải lịch chiếu. Hãy mở lịch để kiểm tra.
                        </p></div>
                      ) : nextShowtime ? (
                        <div className="mt-3 min-w-0">
                          <div className="flex items-end justify-between gap-3">
                            <p className="flex items-baseline gap-2">
                              <span className="text-2xl font-black text-white">
                                {formatCinemaTime(nextShowtime.startTime, showtimeTimezone)}
                              </span>
                              <span className="text-[11px] text-zinc-500">
                                {nextShowtime.endTime
                                  ? `– ${formatCinemaTime(nextShowtime.endTime, showtimeTimezone)}`
                                  : ''}
                              </span>
                            </p>
                            <span className={`inline-flex items-center rounded-md border px-2 py-1 text-[10px] font-black ${
                              showtimeState === 'SHOWING'
                                ? 'border-brand-orange/30 bg-brand-orange/10 text-brand-orange'
                                : 'border-sky-500/30 bg-sky-500/10 text-sky-300'
                            }`}>
                              {showtimeState === 'SHOWING' ? 'Đang chiếu' : 'Sắp chiếu'}
                            </span>
                          </div>
                          <p className="mt-3 truncate text-sm font-black text-zinc-200" title={nextShowtime.movie?.title}>
                            {nextShowtime.movie?.title || 'Phim chưa xác định'}
                          </p>
                          <p className="mt-0.5 truncate text-[11px] text-zinc-500">
                            {nextShowtime.movieVersion?.format || nextShowtime.movieVersion?.versionName || 'Chưa rõ định dạng'}
                            {nextShowtime.movieVersion?.subtitleLanguage
                              ? ` · ${nextShowtime.movieVersion.subtitleLanguage}`
                              : ''}
                          </p>
                        </div>
                      ) : (
                        <div className="flex flex-1 flex-col items-center justify-center py-5 text-center">
                          <CalendarClock className="h-7 w-7 text-zinc-700" />
                          <p className="mt-2 text-xs text-zinc-500">Chưa có suất chiếu trong 7 ngày tới.</p>
                        </div>
                      )}
                      <button
                        type="button"
                        onClick={() => navigate(`/admin/showtimes?cinemaSlug=${encodeURIComponent(selectedCinema?.slug || '')}${showtimeDateKey ? `&date=${showtimeDateKey}` : ''}`)}
                        className="mt-auto inline-flex w-fit items-center gap-1 pt-3 text-[11px] font-black text-brand-orange hover:text-orange-300"
                      >
                        Xem lịch chiếu
                        <ExternalLink className="h-3 w-3" />
                      </button>
                      </section>
                    </div>

                    <footer className="grid grid-cols-2 gap-2 border-t border-zinc-800/80 bg-zinc-950/30 p-4">
                      <button
                        type="button"
                        onClick={() => navigate(`/admin/rooms/edit/${room.publicId}`)}
                        className={`${room.status === 'INACTIVE' ? 'col-span-2' : ''} inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-bold text-zinc-200 hover:border-brand-orange hover:text-brand-orange`}
                      >
                        <Settings2 className="h-4 w-4" />
                        Quản lý phòng
                      </button>
                      {room.status !== 'INACTIVE' && (
                        <button
                          type="button"
                          disabled={isMutating}
                          onClick={() => handlePauseRoom(room)}
                        className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-red-500/30 px-4 py-2.5 text-xs font-bold text-red-400 hover:bg-red-500/10 disabled:opacity-50"
                      >
                          <CirclePause className="h-4 w-4" />
                          Tạm ngừng
                        </button>
                      )}
                    </footer>
                  </article>
                );
              })}
            </div>
            {totalPages > 1 && (
              <div className="mt-5 flex items-center justify-between rounded-2xl border border-zinc-900 bg-zinc-900/20 p-4">
                <p className="text-xs text-zinc-500">
                  Trang {page + 1}/{totalPages} · {filteredRooms.length} phòng
                </p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={page === 0}
                    onClick={() => setPage((current) => Math.max(0, current - 1))}
                    className="inline-flex items-center gap-1 rounded-lg border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-300 disabled:opacity-30"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    Trước
                  </button>
                  <button
                    type="button"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                    className="inline-flex items-center gap-1 rounded-lg border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-300 disabled:opacity-30"
                  >
                    Sau
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
