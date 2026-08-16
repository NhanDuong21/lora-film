import { useCallback, useEffect, useState } from 'react';
import AuthActionCard from '../components/AuthActionCard';
import { getSessions, revokeAllSessions, revokeSession } from '../services/authService';
import { Laptop, Smartphone, Monitor, ShieldAlert, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';

const browserName = (userAgent = '') => {
  if (/Edg\//i.test(userAgent)) return 'Edge';
  if (/Chrome\//i.test(userAgent)) return 'Chrome';
  if (/Firefox\//i.test(userAgent)) return 'Firefox';
  if (/Safari\//i.test(userAgent) && !/Chrome\//i.test(userAgent)) return 'Safari';
  return 'Trình duyệt';
};

const operatingSystem = (userAgent = '') => {
  if (/Windows/i.test(userAgent)) return 'Windows';
  if (/Android/i.test(userAgent)) return 'Android';
  if (/iPhone|iPad|iOS/i.test(userAgent)) return 'iOS';
  if (/Mac OS|Macintosh/i.test(userAgent)) return 'macOS';
  if (/Linux/i.test(userAgent)) return 'Linux';
  return 'thiết bị không xác định';
};

const maskedIp = (value = '') => {
  if (!value) return 'IP được bảo vệ';
  if (value === '127.0.0.1' || value === '::1' || value.startsWith('0:0:0:0:0:0:0:1')) return 'Mạng nội bộ';
  if (value.includes(':')) return `${value.split(':').slice(0, 2).join(':')}:••••`;
  const parts = value.split('.');
  return parts.length === 4 ? `${parts[0]}.${parts[1]}.xxx.${parts[3]}` : 'IP được bảo vệ';
};

export default function SessionsPage({ embedded = false }) {
  const [sessions, setSessions] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [pendingAction, setPendingAction] = useState(null);
  const [success, setSuccess] = useState('');
  const { logout } = useAuth();
  const navigate = useNavigate();

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
    // Loading remote session state is the synchronization performed by this effect.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const revoke = async (id) => {
    setPendingAction(null);
    try {
      await revokeSession(id);
      setSuccess('Phiên đăng nhập đã được thu hồi.');
      await load();
    } catch (reason) {
      setError(reason?.message || 'Không thể thu hồi phiên đăng nhập.');
    }
  };

  const revokeAll = async () => {
    setPendingAction(null);
    try {
      await revokeAllSessions();
      await logout();
      navigate('/login', { replace: true, state: { message: 'Tất cả phiên đăng nhập đã được thu hồi.' } });
    } catch (reason) {
      setError(reason?.message || 'Không thể thu hồi tất cả phiên đăng nhập.');
    }
  };

  const getDeviceIcon = (userAgent) => {
    const ua = (userAgent || '').toLowerCase();
    if (ua.includes('mobile') || ua.includes('android') || ua.includes('iphone')) return <Smartphone className="text-zinc-400" size={24} />;
    if (ua.includes('mac') || ua.includes('windows') || ua.includes('linux')) return <Laptop className="text-zinc-400" size={24} />;
    return <Monitor className="text-zinc-400" size={24} />;
  };

  return (
    <AuthActionCard embedded={embedded} title="Các thiết bị đang hoạt động" subtitle="Nếu không nhận ra thiết bị nào, hãy đăng xuất thiết bị đó và đổi mật khẩu ngay.">
      {error && (
        <div className="mb-4 flex items-start gap-2 rounded-lg bg-red-950/50 p-3 border border-red-900/50">
          <ShieldAlert size={16} className="text-red-500 mt-0.5" />
          <p className="text-sm text-red-400">{error}</p>
        </div>
      )}
      {success && (
        <p role="status" className="mb-4 rounded-lg border border-emerald-900/50 bg-emerald-950/40 p-3 text-sm text-emerald-300">
          {success}
        </p>
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
                      {session.deviceName && !/^Mozilla\//i.test(session.deviceName)
                        ? session.deviceName
                        : `${browserName(session.userAgent || session.deviceName)} trên ${operatingSystem(session.userAgent || session.deviceName)}`}
                      {(session.current || session.currentSession || session.isCurrent) && <span className="ml-2 rounded-full bg-emerald-500/10 px-2 py-0.5 text-[9px] font-black text-emerald-400">Thiết bị này</span>}
                    </p>
                    <div className="mt-1 flex items-center gap-2 text-[11px] text-zinc-500">
                      <span className="font-mono bg-zinc-900 px-1.5 py-0.5 rounded">{maskedIp(session.ipAddress)}</span>
                      <span>·</span>
                      <span>{session.lastActiveAt ? new Date(session.lastActiveAt).toLocaleString('vi-VN') : 'Mới đây'}</span>
                    </div>
                  </div>
                  <button 
                    onClick={() => setPendingAction({ id: session.id, label: session.deviceName || 'thiết bị này' })}
                    className="rounded-lg border border-red-500/20 px-3 py-2 text-xs font-bold text-red-400 hover:bg-red-500/10 transition-colors focus:outline-none focus:ring-2 focus:ring-red-500/20"
                    title="Đăng xuất thiết bị"
                  >
                    Đăng xuất
                  </button>
                </article>
              ))}
              
              {sessions.length > 1 && (
                <button 
                  onClick={() => setPendingAction({ all: true, label: 'tất cả thiết bị' })}
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
      {pendingAction && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4" role="dialog" aria-modal="true" aria-labelledby="session-confirm-title">
          <div className="w-full max-w-sm rounded-2xl border border-zinc-800 bg-zinc-900 p-6 shadow-2xl">
            <h2 id="session-confirm-title" className="text-lg font-black text-white">Xác nhận thu hồi phiên</h2>
            <p className="mt-2 text-sm text-zinc-400">
              {pendingAction.all
                ? 'Bạn sẽ được đăng xuất khỏi tất cả thiết bị.'
                : `Thiết bị “${pendingAction.label}” sẽ phải đăng nhập lại.`}
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <button type="button" onClick={() => setPendingAction(null)}
                className="rounded-xl border border-zinc-700 px-4 py-2 text-sm text-zinc-300">
                Hủy
              </button>
              <button type="button" onClick={() => pendingAction.all ? revokeAll() : revoke(pendingAction.id)}
                className="rounded-xl bg-red-500 px-4 py-2 text-sm font-bold text-white">
                Thu hồi
              </button>
            </div>
          </div>
        </div>
      )}
    </AuthActionCard>
  );
}
