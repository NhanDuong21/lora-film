import { useMemo, useState } from 'react';
import { Link, useSearchParams, useLocation } from 'react-router-dom';
import AuthActionCard from '../components/AuthActionCard';
import { resetPassword } from '../services/authService';

const strongPassword = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/;

export default function ResetPassword() {
  const [params] = useSearchParams();
  const location = useLocation();

  const urlEmail = useMemo(() => params.get('email') || '', [params]);
  const urlToken = useMemo(() => params.get('token') || '', [params]);

  const initialEmail = location.state?.email || urlEmail;

  const [emailInput, setEmailInput] = useState(initialEmail);
  const [tokenInput, setTokenInput] = useState(urlToken);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [status, setStatus] = useState({ error: '', done: false });
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    if (!emailInput.trim()) return setStatus({ error: 'Vui lòng nhập email.', done: false });
    if (!tokenInput.trim()) return setStatus({ error: 'Vui lòng nhập mã OTP.', done: false });
    if (!/^\d{6}$/.test(tokenInput.trim()) && !urlToken) {
      return setStatus({ error: 'Mã OTP phải gồm 6 chữ số.', done: false });
    }
    if (!strongPassword.test(password)) return setStatus({ error: 'Mật khẩu cần ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.', done: false });
    if (password !== confirm) return setStatus({ error: 'Mật khẩu xác nhận không khớp.', done: false });
    
    setLoading(true);
    try {
      await resetPassword(tokenInput.trim(), password, emailInput.trim());
      setStatus({ error: '', done: true });
    } catch (reason) {
      setStatus({ error: reason?.message || 'Mã OTP đã hết hạn hoặc không hợp lệ.', done: false });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthActionCard title="Đặt lại mật khẩu">
      {status.done ? (
        <div className="space-y-5">
          <p className="rounded-xl border border-emerald-800 bg-emerald-950/50 p-4 text-sm text-emerald-200">Mật khẩu đã được cập nhật thành công.</p>
          <Link to="/login" className="block text-center font-bold text-orange-400">Đăng nhập</Link>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold uppercase text-zinc-400 mb-1.5" htmlFor="reset-email">Email</label>
            <input 
              id="reset-email" 
              type="email" 
              required 
              placeholder="example@gmail.com"
              value={emailInput}
              onChange={(e) => setEmailInput(e.target.value)} 
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-white outline-none focus:border-orange-500 disabled:opacity-60" 
              disabled={!!initialEmail || loading}
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-zinc-400 mb-1.5" htmlFor="reset-otp">Mã xác thực OTP</label>
            <input 
              id="reset-otp" 
              type="text" 
              required 
              maxLength={6}
              placeholder="Nhập 6 số OTP"
              value={tokenInput}
              onChange={(e) => setTokenInput(e.target.value.replace(/\D/g, '').slice(0, 6))} 
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-white outline-none focus:border-orange-500 tracking-[0.2em] font-bold text-center text-lg" 
              disabled={!!urlToken || loading}
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-zinc-400 mb-1.5" htmlFor="reset-password">Mật khẩu mới</label>
            <input 
              id="reset-password" 
              type="password" 
              required 
              placeholder="Mật khẩu mới" 
              value={password}
              onChange={(e) => setPassword(e.target.value)} 
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-white outline-none focus:border-orange-500" 
              disabled={loading}
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase text-zinc-400 mb-1.5" htmlFor="reset-confirm">Xác nhận mật khẩu mới</label>
            <input 
              id="reset-confirm" 
              type="password" 
              required 
              placeholder="Xác nhận mật khẩu" 
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)} 
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-white outline-none focus:border-orange-500" 
              disabled={loading}
            />
          </div>

          {status.error && <p className="text-sm text-red-400">{status.error}</p>}
          <button disabled={loading} className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 disabled:opacity-60 cursor-pointer">
            {loading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
          </button>
        </form>
      )}
    </AuthActionCard>
  );
}

