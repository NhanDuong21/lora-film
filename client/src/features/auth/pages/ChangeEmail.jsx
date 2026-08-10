import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthActionCard from '../components/AuthActionCard';
import { requestChangeEmail, verifyChangeEmail } from '../services/authService';
import { useAuth } from '@/contexts/AuthContext';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function ChangeEmail() {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout, account } = useAuth();
  
  const [step, setStep] = useState('STEP_REQUEST'); // 'STEP_REQUEST' or 'STEP_VERIFY'
  const [form, setForm] = useState({ newEmail: location.state?.newEmail || '', password: '', otp: '' });
  const [feedback, setFeedback] = useState({ error: '', success: '' });
  const [loading, setLoading] = useState(false);

  const submitRequest = async (event) => {
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
      await requestChangeEmail(newEmail, form.password);
      setFeedback({
        error: '',
        success: `Mã OTP đã được gửi đến email hiện tại (${account?.email}). Vui lòng kiểm tra hộp thư.`
      });
      setStep('STEP_VERIFY');
    } catch (reason) {
      setFeedback({
        error: reason?.message || 'Không thể yêu cầu thay đổi email. Vui lòng kiểm tra mật khẩu và thử lại.',
        success: ''
      });
    } finally {
      setLoading(false);
    }
  };

  const submitVerify = async (event) => {
    event.preventDefault();
    if (!form.otp || form.otp.length !== 6) {
      setFeedback({ error: 'Vui lòng nhập mã OTP 6 chữ số.', success: '' });
      return;
    }

    setLoading(true);
    setFeedback({ error: '', success: '' });
    try {
      await verifyChangeEmail(form.otp);
      setFeedback({
        error: '',
        success: 'Email đã được thay đổi. Tất cả phiên đăng nhập đã được thu hồi.'
      });
      await logout();
      navigate('/login', {
        replace: true,
        state: { email: form.newEmail, message: 'Vui lòng đăng nhập lại bằng email mới.' }
      });
    } catch (reason) {
      setFeedback({
        error: reason?.message || 'Mã OTP không hợp lệ hoặc đã hết hạn.',
        success: ''
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthActionCard
      title="Thay đổi email"
      subtitle={step === 'STEP_REQUEST' ? "Email mới sẽ trở thành định danh đăng nhập của tài khoản." : "Xác thực yêu cầu thay đổi email."}
    >
      {step === 'STEP_REQUEST' ? (
        <form onSubmit={submitRequest} className="space-y-4" noValidate>
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
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm focus:border-brand-orange outline-none transition-colors"
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
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm focus:border-brand-orange outline-none transition-colors"
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
            className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 hover:bg-brand-orange transition-colors disabled:opacity-60"
          >
            {loading ? 'Đang gửi...' : 'Gửi mã xác nhận'}
          </button>
        </form>
      ) : (
        <form onSubmit={submitVerify} className="space-y-4" noValidate>
          <div className="text-sm text-zinc-300 mb-4">
            Mã xác nhận (OTP) đã được gửi đến địa chỉ email hiện tại của bạn <span className="font-bold text-brand-orange">{account?.email}</span>. Vui lòng kiểm tra hộp thư (bao gồm cả mục Spam) và nhập mã vào bên dưới để tiếp tục.
          </div>
          <div>
            <label htmlFor="otp" className="mb-1.5 block text-xs font-bold text-zinc-400">
              Mã xác nhận (OTP)
            </label>
            <input
              id="otp"
              type="text"
              inputMode="numeric"
              maxLength={6}
              value={form.otp}
              onChange={(event) => setForm((value) => ({ ...value, otp: event.target.value.replace(/\D/g, '') }))}
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-center text-xl font-mono tracking-widest focus:border-brand-orange outline-none transition-colors"
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
          <div className="flex flex-col gap-2">
            <button
              type="submit"
              disabled={loading || form.otp.length !== 6}
              className="w-full rounded-xl bg-orange-500 py-3 font-black text-zinc-950 hover:bg-brand-orange transition-colors disabled:opacity-60"
            >
              {loading ? 'Đang xác thực...' : 'Xác nhận thay đổi'}
            </button>
            <button
              type="button"
              onClick={() => { setStep('STEP_REQUEST'); setFeedback({ error: '', success: '' }); }}
              disabled={loading}
              className="w-full rounded-xl bg-zinc-800 py-3 font-bold text-zinc-300 hover:bg-zinc-700 transition-colors disabled:opacity-60"
            >
              Quay lại
            </button>
          </div>
        </form>
      )}
    </AuthActionCard>
  );
}
