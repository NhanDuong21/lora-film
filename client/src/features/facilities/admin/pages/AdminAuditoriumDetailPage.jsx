import { useEffect } from 'react';
import { useNavigate, useOutletContext, useParams, useSearchParams } from 'react-router-dom';
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

const TABS = [
  { id: 'overview', label: 'Tổng quan & tác vụ', icon: Settings },
  { id: 'seat-layout', label: 'Sơ đồ ghế', icon: LayoutGrid },
  { id: 'maintenance', label: 'Đóng phòng & bảo trì', icon: Wrench },
];

export default function AdminAuditoriumDetailPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { triggerToast } = useOutletContext() || {};
  const {
    auditorium,
    isLoading,
    error,
    fetchAuditorium,
    updateAuditoriumBasicInfo,
    changeAuditoriumStatus,
    updateSeatLayout,
  } = useAuditoriumDetail(roomId, triggerToast);
  const requestedTab = searchParams.get('tab');
  const activeTab = TABS.some(tab => tab.id === requestedTab) ? requestedTab : 'overview';

  useEffect(() => {
    fetchAuditorium();
  }, [fetchAuditorium]);

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
          <button
            type="button"
            onClick={fetchAuditorium}
            className="flex items-center gap-2 self-start rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2.5 text-xs font-bold transition-colors hover:bg-zinc-800 md:self-auto"
          >
            <RefreshCw className={`h-4 w-4 text-brand-orange ${isLoading ? 'animate-spin' : ''}`} />
            Làm mới dữ liệu
          </button>
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
              onUpdate={updateAuditoriumBasicInfo}
              onChangeStatus={changeAuditoriumStatus}
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
          />
        )}

        {activeTab === 'maintenance' && (
          <div className="h-full overflow-auto p-6 md:p-8">
            <AuditoriumMaintenanceTab
              roomId={roomId}
              auditorium={auditorium}
              triggerToast={triggerToast}
            />
          </div>
        )}
      </main>
    </div>
  );
}
