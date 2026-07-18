import { useState, useEffect } from 'react';
import { useParams, useNavigate, useOutletContext } from 'react-router-dom';
import { 
  Building2, Image as ImageIcon, Clock, CalendarX2, Film, ArrowLeft, RefreshCw 
} from 'lucide-react';
import useAdminCinemaDetail from '../hooks/useAdminCinemaDetail';
import { LoadingState, ErrorState } from '@/components/common/ui/uiKit';

import CinemaOverviewTab from './cinema/CinemaOverviewTab';
import CinemaMediaTab from './cinema/CinemaMediaTab';
import CinemaOperatingHoursTab from './cinema/CinemaOperatingHoursTab';
import CinemaClosurePeriodsTab from './cinema/CinemaClosurePeriodsTab';
import CinemaAuditoriumsTab from './cinema/CinemaAuditoriumsTab';

export default function AdminCinemaDetailPage() {
  const { cinemaPublicId } = useParams();
  const navigate = useNavigate();
  const { triggerToast } = useOutletContext() || {};

  const {
    cinema,
    isLoading,
    error,
    fetchCinema,
    updateCinemaBasicInfo,
    updateOperatingHours,
    addMedia,
    updateMedia,
    deleteMedia
  } = useAdminCinemaDetail(cinemaPublicId, triggerToast);

  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
     
    fetchCinema();
  }, [fetchCinema]);

  const tabs = [
    { id: 'overview', label: 'THÔNG TIN CHUNG', icon: Building2 },
    { id: 'media', label: 'HÌNH ẢNH', icon: ImageIcon },
    { id: 'operating-hours', label: 'GIỜ HOẠT ĐỘNG', icon: Clock },
    { id: 'closure-periods', label: 'LỊCH ĐÓNG CỬA', icon: CalendarX2 },
    { id: 'auditoriums', label: 'PHÒNG CHIẾU', icon: Film },
  ];

  if (isLoading && !cinema) {
    return (
      <div className="flex-1 p-8 bg-zinc-950 flex flex-col items-center justify-center">
        <LoadingState message="Đang tải thông tin rạp chiếu..." />
      </div>
    );
  }

  if (error && !cinema) {
    return (
      <div className="flex-1 p-8 bg-zinc-950 flex flex-col items-center justify-center">
        <ErrorState message={error} onRetry={fetchCinema} />
      </div>
    );
  }

  if (!cinema) return null;

  return (
    <div className="flex flex-col flex-1 bg-zinc-950 text-white font-sans h-full overflow-hidden">
      
      {/* Header */}
      <div className="px-6 md:px-8 py-5 border-b border-zinc-900 bg-zinc-950/80 backdrop-blur-md sticky top-0 z-10">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/admin/cinemas')}
              className="p-2 hover:bg-zinc-900 rounded-xl transition-colors text-zinc-400 hover:text-white"
              title="Quay lại danh sách rạp chiếu"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-zinc-50">
                  {cinema.name}
                </h1>
                {cinema.status === 'ACTIVE' && (
                  <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 rounded-lg">
                    HOẠT ĐỘNG
                  </span>
                )}
                {cinema.status === 'DRAFT' && (
                  <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-amber-500/10 border border-amber-500/30 text-amber-400 rounded-lg">
                    BẢN NHÁP
                  </span>
                )}
                {cinema.status === 'MAINTENANCE' && (
                  <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-red-500/10 border border-red-500/30 text-red-400 rounded-lg">
                    BẢO TRÌ
                  </span>
                )}
                {cinema.status === 'INACTIVE' && (
                  <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-zinc-800 border border-zinc-700 text-zinc-400 rounded-lg">
                    NGƯNG HOẠT ĐỘNG
                  </span>
                )}
              </div>
              <p className="text-xs text-zinc-400 mt-1">Quản lý chi tiết rạp chiếu và phòng chiếu</p>
            </div>
          </div>
          <button
            onClick={fetchCinema}
            className="flex items-center gap-2 px-3 py-2 bg-zinc-900 hover:bg-zinc-800 rounded-lg text-xs font-bold transition-colors"
          >
            <RefreshCw className="w-4 h-4 text-brand-coral" />
            <span>Tải lại</span>
          </button>
        </div>

        {/* Tabs navigation */}
        <div className="flex gap-2 mt-6 overflow-x-auto no-scrollbar">
          {tabs.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-t-xl text-xs font-black uppercase tracking-wider transition-all select-none whitespace-nowrap
                ${activeTab === tab.id 
                  ? 'bg-zinc-900/50 text-brand-coral border-b-2 border-brand-coral' 
                  : 'text-zinc-500 hover:bg-zinc-900/30 hover:text-zinc-300 border-b-2 border-transparent'
                }
              `}
            >
              <tab.icon className="w-4 h-4" />
              <span>{tab.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-auto p-6 md:p-8">
        {activeTab === 'overview' && (
          <CinemaOverviewTab 
            cinema={cinema} 
            onUpdate={updateCinemaBasicInfo} 
            triggerToast={triggerToast} 
          />
        )}
        
        {activeTab === 'media' && (
          <CinemaMediaTab 
            cinema={cinema} 
            onAdd={addMedia}
            onUpdate={updateMedia}
            onDelete={deleteMedia}
          />
        )}
        
        {activeTab === 'operating-hours' && (
          <CinemaOperatingHoursTab 
            cinema={cinema} 
            onUpdate={updateOperatingHours}
            triggerToast={triggerToast} 
          />
        )}

        {activeTab === 'closure-periods' && (
          <CinemaClosurePeriodsTab 
            cinemaPublicId={cinemaPublicId} 
            triggerToast={triggerToast} 
          />
        )}

        {activeTab === 'auditoriums' && (
          <CinemaAuditoriumsTab 
            cinema={cinema}
            triggerToast={triggerToast}
          />
        )}
      </div>
    </div>
  );
}
