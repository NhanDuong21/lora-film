import React, { useEffect, useState } from 'react';
import { RefreshCw, CheckCircle, Clock, AlertTriangle } from 'lucide-react';
import adminTmdbService from '@/features/catalog/admin/services/adminTmdbService';
import { formatDate } from '@/utils/movieHelpers';

export default function TmdbSyncStatusPanel() {
  const [syncState, setSyncState] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchSyncState = async () => {
    try {
      const state = await adminTmdbService.getSyncState();
      setSyncState(state);
    } catch (error) {
      console.error('Failed to fetch sync state', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSyncState();
    const interval = setInterval(fetchSyncState, 30000); // Poll every 30s
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-4 flex items-center justify-between mb-6 animate-pulse">
        <div className="h-6 w-32 bg-zinc-800 rounded"></div>
      </div>
    );
  }

  const getStatusIcon = (status) => {
    if (status === 'IN_PROGRESS') return <RefreshCw className="w-5 h-5 text-blue-400 animate-spin" />;
    if (status === 'COMPLETED') return <CheckCircle className="w-5 h-5 text-green-500" />;
    if (status === 'FAILED') return <AlertTriangle className="w-5 h-5 text-red-500" />;
    return <Clock className="w-5 h-5 text-zinc-500" />;
  };

  const getStatusLabel = (status) => {
    if (status === 'IN_PROGRESS') return 'Đang đồng bộ';
    if (status === 'COMPLETED') return 'Hoàn tất';
    if (status === 'FAILED') return 'Lỗi đồng bộ';
    if (status === 'IDLE') return 'Đang chờ';
    return status || 'Không xác định';
  };

  return (
    <div className="bg-zinc-900/50 border border-zinc-800 rounded-xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between mb-6 gap-4">
      <div className="flex items-center gap-3">
        <div className="p-2 bg-zinc-800 rounded-lg border border-zinc-700/50">
          {getStatusIcon(syncState?.status)}
        </div>
        <div>
          <h3 className="text-sm font-bold text-zinc-200">Trạng thái đồng bộ TMDB (Hàng ngày)</h3>
          <p className="text-xs text-zinc-400 mt-1 flex items-center gap-2">
            <span>Trạng thái: <strong className="text-zinc-300">{getStatusLabel(syncState?.status)}</strong></span>
            {syncState?.cursor && (
              <>
                <span className="w-1 h-1 bg-zinc-700 rounded-full"></span>
                <span>Cursor: <strong className="text-zinc-300">{syncState.cursor}</strong></span>
              </>
            )}
          </p>
        </div>
      </div>
      
      <div className="text-right flex flex-col gap-1 w-full sm:w-auto bg-zinc-950 p-2 rounded-lg border border-zinc-800/50">
        <div className="text-[10px] text-zinc-500">Lần hoàn thành gần nhất</div>
        <div className="text-xs font-medium text-zinc-300">
          {syncState?.lastCompletedAt ? formatDate(syncState.lastCompletedAt) : 'Chưa có dữ liệu'}
        </div>
      </div>
    </div>
  );
}
