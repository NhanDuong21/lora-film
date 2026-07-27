import { useCallback, useEffect, useState } from 'react';
import AuthActionCard from '../components/AuthActionCard';
import { getSessions, revokeAllSessions, revokeSession } from '../services/authService';

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
    // Async API loading is the external synchronization performed by this effect.
    // eslint-disable-next-line react-hooks/set-state-in-effect
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

  return (
    <AuthActionCard title="Phiên đăng nhập" subtitle="Kiểm tra và thu hồi các thiết bị đang đăng nhập tài khoản.">
      {error && <p className="mb-4 text-sm text-red-400">{error}</p>}
      {loading ? <p className="text-zinc-400">Đang tải...</p> : (
        <div className="space-y-3">
          {sessions.length === 0 && <p className="text-sm text-zinc-400">Không có phiên hoạt động.</p>}
          {sessions.map((session) => (
            <article key={session.id} className="rounded-xl border border-zinc-800 bg-zinc-950 p-4 text-sm">
              <p className="truncate font-bold">{session.deviceName || session.userAgent || 'Thiết bị không xác định'}</p>
              <p className="mt-1 text-xs text-zinc-500">{session.ipAddress || 'IP không xác định'} · {session.lastActiveAt ? new Date(session.lastActiveAt).toLocaleString('vi-VN') : 'Chưa có hoạt động'}</p>
              <button onClick={() => revoke(session.id)} className="mt-3 text-xs font-bold text-red-400">Thu hồi phiên</button>
            </article>
          ))}
          {sessions.length > 0 && <button onClick={revokeAll} className="w-full rounded-xl border border-red-800 py-3 text-sm font-bold text-red-400">Thu hồi tất cả phiên</button>}
        </div>
      )}
    </AuthActionCard>
  );
}
