import { useState, useEffect } from 'react';
import { useParams, useNavigate, useOutletContext } from 'react-router-dom';
import { 
  ArrowLeft, RefreshCw, LayoutGrid, Settings, Wrench
} from 'lucide-react';
import useAuditoriumDetail from '../hooks/useAuditoriumDetail';
import { LoadingState, ErrorState } from '@/components/common/ui/uiKit';

import AuditoriumOverviewTab from './auditorium/AuditoriumOverviewTab';
import AuditoriumMaintenanceTab from './auditorium/AuditoriumMaintenanceTab';
import AuditoriumSeatLayoutTab from './auditorium/AuditoriumSeatLayoutTab';

export default function AdminAuditoriumDetailPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const { triggerToast } = useOutletContext() || {};

  const {
    auditorium,
    isLoading,
    error,
    fetchAuditorium,
    updateAuditoriumBasicInfo,
    updateSeatLayout
  } = useAuditoriumDetail(roomId, triggerToast);

  const [activeTab, setActiveTab] = useState('seat-layout');

  useEffect(() => {
     
    fetchAuditorium();
  }, [fetchAuditorium]);

  const tabs = [
    { id: 'overview', label: 'CẤU HÌNH PHÒNG', icon: Settings },
    { id: 'seat-layout', label: 'SƠ ĐỒ GHẾ', icon: LayoutGrid },
    { id: 'maintenance', label: 'BẢO TRÌ', icon: Wrench },
  ];

  if (isLoading && !auditorium) {
    return (
      <div className="flex-1 p-8 bg-zinc-950 flex flex-col items-center justify-center">
        <LoadingState message="Đang tải thông tin phòng chiếu..." />
      </div>
    );
  }

  if (error && !auditorium) {
    return (
      <div className="flex-1 p-8 bg-zinc-950 flex flex-col items-center justify-center">
        <ErrorState message={error} onRetry={fetchAuditorium} />
      </div>
    );
  }

  if (!auditorium) return null;

  return (
    <div className="flex flex-col flex-1 bg-zinc-950 text-white font-sans h-full overflow-hidden">
      
      {/* Header */}
      <div className="px-6 md:px-8 py-5 border-b border-zinc-900 bg-zinc-950/80 backdrop-blur-md sticky top-0 z-10 shrink-0">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(-1)}
              className="p-2 hover:bg-zinc-900 rounded-xl transition-colors text-zinc-400 hover:text-white"
              title="Quay lại"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-zinc-50">
                  {auditorium.auditoriumName}
                </h1>
                {auditorium.auditoriumStatus === 'ACTIVE' && (
                  <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 rounded-lg">
                    HOẠT ĐỘNG
                  </span>
                )}
                {auditorium.auditoriumStatus === 'DRAFT' && (
                  <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-amber-500/10 border border-amber-500/30 text-amber-400 rounded-lg">
                    BẢN NHÁP
                  </span>
                )}
                {auditorium.auditoriumStatus === 'MAINTENANCE' && (
                  <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-red-500/10 border border-red-500/30 text-red-400 rounded-lg">
                    BẢO TRÌ
                  </span>
                )}
                {auditorium.auditoriumStatus === 'INACTIVE' && (
                  <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-zinc-800 border border-zinc-700 text-zinc-400 rounded-lg">
                    NGƯNG HOẠT ĐỘNG
                  </span>
                )}
              </div>
              <p className="text-[10px] text-zinc-450 font-bold uppercase tracking-wider mt-1 font-mono">
                Mã ID: {roomId}
              </p>
            </div>
          </div>
          <button
            onClick={fetchAuditorium}
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

      {/* Tab Content - We don't use padding here to allow SeatLayout to use full screen width/height */}
      <div className="flex-1 overflow-hidden relative">
        {activeTab === 'overview' && (
          <div className="h-full overflow-auto p-6 md:p-8">
            <AuditoriumOverviewTab 
              auditorium={auditorium} 
              onUpdate={updateAuditoriumBasicInfo} 
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
              triggerToast={triggerToast} 
            />
          </div>
        )}
      </div>
    </div>
  );
}
