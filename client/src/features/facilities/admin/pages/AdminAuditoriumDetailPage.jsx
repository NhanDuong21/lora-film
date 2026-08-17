import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate, useOutletContext, useParams, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  LayoutGrid,
  RefreshCw,
  Settings,
  Wrench,
} from 'lucide-react';
import useAuditoriumDetail from '../hooks/useAuditoriumDetail';
import { ErrorState, LoadingState } from '@/components/common/ui/uiKit';
import { getAuditoriumStatus } from '../utils/facilityPresentation';
import AuditoriumOverviewTab from './auditorium/AuditoriumOverviewTab';
import AuditoriumMaintenanceTab from './auditorium/AuditoriumMaintenanceTab';
import AuditoriumSeatLayoutTab from './auditorium/AuditoriumSeatLayoutTab';
import adminRoomService from '../services/adminRoomService';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import {
  getAuditoriumOperationalState,
  getShowtimeDateKeys,
} from '@/features/facilities/admin/utils/roomShowtimePresentation';

const TABS = [
  { id: 'overview', label: 'Tổng quan & tác vụ', icon: Settings },
  { id: 'seat-layout', label: 'Sơ đồ ghế', icon: LayoutGrid },
  { id: 'maintenance', label: 'Đóng phòng & bảo trì', icon: Wrench },
];

export default function AdminAuditoriumDetailPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { triggerToast } = useOutletContext() || {};
  const {
    auditorium,
    isLoading,
    error,
    lastUpdatedAt,
    fetchAuditorium,
    updateAuditoriumBasicInfo,
    changeAuditoriumStatus,
    updateSeatLayout,
  } = useAuditoriumDetail(roomId, triggerToast);
  const [showtimes, setShowtimes] = useState([]);
  const [maintenanceWindows, setMaintenanceWindows] = useState([]);
  const [now, setNow] = useState(() => new Date());
  const cinemaSlug = auditorium?.cinemaSlug;
  const cinemaTimezone = auditorium?.cinemaTimezone;
  const requestedTab = searchParams.get('tab');
  const activeTab = TABS.some(tab => tab.id === requestedTab) ? requestedTab : 'overview';

  useEffect(() => {
    fetchAuditorium();
  }, [fetchAuditorium]);

  useEffect(() => {
    if (!auditorium?.auditoriumName || location.state?.breadcrumbLabel === auditorium.auditoriumName) return;
    navigate(`${location.pathname}${location.search}`, {
      replace: true,
      state: { ...(location.state || {}), breadcrumbLabel: auditorium.auditoriumName },
    });
  }, [auditorium?.auditoriumName, location.pathname, location.search, location.state, navigate]);

  const fetchOperationalContext = useCallback(async () => {
    if (!cinemaSlug) return;
    try {
      const dateKeys = getShowtimeDateKeys(new Date(), cinemaTimezone, 7);
      const [showtimeResponses, maintenanceResponse] = await Promise.all([
        Promise.all(dateKeys.map(date => adminShowtimeService.getShowtimes({
          cinemaSlug,
          date,
          page: 0,
          size: 100,
        }))),
        adminRoomService.getMaintenanceWindows(roomId),
      ]);
      setShowtimes(showtimeResponses.flatMap(response => (
        response?.success && Array.isArray(response.data?.data) ? response.data.data : []
      )));
      setMaintenanceWindows(
        maintenanceResponse?.success && Array.isArray(maintenanceResponse.data)
          ? maintenanceResponse.data
          : [],
      );
      setNow(new Date());
    } catch {
      // The base room detail remains usable when operational facts are temporarily unavailable.
    }
  }, [cinemaSlug, cinemaTimezone, roomId]);

  useEffect(() => {
    // Synchronize the detail page with showtime and maintenance facts.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchOperationalContext();
  }, [fetchOperationalContext]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNow(new Date());
      void fetchAuditorium({ silent: true });
      void fetchOperationalContext();
    }, 20_000);
    return () => window.clearInterval(timer);
  }, [fetchAuditorium, fetchOperationalContext]);

  const openTab = tabId => {
    const next = new URLSearchParams(searchParams);
    next.set('tab', tabId);
    setSearchParams(next, { replace: true });
  };

  if (isLoading && !auditorium) {
    return (
      <div className="flex min-h-[480px] flex-1 items-center justify-center bg-zinc-950 p-8">
        <LoadingState message="Đang tải thông tin phòng chiếu..." />
      </div>
    );
  }

  if (error && !auditorium) {
    return (
      <div className="flex min-h-[480px] flex-1 items-center justify-center bg-zinc-950 p-8">
        <ErrorState message={error} onRetry={fetchAuditorium} />
      </div>
    );
  }

  if (!auditorium) return null;
  const status = getAuditoriumStatus(auditorium.auditoriumStatus);
  const operationalState = getAuditoriumOperationalState({
    room: auditorium,
    showtimes,
    maintenanceWindows,
    lockedSeatCount: auditorium.maintenanceSeats,
    now,
  });
  const refreshAge = lastUpdatedAt
    ? Math.max(0, Math.floor((now.getTime() - lastUpdatedAt.getTime()) / 1000))
    : 0;

  return (
    <div className="flex h-full flex-1 flex-col overflow-hidden bg-zinc-950 text-white">
      <header className="sticky top-0 z-10 shrink-0 border-b border-zinc-900 bg-zinc-950/95 px-6 py-5 backdrop-blur-md md:px-8">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div className="flex items-start gap-4">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="flex items-center gap-2 rounded-xl border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-300 hover:border-brand-orange hover:text-white"
            >
              <ArrowLeft className="h-4 w-4" />
              Quay lại
            </button>
            <div>
              <div className="flex flex-wrap items-center gap-3">
                <h1 className="text-xl font-black uppercase tracking-wider text-zinc-50 md:text-2xl">
                  {auditorium.auditoriumName}
                </h1>
                <span className={`rounded-lg border px-2.5 py-1 text-[10px] font-black uppercase tracking-wider ${status.className}`}>
                  {status.label}
                </span>
              </div>
              <p className="mt-1 text-xs text-zinc-500">
                Cấu hình, kiểm tra mức sẵn sàng và thực hiện tác vụ vận hành phòng.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 self-start text-[11px] text-zinc-500 md:self-auto">
            <span>Cập nhật {refreshAge < 60 ? `${refreshAge} giây` : `${Math.floor(refreshAge / 60)} phút`} trước · Tự động làm mới</span>
            <button
              type="button"
              aria-label="Làm mới dữ liệu phòng"
              onClick={() => { void fetchAuditorium(); void fetchOperationalContext(); }}
              className="rounded-xl border border-zinc-800 bg-zinc-900 p-2.5 text-zinc-400 transition-colors hover:bg-zinc-800 hover:text-white"
            >
              <RefreshCw className={`h-4 w-4 text-brand-orange ${isLoading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        <nav className="mt-6 flex gap-2 overflow-x-auto">
          {TABS.map((tab) => (
            <button
              type="button"
              key={tab.id}
              onClick={() => openTab(tab.id)}
              className={`flex items-center gap-2 whitespace-nowrap border-b-2 px-4 py-3 text-xs font-black uppercase tracking-wider transition-all ${
                activeTab === tab.id
                  ? 'border-brand-orange text-brand-orange'
                  : 'border-transparent text-zinc-500 hover:text-zinc-300'
              }`}
            >
              <tab.icon className="h-4 w-4" />
              {tab.label}
            </button>
          ))}
        </nav>
      </header>

      <main className="relative flex-1 overflow-hidden">
        {activeTab === 'overview' && (
          <div className="h-full overflow-auto p-6 md:p-8">
            <AuditoriumOverviewTab
              key={[
                auditorium.publicId,
                auditorium.auditoriumName,
                auditorium.auditoriumStatus,
                auditorium.screenType,
                auditorium.soundType,
                auditorium.cleaningBufferMinutes,
              ].join('-')}
              auditorium={auditorium}
              operationalState={operationalState}
              maintenanceWindows={maintenanceWindows}
              onUpdate={updateAuditoriumBasicInfo}
              onChangeStatus={changeAuditoriumStatus}
              onOpenMaintenance={() => openTab('maintenance')}
            />
          </div>
        )}

        {activeTab === 'seat-layout' && (
          <AuditoriumSeatLayoutTab
            auditorium={auditorium}
            roomId={roomId}
            onUpdateBasicInfo={updateAuditoriumBasicInfo}
            onUpdateSeats={updateSeatLayout}
            triggerToast={triggerToast}
            futureShowtimeCount={showtimes.filter(item => (
              item?.auditorium?.publicId === roomId && new Date(item.startTime) > now
            )).length}
          />
        )}

        {activeTab === 'maintenance' && (
          <div className="h-full overflow-auto p-6 md:p-8">
            <AuditoriumMaintenanceTab
              roomId={roomId}
              auditorium={auditorium}
              lockedSeatCount={auditorium.maintenanceSeats || 0}
              operationalState={operationalState}
              triggerToast={triggerToast}
            />
          </div>
        )}
      </main>
    </div>
  );
}
