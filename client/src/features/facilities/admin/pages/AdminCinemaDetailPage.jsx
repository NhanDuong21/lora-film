import { useEffect } from 'react';
import { useNavigate, useOutletContext, useParams, useSearchParams } from 'react-router-dom';
import {
  Activity,
  ArrowLeft,
  Building2,
  Clock,
  Film,
  Image as ImageIcon,
  RefreshCw,
  Settings2,
  Wrench,
  History,
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
  { id: 'health', label: 'Tổng quan', icon: Activity },
  { id: 'setup', label: 'Thiết lập', icon: Settings2 },
  { id: 'auditoriums', label: 'Phòng chiếu', icon: Film },
  { id: 'availability', label: 'Khả dụng & bảo trì', icon: Wrench },
  { id: 'history', label: 'Lịch sử', icon: History },
];

const SETUP_SECTIONS = [
  { id: 'overview', label: 'Thông tin & vị trí', icon: Building2 },
  { id: 'operating-hours', label: 'Giờ hoạt động', icon: Clock },
  { id: 'media', label: 'Hình ảnh', icon: ImageIcon },
];

export default function AdminCinemaDetailPage() {
  const { cinemaPublicId } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { triggerToast } = useOutletContext() || {};
  const requestedTab = searchParams.get('tab');
  const legacySetupSection = SETUP_SECTIONS.some(section => section.id === requestedTab)
    ? requestedTab
    : null;
  const activeTab = legacySetupSection
    ? 'setup'
    : requestedTab === 'closure-periods'
      ? 'availability'
      : TABS.some(tab => tab.id === requestedTab) ? requestedTab : 'health';
  const requestedSection = searchParams.get('section');
  const setupSection = legacySetupSection
    || (SETUP_SECTIONS.some(section => section.id === requestedSection) ? requestedSection : 'overview');
  const {
    cinema,
    readiness,
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

  const openTab = tabId => {
    const next = new URLSearchParams(searchParams);
    if (SETUP_SECTIONS.some(section => section.id === tabId)) {
      next.set('tab', 'setup');
      next.set('section', tabId);
    } else {
      next.set('tab', tabId);
      if (tabId !== 'setup') next.delete('section');
    }
    setSearchParams(next, { replace: true });
  };

  const publicProfileCompleted = readiness?.publicProfileChecks?.filter(check => check.complete).length || 0;
  const publicProfileTotal = readiness?.publicProfileChecks?.length || 0;

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
            <div className="mt-4 flex flex-wrap gap-2 text-[10px] font-black uppercase tracking-wider">
              <StatusPill label="Cấu hình" value={status.label} tone="brand" />
              <StatusPill
                label="Sẵn sàng"
                value={readiness ? (readiness.readyForActivation ? 'Đạt' : `Còn ${readiness.totalOperationalChecks - readiness.completedOperationalChecks} blocker`) : 'Chưa tải được'}
                tone={readiness?.readyForActivation ? 'success' : 'warning'}
              />
              <StatusPill
                label="Hồ sơ khách"
                value={`${publicProfileCompleted}/${publicProfileTotal}`}
                tone={publicProfileCompleted === publicProfileTotal && publicProfileTotal > 0 ? 'success' : 'info'}
              />
              <StatusPill label="Công khai" value="Chưa tách trạng thái" tone="muted" />
              <StatusPill label="Hôm nay" value="Chưa có API khả dụng" tone="muted" />
            </div>
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
              onClick={() => openTab(tab.id)}
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
            readiness={readiness}
            onOpenTab={openTab}
            onStatusChange={changeCinemaStatus}
          />
        )}
        {activeTab === 'setup' && (
          <div className="space-y-6">
            <nav className="flex gap-2 overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900/40 p-2">
              {SETUP_SECTIONS.map(section => (
                <button
                  type="button"
                  key={section.id}
                  onClick={() => openTab(section.id)}
                  className={`inline-flex shrink-0 items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-bold transition ${
                    setupSection === section.id
                      ? 'bg-zinc-800 text-white'
                      : 'text-zinc-500 hover:text-zinc-200'
                  }`}
                >
                  <section.icon className="h-4 w-4" />
                  {section.label}
                </button>
              ))}
            </nav>
            {setupSection === 'overview' && (
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
            {setupSection === 'operating-hours' && (
              <CinemaOperatingHoursTab
                key={(cinema.operatingHours || [])
                  .map((item) => `${item.dayOfWeek}:${item.openTime}:${item.closeTime}:${item.isClosed}`)
                  .join('|')}
                cinema={cinema}
                onUpdate={updateOperatingHours}
                triggerToast={triggerToast}
              />
            )}
            {setupSection === 'media' && (
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
          </div>
        )}
        {activeTab === 'auditoriums' && (
          <CinemaAuditoriumsTab
            cinema={cinema}
            triggerToast={triggerToast}
            onRefresh={fetchCinema}
          />
        )}
        {activeTab === 'availability' && (
          <CinemaClosurePeriodsTab
            cinema={cinema}
            triggerToast={triggerToast}
            onOpenRooms={() => openTab('auditoriums')}
          />
        )}
        {activeTab === 'history' && (
          <section className="rounded-3xl border border-zinc-800 bg-zinc-900/30 p-8">
            <History className="h-6 w-6 text-zinc-500" />
            <h2 className="mt-4 text-lg font-black text-white">Lịch sử thay đổi facility</h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
              Backend chưa cung cấp audit timeline cho thay đổi cấu hình, trạng thái, closure và maintenance.
              Màn hình không dựng dữ liệu giả; lịch sử sẽ xuất hiện khi có endpoint authoritative.
            </p>
          </section>
        )}
      </main>
    </div>
  );
}

function StatusPill({ label, value, tone }) {
  const tones = {
    brand: 'border-orange-500/20 bg-orange-500/5 text-orange-200',
    success: 'border-emerald-500/20 bg-emerald-500/5 text-emerald-200',
    warning: 'border-amber-500/20 bg-amber-500/5 text-amber-200',
    info: 'border-sky-500/20 bg-sky-500/5 text-sky-200',
    muted: 'border-zinc-800 bg-zinc-900/60 text-zinc-400',
  };
  return (
    <span className={`rounded-lg border px-2.5 py-1.5 ${tones[tone] || tones.muted}`}>
      <span className="text-zinc-500">{label}</span> · {value}
    </span>
  );
}
