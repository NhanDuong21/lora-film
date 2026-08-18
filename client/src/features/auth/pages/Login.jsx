import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AlertCircle, CheckCircle2, Eye, EyeOff, Loader2, Lock, Mail, ShieldCheck } from 'lucide-react';
import { login } from '@/features/auth/services/authService';
import { useAuth } from '@/contexts/AuthContext';
import { getCustomerErrorMessage } from '@/utils/customerErrorMessages';
import { getUserPermissions } from '@/utils/authStorage';
import AuthShell, { AuthDivider, GoogleButton } from '../components/AuthShell';
import { consumeAuthReturn, rememberAuthReturn } from '../utils/authReturn';
import { resolvePostLoginPath } from '../utils/loginRedirect';

const loginErrorMessages = {
  AUTH_INVALID_CREDENTIALS: 'Email hoặc mật khẩu chưa chính xác.',
  AUTH_ACCOUNT_INACTIVE: 'Tài khoản đang tạm khóa. Vui lòng liên hệ hỗ trợ.',
  AUTH_ACCOUNT_LOCKED: 'Tài khoản đang tạm khóa. Vui lòng liên hệ hỗ trợ.',
  AUTH_TOO_MANY_ATTEMPTS: 'Bạn đã thử quá nhiều lần. Vui lòng đợi một lúc rồi thử lại.',
  VALIDATION_ERROR: 'Thông tin đăng nhập chưa hợp lệ. Vui lòng kiểm tra lại.',
  INTERNAL_SERVER_ERROR: 'Hệ thống đang bận. Vui lòng thử lại sau.',
};

