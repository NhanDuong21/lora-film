import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthActionCard from '../components/AuthActionCard';
import { forgotPassword } from '../services/authService';

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      await forgotPassword(email.trim());
      navigate('/reset-password', { state: { email: email.trim() } });
    } catch (reason) {
      setError(reason?.message || 'Không thể gửi yêu cầu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthActionCard title="Quên mật khẩu" subtitle="Nhập email tài khoản để nhận mã OTP đặt lại mật khẩu.">
      <form onSubmit={submit} className="space-y-4">
        <label className="block text-xs font-bold uppercase text-zinc-400" htmlFor="forgot-email">Email</label>
        <input id="forgot-email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
          className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 outline-none focus:border-orange-500 text-white" />
        {error && <p className="text-sm text-red-400">{error}</p>}
        <button disabled={loading} className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 disabled:opacity-60 cursor-pointer">
          {loading ? 'Đang gửi...' : 'Gửi mã OTP'}
        </button>
        <Link to="/login" className="block text-center text-sm font-bold text-orange-400">Quay lại đăng nhập</Link>
      </form>
    </AuthActionCard>
  );
}

