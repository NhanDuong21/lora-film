import { useEffect, useState, useRef, useCallback } from 'react';
import { RefreshCw, CheckCircle, Clock, AlertTriangle, Play, HelpCircle } from 'lucide-react';
import adminTmdbService from '@/features/catalog/admin/services/adminTmdbService';
import { formatDate } from '@/utils/movieHelpers';

const getRelativeTime = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffInSeconds < 60) {
    return 'vừa xong';
  }

  const rtf = new Intl.RelativeTimeFormat('vi', { numeric: 'auto' });

  if (diffInSeconds < 3600) {
    return rtf.format(-Math.floor(diffInSeconds / 60), 'minute');
  }

  if (diffInSeconds < 86400) {
    return rtf.format(-Math.floor(diffInSeconds / 3600), 'hour');
  }

  return rtf.format(-Math.floor(diffInSeconds / 86400), 'day');
};

export default function TmdbSyncStatusPanel() {
  const [syncState, setSyncState] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const timerRef = useRef(null);

  const fetchSyncState = useCallback(async () => {
    try {
      const state = await adminTmdbService.getSyncState();
      setSyncState(state);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch sync state', err);
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSyncState();
  }, [fetchSyncState]);

  useEffect(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    if (!loading) {
      let delay = 60000; // default 60s
      if (syncState?.displayStatus === 'RUNNING') delay = 5000;
      else if (syncState?.displayStatus === 'STALE') delay = 15000;

      timerRef.current = setTimeout(fetchSyncState, delay);
    }

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [syncState, loading, fetchSyncState]);

  if (loading && !syncState) {
    return (
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-4 flex items-center justify-between mb-6 animate-pulse">
        <div className="h-16 w-full bg-zinc-800 rounded"></div>
      </div>
    );
  }

  if (error && !syncState) {
    return (
      <div className="bg-red-900/20 border border-red-500/50 rounded-xl p-4 flex items-center justify-between mb-6 text-red-200">
        <div>Không thể lấy thông tin đồng bộ.</div>
        <button onClick={fetchSyncState} className="px-3 py-1 bg-red-800 hover:bg-red-700 rounded text-sm transition-colors">
          Thử lại
        </button>
      </div>
    );
  }

  const { displayStatus, persistedStatus, cursor, lastSuccessfulSyncAt, stateUpdatedAt } = syncState || {};

  const handleRetry = () => {
    setLoading(true);
    fetchSyncState();
  };

  const getStatusConfig = () => {
    switch (displayStatus) {
      case 'NO_DATA':
        return {
          icon: <HelpCircle className="w-6 h-6 text-zinc-500" />,
          title: 'TMDB BULK IMPORT',
          desc: 'Chưa có dữ liệu đồng bộ\nChưa có tiến trình nhập danh mục phim nào được ghi nhận.\nTrong môi trường phát triển, hãy chạy TMDB Integration API và bật bulk importer của Movie Service.',
          bgColor: 'bg-zinc-800',
        };
      case 'IDLE':
        return {
          icon: <CheckCircle className="w-6 h-6 text-green-500" />,
          title: 'Sẵn sàng',
          desc: 'Hiện không có tiến trình nhập phim đang chạy.',
          bgColor: 'bg-green-500/10 border-green-500/20',
        };
      case 'RUNNING':
        return {
          icon: <RefreshCw className="w-6 h-6 text-blue-400 animate-spin" />,
          title: 'Đang đồng bộ danh mục phim',
          desc: '',
          bgColor: 'bg-blue-500/10 border-blue-500/20',
        };
      case 'SUCCESS':
        return {
          icon: <CheckCircle className="w-6 h-6 text-green-500" />,
          title: 'Hoàn thành',
          desc: '',
          bgColor: 'bg-green-500/10 border-green-500/20',
        };
      case 'FAILED':
        return {
          icon: <AlertTriangle className="w-6 h-6 text-red-500" />,
          title: 'Đồng bộ thất bại',
          desc: 'Không có lỗi chi tiết trong sync-state hiện tại.\nHãy kiểm tra log Movie Service.',
          bgColor: 'bg-red-500/10 border-red-500/20',
        };
      case 'STALE':
        return {
          icon: <AlertTriangle className="w-6 h-6 text-orange-500" />,
          title: 'Tiến trình có thể đã bị gián đoạn',
          desc: 'Không nhận được cập nhật trạng thái trong hơn 5 phút.\nHãy kiểm tra Movie Service bulk importer và TMDB Integration API đang chạy local.',
          bgColor: 'bg-orange-500/10 border-orange-500/20',
        };
      default:
        return {
          icon: <HelpCircle className="w-6 h-6 text-zinc-500" />,
          title: 'Trạng thái không được hỗ trợ',
          desc: `Raw status: ${persistedStatus}`,
          bgColor: 'bg-zinc-800',
        };
    }
  };

  const config = getStatusConfig();

  return (
    <div className={`rounded-xl p-4 flex flex-col sm:flex-row items-start justify-between mb-6 gap-4 border ${config.bgColor || 'bg-zinc-900/50 border-zinc-800'}`}>
      <div className="flex items-start gap-4">
        <div className="p-2 bg-zinc-900/50 rounded-lg border border-white/10 shrink-0 mt-1">
          {config.icon}
        </div>
        <div>
          <h3 className="text-base font-bold text-zinc-100">{config.title}</h3>
          
          {config.desc && (
            <div className="text-sm text-zinc-400 mt-1 whitespace-pre-line">
              {config.desc}
            </div>
          )}

          <div className="mt-3 space-y-1 text-sm">
            {displayStatus !== 'NO_DATA' && displayStatus !== 'UNKNOWN' && (
              <>
                {(displayStatus === 'IDLE' || displayStatus === 'RUNNING' || displayStatus === 'SUCCESS') && (
                  <div>
                    <span className="text-zinc-500">Checkpoint {displayStatus === 'SUCCESS' ? 'cuối' : 'hiện tại'}: </span>
                    <strong className="text-zinc-300">{cursor || '0'}</strong>
                  </div>
                )}
                
                {(displayStatus === 'RUNNING' || displayStatus === 'FAILED' || displayStatus === 'STALE') && stateUpdatedAt && (
                  <div title={formatDate(stateUpdatedAt)}>
                    <span className="text-zinc-500">Cập nhật trạng thái gần nhất: </span>
                    <strong className="text-zinc-300">{getRelativeTime(stateUpdatedAt)}</strong>
                  </div>
                )}

                {(displayStatus === 'IDLE' || displayStatus === 'RUNNING' || displayStatus === 'SUCCESS') && (
                  <div title={lastSuccessfulSyncAt ? formatDate(lastSuccessfulSyncAt) : ''}>
                    <span className="text-zinc-500">Lần {displayStatus === 'SUCCESS' ? 'đồng bộ' : 'hoàn thành'} thành công gần nhất: </span>
                    <strong className="text-zinc-300">
                      {lastSuccessfulSyncAt ? (displayStatus === 'SUCCESS' ? getRelativeTime(lastSuccessfulSyncAt) : formatDate(lastSuccessfulSyncAt)) : 'Chưa có'}
                    </strong>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>
      
      <div className="shrink-0 flex items-center h-full">
        <button
          onClick={handleRetry}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded border border-zinc-700 transition-colors disabled:opacity-50 text-sm"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          {displayStatus === 'NO_DATA' ? 'Thử lại' : 'Làm mới'}
        </button>
      </div>
    </div>
  );
}
