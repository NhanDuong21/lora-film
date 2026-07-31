/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useMemo, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import {
  User, Calendar, Mail, Phone, Lock, Eye, EyeOff, Camera, ChevronRight,
  PhoneCall, HelpCircle, History, Bell, Gift, FileText, CheckCircle, AlertCircle, Award
} from 'lucide-react';
import CustomerNoticeModal from '@/components/common/CustomerNoticeModal';
import CustomerBookingHistory from '@/features/booking/customer/components/CustomerBookingHistory';
import { getBookingSpendingSummary } from '@/features/booking/customer/services/bookingService';
import CustomerNotificationCenter from '@/features/notifications/customer/components/CustomerNotificationCenter';
import useCustomerScore from '@/features/score/customer/hooks/useCustomerScore';
import LoyaltyCenterPage from '@/features/score/customer/pages/LoyaltyCenterPage';
import { updateUserProfile, uploadAvatar } from '@/features/auth/services/userService';
import { changePassword } from '@/features/auth/services/authService';

const normalizeDateForInput = (value) => {
  if (!value) return '';
  return String(value).substring(0, 10);
};

const hasImageSource = value => typeof value === 'string' && value.trim().length > 0;
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const resolveMediaUrl = value => value?.startsWith('/') ? `${apiBaseUrl}${value}` : value;
const customerProfileTabs = new Set(['info', 'history', 'notifications', 'gifts', 'policy', 'loyalty']);
const resolveCustomerProfileTab = (tab, fallback) => customerProfileTabs.has(tab) ? tab : fallback;

