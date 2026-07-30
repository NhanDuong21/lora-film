import { useEffect, useState } from 'react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import {
  Activity,
  ArrowLeft,
  Building2,
  CalendarX2,
  Clock,
  Film,
  Image as ImageIcon,
  RefreshCw,
} from 'lucide-react';
import { ErrorState, LoadingState } from '@/components/common/ui/uiKit';
import useAdminCinemaDetail from '../hooks/useAdminCinemaDetail';
import { getCinemaStatus } from '../utils/facilityPresentation';
import CinemaHealthOverviewTab from './cinema/CinemaHealthOverviewTab';
import CinemaOverviewTab from './cinema/CinemaOverviewTab';
import CinemaMediaTab from './cinema/CinemaMediaTab';
import CinemaOperatingHoursTab from './cinema/CinemaOperatingHoursTab';
import CinemaClosurePeriodsTab from './cinema/CinemaClosurePeriodsTab';
import CinemaAuditoriumsTab from './cinema/CinemaAuditoriumsTab';

const TABS = [
  { id: 'health', label: 'Tổng quan vận hành', icon: Activity },
  { id: 'overview', label: 'Thông tin & vị trí', icon: Building2 },
  { id: 'operating-hours', label: 'Giờ mở cửa', icon: Clock },
  { id: 'media', label: 'Hình ảnh', icon: ImageIcon },
  { id: 'auditoriums', label: 'Phòng chiếu', icon: Film },
  { id: 'closure-periods', label: 'Đóng cửa & bảo trì', icon: CalendarX2 },
];

export default function AdminCinemaDetailPage() {
  const { cinemaPublicId } = useParams();
  const navigate = useNavigate();
  const { triggerToast } = useOutletContext() || {};
  const [activeTab, setActiveTab] = useState('health');
  const {
    cinema,
    isLoading,
    error,
    fetchCinema,
    updateCinemaBasicInfo,
    changeCinemaStatus,
    updateOperatingHours,
    addMedia,
    updateMedia,
    archiveMedia,
    reorderMedia,
  } = useAdminCinemaDetail(cinemaPublicId, triggerToast);

  useEffect(() => {
    fetchCinema();
  }, [fetchCinema]);

  if (isLoading && !cinema) {
    return <LoadingState message="Đang tải trung tâm vận hành cụm rạp..." />;
  }
  if (error && !cinema) {
    return <ErrorState message={error} onRetry={fetchCinema} />;
  }
  if (!cinema) return null;

  const status = getCinemaStatus(cinema.status);

  return (
    <div className="flex h-full flex-1 flex-col overflow-hidden bg-zinc-950 text-white">
      <header className="sticky top-0 z-10 border-b border-zinc-900 bg-zinc-950/90 px-6 py-5 backdrop-blur md:px-8">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div>
            <button
              type="button"
              onClick={() => navigate('/admin/cinemas')}
              className="mb-4 inline-flex items-center gap-2 text-xs font-bold text-zinc-400 transition hover:text-white"
            >
              <ArrowLeft className="h-4 w-4" />
              Danh sách cụm rạp
            </button>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-xl font-black uppercase tracking-wider text-zinc-50 md:text-2xl">
                {cinema.name}
              </h1>
              <span className={`rounded-lg border px-2.5 py-1 text-[10px] font-black ${status.className}`}>
                {status.label}
              </span>
            </div>
            <p className="mt-2 text-xs text-zinc-400">{status.description}</p>
          </div>
          <button
            type="button"
            onClick={fetchCinema}
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-2.5 text-xs font-bold transition hover:bg-zinc-800"
          >
            <RefreshCw className="h-4 w-4 text-orange-400" />
            Làm mới dữ liệu
          </button>
        </div>

        <nav className="mt-6 flex gap-2 overflow-x-auto">
          {TABS.map((tab) => (
            <button
              type="button"
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex shrink-0 items-center gap-2 rounded-xl px-4 py-3 text-xs font-black transition ${
                activeTab === tab.id
                  ? 'bg-orange-500 text-white'
                  : 'text-zinc-500 hover:bg-zinc-900 hover:text-zinc-200'
              }`}
            >
              <tab.icon className="h-4 w-4" />
              {tab.label}
            </button>
          ))}
        </nav>
      </header>

      <main className="flex-1 overflow-auto p-6 md:p-8">
        {activeTab === 'health' && (
          <CinemaHealthOverviewTab
            cinema={cinema}
            onOpenTab={setActiveTab}
            onStatusChange={changeCinemaStatus}
          />
        )}
        {activeTab === 'overview' && (
          <CinemaOverviewTab
            key={[
              cinema.name,
              cinema.address,
              cinema.city,
              cinema.district,
              cinema.hotline,
              cinema.description,
            ].join('|')}
            cinema={cinema}
            onUpdate={updateCinemaBasicInfo}
          />
        )}
        {activeTab === 'operating-hours' && (
          <CinemaOperatingHoursTab
            key={(cinema.operatingHours || [])
              .map((item) => `${item.dayOfWeek}:${item.openTime}:${item.closeTime}:${item.isClosed}`)
              .join('|')}
            cinema={cinema}
            onUpdate={updateOperatingHours}
            triggerToast={triggerToast}
          />
        )}
        {activeTab === 'media' && (
          <CinemaMediaTab
            key={(cinema.gallery || [])
              .map((media) => `${media.publicId}:${media.displayOrder}:${media.status}`)
              .join('|')}
            cinema={cinema}
            onAdd={addMedia}
            onUpdate={updateMedia}
            onArchive={archiveMedia}
            onReorder={reorderMedia}
          />
        )}
        {activeTab === 'auditoriums' && (
          <CinemaAuditoriumsTab
            cinema={cinema}
            triggerToast={triggerToast}
            onRefresh={fetchCinema}
          />
        )}
        {activeTab === 'closure-periods' && (
          <CinemaClosurePeriodsTab
            cinema={cinema}
            triggerToast={triggerToast}
            onOpenRooms={() => setActiveTab('auditoriums')}
          />
        )}
      </main>
    </div>
  );
}
