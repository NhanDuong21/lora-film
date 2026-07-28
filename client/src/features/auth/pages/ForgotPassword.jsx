import { useState } from 'react';
import { Link } from 'react-router-dom';
import AuthActionCard from '../components/AuthActionCard';
import { forgotPassword } from '../services/authService';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      await forgotPassword(email.trim());
      setMessage('Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi.');
    } catch (reason) {
      setError(reason?.message || 'Không thể gửi yêu cầu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthActionCard title="Quên mật khẩu" subtitle="Nhập email tài khoản để nhận liên kết đặt lại mật khẩu.">
      {message ? (
        <div className="space-y-5">
          <p className="rounded-xl border border-emerald-800 bg-emerald-950/50 p-4 text-sm text-emerald-200">{message}</p>
          <Link to="/login" className="block text-center text-sm font-bold text-orange-400">Quay lại đăng nhập</Link>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          <label className="block text-xs font-bold uppercase text-zinc-400" htmlFor="forgot-email">Email</label>
          <input id="forgot-email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
            className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 outline-none focus:border-orange-500" />
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button disabled={loading} className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 disabled:opacity-60">
            {loading ? 'Đang gửi...' : 'Gửi hướng dẫn'}
          </button>
        </form>
      )}
    </AuthActionCard>
  );
}
