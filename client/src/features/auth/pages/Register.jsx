import { useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  Check,
  CheckCircle2,
  CreditCard,
  Eye,
  EyeOff,
  Info,
  Loader2,
  Lock,
  Mail,
  Phone,
  RefreshCw,
  ShieldCheck,
  UserRound,
} from 'lucide-react';
import { inspectIdentityNumber, register } from '@/features/auth/services/authService';
import { getCustomerErrorMessage } from '@/utils/customerErrorMessages';
import AuthShell, { AuthDivider, AuthStepper, GoogleButton } from '../components/AuthShell';
import { rememberAuthReturn } from '../utils/authReturn';

const initialFormData = {
  fullName: '',
  email: '',
  phoneNumber: '',
  cccd: '',
  birthday: '',
  password: '',
  confirmPassword: '',
};

const accountFields = ['fullName', 'email', 'phoneNumber', 'password', 'confirmPassword'];
const profileFields = ['cccd', 'birthday'];

const inputClass = error => `min-h-12 w-full rounded-xl border bg-zinc-950 py-3 pl-11 pr-4 text-sm text-zinc-100 outline-none transition placeholder:text-zinc-600 hover:border-zinc-700 focus:ring-2 focus:ring-brand-orange/10 ${
  error ? 'border-red-500/80 focus:border-red-500' : 'border-zinc-800 focus:border-brand-orange'
}`;

function FieldMessage({ id, error, children }) {
  if (error) {
    return (
      <p id={id} role="alert" className="flex items-start gap-1.5 text-xs font-semibold leading-relaxed text-red-400">
        <AlertCircle aria-hidden="true" className="mt-0.5 h-3.5 w-3.5 shrink-0" />
        {error}
      </p>
    );
  }
  return children ? <p id={id} className="text-xs leading-relaxed text-zinc-600">{children}</p> : null;
}

function TextField({
  label,
  name,
  type = 'text',
  value,
  onChange,
  onBlur,
  error,
  icon: Icon,
  helper,
  autoComplete,
  inputMode,
  maxLength,
  disabled,
}) {
  const messageId = `${name}-message`;
  return (
    <div className="space-y-1.5">
      <label htmlFor={name} className="block text-xs font-black uppercase tracking-wider text-zinc-400">
        {label}
      </label>
      <div className="relative">
        <Icon aria-hidden="true" className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
        <input
          id={name}
          name={name}
          type={type}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          placeholder={helper?.placeholder}
          autoComplete={autoComplete}
          inputMode={inputMode}
          maxLength={maxLength}
          disabled={disabled}
          aria-invalid={Boolean(error)}
          aria-describedby={error || helper?.text ? messageId : undefined}
          className={inputClass(error)}
        />
      </div>
      <FieldMessage id={messageId} error={error}>{helper?.text}</FieldMessage>
    </div>
  );
}