export default function CustomerProfileView({ onBackHome, initialTab = 'info' }) {
  const navigate = useNavigate();
  const location = useLocation();
  const {
    user,
    profile,
    email: sessionEmail,
    accountId,
    updateUser,
    isAuthenticated,
    profileLoading,
    profilePending,
    profileError,
    refreshProfile,
    logout
  } = useAuth();

  useEffect(() => {
    document.title = "Tài Khoản Thành Viên - LoraFilm";
    if (!isAuthenticated || !accountId) {
      navigate('/login');
    }
  }, [isAuthenticated, accountId, navigate]);

  const handleBackHome = () => {
    if (onBackHome) {
      onBackHome();
    } else {
      navigate('/');
    }
  };


  // Unknown query values must never leave the customer profile content blank.
  const [activeTab, setActiveTab] = useState(() => {
    const searchParams = new URLSearchParams(location.search || (location.hash && location.hash.includes('?') ? location.hash.substring(location.hash.indexOf('?')) : ''));
    return resolveCustomerProfileTab(searchParams.get('tab'), initialTab);
  });

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search || (location.hash && location.hash.includes('?') ? location.hash.substring(location.hash.indexOf('?')) : ''));
    setActiveTab(resolveCustomerProfileTab(searchParams.get('tab'), initialTab));
  }, [initialTab, location]);

  // Load user data fields (Name, Birthday, Gender are strictly read-only disabled)
  const fullName = profile?.fullName ?? '';
  const birthday = normalizeDateForInput(profile?.birthday);
  const gender = profile?.gender ?? '';
  
  // Editable fields
  const [email, setEmail] = useState(sessionEmail || '');
  const [phone, setPhone] = useState(profile?.phoneNumber ?? '');
  
  // Avatar URL state
  const [avatarUrl, setAvatarUrl] = useState(
    user?.avatarUrl || ''
  );

  // Loyalty and spending states connected to Score Service API
  const { scoreData } = useCustomerScore();
  const spendingYear = new Date().getFullYear();
  const [spendingSummary, setSpendingSummary] = useState(undefined);
  const totalSpending = useMemo(
    () => Number(spendingSummary?.totalSpending ?? 0),
    [spendingSummary]
  );
  const points = useMemo(() => scoreData?.currentPoints ?? user?.points ?? 0, [scoreData, user]);

  useEffect(() => {
    if (!isAuthenticated || !accountId) {
      setSpendingSummary(undefined);
      return undefined;
    }

    let active = true;
    getBookingSpendingSummary(spendingYear)
      .then(summary => {
        if (active) setSpendingSummary(summary);
      })
      .catch(() => {
        if (active) setSpendingSummary(null);
      });
    return () => {
      active = false;
    };
  }, [accountId, isAuthenticated, spendingYear]);

  // Modal / toggle states
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [errorNotice, setErrorNotice] = useState(null);

  const [isChangingEmail, setIsChangingEmail] = useState(false);
  const [newEmail, setNewEmail] = useState(email);

  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPasswordRaw, setShowPasswordRaw] = useState(false);

  const [isEditingAvatar, setIsEditingAvatar] = useState(false);
  const [avatarFile, setAvatarFile] = useState(null);
  const [tempAvatarUrl, setTempAvatarUrl] = useState(resolveMediaUrl(avatarUrl));

  useEffect(() => {
    if (profile || sessionEmail) {
      const timer = setTimeout(() => {
        setEmail(sessionEmail || '');
        setPhone(profile?.phoneNumber ?? '');
        setAvatarUrl(resolveMediaUrl(profile?.avatarUrl || ''));
        setTempAvatarUrl(resolveMediaUrl(profile?.avatarUrl || ''));
        setNewEmail(sessionEmail || '');
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [profile, sessionEmail]);



  // Calculate membership progress milestones from Score Service API
  const currentRank = useMemo(() => {
    return scoreData?.currentTier?.tierName || 'Silver Member';
  }, [scoreData]);

  const membershipProgress = useMemo(() => {
    const currentMin = scoreData?.currentTier?.minAccumulatedPoints || 0;
    const targetMin = scoreData?.nextTier?.minAccumulatedPoints || (currentMin + 400);
    if (targetMin === currentMin) return 100;
    const currentProgress = Math.max(0, (scoreData?.accumulatedPoints || 0) - currentMin);
    return Math.min(100, Math.max(0, Math.round((currentProgress / (targetMin - currentMin)) * 100)));
  }, [scoreData]);

  const showErrorNotice = message => {
    setErrorNotice({
      title: 'Không thể thực hiện',
      message
    });
  };

  const showSuccessToast = message => {
    setToastMessage(message);
    setShowToast(true);
    setTimeout(() => {
      setShowToast(false);
    }, 3500);
  };

  // Profile Form submit (Updates Email & SĐT)
  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!phone.trim()) {
      showErrorNotice('Số điện thoại không được để trống!');
      return;
    }

    try {
      const updated = await updateUserProfile({ phoneNumber: phone.trim() });
      updateUser(updated);
      showSuccessToast('Cập nhật thông tin cá nhân thành công!');
    } catch (reason) {
      showErrorNotice(reason?.message || 'Không thể cập nhật hồ sơ.');
    }
  };

  // Change Email Action
  const handleSaveEmail = () => {
    setIsChangingEmail(false);
    navigate('/change-email', { state: { newEmail } });
  };

  // Change Password Action
  const handleSavePassword = async () => {
    if (!currentPassword) {
      showErrorNotice('Vui lòng nhập mật khẩu hiện tại!');
      return;
    }
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).{8,50}$/.test(newPassword)) {
      showErrorNotice('Mật khẩu mới cần ít nhất 8 ký tự, chữ hoa, chữ thường, số và ký tự đặc biệt.');
      return;
    }
    if (newPassword !== confirmPassword) {
      showErrorNotice('Mật khẩu xác nhận không trùng khớp!');
      return;
    }

    try {
      await changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setIsChangingPassword(false);
      await logout();
      navigate('/login', {
        replace: true,
        state: { message: 'Đổi mật khẩu thành công. Vui lòng đăng nhập lại.' }
      });
    } catch (reason) {
      showErrorNotice(reason?.message || 'Không thể đổi mật khẩu.');
    }
  };

  // Change Avatar Action
  const handleSaveAvatar = async () => {
    if (!avatarFile) {
      showErrorNotice('Vui lòng chọn ảnh JPEG, PNG hoặc WebP.');
      return;
    }
    try {
      const result = await uploadAvatar(avatarFile);
      const resolvedUrl = resolveMediaUrl(result.avatarUrl);
      setAvatarUrl(resolvedUrl);
      updateUser({ avatarUrl: result.avatarUrl });
      setAvatarFile(null);
      setIsEditingAvatar(false);
      showSuccessToast('Cập nhật ảnh đại diện thành công!');
    } catch (reason) {
      showErrorNotice(reason?.message || 'Không thể tải ảnh đại diện.');
    }
  };

  if (!accountId) {
    return (
      <div className="flex flex-col min-h-screen bg-[#050506] text-white selection:bg-brand-orange selection:text-zinc-950 font-sans font-medium">
        <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-6xl mx-auto w-full flex flex-col items-center justify-center text-center">
          <div className="bg-zinc-900/40 border border-zinc-800 p-8 rounded-2xl max-w-md w-full space-y-4">
            <h2 className="text-xl font-bold text-white uppercase tracking-wider">Vui lòng đăng nhập</h2>
            <p className="text-sm text-zinc-400">Vui lòng đăng nhập để xem thông tin hồ sơ.</p>
            <button
              onClick={() => navigate('/login')}
              className="w-full bg-brand-orange hover:bg-opacity-95 text-white font-black py-3 rounded-xl text-xs uppercase tracking-wider transition-colors"
            >
              Đăng nhập ngay
            </button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-screen bg-[#050506] text-white selection:bg-brand-orange selection:text-zinc-950 font-sans">
      {errorNotice && (
        <CustomerNoticeModal
          title={errorNotice.title}
          message={errorNotice.message}
          variant="error"
          onClose={() => setErrorNotice(null)}
        />
      )}
      <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-6xl mx-auto w-full">
        {/* Non-blocking success toast */}
        {showToast && (
          <div className="fixed top-24 left-1/2 -translate-x-1/2 z-50 py-3.5 px-6 rounded-2xl shadow-2xl border flex items-center gap-3 transition-all duration-300 bg-emerald-950 border-emerald-500/30 text-emerald-400">
            <CheckCircle className="w-5 h-5 shrink-0 text-emerald-500" />
            <span className="text-xs md:text-sm font-bold">{toastMessage}</span>
          </div>
        )}

        {/* Main Container */}
        <div className="space-y-8">
          
          {/* Breadcrumb back path */}
          <div className="flex items-center justify-between pb-4 border-b border-zinc-900">
            <h1 className="text-xl md:text-2xl font-black text-white uppercase tracking-wider">TÀI KHOẢN THÀNH VIÊN</h1>
            <button 
              onClick={handleBackHome}
              className="text-xs font-bold text-zinc-500 hover:text-brand-orange transition-colors flex items-center gap-1"
            >
              Quay lại trang chủ
            </button>
          </div>

        {profileLoading ? (
          <div className="flex min-h-80 items-center justify-center" role="status" aria-live="polite">
            <div className="flex flex-col items-center gap-4 text-zinc-400">
              <div className="h-10 w-10 animate-spin rounded-full border-4 border-[#ff7a1a] border-t-transparent" />
              <p className="text-xs font-bold uppercase tracking-wider">Đang tải hồ sơ...</p>
            </div>
          </div>
        ) : profilePending ? (
          <div className="bg-zinc-900 border border-zinc-800 p-8 rounded-3xl text-center space-y-4 max-w-xl mx-auto my-12 shadow-2xl animate-in fade-in zoom-in-95 duration-300">
            <div className="flex justify-center text-amber-500">
              <AlertCircle className="w-12 h-12" />
            </div>
            <h3 className="text-lg font-black text-white uppercase tracking-wider">Đang khởi tạo hồ sơ</h3>
            <p className="text-xs text-zinc-400 leading-relaxed">Hồ sơ thành viên của bạn đang được hệ thống thiết lập và đồng bộ. Vui lòng nhấn nút tải lại bên dưới sau vài giây.</p>
            <button
              type="button"
              onClick={async () => {
                showSuccessToast("Đang tải lại hồ sơ...");
                await refreshProfile();
              }}
              className="bg-brand-orange hover:bg-opacity-95 text-zinc-950 font-black py-3.5 px-8 rounded-xl text-xs uppercase tracking-wider transition-all shadow-lg shadow-amber-500/10 cursor-pointer"
            >
              Tải lại hồ sơ
            </button>
          </div>
        ) : profileError ? (
          <div className="mx-auto my-12 max-w-xl space-y-4 rounded-3xl border border-red-500/30 bg-zinc-900 p-8 text-center shadow-2xl">
            <AlertCircle className="mx-auto h-12 w-12 text-red-400" />
            <h3 className="text-lg font-black uppercase tracking-wider text-white">Không thể tải hồ sơ</h3>
            <p className="text-xs leading-relaxed text-zinc-400">{profileError}</p>
            <button
              type="button"
              onClick={refreshProfile}
              className="rounded-xl bg-brand-orange px-8 py-3.5 text-xs font-black uppercase tracking-wider text-zinc-950"
            >
              Thử lại
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* LEFT PANEL: Member Card & Loyalty Stars Widget */}
          <div className="space-y-6">
            
            {/* Card Widget */}
            <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 relative overflow-hidden shadow-2xl">
              {/* Premium Card Glow background */}
              <div className="absolute -top-24 -right-24 w-48 h-48 bg-brand-orange/10 rounded-full filter blur-3xl pointer-events-none"></div>

              {/* User Avatar + Metadata Header */}
              <div className="flex items-center gap-4 pb-6 border-b border-zinc-800/80">
                <div className="relative w-16 h-16 shrink-0 rounded-full border-2 border-brand-orange overflow-hidden bg-zinc-950 group">
                  {hasImageSource(avatarUrl) ? (
                    <img
                      src={avatarUrl}
                      alt={fullName}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80';
                      }}
                    />
                  ) : (
                    <User className="h-full w-full p-4 text-zinc-600" aria-label="Chưa có ảnh đại diện" />
                  )}
                  {/* Camera overlay */}
                  <button 
                    type="button"
                    onClick={() => {
                      setTempAvatarUrl(avatarUrl);
                      setAvatarFile(null);
                      setIsEditingAvatar(true);
                    }}
                    className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity duration-300 focus:outline-none"
                  >
                    <Camera className="w-5 h-5 text-white" />
                  </button>
                </div>
                <div>
                  <h3 className="font-black text-white text-base leading-snug">{fullName}</h3>
                  <div className="flex flex-wrap items-center gap-2 mt-1">
                    <span className="text-[9px] font-black uppercase tracking-wider bg-brand-orange/15 text-brand-orange border border-brand-orange/20 px-2 py-0.5 rounded">
                      {currentRank}
                    </span>
                    <span className="text-[10px] text-zinc-400 font-bold">
                      {points} Điểm
                    </span>
                  </div>
                </div>
              </div>

              {/* Progress Milestones Bar */}
              <div className="pt-6 space-y-4">
                <div className="flex justify-between items-end">
                  <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider">Tổng chi tiêu {spendingYear}</span>
                  <span className="text-sm font-black text-white">
                    {spendingSummary === undefined
                      ? '...'
                      : spendingSummary === null
                        ? '—'
                        : `${totalSpending.toLocaleString('vi-VN')}đ`}
                  </span>
                </div>

                {/* Progress bar container */}
                <div className="relative w-full h-2.5 bg-zinc-950 rounded-full border border-zinc-800/80 overflow-hidden">
                  <div 
                    className="absolute top-0 left-0 h-full bg-gradient-to-r from-brand-orange to-brand-yellow rounded-full transition-all duration-500"
                    style={{ width: `${membershipProgress}%` }}
                  ></div>
                </div>

                {/* Milestone Markers */}
                <div className="flex justify-between items-center text-[9px] text-zinc-500 font-bold pt-1.5">
                  <div className="text-left">
                    <span className="block text-white">0đ</span>
                    <span>Standard</span>
                  </div>
                  <div className="text-center">
                    <span className={`block ${totalSpending >= 2000000 ? 'text-brand-orange' : ''}`}>2.000.000đ</span>
                    <span>Bạc (Silver)</span>
                  </div>
                  <div className="text-right">
                    <span className={`block ${totalSpending >= 4000000 ? 'text-brand-yellow' : ''}`}>4.000.000đ</span>
                    <span>Vàng (Gold)</span>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => setActiveTab('loyalty')}
                  className="w-full mt-4 bg-gradient-to-r from-amber-500/20 to-brand-orange/20 hover:from-amber-500/30 hover:to-brand-orange/30 text-amber-400 border border-amber-500/30 rounded-xl py-2.5 px-4 text-xs font-black uppercase tracking-wider flex items-center justify-center gap-2 transition-all shadow-md cursor-pointer"
                >
                  <Award className="w-4 h-4" />
                  <span>Trung tâm Điểm thưởng & Hạng thẻ</span>
                </button>
              </div>
            </div>

            {/* Support Action Links */}
            <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 space-y-4 shadow-xl">
              <span className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block border-b border-zinc-800/80 pb-2">HỖ TRỢ THÀNH VIÊN</span>
              
              <a 
                href="tel:19001000"
                className="flex items-center justify-between text-xs text-zinc-300 hover:text-brand-orange transition-colors py-1 focus:outline-none"
              >
                <div className="flex items-center gap-2.5">
                  <PhoneCall className="w-4 h-4 text-zinc-500 shrink-0" />
                  <span>HOTLINE hỗ trợ (1900 1000)</span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 text-zinc-600" />
              </a>

              <a 
                href="mailto:support@lorafilm.com"
                className="flex items-center justify-between text-xs text-zinc-300 hover:text-brand-orange transition-colors py-1 focus:outline-none"
              >
                <div className="flex items-center gap-2.5">
                  <Mail className="w-4 h-4 text-zinc-500 shrink-0" />
                  <span>Email hỗ trợ (support@lorafilm.com)</span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 text-zinc-600" />
              </a>

              <button 
                type="button"
                onClick={() => setActiveTab('policy')}
                className="w-full flex items-center justify-between text-xs text-zinc-300 hover:text-brand-orange transition-colors py-1 text-left focus:outline-none"
              >
                <div className="flex items-center gap-2.5">
                  <HelpCircle className="w-4 h-4 text-zinc-500 shrink-0" />
                  <span>Câu hỏi thường gặp</span>
                </div>
                <ChevronRight className="w-3.5 h-3.5 text-zinc-600" />
              </button>
            </div>

          </div>

          {/* RIGHT PANEL: Multi-tab Information Hub */}
          <div className="lg:col-span-2 space-y-6">
            
            {/* Horizontal Tabs Menu bar */}
            <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-1.5 flex flex-wrap gap-1 shadow-lg">
              <button
                type="button"
                onClick={() => setActiveTab('info')}
                className={`flex-grow sm:flex-grow-0 px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 flex items-center justify-center gap-2 ${
                  activeTab === 'info'
                    ? 'bg-brand-orange text-white shadow-md'
                    : 'text-zinc-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <User className="w-4 h-4 shrink-0" />
                <span>Thông Tin Cá Nhân</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('history')}
                className={`flex-grow sm:flex-grow-0 px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 flex items-center justify-center gap-2 ${
                  activeTab === 'history'
                    ? 'bg-brand-orange text-white shadow-md'
                    : 'text-zinc-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <History className="w-4 h-4 shrink-0" />
                <span>Lịch Sử Giao Dịch</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('notifications')}
                className={`flex-grow sm:flex-grow-0 px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 flex items-center justify-center gap-2 ${
                  activeTab === 'notifications'
                    ? 'bg-brand-orange text-white shadow-md'
                    : 'text-zinc-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <Bell className="w-4 h-4 shrink-0" />
                <span>Thông Báo</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('gifts')}
                className={`flex-grow sm:flex-grow-0 px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 flex items-center justify-center gap-2 ${
                  activeTab === 'gifts'
                    ? 'bg-brand-orange text-white shadow-md'
                    : 'text-zinc-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <Gift className="w-4 h-4 shrink-0" />
                <span>Quà Tặng</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('policy')}
                className={`flex-grow sm:flex-grow-0 px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 flex items-center justify-center gap-2 ${
                  activeTab === 'policy'
                    ? 'bg-brand-orange text-white shadow-md'
                    : 'text-zinc-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <FileText className="w-4 h-4 shrink-0" />
                <span>Chính Sách</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('loyalty')}
                className={`flex-grow sm:flex-grow-0 px-4 py-2.5 rounded-xl text-xs font-bold transition-all duration-300 flex items-center justify-center gap-2 ${
                  activeTab === 'loyalty'
                    ? 'bg-brand-orange text-white shadow-md'
                    : 'text-zinc-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <Award className="w-4 h-4 shrink-0" />
                <span>Điểm Thưởng & Hạng Thẻ</span>
              </button>
            </div>

            {/* TAB CONTENTS CONTAINER */}
            <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 md:p-8 shadow-2xl">
              
              {/* TAB 1: Personal info form */}
              {activeTab === 'info' && (
                <form onSubmit={handleUpdateProfile} className="space-y-6">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    {/* Full Name field (STRICTLY READ-ONLY DISABLED) */}
                    <div className="space-y-2">
                      <label className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Họ và tên</label>
                      <div className="relative">
                        <User className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-655" />
                        <input
                          type="text"
                          disabled
                          value={fullName}
                          className="w-full bg-zinc-900/50 text-zinc-500 border border-zinc-800 rounded-xl py-3 pl-11 pr-4 text-xs font-semibold select-none cursor-not-allowed"
                        />
                      </div>
                    </div>

                    {/* Birthday field (STRICTLY READ-ONLY DISABLED) */}
                    <div className="space-y-2">
                      <label className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Ngày sinh</label>
                      <div className="relative">
                        <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-655" />
                        <input
                          type="date"
                          disabled
                          value={birthday}
                          className="w-full bg-zinc-900/50 text-zinc-500 border border-zinc-800 rounded-xl py-3 pl-11 pr-4 text-xs font-semibold select-none cursor-not-allowed"
                        />
                      </div>
                    </div>

                    {/* Email field (EDITABLE - toggle to update) */}
                    <div className="space-y-2">
                      <label className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block flex justify-between">
                        <span>Địa chỉ Email</span>
                        <button
                          type="button"
                          onClick={() => {
                            setNewEmail(email);
                            setIsChangingEmail(!isChangingEmail);
                          }}
                          className="text-brand-orange hover:underline focus:outline-none"
                        >
                          {isChangingEmail ? 'Hủy' : 'Thay đổi'}
                        </button>
                      </label>
                      {isChangingEmail ? (
                        <div className="flex gap-2">
                          <div className="relative flex-grow">
                            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-600" />
                            <input
                              type="email"
                              value={newEmail}
                              onChange={(e) => setNewEmail(e.target.value)}
                              className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl py-3 pl-11 pr-4 text-xs font-semibold text-white focus:outline-none transition-colors"
                            />
                          </div>
                          <button
                            type="button"
                            onClick={handleSaveEmail}
                            className="bg-brand-orange hover:bg-opacity-95 text-white font-bold px-4 rounded-xl text-xs transition-colors"
                          >
                            Lưu
                          </button>
                        </div>
                      ) : (
                        <div className="relative">
                          <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-600" />
                          <input
                            type="email"
                            disabled
                            value={email}
                            className="w-full bg-zinc-950/40 border border-zinc-900 text-zinc-400 rounded-xl py-3 pl-11 pr-4 text-xs font-semibold select-none cursor-not-allowed"
                          />
                        </div>
                      )}
                    </div>

                    {/* Phone field (EDITABLE) */}
                    <div className="space-y-2">
                      <label className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Số điện thoại</label>
                      <div className="relative">
                        <Phone className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-600" />
                        <input
                          type="text"
                          value={phone}
                          onChange={(e) => setPhone(e.target.value)}
                          className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl py-3 pl-11 pr-4 text-xs font-semibold text-white focus:outline-none transition-colors"
                        />
                      </div>
                    </div>

                    {/* Gender field (STRICTLY READ-ONLY DISABLED) */}
                    <div className="space-y-2">
                      <label className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">Giới tính</label>
                      <div className="flex gap-4">
                        <button
                          type="button"
                          disabled
                          className={`flex-grow py-3 rounded-xl border text-xs font-bold transition-all duration-300 bg-zinc-900/50 text-zinc-500 border-zinc-800 cursor-not-allowed ${
                            gender === 'MALE' ? 'opacity-100 border-brand-orange/40 text-brand-orange' : 'opacity-40'
                          }`}
                        >
                          Nam
                        </button>
                        <button
                          type="button"
                          disabled
                          className={`flex-grow py-3 rounded-xl border text-xs font-bold transition-all duration-300 bg-zinc-900/50 text-zinc-500 border-zinc-800 cursor-not-allowed ${
                            gender === 'FEMALE' ? 'opacity-100 border-brand-orange/40 text-brand-orange' : 'opacity-40'
                          }`}
                        >
                          Nữ
                        </button>
                        <button
                          type="button"
                          disabled
                          className={`flex-grow py-3 rounded-xl border text-xs font-bold transition-all duration-300 bg-zinc-900/50 text-zinc-500 border-zinc-800 cursor-not-allowed ${
                            gender === 'OTHER' ? 'opacity-100 border-brand-orange/40 text-brand-orange' : 'opacity-40'
                          }`}
                        >
                          Khác
                        </button>
                      </div>
                    </div>

                    {/* Password field (Obscured mask with active Change trigger) */}
                    <div className="space-y-2">
                      <label className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block flex justify-between">
                        <span>Mật khẩu</span>
                        <button
                          type="button"
                          onClick={() => setIsChangingPassword(!isChangingPassword)}
                          className="text-brand-orange hover:underline focus:outline-none"
                        >
                          {isChangingPassword ? 'Hủy' : 'Thay đổi'}
                        </button>
                      </label>
                      <div className="relative">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-700" />
                        <input
                          type="text"
                          disabled
                          value="••••••••••••"
                          className="w-full bg-zinc-950/40 border border-zinc-900 text-zinc-500 rounded-xl py-3 pl-11 pr-4 text-xs font-semibold select-none cursor-not-allowed"
                        />
                      </div>
                    </div>

                    {/* Citizen ID field (STRICTLY READ-ONLY DISABLED, MASKED) */}
                    <div className="space-y-2">
                      <label className="text-[10px] text-zinc-500 font-black uppercase tracking-wider block">
                        Số Căn cước công dân (CCCD)
                      </label>
                      <div className="relative">
                        <User className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-700" />
                        <input
                          type="text"
                          disabled
                          value={profile?.cccdMasked ?? ''}
                          className="w-full bg-zinc-950/40 border border-zinc-900 text-zinc-500 rounded-xl py-3 pl-11 pr-4 text-xs font-semibold select-none cursor-not-allowed font-mono tracking-widest"
                        />
                      </div>
                    </div>

                  </div>

                  {/* Submit Update */}
                  <div className="pt-4 flex justify-center">
                    <button
                      type="submit"
                      className="bg-brand-orange hover:bg-opacity-95 text-white font-black py-4 px-12 rounded-xl text-xs uppercase tracking-wider shadow-lg shadow-brand-orange/20 transition-all transform hover:scale-[1.02]"
                    >
                      Cập nhật
                    </button>
                  </div>
                </form>
              )}

              {/* TAB 2: Transaction History */}
              {activeTab === 'history' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-sm font-black text-white uppercase tracking-wider mb-1">LỊCH SỬ ĐẶT VÉ</h3>
                    <p className="text-zinc-500 text-[10px]">Danh sách các vé và dịch vụ đã mua trực tuyến hoặc tại quầy</p>
                  </div>

                  <div className="min-h-[400px]">
                    <CustomerBookingHistory />
                  </div>
                </div>
              )}

              {/* TAB 3: Notifications */}
              {activeTab === 'notifications' && (
                <CustomerNotificationCenter />
              )}

              {/* TAB 4: Rewards / Gifts */}
              {activeTab === 'gifts' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-sm font-black text-white uppercase tracking-wider mb-1">ƯU ĐÃI CỦA BẠN</h3>
                    <p className="text-zinc-500 text-[10px]">Danh sách voucher và quà tặng đang có hiệu lực sử dụng</p>
                  </div>

                  <div className="flex flex-col items-center justify-center py-12 text-center">
                    <p className="text-xs text-zinc-500 font-bold">Hiện tại chưa có dữ liệu.</p>
                  </div>
                </div>
              )}

              {/* TAB 5: Policies / Member rules */}
              {activeTab === 'policy' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-sm font-black text-white uppercase tracking-wider mb-1">CHÍNH SÁCH THÀNH VIÊN</h3>
                    <p className="text-zinc-500 text-[10px]">Quy định tích lũy điểm thưởng và thăng hạng thành viên</p>
                  </div>

                  <div className="space-y-4 text-xs text-zinc-400 leading-relaxed">
                    <div className="bg-zinc-950/40 border border-zinc-850 rounded-2xl p-4 space-y-2.5">
                      <h4 className="font-extrabold text-zinc-200 uppercase text-[11px] border-l-2 border-brand-orange pl-2">Quy định tích điểm</h4>
                      <p>
                        Với mỗi giao dịch đặt vé xem phim hoặc bắp nước tại hệ thống LoraFilm, thành viên sẽ nhận được điểm tích lũy tương đương 10% giá trị hóa đơn thực tế thanh toán (10.000đ = 1 điểm).
                      </p>
                    </div>

                    <div className="bg-zinc-950/40 border border-zinc-850 rounded-2xl p-4 space-y-2.5">
                      <h4 className="font-extrabold text-zinc-200 uppercase text-[11px] border-l-2 border-brand-orange pl-2">Cấp bậc thành viên Lora</h4>
                      <ul className="list-disc pl-4 space-y-1.5">
                        <li><strong>Standard Member</strong>: Doanh số chi tiêu lũy kế dưới 2.000.000đ trong năm.</li>
                        <li><strong>Silver VIP Member</strong>: Chi tiêu từ 2.000.000đ đến dưới 4.000.000đ. Nhận ưu đãi giảm giá 5% tại quầy bắp nước.</li>
                        <li><strong>Gold VIP Member</strong>: Chi tiêu tích lũy từ 4.000.000đ trở lên. Giảm giá 10% tại quầy bắp nước và tặng vé sinh nhật miễn phí.</li>
                      </ul>
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 6: Loyalty Center */}
              {activeTab === 'loyalty' && (
                <div className="-m-6 md:-m-8">
                  <LoyaltyCenterPage />
                </div>
              )}
            </div>

          </div>

        </div>
        )}
      </div>

      {/* Change Password Modal */}
      {isChangingPassword && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 md:p-8 max-w-md w-full shadow-2xl space-y-6 animate-in zoom-in duration-300">
            <div>
              <h3 className="text-base font-black text-white uppercase tracking-wider">ĐỔI MẬT KHẨU TÀI KHOẢN</h3>
              <p className="text-zinc-500 text-[10px] mt-0.5">Nhập mật khẩu hiện tại và mật khẩu mới của bạn</p>
            </div>

            <div className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">Mật khẩu hiện tại</label>
                <div className="relative">
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-650" />
                  <input
                    type={showPasswordRaw ? 'text' : 'password'}
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl py-3 pl-11 pr-12 text-xs font-semibold text-white focus:outline-none transition-colors"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPasswordRaw(!showPasswordRaw)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-white"
                  >
                    {showPasswordRaw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">Mật khẩu mới</label>
                <div className="relative">
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-655" />
                  <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl py-3 pl-11 pr-4 text-xs font-semibold text-white focus:outline-none transition-colors"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">Xác nhận mật khẩu mới</label>
                <div className="relative">
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-655" />
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl py-3 pl-11 pr-4 text-xs font-semibold text-white focus:outline-none transition-colors"
                  />
                </div>
              </div>
            </div>

            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => setIsChangingPassword(false)}
                className="flex-grow bg-zinc-950 hover:bg-zinc-850 border border-zinc-800 text-zinc-400 font-bold py-3.5 rounded-xl text-xs uppercase tracking-wider transition-colors"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleSavePassword}
                className="flex-grow bg-brand-orange hover:bg-opacity-95 text-white font-black py-3.5 rounded-xl text-xs uppercase tracking-wider transition-colors"
              >
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Avatar Modal */}
      {isEditingAvatar && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 md:p-8 max-w-md w-full shadow-2xl space-y-6 animate-in zoom-in duration-300">
            <div>
              <h3 className="text-base font-black text-white uppercase tracking-wider">CẬP NHẬT ẢNH ĐẠI DIỆN</h3>
              <p className="text-zinc-500 text-[10px] mt-0.5">Chọn ảnh JPEG, PNG hoặc WebP có dung lượng tối đa 5 MB.</p>
            </div>

            <div className="space-y-4">
              {/* Preview image */}
              <div className="flex justify-center">
                <div className="w-24 h-24 rounded-full border border-zinc-700 overflow-hidden bg-zinc-950">
                  {hasImageSource(tempAvatarUrl) ? (
                    <img
                      src={tempAvatarUrl}
                      alt="Preview avatar"
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80';
                      }}
                    />
                  ) : (
                    <User className="h-full w-full p-6 text-zinc-600" aria-label="Chưa có ảnh xem trước" />
                  )}
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">Tệp ảnh</label>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={(event) => {
                    const file = event.target.files?.[0] || null;
                    setAvatarFile(file);
                    setTempAvatarUrl(file ? URL.createObjectURL(file) : avatarUrl);
                  }}
                  className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl py-3 px-4 text-xs font-semibold text-white focus:outline-none transition-colors"
                />
              </div>
            </div>

            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => setIsEditingAvatar(false)}
                className="flex-grow bg-zinc-950 hover:bg-zinc-800 border border-zinc-800 text-zinc-400 font-bold py-3.5 rounded-xl text-xs uppercase tracking-wider transition-colors"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleSaveAvatar}
                className="flex-grow bg-brand-orange hover:bg-opacity-95 text-white font-black py-3.5 rounded-xl text-xs uppercase tracking-wider transition-colors"
              >
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      )}

      </main>
    </div>
  );
}
