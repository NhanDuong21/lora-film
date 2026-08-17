import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  CalendarClock,
  ChevronLeft,
  ChevronRight,
  Clock3,
  CirclePause,
  DoorOpen,
  Ellipsis,
  ExternalLink,
  LayoutGrid,
  PlusCircle,
  RefreshCw,
  Search,
  Wrench,
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
  SCREEN_TYPE_LABELS,
  SOUND_TYPE_LABELS,
} from '@/features/facilities/admin/utils/facilityPresentation';
import {
  addDaysToDateKey,
  getAuditoriumOperationalState,
  getShowtimeDateKeys,
} from '@/features/facilities/admin/utils/roomShowtimePresentation';

const PAGE_SIZE = 8;

const minutesBetween = (later, earlier) => Math.max(
  0,
  Math.ceil((new Date(later).getTime() - new Date(earlier).getTime()) / 60_000),
);

const formatRefreshAge = (updatedAt, now) => {
  if (!updatedAt) return 'Đang đồng bộ';
  const seconds = Math.max(0, Math.floor((now.getTime() - updatedAt.getTime()) / 1000));
  return seconds < 60 ? `${seconds} giây trước` : `${Math.floor(seconds / 60)} phút trước`;
};

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
  const [roomInsights, setRoomInsights] = useState({});
  const [openMenuId, setOpenMenuId] = useState(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
  const showtimeRequestId = useRef(0);

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
    const timer = window.setInterval(() => setNow(new Date()), 15_000);
    return () => window.clearInterval(timer);
  }, []);

  const selectedCinema = useMemo(
    () => cinemas.find(cinema => cinema.publicId === selectedCinemaId),
    [cinemas, selectedCinemaId],
  );

  const fetchUpcomingShowtimes = useCallback(async ({ silent = false } = {}) => {
      const requestId = ++showtimeRequestId.current;
      if (!selectedCinema?.slug) {
        setShowtimes([]);
        setShowtimesError(null);
        setIsShowtimesLoading(false);
        return;
      }

      if (!silent) setIsShowtimesLoading(true);
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
        if (requestId !== showtimeRequestId.current) return;

        const rows = responses.flatMap(response => (
          response?.success && Array.isArray(response.data?.data) ? response.data.data : []
        ));
        setShowtimes(rows);
        setLastUpdatedAt(new Date());
      } catch (requestError) {
        if (requestId !== showtimeRequestId.current) return;
        setShowtimes([]);
        setShowtimesError(
          requestError.response?.data?.message
            || requestError.message
            || 'Không thể tải lịch chiếu',
        );
      } finally {
        if (requestId === showtimeRequestId.current) setIsShowtimesLoading(false);
      }
  }, [selectedCinema]);

  useEffect(() => {
    // This effect synchronizes the selected cinema with its operational showtimes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchUpcomingShowtimes();
  }, [fetchUpcomingShowtimes]);

  const fetchRoomInsights = useCallback(async () => {
    if (!rooms.length) {
      setRoomInsights({});
      return;
    }
    const entries = await Promise.all(rooms.map(async (room) => {
      const [layoutResult, maintenanceResult] = await Promise.allSettled([
        adminRoomService.getAdminSeatLayout(room.publicId),
        adminRoomService.getMaintenanceWindows(room.publicId),
      ]);
      const layout = layoutResult.status === 'fulfilled' && layoutResult.value?.success
        ? layoutResult.value.data
        : null;
      const windows = maintenanceResult.status === 'fulfilled'
        && maintenanceResult.value?.success
        && Array.isArray(maintenanceResult.value.data)
        ? maintenanceResult.value.data
        : [];
      return [room.publicId, {
        maintenanceSeats: Number(layout?.maintenanceSeats || 0),
        activeSeats: Number(layout?.activeSeats || room.capacity || 0),
        totalSeats: Number(layout?.totalSeats || room.capacity || 0),
        windows,
      }];
    }));
    setRoomInsights(Object.fromEntries(entries));
    setLastUpdatedAt(new Date());
  }, [rooms]);

  useEffect(() => {
    // This effect synchronizes room cards with seat and maintenance facts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchRoomInsights();
  }, [fetchRoomInsights]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNow(new Date());
      void fetchUpcomingShowtimes({ silent: true });
      void fetchRoomInsights();
    }, 20_000);
    return () => window.clearInterval(timer);
  }, [fetchRoomInsights, fetchUpcomingShowtimes]);

  const roomViewModels = useMemo(() => rooms.map((room) => {
    const insight = roomInsights[room.publicId] || {};
    return {
      room,
      insight,
      readiness: getAuditoriumReadiness(room),
      operational: getAuditoriumOperationalState({
        room,
        showtimes,
        maintenanceWindows: insight.windows || [],
        lockedSeatCount: insight.maintenanceSeats || 0,
        now,
      }),
    };
  }), [now, roomInsights, rooms, showtimes]);

  const filteredRooms = useMemo(() => {
    const keyword = searchTerm.trim().toLocaleLowerCase('vi');
    return roomViewModels.filter(({ room, readiness, operational }) => {
      const roomName = room.name || room.auditoriumName || '';
      const matchesKeyword = !keyword || roomName.toLocaleLowerCase('vi').includes(keyword);
      const matchesStatus = statusFilter === 'ALL'
        || (statusFilter === 'READY' && readiness.canServe)
        || (statusFilter === 'ATTENTION' && operational.priority <= 1)
        || operational.key === statusFilter
        || room.status === statusFilter;
      return matchesKeyword && matchesStatus;
    }).sort((left, right) => (
      left.operational.priority - right.operational.priority
      || new Date(left.operational.nextShowtime?.startTime || '9999-12-31').getTime()
        - new Date(right.operational.nextShowtime?.startTime || '9999-12-31').getTime()
      || (left.room.name || '').localeCompare(right.room.name || '', 'vi')
    ));
  }, [roomViewModels, searchTerm, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredRooms.length / PAGE_SIZE));
  const visibleRooms = filteredRooms.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const roomSummary = useMemo(() => ({
    ready: roomViewModels.filter(item => item.readiness.canServe).length,
    attention: roomViewModels.filter(item => item.operational.priority <= 1).length,
    seats: roomViewModels.reduce((total, item) => total + Number(item.room.capacity || 0), 0),
  }), [roomViewModels]);

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
    setIsMutating(true);
    try {
      const previewResponse = await adminRoomService.previewMaintenanceImpact(room.publicId, {
        startTime: new Date().toISOString(),
        endTime: new Date(Date.now() + 365 * 24 * 60 * 60_000).toISOString(),
        reason: 'Kiểm tra ảnh hưởng trước khi tạm ngừng phòng',
        maintenanceType: 'EMERGENCY',
      });
      const impact = previewResponse?.data;
      if (!previewResponse?.success || !impact) throw new Error('Không thể kiểm tra phạm vi ảnh hưởng');
      if (impact.affectedShowtimeCount > 0 || !impact.bookingDataComplete) {
        const viewAffected = await triggerConfirm?.({
          title: `Chưa thể tạm ngừng ${roomName}`,
          message: `Phòng hiện có ${impact.affectedShowtimeCount} suất chiếu tương lai, ${impact.openForBookingCount || 0} suất đang mở bán và ${impact.occupiedSeatCount || 0} ghế đang được giữ hoặc đã bán. Không thể tạm ngừng trực tiếp khi còn suất bị ảnh hưởng.`,
          confirmLabel: 'Xem các suất bị ảnh hưởng',
          cancelLabel: 'Để sau',
          tone: 'warning',
        });
        if (viewAffected) navigate(`/admin/showtimes?cinemaSlug=${encodeURIComponent(selectedCinema?.slug || '')}`);
        return;
      }

      const confirmed = await triggerConfirm?.({
      title: `Tạm ngừng ${roomName}?`,
      message: 'Đã kiểm tra: phòng không có suất chiếu hoặc ghế bán bị ảnh hưởng. Phòng sẽ ngừng tham gia xếp lịch cho đến khi được mở lại.',
      confirmLabel: 'Tạm ngừng phòng',
      cancelLabel: 'Giữ phòng hoạt động',
      tone: 'danger',
      });
      if (!confirmed) return;

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
      setOpenMenuId(null);
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
            <option value="READY">Cấu hình sẵn sàng</option>
            <option value="ATTENTION">Cần chú ý</option>
            <option value="IN_SHOW">Đang chiếu</option>
            <option value="CLEANING">Đang dọn phòng</option>
            <option value="UPCOMING">Sắp chiếu</option>
            <option value="IDLE">Đang trống</option>
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
                <div className="mt-2 flex items-center gap-2 text-[11px] text-zinc-500">
                  <span>Cập nhật {formatRefreshAge(lastUpdatedAt, now)} · Tự động làm mới</span>
                  <button
                    type="button"
                    aria-label="Làm mới trạng thái phòng"
                    onClick={() => {
                      void fetchUpcomingShowtimes();
                      void fetchRoomInsights();
                    }}
                    className="rounded-lg border border-zinc-800 p-1.5 text-zinc-400 hover:border-zinc-700 hover:text-white"
                  >
                    <RefreshCw className={`h-3.5 w-3.5 ${isShowtimesLoading ? 'animate-spin' : ''}`} />
                  </button>
                </div>
              </div>
              <div className="flex flex-wrap gap-2 text-[11px] font-bold">
                <button type="button" onClick={() => { setStatusFilter('ALL'); setPage(0); }} className={`rounded-full border px-3 py-1.5 ${statusFilter === 'ALL' ? 'border-white/30 bg-white/10 text-white' : 'border-zinc-800 bg-zinc-900/60 text-zinc-300'}`}>
                  {rooms.length} phòng
                </button>
                <button type="button" onClick={() => { setStatusFilter('READY'); setPage(0); }} className={`rounded-full border px-3 py-1.5 ${statusFilter === 'READY' ? 'border-emerald-400/40 bg-emerald-500/20' : 'border-emerald-500/20 bg-emerald-500/10'} text-emerald-300`}>
                  {roomSummary.ready} sẵn sàng
                </button>
                {roomSummary.attention > 0 && (
                  <button type="button" onClick={() => { setStatusFilter('ATTENTION'); setPage(0); }} className={`rounded-full border px-3 py-1.5 ${statusFilter === 'ATTENTION' ? 'border-amber-400/50 bg-amber-500/20' : 'border-amber-500/25 bg-amber-500/10'} text-amber-300`}>
                    {roomSummary.attention} cần chú ý
                  </button>
                )}
                <span className="rounded-full border border-sky-500/20 bg-sky-500/10 px-3 py-1.5 text-sky-300">
                  {roomSummary.seats} chỗ
                </span>
              </div>
            </div>

            <div className="grid gap-4 xl:grid-cols-2 2xl:grid-cols-3">
              {visibleRooms.map(({ room, readiness, operational, insight }) => {
                const roomName = room.name || room.auditoriumName || 'Phòng chiếu';
                const nextShowtime = operational.nextShowtime;
                const currentShowtime = operational.currentShowtime;
                const showtimeTimezone = nextShowtime?.cinema?.timezone
                  || currentShowtime?.cinema?.timezone
                  || selectedCinema?.timezone;
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
                const openDetail = (tab = 'overview') => navigate(
                  `/admin/rooms/edit/${room.publicId}${tab === 'overview' ? '' : `?tab=${tab}`}`,
                  { state: { breadcrumbLabel: roomName } },
                );
                let currentMessage = 'Không có hoạt động đang diễn ra.';
                if (operational.key === 'IN_SHOW') {
                  currentMessage = `Còn ${minutesBetween(currentShowtime.endTime, now)} phút`;
                } else if (operational.key === 'CLEANING') {
                  currentMessage = `Dự kiến hoàn tất lúc ${formatCinemaTime(operational.cleaningUntil, showtimeTimezone)}`;
                } else if (operational.key === 'UPCOMING') {
                  currentMessage = `Bắt đầu sau ${minutesBetween(nextShowtime.startTime, now)} phút`;
                } else if (operational.key === 'MAINTENANCE') {
                  currentMessage = operational.activeMaintenance?.reason || 'Phòng đang ngừng phục vụ để bảo trì.';
                } else if (operational.key === 'ATTENTION') {
                  currentMessage = `${operational.lockedSeatCount} ghế đang bảo trì`;
                } else if (operational.key === 'SETUP') {
                  currentMessage = 'Cần hoàn thiện cấu hình trước khi mở phòng.';
                } else if (operational.key === 'SUSPENDED') {
                  currentMessage = 'Phòng đã được tạm ngừng khỏi lịch vận hành.';
                }
                return (
                  <article
                    key={room.publicId}
                    aria-labelledby={`room-${room.publicId}-title`}
                    tabIndex={0}
                    onClick={() => openDetail()}
                    onKeyDown={(event) => { if (event.key === 'Enter') openDetail(); }}
                    className="group relative flex min-h-full cursor-pointer flex-col rounded-3xl border border-zinc-800/80 bg-zinc-900/30 transition-all hover:-translate-y-0.5 hover:border-zinc-700 hover:bg-zinc-900/50 hover:shadow-2xl hover:shadow-black/20 focus:outline-none focus:ring-2 focus:ring-brand-orange/40"
                  >
                    <header className="flex items-start justify-between gap-3 border-b border-zinc-800/70 p-4">
                      <div className="flex min-w-0 items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-brand-orange/25 bg-brand-orange/10 transition-colors group-hover:bg-brand-orange/15">
                          <DoorOpen className="h-5 w-5 text-brand-orange" />
                        </div>
                        <div className="min-w-0">
                          <h3 id={`room-${room.publicId}-title`} className="truncate font-black text-white" title={roomName}>{roomName}</h3>
                          <p className="mt-1 truncate text-xs text-zinc-400">
                            {SCREEN_TYPE_LABELS[room.screenType] || 'Chưa rõ màn hình'}
                            {' · '}
                            {SOUND_TYPE_LABELS[room.soundType] || 'Chưa rõ âm thanh'}
                          </p>
                        </div>
                      </div>
                      <span className={`shrink-0 rounded-lg border px-2.5 py-1.5 text-[10px] font-black uppercase ${operational.className}`}>
                        {operational.label}
                      </span>
                    </header>

                    <div className="flex flex-1 flex-col gap-3 p-4">
                      <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                        <p className="font-bold text-zinc-300">
                          {Number(room.capacity || insight.totalSeats || 0)} chỗ
                          <span className="mx-2 text-zinc-700">·</span>
                          <span className={operational.lockedSeatCount > 0 ? 'text-amber-300' : 'text-zinc-500'}>
                            {operational.lockedSeatCount} ghế tạm khóa
                          </span>
                        </p>
                        <span className={`text-[10px] font-bold ${readiness.canServe ? 'text-emerald-400' : 'text-zinc-500'}`}>
                          Cấu hình: {readiness.canServe ? 'Sẵn sàng' : 'Chưa sẵn sàng'}
                        </span>
                      </div>

                      <section className="rounded-2xl bg-black/25 p-4" aria-label={`Trạng thái hiện tại của ${roomName}`}>
                        <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Hiện tại</p>
                        <div className="mt-2 flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="text-base font-black uppercase text-white">{operational.label}</p>
                            {currentShowtime && (
                              <p className="mt-1 truncate text-xs font-bold text-zinc-300">{currentShowtime.movie?.title || 'Phim chưa xác định'}</p>
                            )}
                            <p className="mt-1 text-xs text-zinc-400">{currentMessage}</p>
                          </div>
                          {operational.key === 'ATTENTION' && <AlertTriangle className="h-5 w-5 shrink-0 text-amber-300" />}
                          {operational.key === 'MAINTENANCE' && <Wrench className="h-5 w-5 shrink-0 text-amber-300" />}
                        </div>
                      </section>

                      <section className="rounded-2xl bg-black/20 p-4" aria-label={`Suất chiếu kế tiếp của ${roomName}`}>
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
                        <div className="py-4 text-center">
                          <p className="inline-flex items-center gap-2 text-xs text-zinc-500">
                          <Clock3 className="h-3.5 w-3.5 animate-pulse" />
                          Đang cập nhật lịch...
                          </p>
                        </div>
                      ) : showtimesError ? (
                        <div className="py-3"><p className="text-xs leading-5 text-rose-300">
                          Không thể tải lịch chiếu. Hãy mở lịch để kiểm tra.
                        </p></div>
                      ) : nextShowtime ? (
                        <div className="mt-3 min-w-0">
                          <div className="flex items-end justify-between gap-3">
                            <p className="flex items-baseline gap-2">
                              <span className="text-xl font-black text-white">
                                {formatCinemaTime(nextShowtime.startTime, showtimeTimezone)}
                              </span>
                              <span className="text-[11px] text-zinc-500">
                                {nextShowtime.endTime
                                  ? `– ${formatCinemaTime(nextShowtime.endTime, showtimeTimezone)}`
                                  : ''}
                              </span>
                            </p>
                            <span className="text-[11px] font-bold text-sky-300">
                              Bắt đầu sau {minutesBetween(nextShowtime.startTime, now)} phút
                            </span>
                          </div>
                          <p className="mt-2 truncate text-sm font-black text-zinc-200" title={nextShowtime.movie?.title}>
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
                        <div className="py-4 text-center">
                          <p className="text-xs text-zinc-500">Chưa có suất chiếu trong 7 ngày tới.</p>
                        </div>
                      )}
                      <button
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation();
                          navigate(`/admin/showtimes?cinemaSlug=${encodeURIComponent(selectedCinema?.slug || '')}${showtimeDateKey ? `&date=${showtimeDateKey}` : ''}`);
                        }}
                        className="inline-flex w-fit items-center gap-1 pt-2 text-[11px] font-black text-brand-orange hover:text-orange-300"
                      >
                        Xem lịch chiếu
                        <ExternalLink className="h-3 w-3" />
                      </button>
                      </section>
                    </div>

                    <footer className="grid grid-cols-[1fr_auto] gap-2 border-t border-zinc-800/70 bg-zinc-950/30 p-3">
                      <button
                        type="button"
                        onClick={(event) => { event.stopPropagation(); openDetail(); }}
                        className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-bold text-zinc-200 hover:border-brand-orange hover:text-brand-orange"
                      >
                        Xem chi tiết
                      </button>
                      <div className="relative">
                        <button
                          type="button"
                          aria-label={`Mở menu tác vụ của ${roomName}`}
                          aria-expanded={openMenuId === room.publicId}
                          onClick={(event) => {
                            event.stopPropagation();
                            setOpenMenuId(current => current === room.publicId ? null : room.publicId);
                          }}
                          className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-zinc-700 bg-zinc-900 text-zinc-300 hover:border-brand-orange hover:text-brand-orange"
                        >
                          <Ellipsis className="h-5 w-5" />
                        </button>
                        {openMenuId === room.publicId && (
                          <div className="absolute bottom-12 right-0 z-20 w-52 overflow-hidden rounded-xl border border-zinc-700 bg-zinc-950 p-1.5 shadow-2xl">
                            <button type="button" onClick={(event) => { event.stopPropagation(); openDetail('seat-layout'); }} className="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-left text-xs font-bold text-zinc-300 hover:bg-zinc-800"><LayoutGrid className="h-4 w-4" /> Xem sơ đồ ghế</button>
                            <button type="button" onClick={(event) => { event.stopPropagation(); openDetail('maintenance'); }} className="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-left text-xs font-bold text-zinc-300 hover:bg-zinc-800"><Wrench className="h-4 w-4" /> Lập lịch bảo trì</button>
                            {room.status !== 'INACTIVE' && (
                              <button type="button" disabled={isMutating} onClick={(event) => { event.stopPropagation(); void handlePauseRoom(room); }} className="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-left text-xs font-bold text-red-300 hover:bg-red-500/10 disabled:opacity-50"><CirclePause className="h-4 w-4" /> Tạm ngừng phòng</button>
                            )}
                          </div>
                        )}
                      </div>
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
