import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useOutletContext, useSearchParams } from 'react-router-dom';
import {
  AlertTriangle,
  Camera,
  CheckCircle2,
  Eye,
  EyeOff,
  Laptop,
  LoaderCircle,
  LockKeyhole,
  LogOut,
  Mail,
  Monitor,
  MonitorSmartphone,
  RefreshCw,
  Save,
  ShieldCheck,
  Smartphone,
  Trash2,
  UserRound
} from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import {
  changeEmail,
  changePassword,
  getSessions,
  revokeAllSessions,
  revokeSession
} from '@/features/auth/services/authService';
import { updateUserProfile, uploadAvatar } from '@/features/auth/services/userService';

const accountTabs = new Set(['profile', 'password', 'email', 'sessions']);
const strongPassword = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';
const fieldClass = 'w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none transition-colors placeholder:text-zinc-600 focus:border-brand-orange disabled:cursor-not-allowed disabled:opacity-60';

const resolveMediaUrl = (value) => value?.startsWith('/') ? `${apiBaseUrl}${value}` : value;

const roleName = (role) => {
  const normalized = String(role || '').replace(/^ROLE_/, '');
  if (normalized === 'ADMIN') return 'Quản trị viên';
  if (normalized === 'ACCOUNTANT') return 'Kế toán';
  if (normalized === 'SUPERVISOR') return 'Giám sát';
  if (normalized === 'EMPLOYEE') return 'Nhân viên';
  return normalized.replaceAll('_', ' ') || 'Nhân sự LoraFilm';
};

const deviceIcon = (userAgent) => {
  const normalized = String(userAgent || '').toLowerCase();
  if (normalized.includes('mobile') || normalized.includes('android') || normalized.includes('iphone')) {
    return <Smartphone className="h-5 w-5" />;
  }
  if (normalized.includes('mac') || normalized.includes('windows') || normalized.includes('linux')) {
    return <Laptop className="h-5 w-5" />;
  }
  return <Monitor className="h-5 w-5" />;
};

function InlineNotice({ notice }) {
  if (!notice) return null;
  const successful = notice.type === 'success';

  return (
    <div
      role={successful ? 'status' : 'alert'}
      className={`flex items-start gap-3 rounded-xl border px-4 py-3 text-sm ${
        successful
          ? 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300'
          : 'border-red-500/25 bg-red-500/10 text-red-300'
      }`}
    >
      {successful
        ? <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
        : <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />}
      <span>{notice.message}</span>
    </div>
  );
}

function PasswordInput({ id, label, value, onChange, visible, onToggle, autoComplete }) {
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="block text-[11px] font-black uppercase tracking-wider text-zinc-500">
        {label}
      </label>
      <div className="relative">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          className={`${fieldClass} pr-12`}
          required
        />
        <button
          type="button"
          onClick={onToggle}
          className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-500 transition-colors hover:text-white"
          aria-label={visible ? `Ẩn ${label.toLowerCase()}` : `Hiện ${label.toLowerCase()}`}
        >
          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
    </div>
  );
}

