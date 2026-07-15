/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useMemo, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { 
  User, Calendar, Mail, Phone, Lock, Eye, EyeOff, Camera, ChevronRight, 
  PhoneCall, HelpCircle, History, Bell, Gift, FileText, CheckCircle, AlertCircle 
} from 'lucide-react';
import SystemUpdating from '@/components/common/SystemUpdating';

const normalizeDateForInput = (value) => {
  if (!value) return '';
  return String(value).substring(0, 10);
};

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
    refreshProfile
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


  // Local state for tabs: 'info', 'history', 'notifications', 'gifts', 'policy'
  const [activeTab, setActiveTab] = useState(() => {
    const searchParams = new URLSearchParams(location.search || (location.hash && location.hash.includes('?') ? location.hash.substring(location.hash.indexOf('?')) : ''));
    const tabParam = searchParams.get('tab');
    if (tabParam) return tabParam;
    return initialTab;
  });

  useEffect(() => {
    const searchParams = new URLSearchParams(location.search || (location.hash && location.hash.includes('?') ? location.hash.substring(location.hash.indexOf('?')) : ''));
    const tabParam = searchParams.get('tab');
    if (tabParam) {
      setActiveTab(tabParam);
    }
  }, [location]);

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

  // Loyalty and spending states
  // TODO: Connect to loyalty API
  const totalSpending = useMemo(() => user?.totalSpending ?? 0, [user]);
  // TODO: Connect to loyalty API
  const points = useMemo(() => user?.points ?? 0, [user]);

  // Modal / toggle states
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [toastType, setToastType] = useState('success'); // 'success' | 'error'

  const [isChangingEmail, setIsChangingEmail] = useState(false);
  const [newEmail, setNewEmail] = useState(email);

  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPasswordRaw, setShowPasswordRaw] = useState(false);

  const [isEditingAvatar, setIsEditingAvatar] = useState(false);
  const [tempAvatarUrl, setTempAvatarUrl] = useState(avatarUrl);

  useEffect(() => {
    if (profile || sessionEmail) {
      const timer = setTimeout(() => {
        setEmail(sessionEmail || '');
        setPhone(profile?.phoneNumber ?? '');
        setAvatarUrl(profile?.avatarUrl || '');
        setTempAvatarUrl(profile?.avatarUrl || '');
        setNewEmail(sessionEmail || '');
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [profile, sessionEmail]);



  // Calculate membership progress milestones
  // Milestones: 0 (Standard), 2.000.000 (Silver), 4.000.000 (Gold)
  const membershipProgress = useMemo(() => {
    const maxMilestone = 4000000;
    return Math.min((totalSpending / maxMilestone) * 100, 100);
  }, [totalSpending]);

  const currentRank = useMemo(() => {
    if (totalSpending >= 4000000) return 'Gold VIP';
    if (totalSpending >= 2000000) return 'Silver VIP';
    return 'Standard Member';
  }, [totalSpending]);

  const triggerToast = (message, type = 'success') => {
    setToastMessage(message);
    setToastType(type);
    setShowToast(true);
    setTimeout(() => {
      setShowToast(false);
    }, 3500);
  };

  // Profile Form submit (Updates Email & SĐT)
  const handleUpdateProfile = (e) => {
    e.preventDefault();
    if (!phone.trim()) {
      triggerToast('Số điện thoại không được để trống!', 'error');
      return;
    }

    updateUser({
      email,
      phoneNumber: phone,
      avatarUrl,
      totalSpending,
      points
    });

    triggerToast('Cập nhật thông tin cá nhân thành công!');
  };

  // Change Email Action
  const handleSaveEmail = () => {
    if (!newEmail.includes('@')) {
      triggerToast('Email không đúng định dạng!', 'error');
      return;
    }
    setEmail(newEmail);
    updateUser({ email: newEmail });
    setIsChangingEmail(false);
    triggerToast('Đổi địa chỉ email thành công!');
  };

  // Change Password Action
  const handleSavePassword = () => {
    if (!currentPassword) {
      triggerToast('Vui lòng nhập mật khẩu hiện tại!', 'error');
      return;
    }
    if (newPassword.length < 6) {
      triggerToast('Mật khẩu mới phải có ít nhất 6 ký tự!', 'error');
      return;
    }
    if (newPassword !== confirmPassword) {
      triggerToast('Mật khẩu xác nhận không trùng khớp!', 'error');
      return;
    }

    // Password change is currently handled via a separate auth flow or not supported in this view yet
    triggerToast('Tính năng đổi mật khẩu đang được nâng cấp!', 'error');
  };

  // Change Avatar Action
  const handleSaveAvatar = () => {
    if (!tempAvatarUrl.trim()) {
      triggerToast('Đường dẫn ảnh không được để trống!', 'error');
      return;
    }
    setAvatarUrl(tempAvatarUrl);
    updateUser({ avatarUrl: tempAvatarUrl });
    setIsEditingAvatar(false);
    triggerToast('Cập nhật ảnh đại diện thành công!');
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
      <main className="flex-grow pt-32 pb-16 px-4 sm:px-6 md:px-8 max-w-6xl mx-auto w-full">
        {/* Toast alert popup */}
        {showToast && (
          <div className={`fixed top-24 left-1/2 -translate-x-1/2 z-50 py-3.5 px-6 rounded-2xl shadow-2xl border flex items-center gap-3 transition-all duration-300 ${
            toastType === 'success' 
              ? 'bg-emerald-950 border-emerald-500/30 text-emerald-400' 
              : 'bg-red-950 border-red-500/30 text-red-400'
          }`}>
            {toastType === 'success' ? (
              <CheckCircle className="w-5 h-5 shrink-0 text-emerald-500" />
            ) : (
              <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
            )}
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
                triggerToast("Đang tải lại hồ sơ...", "success");
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
                  <img 
                    src={avatarUrl} 
                    alt={fullName} 
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80';
                    }}
                  />
                  {/* Camera overlay */}
                  <button 
                    type="button"
                    onClick={() => {
                      setTempAvatarUrl(avatarUrl);
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
                  <span className="text-[10px] text-zinc-500 font-black uppercase tracking-wider">Tổng chi tiêu 2026</span>
                  <span className="text-sm font-black text-white">{(totalSpending).toLocaleString('vi-VN')}đ</span>
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
                    <h3 className="text-sm font-black text-white uppercase tracking-wider mb-1">LỊCH SỬ GIAO DỊCH</h3>
                    <p className="text-zinc-500 text-[10px]">Danh sách các vé và dịch vụ đã mua trực tuyến hoặc tại quầy</p>
                  </div>

                  <div className="min-h-[400px]">
                    <SystemUpdating message="Lịch sử giao dịch sẽ được đồng bộ từ Booking Service sau khi ra mắt." />
                  </div>
                </div>
              )}

              {/* TAB 3: Notifications */}
              {activeTab === 'notifications' && (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-sm font-black text-white uppercase tracking-wider mb-1">HỘP THƯ THÔNG BÁO</h3>
                    <p className="text-zinc-500 text-[10px]">Cập nhật tin tức khuyến mãi và thay đổi suất chiếu</p>
                  </div>

                  <div className="flex flex-col items-center justify-center py-12 text-center">
                    <p className="text-xs text-zinc-500 font-bold">Hiện tại chưa có dữ liệu.</p>
                  </div>
                </div>
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
              <p className="text-zinc-500 text-[10px] mt-0.5">Dán đường dẫn (URL) hình ảnh bạn muốn sử dụng làm Avatar</p>
            </div>

            <div className="space-y-4">
              {/* Preview image */}
              <div className="flex justify-center">
                <div className="w-24 h-24 rounded-full border border-zinc-700 overflow-hidden bg-zinc-950">
                  <img 
                    src={tempAvatarUrl} 
                    alt="Preview avatar" 
                    className="w-full h-full object-cover" 
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80';
                    }}
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">Đường dẫn ảnh (Image URL)</label>
                <input
                  type="text"
                  value={tempAvatarUrl}
                  onChange={(e) => setTempAvatarUrl(e.target.value)}
                  placeholder="https://example.com/image.jpg"
                  className="w-full bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl py-3 px-4 text-xs font-semibold text-white focus:outline-none transition-colors"
                />
              </div>

              {/* Presets options */}
              <div className="space-y-1.5">
                <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider block">Hoặc chọn ảnh có sẵn:</span>
                <div className="flex gap-2">
                  {[
                    'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80',
                    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
                    'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop&q=80',
                    'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150&auto=format&fit=crop&q=80'
                  ].map((url, i) => (
                    <button
                      key={i}
                      type="button"
                      onClick={() => setTempAvatarUrl(url)}
                      className="w-10 h-10 rounded-full overflow-hidden border-2 border-zinc-800 hover:border-brand-orange transition-colors shrink-0"
                    >
                      <img src={url} alt={`Preset ${i}`} className="w-full h-full object-cover" />
                    </button>
                  ))}
                </div>
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
