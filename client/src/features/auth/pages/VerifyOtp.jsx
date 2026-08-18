import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AlertCircle, CheckCircle2, Loader2, MailCheck, RotateCcw } from 'lucide-react';
import { resendOtp, verifyOtp } from '@/features/auth/services/authService';
import { getCustomerErrorMessage } from '@/utils/customerErrorMessages';
import AuthShell, { AuthStepper } from '../components/AuthShell';

const maskEmail = email => {
  const [local = '', domain = ''] = email.split('@');
  if (!local || !domain) return email;
  return `${local.slice(0, 1)}${'*'.repeat(Math.max(3, local.length - 1))}@${domain}`;
};

export default function VerifyOtp() {
  const navigate = useNavigate();
  const location = useLocation();
  const otpInputRef = useRef(null);
  const email = location.state?.email || sessionStorage.getItem('pending_otp_email') || '';
  const purpose = location.state?.purpose || sessionStorage.getItem('pending_otp_purpose') || 'REGISTRATION';
  const [inputEmail, setInputEmail] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const resendImmediately = location.state?.resendImmediately === true
    || sessionStorage.getItem('pending_otp_resend_immediately') === 'true';
  const [countdown, setCountdown] = useState(resendImmediately ? 0 : 60);
  const [isResending, setIsResending] = useState(false);
  const [deliveryConfirmed, setDeliveryConfirmed] = useState(!resendImmediately);
  const [registrationExpired, setRegistrationExpired] = useState(false);
  const activeEmail = email || inputEmail;
  const otpDigits = useMemo(() => Array.from({ length: 6 }, (_, index) => otpCode[index] || ''), [otpCode]);

  useEffect(() => {
    if (countdown <= 0) return undefined;
    const timer = window.setInterval(() => setCountdown(previous => previous - 1), 1000);
    return () => window.clearInterval(timer);
  }, [countdown]);

  const handleResend = async () => {
    setError('');
    setSuccess('');
    setRegistrationExpired(false);
    if (!activeEmail.trim()) {
      setError('Vui lòng nhập địa chỉ email để nhận mã.');
      return;
    }

    setIsResending(true);
    try {
      const response = await resendOtp(activeEmail.trim(), purpose);
      if (!response?.success) {
        setError(getCustomerErrorMessage(response, 'Chưa thể gửi lại mã. Vui lòng thử lại.'));
        return;
      }
      setSuccess('Mã mới đã được gửi. Vui lòng kiểm tra cả thư mục spam.');
      setOtpCode('');
      setCountdown(response.data?.resendAvailableIn || 60);
      setDeliveryConfirmed(true);
      sessionStorage.setItem('pending_otp_resend_immediately', 'false');
      window.setTimeout(() => otpInputRef.current?.focus(), 0);
    } catch (requestError) {
      const errorCode = requestError?.errorCode || requestError?.code || requestError?.error;
      if (errorCode === 'AUTH_ACCOUNT_ALREADY_VERIFIED') {
        navigate('/login', { replace: true, state: { email: activeEmail, from: location.state?.from } });
        return;
      }
      if (errorCode === 'OTP_RATE_LIMIT') {
        const retryAfter = requestError?.data?.retryAfter || requestError?.retryAfter || 60;
        setCountdown(retryAfter);
        setError(`Bạn vừa yêu cầu mã. Vui lòng thử lại sau ${retryAfter} giây.`);
        return;
      }
      if (errorCode === 'AUTH_REGISTRATION_EXPIRED') {
        setCountdown(0);
        setDeliveryConfirmed(false);
        setRegistrationExpired(true);
        sessionStorage.setItem('pending_otp_resend_immediately', 'true');
        setError('Phiên đăng ký đã hết hạn nên tài khoản chưa thể kích hoạt. Vui lòng đăng ký lại để tạo một phiên xác minh mới.');
        return;
      }
      if (errorCode === 'AUTH_OTP_DELIVERY_FAILED') {
        setCountdown(0);
        setDeliveryConfirmed(false);
        sessionStorage.setItem('pending_otp_resend_immediately', 'true');
        setError('Máy chủ email đã từ chối thư nên mã chưa được gửi. Bạn có thể bấm “Gửi lại mã” ngay sau khi cấu hình email được xử lý.');
        return;
      }
      if (errorCode === 'AUTH_OTP_DELIVERY_PENDING') {
        setCountdown(60);
        setError('Yêu cầu gửi mã vẫn đang được xử lý. Vui lòng kiểm tra hộp thư và thư mục spam trước khi yêu cầu mã khác.');
        return;
      }
      setCountdown(0);
      setError(getCustomerErrorMessage(requestError, 'Không thể gửi lại mã lúc này. Vui lòng thử lại sau.'));
    } finally {
      setIsResending(false);
    }
  };

  const handleSubmit = async event => {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (!activeEmail.trim()) {
      setError('Vui lòng cung cấp email cần xác minh.');
      return;
    }
    if (!/^\d{6}$/.test(otpCode)) {
      setError('Mã xác minh phải gồm đủ 6 chữ số.');
      otpInputRef.current?.focus();
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await verifyOtp(activeEmail.trim(), otpCode, purpose);
      if (!response?.success) {
        setError(getCustomerErrorMessage(response, 'Xác minh không thành công. Vui lòng thử lại.'));
        return;
      }
      sessionStorage.removeItem('pending_otp_email');
      sessionStorage.removeItem('pending_otp_purpose');
      sessionStorage.removeItem('pending_otp_resend_immediately');
      setSuccess('Email đã được xác minh. Đang chuyển sang đăng nhập…');
      window.setTimeout(() => {
        navigate('/login', {
          replace: true,
          state: { email: activeEmail.trim(), verified: true, from: location.state?.from },
        });
      }, 700);
    } catch (requestError) {
      const errorCode = requestError?.errorCode || requestError?.code || requestError?.error;
      const messages = {
        AUTH_INVALID_OTP: 'Mã xác minh chưa chính xác. Vui lòng kiểm tra lại.',
        AUTH_VERIFICATION_EXPIRED: 'Mã đã hết hạn. Hãy yêu cầu gửi lại mã mới.',
        AUTH_ACCOUNT_NOT_FOUND: 'Không tìm thấy tài khoản tương ứng.',
        INTERNAL_SERVER_ERROR: 'Hệ thống đang bận. Vui lòng thử lại sau.',
      };
      setError(messages[errorCode] || getCustomerErrorMessage(requestError, 'Không thể xác minh. Vui lòng thử lại.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthShell maxWidth="max-w-md">
      {purpose === 'REGISTRATION' && <AuthStepper currentStep={3} />}
      <header className="mb-7 text-center">
        <span className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl border border-brand-orange/20 bg-brand-orange/10 text-brand-orange">
          <MailCheck aria-hidden="true" className="h-6 w-6" />
        </span>
        <p className="mb-2 text-[10px] font-black uppercase tracking-[0.24em] text-brand-orange">Bước 3 trong 3</p>
        <h1 className="text-2xl font-black uppercase tracking-[0.07em] text-white sm:text-3xl">Xác minh email</h1>
        <p className="mx-auto mt-2 max-w-sm text-sm leading-relaxed text-zinc-500">
          {activeEmail
            ? deliveryConfirmed
              ? <>Nhập mã gồm 6 chữ số đã gửi tới <strong className="font-bold text-zinc-300">{maskEmail(activeEmail)}</strong>.</>
              : <>Tài khoản <strong className="font-bold text-zinc-300">{maskEmail(activeEmail)}</strong> chưa được xác minh. Hãy yêu cầu một mã mới.</>
            : 'Nhập email và mã gồm 6 chữ số để kích hoạt tài khoản.'}
        </p>
      </header>

      {error && (
        <div role="alert" className="mb-5 flex gap-3 rounded-xl border border-red-900/70 bg-red-950/30 p-3.5 text-sm leading-relaxed text-red-200">
          <AlertCircle aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-red-400" />
          <span>{error}</span>
        </div>
      )}
      {success && (
        <div role="status" className="mb-5 flex gap-3 rounded-xl border border-emerald-900/70 bg-emerald-950/30 p-3.5 text-sm leading-relaxed text-emerald-200">
          <CheckCircle2 aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />
          <span>{success}</span>
        </div>
      )}
      {registrationExpired && (
        <button
          type="button"
          onClick={() => navigate('/register', { replace: true })}
          className="mb-5 flex min-h-11 w-full items-center justify-center rounded-xl border border-brand-orange/40 px-4 py-3 text-xs font-black uppercase tracking-wider text-brand-orange transition hover:bg-brand-orange/10"
        >
          Quay lại đăng ký
        </button>
      )}

      <form onSubmit={handleSubmit} className="space-y-5" noValidate>
        {!email && (
          <div className="space-y-1.5">
            <label htmlFor="inputEmail" className="block text-xs font-black uppercase tracking-wider text-zinc-400">Địa chỉ email</label>
            <input id="inputEmail" name="inputEmail" type="email" inputMode="email" autoComplete="email" placeholder="tenban@email.com" value={inputEmail} onChange={event => setInputEmail(event.target.value)} className="min-h-12 w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-brand-orange focus:ring-2 focus:ring-brand-orange/10" disabled={isSubmitting} />
          </div>
        )}

        <div className="space-y-2">
          <label htmlFor="otp" className="block text-xs font-black uppercase tracking-wider text-zinc-400">Mã xác minh</label>
          <div className="relative grid grid-cols-6 gap-2" onClick={() => otpInputRef.current?.focus()}>
            <input
              ref={otpInputRef}
              id="otp"
              name="otp"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              value={otpCode}
              onChange={event => setOtpCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
              maxLength={6}
              aria-label="Mã xác minh gồm 6 chữ số"
              disabled={isSubmitting}
              className="peer absolute inset-0 z-10 cursor-text opacity-0"
            />
            {otpDigits.map((digit, index) => (
              <span key={index} aria-hidden="true" className={`flex aspect-square items-center justify-center rounded-xl border bg-zinc-950 text-xl font-black transition peer-focus:border-brand-orange peer-focus:ring-2 peer-focus:ring-brand-orange/10 ${digit ? 'border-brand-orange/50 text-white' : 'border-zinc-800 text-zinc-700'}`}>
                {digit || '·'}
              </span>
            ))}
          </div>
        </div>

        <button type="submit" disabled={isSubmitting} className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-3.5 text-sm font-black uppercase tracking-[0.12em] text-zinc-950 transition hover:bg-orange-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-[#141417] disabled:cursor-not-allowed disabled:opacity-60">
          {isSubmitting && <Loader2 aria-hidden="true" className="h-4 w-4 animate-spin" />}
          {isSubmitting ? 'Đang xác minh…' : 'Hoàn tất đăng ký'}
        </button>

        <div className="text-center text-xs">
          {countdown > 0 ? (
            <span className="font-semibold text-zinc-600">Gửi lại mã sau {countdown} giây</span>
          ) : (
            <button type="button" onClick={handleResend} disabled={isResending} className="inline-flex items-center gap-2 font-black text-brand-orange hover:underline focus:outline-none focus-visible:underline disabled:opacity-50">
              {isResending ? <Loader2 aria-hidden="true" className="h-3.5 w-3.5 animate-spin" /> : <RotateCcw aria-hidden="true" className="h-3.5 w-3.5" />}
              {isResending ? 'Đang gửi…' : 'Gửi lại mã'}
            </button>
          )}
        </div>
      </form>
    </AuthShell>
  );
}
