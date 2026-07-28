import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import AuthActionCard from '../components/AuthActionCard';
import { resetPassword } from '../services/authService';

const strongPassword = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/;

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = useMemo(() => params.get('token') || '', [params]);
  const email = useMemo(() => params.get('email') || '', [params]);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [status, setStatus] = useState({ error: '', done: false });
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    if (!token) return setStatus({ error: 'Liên kết đặt lại mật khẩu không hợp lệ.', done: false });
    if (!strongPassword.test(password)) return setStatus({ error: 'Mật khẩu cần ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.', done: false });
    if (password !== confirm) return setStatus({ error: 'Mật khẩu xác nhận không khớp.', done: false });
    setLoading(true);
    try {
      await resetPassword(token, password, email);
      setStatus({ error: '', done: true });
    } catch (reason) {
      setStatus({ error: reason?.message || 'Liên kết đã hết hạn hoặc không hợp lệ.', done: false });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthActionCard title="Đặt lại mật khẩu">
      {status.done ? (
        <div className="space-y-5">
          <p className="rounded-xl border border-emerald-800 bg-emerald-950/50 p-4 text-sm text-emerald-200">Mật khẩu đã được cập nhật.</p>
          <Link to="/login" className="block text-center font-bold text-orange-400">Đăng nhập</Link>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          <input aria-label="Mật khẩu mới" type="password" required placeholder="Mật khẩu mới" value={password}
            onChange={(e) => setPassword(e.target.value)} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3" />
          <input aria-label="Xác nhận mật khẩu" type="password" required placeholder="Xác nhận mật khẩu" value={confirm}
            onChange={(e) => setConfirm(e.target.value)} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3" />
          {status.error && <p className="text-sm text-red-400">{status.error}</p>}
          <button disabled={loading} className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 disabled:opacity-60">
            {loading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
          </button>
        </form>
      )}
    </AuthActionCard>
  );
}
