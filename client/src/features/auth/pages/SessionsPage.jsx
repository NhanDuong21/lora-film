import { useCallback, useEffect, useState } from 'react';
import AuthActionCard from '../components/AuthActionCard';
import { getSessions, revokeAllSessions, revokeSession } from '../services/authService';
import { Laptop, Smartphone, Monitor, ShieldAlert, Trash2, LogOut } from 'lucide-react';

export default function SessionsPage() {
  const [sessions, setSessions] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setSessions(await getSessions());
      setError('');
    } catch (reason) {
      setError(reason?.message || 'Không thể tải danh sách phiên.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const revoke = async (id) => {
    await revokeSession(id);
    await load();
  };

  const revokeAll = async () => {
    await revokeAllSessions();
    await load();
  };

  const getDeviceIcon = (userAgent) => {
    const ua = (userAgent || '').toLowerCase();
    if (ua.includes('mobile') || ua.includes('android') || ua.includes('iphone')) return <Smartphone className="text-zinc-400" size={24} />;
    if (ua.includes('mac') || ua.includes('windows') || ua.includes('linux')) return <Laptop className="text-zinc-400" size={24} />;
    return <Monitor className="text-zinc-400" size={24} />;
  };

  return (
    <AuthActionCard title="Thiết bị đăng nhập" subtitle="Quản lý và thu hồi các thiết bị đang truy cập vào tài khoản của bạn.">
      {error && (
        <div className="mb-4 flex items-start gap-2 rounded-lg bg-red-950/50 p-3 border border-red-900/50">
          <ShieldAlert size={16} className="text-red-500 mt-0.5" />
          <p className="text-sm text-red-400">{error}</p>
        </div>
      )}
      
      {loading ? (
        <div className="space-y-3 animate-pulse">
          {[1, 2, 3].map(i => (
            <div key={i} className="flex gap-4 items-center rounded-xl border border-zinc-800/50 bg-zinc-950/50 p-4">
              <div className="w-10 h-10 rounded-full bg-zinc-800"></div>
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-zinc-800 rounded w-1/2"></div>
                <div className="h-3 bg-zinc-800 rounded w-1/3"></div>
              </div>
              <div className="w-8 h-8 rounded-full bg-zinc-800"></div>
            </div>
          ))}
        </div>
      ) : (
        <div className="space-y-4">
          {sessions.length === 0 ? (
            <div className="py-12 flex flex-col items-center justify-center border border-dashed border-zinc-800 rounded-2xl bg-zinc-950/30">
              <Monitor size={48} className="text-zinc-700 mb-4" />
              <p className="text-sm font-semibold text-zinc-300">Không có thiết bị hoạt động</p>
              <p className="text-xs text-zinc-500 mt-1">Tài khoản của bạn hiện chưa được đăng nhập trên thiết bị nào.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {sessions.map((session) => (
                <article key={session.id} className="flex items-center gap-4 rounded-xl border border-zinc-800 bg-zinc-950/80 p-4 hover:border-zinc-700 hover:bg-zinc-900 transition-colors group">
                  <div className="p-2 rounded-full bg-zinc-900 group-hover:bg-zinc-800 transition-colors">
                    {getDeviceIcon(session.userAgent)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="truncate font-bold text-sm text-zinc-100">
                      {session.deviceName || session.userAgent?.split(' ')[0] || 'Thiết bị không xác định'}
                    </p>
                    <div className="mt-1 flex items-center gap-2 text-[11px] text-zinc-500">
                      <span className="font-mono bg-zinc-900 px-1.5 py-0.5 rounded">{session.ipAddress || 'IP không xác định'}</span>
                      <span>·</span>
                      <span>{session.lastActiveAt ? new Date(session.lastActiveAt).toLocaleString('vi-VN') : 'Mới đây'}</span>
                    </div>
                  </div>
                  <button 
                    onClick={() => revoke(session.id)} 
                    className="p-2 rounded-lg text-zinc-500 hover:text-red-400 hover:bg-red-500/10 transition-colors focus:outline-none focus:ring-2 focus:ring-red-500/20"
                    title="Thu hồi phiên"
                  >
                    <Trash2 size={18} />
                  </button>
                </article>
              ))}
              
              {sessions.length > 1 && (
                <button 
                  onClick={revokeAll} 
                  className="w-full flex items-center justify-center gap-2 rounded-xl border border-red-900/50 bg-red-950/20 py-3 text-sm font-bold text-red-400 hover:bg-red-950/40 hover:border-red-900 transition-colors mt-6"
                >
                  <LogOut size={16} />
                  <span>Đăng xuất khỏi tất cả thiết bị</span>
                </button>
              )}
            </div>
          )}
        </div>
      )}
    </AuthActionCard>
  );
}
