import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthActionCard from '../components/AuthActionCard';
import { changeEmail } from '../services/authService';
import { useAuth } from '@/contexts/AuthContext';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function ChangeEmail() {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout } = useAuth();
  const [form, setForm] = useState({ newEmail: location.state?.newEmail || '', password: '' });
  const [feedback, setFeedback] = useState({ error: '', success: '' });
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    const newEmail = form.newEmail.trim().toLowerCase();
    if (!emailPattern.test(newEmail)) {
      setFeedback({ error: 'Vui lòng nhập địa chỉ email hợp lệ.', success: '' });
      return;
    }
    if (!form.password) {
      setFeedback({ error: 'Vui lòng nhập mật khẩu hiện tại.', success: '' });
      return;
    }

    setLoading(true);
    setFeedback({ error: '', success: '' });
    try {
      await changeEmail(newEmail, form.password);
      setFeedback({
        error: '',
        success: 'Email đã được thay đổi. Tất cả phiên đăng nhập đã được thu hồi.'
      });
      await logout();
      navigate('/login', {
        replace: true,
        state: { email: newEmail, message: 'Vui lòng đăng nhập lại bằng email mới.' }
      });
    } catch (reason) {
      setFeedback({
        error: reason?.message || 'Không thể thay đổi email. Vui lòng kiểm tra mật khẩu và thử lại.',
        success: ''
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthActionCard
      title="Thay đổi email"
      subtitle="Email mới sẽ trở thành định danh đăng nhập của tài khoản."
    >
      <form onSubmit={submit} className="space-y-4" noValidate>
        <div>
          <label htmlFor="new-email" className="mb-1.5 block text-xs font-bold text-zinc-400">
            Email mới
          </label>
          <input
            id="new-email"
            type="email"
            autoComplete="email"
            value={form.newEmail}
            onChange={(event) => setForm((value) => ({ ...value, newEmail: event.target.value }))}
            className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3"
            disabled={loading}
            required
          />
        </div>
        <div>
          <label htmlFor="change-email-password" className="mb-1.5 block text-xs font-bold text-zinc-400">
            Mật khẩu hiện tại
          </label>
          <input
            id="change-email-password"
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={(event) => setForm((value) => ({ ...value, password: event.target.value }))}
            className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3"
            disabled={loading}
            required
          />
        </div>
        {feedback.error && (
          <p role="alert" className="rounded-xl border border-red-900 bg-red-950/40 p-3 text-sm text-red-300">
            {feedback.error}
          </p>
        )}
        {feedback.success && (
          <p role="status" className="rounded-xl border border-emerald-900 bg-emerald-950/40 p-3 text-sm text-emerald-300">
            {feedback.success}
          </p>
        )}
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 disabled:opacity-60"
        >
          {loading ? 'Đang cập nhật...' : 'Xác nhận thay đổi email'}
        </button>
      </form>
    </AuthActionCard>
  );
}