export default function Register() {
  const navigate = useNavigate();
  const location = useLocation();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState(initialFormData);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [marketingConsent, setMarketingConsent] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [capsLockOn, setCapsLockOn] = useState(false);
  const [identityInfo, setIdentityInfo] = useState(null);
  const [isInspecting, setIsInspecting] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [globalError, setGlobalError] = useState('');
  const [globalSuccess, setGlobalSuccess] = useState('');

  const passwordRules = useMemo(() => [
    { label: '8–50 ký tự', met: formData.password.length >= 8 && formData.password.length <= 50 },
    { label: 'Có chữ hoa', met: /[A-Z]/.test(formData.password) },
    { label: 'Có chữ thường', met: /[a-z]/.test(formData.password) },
    { label: 'Có chữ số', met: /\d/.test(formData.password) },
    { label: 'Có ký tự đặc biệt', met: /[!@#$%^&*(),.?":{}|<>]/.test(formData.password) },
  ], [formData.password]);

  const derivedBirthYear = identityInfo?.birthYear;
  const enteredBirthYear = formData.birthday ? Number(formData.birthday.slice(0, 4)) : null;
  const birthYearMismatch = Boolean(derivedBirthYear && enteredBirthYear && derivedBirthYear !== enteredBirthYear);

  const validateField = (name, value = formData[name]) => {
    switch (name) {
      case 'fullName': {
        if (!value.trim()) return 'Vui lòng nhập họ và tên.';
        if (value.trim().length > 200) return 'Họ và tên không được vượt quá 200 ký tự.';
        if (!/^[a-zA-ZÀ-ỹ\s]+$/.test(value)) return 'Họ và tên không được chứa số hoặc ký tự đặc biệt.';
        if (value.trim().split(/\s+/).length < 2) return 'Họ và tên cần có ít nhất 2 từ.';
        return '';
      }
      case 'email':
        if (!value.trim()) return 'Vui lòng nhập địa chỉ email.';
        if (value.trim().length > 100 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Email chưa đúng định dạng.';
        return '';
      case 'phoneNumber':
        if (!value) return 'Vui lòng nhập số điện thoại.';
        if (!/^0\d{9}$/.test(value)) return 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.';
        return '';
      case 'cccd':
        if (!value) return 'Vui lòng nhập số định danh cá nhân.';
        if (!/^\d{12}$/.test(value)) return 'Số định danh phải gồm đúng 12 chữ số.';
        return '';
      case 'birthday': {
        if (!value) return 'Vui lòng chọn ngày sinh.';
        const birthDate = new Date(`${value}T00:00:00`);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (Number.isNaN(birthDate.getTime()) || birthDate > today) return 'Ngày sinh chưa hợp lệ.';
        let age = today.getFullYear() - birthDate.getFullYear();
        if (today < new Date(today.getFullYear(), birthDate.getMonth(), birthDate.getDate())) age -= 1;
        if (age < 13) return 'Bạn cần đủ 13 tuổi để đăng ký thành viên.';
        if (birthYearMismatch) return `Năm sinh đã nhập là ${enteredBirthYear}, nhưng mã định danh thể hiện ${derivedBirthYear}.`;
        return '';
      }
      case 'password':
        if (!value) return 'Vui lòng nhập mật khẩu.';
        if (!passwordRules.every(rule => rule.met)) return 'Mật khẩu chưa đáp ứng đầy đủ các yêu cầu bên dưới.';
        return '';
      case 'confirmPassword':
        if (!value) return 'Vui lòng nhập lại mật khẩu.';
        if (value !== formData.password) return 'Mật khẩu xác nhận chưa khớp.';
        return '';
      default:
        return '';
    }
  };

  const updateField = event => {
    const { name } = event.target;
    let { value } = event.target;
    if (name === 'phoneNumber') value = value.replace(/\D/g, '').slice(0, 10);
    if (name === 'cccd') value = value.replace(/\D/g, '').slice(0, 12);

    setFormData(previous => ({ ...previous, [name]: value }));
    setGlobalError('');
    if (name === 'cccd') setIdentityInfo(null);

    if (touched[name]) {
      setErrors(previous => ({ ...previous, [name]: validateField(name, value) }));
    }
    if (name === 'password' && touched.confirmPassword) {
      setErrors(previous => ({
        ...previous,
        confirmPassword: formData.confirmPassword && formData.confirmPassword !== value
          ? 'Mật khẩu xác nhận chưa khớp.'
          : '',
      }));
    }
  };

  const blurField = event => {
    const { name, value } = event.target;
    setTouched(previous => ({ ...previous, [name]: true }));
    setErrors(previous => ({ ...previous, [name]: validateField(name, value) }));
  };

  const validateFields = fields => {
    const nextErrors = {};
    const nextTouched = {};
    fields.forEach(field => {
      nextTouched[field] = true;
      nextErrors[field] = validateField(field);
    });
    setTouched(previous => ({ ...previous, ...nextTouched }));
    setErrors(previous => ({ ...previous, ...nextErrors }));
    const invalidField = fields.find(field => nextErrors[field]);
    if (invalidField) document.getElementById(invalidField)?.focus();
    return !invalidField;
  };

  const continueToProfile = event => {
    event.preventDefault();
    setGlobalError('');
    const valid = validateFields(accountFields);
    if (!acceptedTerms) {
      setErrors(previous => ({ ...previous, terms: 'Bạn cần đồng ý với Điều khoản sử dụng và Chính sách bảo mật.' }));
    }
    if (!valid || !acceptedTerms) return;
    setErrors(previous => ({ ...previous, terms: '' }));
    setCurrentStep(2);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const inspectCccd = async () => {
    setTouched(previous => ({ ...previous, cccd: true }));
    const cccdError = validateField('cccd');
    setErrors(previous => ({ ...previous, cccd: cccdError }));
    setIdentityInfo(null);
    setGlobalError('');
    if (cccdError) return;

    setIsInspecting(true);
    try {
      const response = await inspectIdentityNumber(formData.cccd);
      if (!response?.success || !response?.data) throw new Error('Identity number insight response is empty');
      setIdentityInfo(response.data);
      if (formData.birthday) {
        const mismatch = Number(formData.birthday.slice(0, 4)) !== response.data.birthYear;
        setErrors(previous => ({
          ...previous,
          birthday: mismatch
            ? `Năm sinh đã nhập là ${formData.birthday.slice(0, 4)}, nhưng mã định danh thể hiện ${response.data.birthYear}.`
            : '',
        }));
      }
    } catch (error) {
      const errorCode = error?.errorCode || error?.code || error?.error;
      const message = errorCode === 'USER_CCCD_INVALID'
        ? 'Không thể đọc cấu trúc số định danh này. Vui lòng kiểm tra lại 12 chữ số.'
        : 'Chưa thể kiểm tra số định danh lúc này. Vui lòng thử lại.';
      setErrors(previous => ({ ...previous, cccd: message }));
    } finally {
      setIsInspecting(false);
    }
  };

  const handleSubmit = async event => {
    event.preventDefault();
    setGlobalError('');
    setGlobalSuccess('');
    if (!validateFields(profileFields)) return;
    if (!identityInfo) {
      setErrors(previous => ({ ...previous, cccd: 'Hãy kiểm tra thông tin suy ra trước khi tiếp tục.' }));
      return;
    }
    if (birthYearMismatch) return;

    setIsSubmitting(true);
    try {
      const response = await register({
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phoneNumber: formData.phoneNumber,
        cccd: formData.cccd,
        birthday: formData.birthday,
        password: formData.password,
      });

      if (!response?.success && response?.message !== 'Registration initiated') {
        setGlobalError(getCustomerErrorMessage(response, 'Đăng ký không thành công. Vui lòng thử lại.'));
        return;
      }
      if (!response?.data?.requestId) throw new Error('Registration response did not contain a request ID');

      sessionStorage.setItem('pending_otp_email', formData.email.trim());
      sessionStorage.setItem('pending_otp_purpose', 'REGISTRATION');
      rememberAuthReturn(location.state?.from);
      setGlobalSuccess('Thông tin đã được ghi nhận. Đang chuyển sang bước xác minh email…');
      window.setTimeout(() => {
        navigate('/verify-otp', {
          state: {
            email: formData.email.trim(),
            purpose: 'REGISTRATION',
            from: location.state?.from,
          },
        });
      }, 600);
    } catch (error) {
      const errorCode = error?.errorCode || error?.code || error?.error;
      const fieldErrorMap = {
        AUTH_EMAIL_ALREADY_EXISTS: ['email', 'Email này đã được sử dụng.', 1],
        PHONE_NUMBER_ALREADY_EXISTS: ['phoneNumber', 'Số điện thoại này đã được sử dụng.', 1],
        CCCD_ALREADY_EXISTS: ['cccd', 'Số định danh này đã được sử dụng.', 2],
        USER_CCCD_ALREADY_EXISTS: ['cccd', 'Số định danh này đã được sử dụng.', 2],
        USER_CCCD_INVALID: ['cccd', 'Không thể đọc cấu trúc số định danh này.', 2],
        USER_BIRTHDAY_CCCD_MISMATCH: ['birthday', 'Năm sinh không khớp với mã định danh.', 2],
      };
      const mapped = fieldErrorMap[errorCode];
      if (mapped) {
        const [field, message, step] = mapped;
        setErrors(previous => ({ ...previous, [field]: message }));
        setTouched(previous => ({ ...previous, [field]: true }));
        setCurrentStep(step);
        window.setTimeout(() => document.getElementById(field)?.focus(), 0);
        return;
      }

      if (errorCode === 'REGISTRATION_ALREADY_PENDING') {
        sessionStorage.setItem('pending_otp_email', formData.email.trim());
        sessionStorage.setItem('pending_otp_purpose', 'REGISTRATION');
        navigate('/verify-otp', {
          state: { email: formData.email.trim(), purpose: 'REGISTRATION', from: location.state?.from },
        });
        return;
      }

      if (errorCode === 'VALIDATION_ERROR' && Array.isArray(error.errors)) {
        const nextErrors = {};
        error.errors.forEach(item => { nextErrors[item.field] = getCustomerErrorMessage(item, 'Giá trị này chưa hợp lệ.'); });
        setErrors(previous => ({ ...previous, ...nextErrors }));
        if (accountFields.some(field => nextErrors[field])) setCurrentStep(1);
        setGlobalError('Một số thông tin chưa hợp lệ. Vui lòng kiểm tra các trường được đánh dấu.');
        return;
      }

      if (['PHONE_NUMBER_RESERVED', 'CCCD_RESERVED', 'PHONE_NUMBER_AND_CCCD_RESERVED'].includes(errorCode)) {
        const retrySeconds = error?.data?.retryAfterSeconds || error?.retryAfterSeconds || 60;
        setGlobalError(`Thông tin này thuộc một đăng ký đang chờ xử lý. Vui lòng thử lại sau ${retrySeconds} giây.`);
        return;
      }

      setGlobalError(getCustomerErrorMessage(error, 'Không thể đăng ký tài khoản. Vui lòng thử lại sau.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthShell maxWidth="max-w-xl">
      <AuthStepper currentStep={currentStep} />

      <header className="mb-6 text-center">
        <p className="mb-2 text-[10px] font-black uppercase tracking-[0.24em] text-brand-orange">
          Bước {currentStep} trong 3
        </p>
        <h1 className="text-2xl font-black uppercase tracking-[0.07em] text-white sm:text-3xl">
          {currentStep === 1 ? 'Tạo tài khoản' : 'Hoàn thiện hồ sơ'}
        </h1>
        <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-zinc-500">
          {currentStep === 1
            ? 'Tạo thông tin đăng nhập để quản lý vé, nhận ưu đãi và tích điểm.'
            : 'Bổ sung thông tin bắt buộc và kiểm tra dữ liệu được suy ra trước khi gửi.'}
        </p>
      </header>

      {globalError && (
        <div role="alert" className="mb-5 flex gap-3 rounded-xl border border-red-900/70 bg-red-950/30 p-3.5 text-sm leading-relaxed text-red-200">
          <AlertCircle aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-red-400" />
          <span>{globalError}</span>
        </div>
      )}
      {globalSuccess && (
        <div role="status" className="mb-5 flex gap-3 rounded-xl border border-emerald-900/70 bg-emerald-950/30 p-3.5 text-sm leading-relaxed text-emerald-200">
          <CheckCircle2 aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />
          <span>{globalSuccess}</span>
        </div>
      )}

      {currentStep === 1 ? (
        <>
          <GoogleButton onStart={() => rememberAuthReturn(location.state?.from)} />
          <AuthDivider>Hoặc đăng ký bằng email</AuthDivider>

          <form onSubmit={continueToProfile} className="space-y-4" noValidate>
            <TextField
              label="Họ và tên"
              name="fullName"
              value={formData.fullName}
              onChange={updateField}
              onBlur={blurField}
              error={touched.fullName ? errors.fullName : ''}
              icon={UserRound}
              autoComplete="name"
              helper={{ placeholder: 'Nhập họ và tên của bạn' }}
            />
            <TextField
              label="Địa chỉ email"
              name="email"
              type="email"
              value={formData.email}
              onChange={updateField}
              onBlur={blurField}
              error={touched.email ? errors.email : ''}
              icon={Mail}
              autoComplete="email"
              inputMode="email"
              helper={{ placeholder: 'tenban@email.com', text: 'Mã xác minh sẽ được gửi đến email này.' }}
            />
            <TextField
              label="Số điện thoại"
              name="phoneNumber"
              value={formData.phoneNumber}
              onChange={updateField}
              onBlur={blurField}
              error={touched.phoneNumber ? errors.phoneNumber : ''}
              icon={Phone}
              autoComplete="tel"
              inputMode="numeric"
              maxLength={10}
              helper={{ placeholder: 'Nhập 10 chữ số', text: 'Dùng để liên hệ khi suất chiếu hoặc vé của bạn có thay đổi.' }}
            />

            <div className="space-y-1.5">
              <label htmlFor="password" className="block text-xs font-black uppercase tracking-wider text-zinc-400">Mật khẩu mới</label>
              <div className="relative">
                <Lock aria-hidden="true" className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={updateField}
                  onBlur={blurField}
                  onKeyUp={event => setCapsLockOn(event.getModifierState('CapsLock'))}
                  onKeyDown={event => setCapsLockOn(event.getModifierState('CapsLock'))}
                  autoComplete="new-password"
                  placeholder="Tạo mật khẩu"
                  aria-invalid={Boolean(touched.password && errors.password)}
                  aria-describedby="password-guidance"
                  className={`${inputClass(touched.password && errors.password)} pr-12`}
                />
                <button type="button" onClick={() => setShowPassword(value => !value)} aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} className="absolute right-1.5 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-lg text-zinc-500 hover:bg-zinc-900 hover:text-zinc-300 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange">
                  {showPassword ? <EyeOff aria-hidden="true" className="h-4 w-4" /> : <Eye aria-hidden="true" className="h-4 w-4" />}
                </button>
              </div>
              <FieldMessage id="password-error" error={touched.password ? errors.password : ''} />
              <div id="password-guidance" className="grid grid-cols-2 gap-x-3 gap-y-1 pt-1 sm:grid-cols-3">
                {passwordRules.map(rule => (
                  <span key={rule.label} className={`flex items-center gap-1.5 text-[11px] font-semibold ${rule.met ? 'text-emerald-400' : 'text-zinc-600'}`}>
                    <Check aria-hidden="true" className="h-3 w-3" /> {rule.label}
                  </span>
                ))}
              </div>
              {capsLockOn && <p className="text-xs font-semibold text-amber-400">Caps Lock đang bật.</p>}
            </div>

            <div className="space-y-1.5">
              <label htmlFor="confirmPassword" className="block text-xs font-black uppercase tracking-wider text-zinc-400">Xác nhận mật khẩu</label>
              <div className="relative">
                <Lock aria-hidden="true" className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={formData.confirmPassword}
                  onChange={updateField}
                  onBlur={blurField}
                  autoComplete="new-password"
                  placeholder="Nhập lại mật khẩu"
                  aria-invalid={Boolean(touched.confirmPassword && errors.confirmPassword)}
                  aria-describedby="confirmPassword-message"
                  className={`${inputClass(touched.confirmPassword && errors.confirmPassword)} pr-12`}
                />
                <button type="button" onClick={() => setShowConfirmPassword(value => !value)} aria-label={showConfirmPassword ? 'Ẩn mật khẩu xác nhận' : 'Hiện mật khẩu xác nhận'} className="absolute right-1.5 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-lg text-zinc-500 hover:bg-zinc-900 hover:text-zinc-300 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange">
                  {showConfirmPassword ? <EyeOff aria-hidden="true" className="h-4 w-4" /> : <Eye aria-hidden="true" className="h-4 w-4" />}
                </button>
              </div>
              <FieldMessage id="confirmPassword-message" error={touched.confirmPassword ? errors.confirmPassword : ''}>
                {formData.confirmPassword && formData.confirmPassword === formData.password ? 'Mật khẩu đã khớp.' : ''}
              </FieldMessage>
            </div>

            <div className="space-y-3 border-t border-zinc-800 pt-4">
              <label className="flex cursor-pointer items-start gap-3 text-sm text-zinc-400">
                <input type="checkbox" checked={acceptedTerms} onChange={event => { setAcceptedTerms(event.target.checked); setErrors(previous => ({ ...previous, terms: '' })); }} className="mt-0.5 h-4 w-4 rounded border-zinc-700 bg-zinc-950 text-brand-orange focus:ring-brand-orange" />
                <span>
                  Tôi đồng ý với <Link target="_blank" to="/support/terms" className="font-bold text-brand-orange hover:underline">Điều khoản sử dụng</Link> và xác nhận đã đọc <Link target="_blank" to="/support/privacy" className="font-bold text-brand-orange hover:underline">Chính sách bảo mật</Link>.
                </span>
              </label>
              {errors.terms && <FieldMessage id="terms-message" error={errors.terms} />}
              <label className="flex cursor-pointer items-start gap-3 text-sm text-zinc-500">
                <input type="checkbox" checked={marketingConsent} onChange={event => setMarketingConsent(event.target.checked)} className="mt-0.5 h-4 w-4 rounded border-zinc-700 bg-zinc-950 text-brand-orange focus:ring-brand-orange" />
                <span>Tôi muốn nhận thông tin phim mới và ưu đãi từ LoraFilm. <span className="text-zinc-600">(Không bắt buộc)</span></span>
              </label>
            </div>

            <button type="submit" className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-3.5 text-sm font-black uppercase tracking-[0.14em] text-zinc-950 transition hover:bg-orange-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-[#141417]">
              Tiếp tục <ArrowRight aria-hidden="true" className="h-4 w-4" />
            </button>
          </form>
        </>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-5" noValidate>
          <div className="rounded-2xl border border-brand-orange/20 bg-brand-orange/[0.05] p-4">
            <div className="mb-3 flex items-start gap-3">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-brand-orange/10 text-brand-orange"><ShieldCheck aria-hidden="true" className="h-5 w-5" /></span>
              <div>
                <h2 className="text-sm font-black text-zinc-100">Vì sao LoraFilm cần thông tin này?</h2>
                <p className="mt-1 text-xs leading-relaxed text-zinc-500">Dùng để kiểm tra trùng hồ sơ và tự động gợi ý năm sinh, giới tính theo mã và nơi đăng ký khai sinh.</p>
              </div>
            </div>
            <p className="text-xs leading-relaxed text-zinc-600">Số đầy đủ không được đưa vào URL hoặc dữ liệu phân tích. Hồ sơ chỉ hiển thị phiên bản đã che.</p>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="cccd" className="block text-xs font-black uppercase tracking-wider text-zinc-400">Số định danh cá nhân</label>
            <div className="flex flex-col gap-2 sm:flex-row">
              <div className="relative flex-1">
                <CreditCard aria-hidden="true" className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
                <input
                  id="cccd"
                  name="cccd"
                  value={formData.cccd}
                  onChange={updateField}
                  onBlur={blurField}
                  inputMode="numeric"
                  autoComplete="off"
                  maxLength={12}
                  placeholder="Nhập 12 chữ số"
                  aria-invalid={Boolean(touched.cccd && errors.cccd)}
                  aria-describedby="cccd-message"
                  disabled={isSubmitting || isInspecting}
                  className={inputClass(touched.cccd && errors.cccd)}
                />
              </div>
              <button
                type="button"
                onClick={inspectCccd}
                disabled={formData.cccd.length !== 12 || isInspecting || isSubmitting}
                className="flex min-h-12 shrink-0 items-center justify-center gap-2 rounded-xl border border-brand-orange/50 px-4 text-xs font-black uppercase tracking-wider text-brand-orange transition hover:bg-brand-orange/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange disabled:cursor-not-allowed disabled:border-zinc-800 disabled:text-zinc-600"
              >
                {isInspecting ? <Loader2 aria-hidden="true" className="h-4 w-4 animate-spin" /> : <RefreshCw aria-hidden="true" className="h-4 w-4" />}
                {isInspecting ? 'Đang kiểm tra…' : 'Kiểm tra thông tin'}
              </button>
            </div>
            <FieldMessage id="cccd-message" error={touched.cccd ? errors.cccd : ''}>Thông tin này chỉ được gửi khi bạn chủ động kiểm tra hoặc hoàn tất đăng ký.</FieldMessage>
          </div>

          {identityInfo && (
            <section aria-labelledby="identity-insight-title" className="rounded-2xl border border-emerald-900/70 bg-emerald-950/20 p-4">
              <div className="mb-4 flex items-center gap-2 text-emerald-300">
                <CheckCircle2 aria-hidden="true" className="h-4 w-4" />
                <h2 id="identity-insight-title" className="text-xs font-black uppercase tracking-[0.14em]">Thông tin suy ra từ mã định danh</h2>
              </div>
              <dl className="grid gap-3 text-sm sm:grid-cols-3">
                <div><dt className="text-[10px] font-bold uppercase tracking-wider text-zinc-600">Nơi đăng ký khai sinh</dt><dd className="mt-1 font-bold text-zinc-200">{identityInfo.birthRegistrationProvinceName || 'Chưa xác định'}</dd></div>
                <div><dt className="text-[10px] font-bold uppercase tracking-wider text-zinc-600">Năm sinh</dt><dd className="mt-1 font-bold text-zinc-200">{identityInfo.birthYear}</dd></div>
                <div><dt className="text-[10px] font-bold uppercase tracking-wider text-zinc-600">Giới tính theo mã</dt><dd className="mt-1 font-bold text-zinc-200">{identityInfo.legalSexLabel || 'Chưa xác định'}</dd></div>
              </dl>
              <p className="mt-4 border-t border-emerald-900/40 pt-3 text-xs leading-relaxed text-zinc-500">Đây là kết quả đọc cấu trúc mã, không phải xác minh danh tính hay xác nhận số này thuộc về người đăng ký.</p>
              <details className="mt-2 text-xs text-zinc-500">
                <summary className="cursor-pointer font-bold text-brand-orange focus:outline-none">Thông tin suy ra chưa đúng?</summary>
                <p className="mt-2 leading-relaxed">Vui lòng kiểm tra lại số đã nhập. Nếu vẫn không khớp, hãy liên hệ hỗ trợ để được hướng dẫn xử lý hồ sơ ngoại lệ.</p>
              </details>
            </section>
          )}

          <div className="space-y-1.5">
            <label htmlFor="birthday" className="block text-xs font-black uppercase tracking-wider text-zinc-400">Ngày sinh</label>
            <div className="relative">
              <CalendarDays aria-hidden="true" className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
              <input
                id="birthday"
                name="birthday"
                type="date"
                value={formData.birthday}
                onChange={updateField}
                onBlur={blurField}
                autoComplete="bday"
                max={new Date().toISOString().slice(0, 10)}
                aria-invalid={Boolean(touched.birthday && errors.birthday)}
                aria-describedby="birthday-message"
                disabled={isSubmitting}
                className={inputClass(touched.birthday && errors.birthday)}
              />
            </div>
            <FieldMessage id="birthday-message" error={touched.birthday ? errors.birthday : ''}>
              {identityInfo && formData.birthday && !birthYearMismatch
                ? `Năm sinh khớp với mã định danh (${identityInfo.birthYear}).`
                : 'Dùng để xác định độ tuổi và quyền lợi sinh nhật.'}
            </FieldMessage>
          </div>

          <div className="flex items-start gap-2 rounded-xl border border-zinc-800 bg-zinc-950/60 p-3 text-xs leading-relaxed text-zinc-500">
            <Info aria-hidden="true" className="mt-0.5 h-4 w-4 shrink-0 text-brand-orange" />
            Bằng việc tiếp tục, bạn xác nhận đã kiểm tra thông tin được suy ra. Xem thêm trong <Link target="_blank" to="/support/privacy" className="font-bold text-brand-orange hover:underline">Chính sách bảo mật</Link>.
          </div>

          <div className="grid gap-3 sm:grid-cols-[auto_1fr]">
            <button type="button" onClick={() => setCurrentStep(1)} disabled={isSubmitting} className="flex min-h-12 items-center justify-center gap-2 rounded-xl border border-zinc-700 px-5 text-sm font-bold text-zinc-300 transition hover:border-zinc-600 hover:bg-zinc-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange disabled:opacity-50">
              <ArrowLeft aria-hidden="true" className="h-4 w-4" /> Quay lại
            </button>
            <button type="submit" disabled={isSubmitting || isInspecting || !identityInfo || birthYearMismatch} className="flex min-h-12 items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black uppercase tracking-[0.1em] text-zinc-950 transition hover:bg-orange-500 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-orange focus-visible:ring-offset-2 focus-visible:ring-offset-[#141417] disabled:cursor-not-allowed disabled:opacity-50">
              {isSubmitting && <Loader2 aria-hidden="true" className="h-4 w-4 animate-spin" />}
              {isSubmitting ? 'Đang gửi…' : 'Tiếp tục xác minh email'}
            </button>
          </div>
        </form>
      )}
    </AuthShell>
  );
}