export default function Login() {
  const { login: contextLogin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState(() => location.state?.email || '');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [capsLockOn, setCapsLockOn] = useState(false);
  const [errorMsg, setErrorMsg] = useState(() => location.state?.error || '');
  const [successMessage, setSuccessMessage] = useState(() => location.state?.message || (
    location.state?.verified ? 'Email đã được xác minh. Bạn có thể đăng nhập ngay.' : ''
  ));
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async event => {
    event.preventDefault();
    setErrorMsg('');
    setSuccessMessage('');

    if (!email.trim() || !password) {
      setErrorMsg('Vui lòng nhập đầy đủ email và mật khẩu.');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await login(email.trim(), password, rememberMe);
      if (!response?.success || !response?.data) {
        setErrorMsg('Hệ thống chưa thể đăng nhập lúc này. Vui lòng thử lại.');
        return;
      }

      await contextLogin({ ...response.data, rememberMe });
      setSuccessMessage('Đăng nhập thành công. Đang đưa bạn trở lại...');
      window.setTimeout(() => {
        const rememberedFrom = consumeAuthReturn();
        navigate(resolvePostLoginPath({
          role: response.data.role,
          permissions: getUserPermissions(),
          from: location.state?.from || rememberedFrom,
        }), { replace: true });
      }, 350);
    } catch (error) {
      const errorCode = error?.errorCode || error?.code || error?.error;

      if (errorCode === 'AUTH_ACCOUNT_NOT_VERIFIED') {
        sessionStorage.setItem('pending_otp_email', email.trim());
        sessionStorage.setItem('pending_otp_purpose', 'REGISTRATION');
        sessionStorage.setItem('pending_otp_resend_immediately', 'true');
        setErrorMsg('Tài khoản chưa xác minh email. Bạn có thể nhập mã hoặc yêu cầu gửi lại mã mới.');
        window.setTimeout(() => {
          navigate('/verify-otp', {
            state: {
              email: email.trim(),
              purpose: 'REGISTRATION',
              resendImmediately: true,
              from: location.state?.from,
            },
          });
        }, 900);
        return;
      }

      if (errorCode === 'AUTH_GOOGLE_ACCOUNT' || errorCode === 'AUTH_PASSWORD_NOT_SET') {
        setErrorMsg('Tài khoản này sử dụng Google. Hãy chọn “Tiếp tục với Google”.');
        return;
      }

      if (!error?.response && !error?.status) {
        setErrorMsg('Không thể kết nối. Hãy kiểm tra Internet và thử lại.');
        return;
      }

      setErrorMsg(loginErrorMessages[errorCode] || getCustomerErrorMessage(
        error,
        'Không thể đăng nhập. Vui lòng thử lại sau.'
      ));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthShell maxWidth="max-w-md">
      <header className="mb-7 text-center">
        <p className="mb-2 text-[10px] font-black uppercase tracking-[0.24em] text-brand-orange">
          Thành viên LoraFilm
        </p>
        <h1 className="text-3xl font-black uppercase tracking-[0.08em] text-white">Đăng nhập</h1>
        <p className="mx-auto mt-2 max-w-sm text-sm leading-relaxed text-zinc-500">
          Quản lý vé, ưu đãi và điểm thành viên của bạn.
        </p>
      </header>

      {errorMsg && (
        <div id="login-error" role="alert" className="mb-5 flex gap-3 rounded-xl border border-red-900/70 bg-red-950/30 p-3.5 text-sm leading-relaxed text-red-200">
          <AlertCircle aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-red-400" />
          <span>{errorMsg}</span>
        </div>
      )}

      {successMessage && (
        <div role="status" className="mb-5 flex gap-3 rounded-xl border border-emerald-900/70 bg-emerald-950/30 p-3.5 text-sm leading-relaxed text-emerald-200">
          <CheckCircle2 aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />
          <span>{successMessage}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-5" noValidate>
        <div className="space-y-1.5">
          <label htmlFor="email-input" className="block text-xs font-black uppercase tracking-wider text-zinc-400">
            Địa chỉ email
          </label>
          <div className="relative">
            <Mail aria-hidden="true" className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
            <input
              id="email-input"
              name="email"
              type="email"
              inputMode="email"
              autoComplete="email"
              placeholder="tenban@email.com"
              value={email}
              onChange={event => setEmail(event.target.value)}
              aria-invalid={Boolean(errorMsg)}
              aria-describedby={errorMsg ? 'login-error' : undefined}
              className="min-h-12 w-full rounded-xl border border-zinc-800 bg-zinc-950 py-3 pl-11 pr-4 text-sm text-zinc-100 outline-none transition placeholder:text-zinc-600 hover:border-zinc-700 focus:border-brand-orange focus:ring-2 focus:ring-brand-orange/10"
              required
              disabled={isSubmitting}
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <div className="flex items-center justify-between gap-4">
            <label htmlFor="password-input" className="block text-xs font-black uppercase tracking-wider text-zinc-400">
              Mật khẩu
            </label>
            <Link to="/forgot-password" className="text-xs font-bold text-brand-orange hover:underline focus:outline-none focus-visible:underline">
              Quên mật khẩu?
            </Link>
          </div>
          <div className="relative">
            <Lock aria-hidden="true" className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
            <input
              id="password-input"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="Nhập mật khẩu"
              value={password}
              onChange={event => setPassword(event.target.value)}
              onKeyUp={event => setCapsLockOn(event.getModifierState('CapsLock'))}
              onKeyDown={event => setCapsLockOn(event.getModifierState('CapsLock'))}
              className="min-h-12 w-full rounded-xl border border-zinc-800 bg-zinc-950 py-3 pl-11 pr-12 text-sm text-zinc-100 outline-none transition placeholder:text-zinc-600 hover:border-zinc-700 focus:border-brand-orange focus:ring-2 focus:ring-brand-orange/10"
              required
              disabled={isSubmitting}
            />
            <button
              type="button"
              className="absolute right-1.5 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-lg text-zinc-500 transition hover:bg-zinc-900 hover:text-zinc-300 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange"
              onClick={() => setShowPassword(current => !current)}
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              disabled={isSubmitting}
            >
              {showPassword ? <EyeOff aria-hidden="true" className="h-4 w-4" /> : <Eye aria-hidden="true" className="h-4 w-4" />}
            </button>
          </div>
          {capsLockOn && <p className="text-xs font-semibold text-amber-400">Caps Lock đang bật.</p>}
        </div>

        <label className="group flex cursor-pointer items-start gap-3 rounded-xl p-1 text-sm text-zinc-400">
          <input
            type="checkbox"
            checked={rememberMe}
            onChange={event => setRememberMe(event.target.checked)}
            disabled={isSubmitting}
            className="peer sr-only"
          />
          <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-md border border-zinc-700 bg-zinc-950 text-transparent transition group-hover:border-zinc-600 peer-checked:border-brand-orange peer-checked:bg-brand-orange peer-checked:text-zinc-950 peer-focus-visible:ring-2 peer-focus-visible:ring-brand-orange peer-focus-visible:ring-offset-2 peer-focus-visible:ring-offset-[#141417]">
            <CheckCircle2 aria-hidden="true" className="h-3.5 w-3.5" />
          </span>
          <span>
            <span className="block font-semibold text-zinc-300">Ghi nhớ đăng nhập</span>
            <span className="mt-0.5 block text-xs text-zinc-600">Chỉ nên bật trên thiết bị cá nhân.</span>
          </span>
        </label>

        <button
          type="submit"
          disabled={isSubmitting}
          className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-3.5 text-sm font-black uppercase tracking-[0.14em] text-zinc-950 shadow-lg shadow-brand-orange/10 transition hover:bg-orange-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-[#141417] disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmitting && <Loader2 aria-hidden="true" className="h-4 w-4 animate-spin" />}
          {isSubmitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </button>
      </form>

      <AuthDivider />
      <GoogleButton onStart={() => rememberAuthReturn(location.state?.from)} />

      <footer className="mt-6 flex items-center justify-center gap-1.5 text-center text-xs text-zinc-500">
        <ShieldCheck aria-hidden="true" className="h-4 w-4" />
        Phiên đăng nhập được bảo vệ trên kết nối an toàn.
      </footer>
    </AuthShell>
  );
}