export default function AdminMyAccountPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const adminActions = useOutletContext() || {};
  const triggerToast = adminActions.triggerToast || (() => {});
  const triggerConfirm = adminActions.triggerConfirm || (async () => false);
  const {
    user,
    userRole,
    email,
    profile,
    profileLoading,
    profilePending,
    profileError,
    refreshProfile,
    updateUser,
    logout
  } = useAuth();

  const requestedTab = searchParams.get('tab');
  const activeTab = accountTabs.has(requestedTab) ? requestedTab : 'profile';
  const fileInputRef = useRef(null);
  const [phone, setPhone] = useState(profile?.phoneNumber || '');
  const [profileSaving, setProfileSaving] = useState(false);
  const [avatarSaving, setAvatarSaving] = useState(false);
  const [profileNotice, setProfileNotice] = useState(null);
  const [passwordForm, setPasswordForm] = useState({ current: '', next: '', confirm: '' });
  const [passwordVisible, setPasswordVisible] = useState({ current: false, next: false, confirm: false });
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordNotice, setPasswordNotice] = useState(null);
  const [emailForm, setEmailForm] = useState({ next: '', password: '' });
  const [emailSaving, setEmailSaving] = useState(false);
  const [emailNotice, setEmailNotice] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [sessionsNotice, setSessionsNotice] = useState(null);
  const [sessionAction, setSessionAction] = useState('');

  const displayName = profile?.fullName || user?.fullName || email?.split('@')[0] || 'Tài khoản LoraFilm';
  const displayRole = roleName(userRole || user?.role);
  const avatarUrl = resolveMediaUrl(profile?.avatarUrl || user?.avatarUrl);

  const tabs = useMemo(() => ([
    { id: 'profile', label: 'Thông tin cá nhân', icon: UserRound },
    { id: 'password', label: 'Đổi mật khẩu', icon: LockKeyhole },
    { id: 'email', label: 'Email đăng nhập', icon: Mail },
    { id: 'sessions', label: 'Phiên đăng nhập', icon: MonitorSmartphone }
  ]), []);

  useEffect(() => {
    // Profile is loaded asynchronously by AuthContext.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPhone(profile?.phoneNumber || '');
  }, [profile?.phoneNumber]);

  useEffect(() => {
    if (requestedTab && !accountTabs.has(requestedTab)) {
      setSearchParams({ tab: 'profile' }, { replace: true });
    }
  }, [requestedTab, setSearchParams]);

  const loadSessions = useCallback(async () => {
    setSessionsLoading(true);
    setSessionsNotice(null);
    try {
      setSessions(await getSessions());
    } catch {
      setSessionsNotice({ type: 'error', message: 'Không thể tải danh sách phiên đăng nhập. Vui lòng thử lại.' });
    } finally {
      setSessionsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === 'sessions') {
      // Loading remote session state is the synchronization performed by this effect.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      loadSessions();
    }
  }, [activeTab, loadSessions]);

  const selectTab = (tabId) => {
    setSearchParams({ tab: tabId }, { replace: true });
  };

  const saveProfile = async (event) => {
    event.preventDefault();
    const normalizedPhone = phone.trim();
    if (normalizedPhone && !/^\d{10,15}$/.test(normalizedPhone)) {
      setProfileNotice({ type: 'error', message: 'Số điện thoại phải gồm từ 10 đến 15 chữ số.' });
      return;
    }

    setProfileSaving(true);
    setProfileNotice(null);
    try {
      const updated = await updateUserProfile({ phoneNumber: normalizedPhone || null });
      updateUser(updated);
      setProfileNotice({ type: 'success', message: 'Thông tin cá nhân đã được cập nhật.' });
      triggerToast('Đã cập nhật thông tin tài khoản.');
    } catch {
      setProfileNotice({ type: 'error', message: 'Không thể cập nhật thông tin cá nhân. Vui lòng kiểm tra lại và thử sau.' });
    } finally {
      setProfileSaving(false);
    }
  };

  const saveAvatar = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setProfileNotice({ type: 'error', message: 'Ảnh đại diện phải có định dạng JPG, PNG hoặc WebP.' });
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setProfileNotice({ type: 'error', message: 'Ảnh đại diện không được vượt quá 5 MB.' });
      return;
    }

    setAvatarSaving(true);
    setProfileNotice(null);
    try {
      const updated = await uploadAvatar(file);
      updateUser({ avatarUrl: updated.avatarUrl });
      setProfileNotice({ type: 'success', message: 'Ảnh đại diện đã được cập nhật.' });
      triggerToast('Đã cập nhật ảnh đại diện.');
    } catch {
      setProfileNotice({ type: 'error', message: 'Không thể tải ảnh đại diện lên. Vui lòng thử lại.' });
    } finally {
      setAvatarSaving(false);
    }
  };

  const submitPassword = async (event) => {
    event.preventDefault();
    if (!passwordForm.current) {
      setPasswordNotice({ type: 'error', message: 'Vui lòng nhập mật khẩu hiện tại.' });
      return;
    }
    if (!strongPassword.test(passwordForm.next)) {
      setPasswordNotice({
        type: 'error',
        message: 'Mật khẩu mới cần ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'
      });
      return;
    }
    if (passwordForm.next !== passwordForm.confirm) {
      setPasswordNotice({ type: 'error', message: 'Mật khẩu xác nhận không khớp.' });
      return;
    }

    setPasswordSaving(true);
    setPasswordNotice(null);
    try {
      await changePassword(passwordForm.current, passwordForm.next);
      await logout();
      navigate('/login', {
        replace: true,
        state: { message: 'Mật khẩu đã được thay đổi. Vui lòng đăng nhập lại.' }
      });
    } catch (error) {
      const unauthorized = error?.response?.status === 400 || error?.response?.status === 401;
      setPasswordNotice({
        type: 'error',
        message: unauthorized
          ? 'Mật khẩu hiện tại không đúng hoặc mật khẩu mới chưa hợp lệ.'
          : 'Không thể đổi mật khẩu lúc này. Vui lòng thử lại sau.'
      });
    } finally {
      setPasswordSaving(false);
    }
  };

  const submitEmail = async (event) => {
    event.preventDefault();
    const normalizedEmail = emailForm.next.trim().toLowerCase();
    if (!emailPattern.test(normalizedEmail)) {
      setEmailNotice({ type: 'error', message: 'Vui lòng nhập địa chỉ email hợp lệ.' });
      return;
    }
    if (normalizedEmail === String(email || '').toLowerCase()) {
      setEmailNotice({ type: 'error', message: 'Email mới phải khác email đang sử dụng.' });
      return;
    }
    if (!emailForm.password) {
      setEmailNotice({ type: 'error', message: 'Vui lòng nhập mật khẩu hiện tại để xác nhận.' });
      return;
    }

    setEmailSaving(true);
    setEmailNotice(null);
    try {
      await changeEmail(normalizedEmail, emailForm.password);
      await logout();
      navigate('/login', {
        replace: true,
        state: {
          email: normalizedEmail,
          message: 'Email đăng nhập đã được thay đổi. Vui lòng đăng nhập lại bằng email mới.'
        }
      });
    } catch (error) {
      const conflict = error?.response?.status === 409;
      setEmailNotice({
        type: 'error',
        message: conflict
          ? 'Email này đã được sử dụng bởi một tài khoản khác.'
          : 'Không thể thay đổi email. Vui lòng kiểm tra mật khẩu và thử lại.'
      });
    } finally {
      setEmailSaving(false);
    }
  };

  const removeSession = async (session) => {
    const accepted = await triggerConfirm(
      `Thu hồi phiên “${session.deviceName || 'Thiết bị không xác định'}”? Thiết bị này sẽ phải đăng nhập lại.`
    );
    if (!accepted) return;

    setSessionAction(session.id);
    setSessionsNotice(null);
    try {
      await revokeSession(session.id);
      setSessionsNotice({ type: 'success', message: 'Phiên đăng nhập đã được thu hồi.' });
      await loadSessions();
    } catch {
      setSessionsNotice({ type: 'error', message: 'Không thể thu hồi phiên đăng nhập. Vui lòng thử lại.' });
    } finally {
      setSessionAction('');
    }
  };

  const removeAllSessions = async () => {
    const accepted = await triggerConfirm(
      'Đăng xuất khỏi tất cả thiết bị? Bạn cũng sẽ phải đăng nhập lại trên thiết bị này.'
    );
    if (!accepted) return;

    setSessionAction('all');
    setSessionsNotice(null);
    try {
      await revokeAllSessions();
      await logout();
      navigate('/login', {
        replace: true,
        state: { message: 'Tất cả phiên đăng nhập đã được thu hồi.' }
      });
    } catch {
      setSessionsNotice({ type: 'error', message: 'Không thể thu hồi tất cả phiên đăng nhập. Vui lòng thử lại.' });
      setSessionAction('');
    }
  };

  if (profileLoading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center text-zinc-400" role="status">
        <LoaderCircle className="mr-3 h-6 w-6 animate-spin text-brand-orange" />
        Đang tải thông tin tài khoản...
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-7xl space-y-6 pb-10">
      <header className="border-b border-zinc-800 pb-5">
        <p className="text-xs font-black uppercase tracking-[0.25em] text-brand-orange">Tài khoản quản trị</p>
        <h1 className="mt-2 text-2xl font-black uppercase tracking-tight text-white sm:text-3xl">
          Tài khoản của tôi
        </h1>
        <p className="mt-2 text-sm text-zinc-400">
          Quản lý thông tin cá nhân, định danh đăng nhập và các thiết bị đang truy cập.
        </p>
      </header>

      <section className="grid gap-6 lg:grid-cols-[300px_minmax(0,1fr)]">
        <aside className="h-fit rounded-2xl border border-zinc-800 bg-zinc-900 p-5 shadow-xl">
          <div className="flex items-center gap-4 border-b border-zinc-800 pb-5">
            <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-full border-2 border-brand-orange bg-zinc-950">
              {avatarUrl ? (
                <img
                  src={avatarUrl}
                  alt={`Ảnh đại diện của ${displayName}`}
                  className="h-full w-full object-cover"
                  referrerPolicy="no-referrer"
                />
              ) : (
                <UserRound className="h-full w-full p-4 text-zinc-600" />
              )}
              {avatarSaving && (
                <div className="absolute inset-0 flex items-center justify-center bg-black/70">
                  <LoaderCircle className="h-5 w-5 animate-spin text-brand-orange" />
                </div>
              )}
            </div>
            <div className="min-w-0">
              <p className="truncate text-base font-black text-white">{displayName}</p>
              <p className="mt-1 text-xs font-bold uppercase tracking-wider text-brand-orange">{displayRole}</p>
            </div>
          </div>

          <div className="mt-5 space-y-3 text-sm">
            <div>
              <p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Email đăng nhập</p>
              <p className="mt-1 break-all text-zinc-300">{email || 'Chưa có email'}</p>
            </div>
            <div>
              <p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Số điện thoại</p>
              <p className="mt-1 text-zinc-300">{profile?.phoneNumber || 'Chưa cập nhật'}</p>
            </div>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
            onChange={saveAvatar}
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={avatarSaving}
            className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-bold text-zinc-300 transition-colors hover:border-brand-orange/50 hover:text-brand-orange disabled:opacity-60"
          >
            <Camera className="h-4 w-4" />
            Đổi ảnh đại diện
          </button>
        </aside>

        <div className="min-w-0 space-y-5">
          <nav className="grid grid-cols-2 gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 p-2 xl:grid-cols-4" aria-label="Quản lý tài khoản">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const selected = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => selectTab(tab.id)}
                  aria-current={selected ? 'page' : undefined}
                  className={`flex items-center justify-center gap-2 rounded-xl px-3 py-3 text-xs font-black transition-colors ${
                    selected
                      ? 'bg-brand-orange text-zinc-950'
                      : 'text-zinc-400 hover:bg-zinc-800 hover:text-white'
                  }`}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </nav>

          <section className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5 shadow-xl sm:p-7">
            {activeTab === 'profile' && (
              <form onSubmit={saveProfile} className="space-y-6">
                <div>
                  <h2 className="text-lg font-black uppercase text-white">Thông tin cá nhân</h2>
                  <p className="mt-1 text-sm text-zinc-500">
                    Thông tin dùng để nhận diện tài khoản trong khu vực vận hành.
                  </p>
                </div>

                {profilePending && (
                  <InlineNotice notice={{ type: 'error', message: 'Hồ sơ đang được khởi tạo. Vui lòng tải lại sau ít phút.' }} />
                )}
                {profileError && (
                  <div className="space-y-3">
                    <InlineNotice notice={{ type: 'error', message: 'Không thể tải đầy đủ hồ sơ. Vui lòng thử lại.' }} />
                    <button type="button" onClick={refreshProfile} className="flex items-center gap-2 text-xs font-bold text-brand-orange">
                      <RefreshCw className="h-4 w-4" /> Tải lại hồ sơ
                    </button>
                  </div>
                )}
                <InlineNotice notice={profileNotice} />

                <div className="grid gap-5 sm:grid-cols-2">
                  <div className="space-y-2">
                    <label htmlFor="admin-full-name" className="block text-[11px] font-black uppercase tracking-wider text-zinc-500">
                      Họ và tên
                    </label>
                    <input id="admin-full-name" value={displayName} readOnly className={fieldClass} />
                    <p className="text-xs text-zinc-600">Liên hệ quản trị hệ thống nếu cần đổi họ tên pháp lý.</p>
                  </div>
                  <div className="space-y-2">
                    <label htmlFor="admin-role" className="block text-[11px] font-black uppercase tracking-wider text-zinc-500">
                      Vai trò hiện tại
                    </label>
                    <input id="admin-role" value={displayRole} readOnly className={fieldClass} />
                    <p className="text-xs text-zinc-600">Vai trò và quyền hạn không thể tự thay đổi.</p>
                  </div>
                  <div className="space-y-2 sm:col-span-2">
                    <label htmlFor="admin-phone" className="block text-[11px] font-black uppercase tracking-wider text-zinc-500">
                      Số điện thoại
                    </label>
                    <input
                      id="admin-phone"
                      inputMode="numeric"
                      value={phone}
                      onChange={(event) => setPhone(event.target.value.replace(/\D/g, '').slice(0, 15))}
                      placeholder="Ví dụ: 0901234567"
                      className={fieldClass}
                    />
                  </div>
                </div>

                <div className="flex justify-end border-t border-zinc-800 pt-5">
                  <button
                    type="submit"
                    disabled={profileSaving || profilePending}
                    className="flex items-center gap-2 rounded-xl bg-brand-orange px-5 py-3 text-xs font-black uppercase text-zinc-950 transition-opacity disabled:opacity-60"
                  >
                    {profileSaving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                    {profileSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
                  </button>
                </div>
              </form>
            )}

            {activeTab === 'password' && (
              <form onSubmit={submitPassword} className="mx-auto max-w-xl space-y-5">
                <div>
                  <h2 className="text-lg font-black uppercase text-white">Đổi mật khẩu</h2>
                  <p className="mt-1 text-sm text-zinc-500">
                    Sau khi đổi mật khẩu, bạn cần đăng nhập lại để tiếp tục vận hành.
                  </p>
                </div>
                <InlineNotice notice={passwordNotice} />
                <PasswordInput
                  id="admin-current-password"
                  label="Mật khẩu hiện tại"
                  value={passwordForm.current}
                  onChange={(event) => setPasswordForm((value) => ({ ...value, current: event.target.value }))}
                  visible={passwordVisible.current}
                  onToggle={() => setPasswordVisible((value) => ({ ...value, current: !value.current }))}
                  autoComplete="current-password"
                />
                <PasswordInput
                  id="admin-new-password"
                  label="Mật khẩu mới"
                  value={passwordForm.next}
                  onChange={(event) => setPasswordForm((value) => ({ ...value, next: event.target.value }))}
                  visible={passwordVisible.next}
                  onToggle={() => setPasswordVisible((value) => ({ ...value, next: !value.next }))}
                  autoComplete="new-password"
                />
                <PasswordInput
                  id="admin-confirm-password"
                  label="Xác nhận mật khẩu mới"
                  value={passwordForm.confirm}
                  onChange={(event) => setPasswordForm((value) => ({ ...value, confirm: event.target.value }))}
                  visible={passwordVisible.confirm}
                  onToggle={() => setPasswordVisible((value) => ({ ...value, confirm: !value.confirm }))}
                  autoComplete="new-password"
                />
                <button
                  type="submit"
                  disabled={passwordSaving}
                  className="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-orange py-3 text-xs font-black uppercase text-zinc-950 disabled:opacity-60"
                >
                  {passwordSaving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <LockKeyhole className="h-4 w-4" />}
                  {passwordSaving ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
                </button>
              </form>
            )}

            {activeTab === 'email' && (
              <form onSubmit={submitEmail} className="mx-auto max-w-xl space-y-5">
                <div>
                  <h2 className="text-lg font-black uppercase text-white">Email đăng nhập</h2>
                  <p className="mt-1 text-sm text-zinc-500">
                    Email là định danh đăng nhập. Thay đổi email sẽ thu hồi các phiên hiện tại.
                  </p>
                </div>
                <InlineNotice notice={emailNotice} />
                <div className="space-y-2">
                  <label htmlFor="admin-current-email" className="block text-[11px] font-black uppercase tracking-wider text-zinc-500">
                    Email đang sử dụng
                  </label>
                  <input id="admin-current-email" type="email" value={email || ''} readOnly className={fieldClass} />
                </div>
                <div className="space-y-2">
                  <label htmlFor="admin-new-email" className="block text-[11px] font-black uppercase tracking-wider text-zinc-500">
                    Email mới
                  </label>
                  <input
                    id="admin-new-email"
                    type="email"
                    value={emailForm.next}
                    onChange={(event) => setEmailForm((value) => ({ ...value, next: event.target.value }))}
                    autoComplete="email"
                    placeholder="tenmoi@example.com"
                    className={fieldClass}
                    required
                  />
                </div>
                <PasswordInput
                  id="admin-email-password"
                  label="Mật khẩu hiện tại"
                  value={emailForm.password}
                  onChange={(event) => setEmailForm((value) => ({ ...value, password: event.target.value }))}
                  visible={passwordVisible.email}
                  onToggle={() => setPasswordVisible((value) => ({ ...value, email: !value.email }))}
                  autoComplete="current-password"
                />
                <button
                  type="submit"
                  disabled={emailSaving}
                  className="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-orange py-3 text-xs font-black uppercase text-zinc-950 disabled:opacity-60"
                >
                  {emailSaving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Mail className="h-4 w-4" />}
                  {emailSaving ? 'Đang cập nhật...' : 'Xác nhận email mới'}
                </button>
              </form>
            )}

            {activeTab === 'sessions' && (
              <div className="space-y-5">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h2 className="text-lg font-black uppercase text-white">Phiên đăng nhập</h2>
                    <p className="mt-1 text-sm text-zinc-500">
                      Kiểm tra và thu hồi thiết bị không còn được phép truy cập tài khoản.
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={loadSessions}
                    disabled={sessionsLoading}
                    className="flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-bold text-zinc-300 hover:text-white disabled:opacity-60"
                  >
                    <RefreshCw className={`h-4 w-4 ${sessionsLoading ? 'animate-spin' : ''}`} />
                    Làm mới
                  </button>
                </div>
                <InlineNotice notice={sessionsNotice} />

                {sessionsLoading ? (
                  <div className="flex min-h-48 items-center justify-center text-sm text-zinc-500" role="status">
                    <LoaderCircle className="mr-3 h-5 w-5 animate-spin text-brand-orange" />
                    Đang tải danh sách thiết bị...
                  </div>
                ) : sessions.length === 0 ? (
                  <div className="flex min-h-48 flex-col items-center justify-center rounded-2xl border border-dashed border-zinc-700 text-center">
                    <MonitorSmartphone className="h-10 w-10 text-zinc-700" />
                    <p className="mt-3 text-sm font-bold text-zinc-300">Không có phiên đăng nhập nào</p>
                    <p className="mt-1 text-xs text-zinc-600">Danh sách thiết bị sẽ xuất hiện khi có phiên hoạt động.</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {sessions.map((session) => (
                      <article key={session.id} className="flex items-center gap-4 rounded-xl border border-zinc-800 bg-zinc-950 p-4">
                        <div className="rounded-full bg-zinc-900 p-3 text-zinc-400">
                          {deviceIcon(session.userAgent)}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-bold text-zinc-100">
                            {session.deviceName || 'Thiết bị không xác định'}
                          </p>
                          <p className="mt-1 text-xs text-zinc-500">
                            {session.ipAddress || 'Không xác định IP'} ·{' '}
                            {session.lastActiveAt
                              ? new Date(session.lastActiveAt).toLocaleString('vi-VN')
                              : 'Vừa hoạt động'}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeSession(session)}
                          disabled={sessionAction === session.id}
                          className="rounded-lg p-2 text-zinc-500 transition-colors hover:bg-red-500/10 hover:text-red-400 disabled:opacity-50"
                          title="Thu hồi phiên"
                          aria-label={`Thu hồi phiên ${session.deviceName || 'không xác định'}`}
                        >
                          {sessionAction === session.id
                            ? <LoaderCircle className="h-4 w-4 animate-spin" />
                            : <Trash2 className="h-4 w-4" />}
                        </button>
                      </article>
                    ))}

                    <button
                      type="button"
                      onClick={removeAllSessions}
                      disabled={sessionAction === 'all'}
                      className="flex w-full items-center justify-center gap-2 rounded-xl border border-red-500/30 bg-red-500/5 py-3 text-xs font-black uppercase text-red-400 transition-colors hover:bg-red-500/10 disabled:opacity-60"
                    >
                      {sessionAction === 'all'
                        ? <LoaderCircle className="h-4 w-4 animate-spin" />
                        : <LogOut className="h-4 w-4" />}
                      Đăng xuất khỏi tất cả thiết bị
                    </button>
                  </div>
                )}
              </div>
            )}
          </section>

          <div className="flex items-start gap-3 rounded-2xl border border-blue-500/20 bg-blue-500/5 p-4 text-sm text-zinc-400">
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-blue-400" />
            <p>
              Các thay đổi về mật khẩu, email và phiên đăng nhập chỉ tác động đến tài khoản của bạn.
              Vai trò và quyền vận hành do quản trị hệ thống cấp riêng.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
