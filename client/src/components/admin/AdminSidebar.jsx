import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Film, 
  Users, 
  Calendar, 
  Gift, 
  Ticket, 
  Coffee, 
  Home, 
  LogOut, 
  ChevronDown,
  TrendingUp,
  Coins,
  Sliders,
  Building,
  Shield,
  Tags,
  DoorOpen,
  Armchair,
  History,
  Zap,
  BadgeDollarSign,
  BarChart3,
  Award
} from 'lucide-react';

export default function AdminSidebar({ 
  activeTab, 
  setActiveTab, 
  user, 
  onBackHome, 
  handleLogout 
}) {
  const navigate = useNavigate();
  const permissions = user?.permissions || [];
  const isAccountantOnly = permissions.includes('PERM_VIEW_FINANCE') && !permissions.includes('PERM_ROOT_ACCESS');

  // Collapsible categories state (default false = expanded, true = collapsed)
  const [collapsedSections, setCollapsedSections] = useState({
    noiDung: false,
    coSo: false,
    vanHanhTaiChinh: false,
    nhanSu: false,
    cauHinh: false
  });

  const toggleSection = (section) => {
    setCollapsedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  const handleTabClick = (tabKey, hashPath) => {
    if (setActiveTab) setActiveTab(tabKey);
    const path = hashPath.replace('#', '');
    navigate(path);
  };

  // Standardized styling for nested child sub-links
  const getSubLinkClass = (tabKey) => {
    const isActive = activeTab === tabKey;
    if (isActive) {
      return "pl-11 pr-4 py-2.5 w-full flex items-center justify-start text-left gap-3 text-sm text-brand-orange bg-brand-orange/10 border-l-[3px] border-brand-orange font-semibold transition-colors whitespace-nowrap";
    }
    return "pl-11 pr-4 py-2.5 w-full flex items-center justify-start text-left gap-3 text-sm text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/40 border-l-[3px] border-transparent transition-colors whitespace-nowrap";
  };

  // Standardized styling for standalone top-level links
  const getTopLinkClass = (tabKey) => {
    const isActive = activeTab === tabKey;
    if (isActive) {
      return "w-full flex items-center justify-start text-left gap-3 px-6 py-3 text-sm font-semibold text-brand-orange bg-brand-orange/10 border-l-[3px] border-brand-orange transition-colors whitespace-nowrap";
    }
    return "w-full flex items-center justify-start text-left gap-3 px-6 py-3 text-sm font-medium text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/40 border-l-[3px] border-transparent transition-colors whitespace-nowrap";
  };

  return (
    <aside className="w-[280px] h-screen sticky top-0 bg-zinc-950 border-r border-zinc-800 flex flex-col justify-between shrink-0 z-30 select-none overflow-hidden font-sans">
      
      <div>
        {/* Brand Top Header */}
        <div className="px-6 py-6 border-b border-zinc-800/60 flex items-center justify-between shrink-0 h-[72px]">
          <Link to="/admin" className="flex items-center gap-2.5 bg-transparent p-0 m-0 shadow-none border-none select-none decoration-none group">
            <img 
              src="/images/main-logo.png" 
              alt="LoraFilm Icon" 
              className="h-8 w-auto object-contain bg-transparent" 
            />
            <span className="text-xl font-black tracking-tight text-white leading-none">
              Lora<span className="text-brand-orange ml-0.5">Film</span>
            </span>
          </Link>
          <span className="bg-brand-orange/10 border border-brand-orange/30 text-brand-orange font-mono text-[9px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider select-none">
            {isAccountantOnly ? 'Finance' : 'Admin'}
          </span>
        </div>

        {/* Scrollable Navigation List */}
        <nav className="py-4 space-y-1 overflow-y-auto max-h-[calc(100vh-160px)] scrollbar-thin scrollbar-thumb-zinc-800 scrollbar-track-transparent">
          
          {/* Section 1: Dashboard */}
          {!isAccountantOnly && (
            <div className="mb-2">
              <button
                onClick={() => handleTabClick('dashboard', '#/admin')}
                className={getTopLinkClass('dashboard')}
              >
                <LayoutDashboard className="w-4 h-4 shrink-0" />
                <span>Dashboard</span>
              </button>
            </div>
          )}

          {/* Section 2: Content Management */}
          {!isAccountantOnly && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('noiDung')}
                className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
              >
                <span>Nội dung</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${
                  !collapsedSections.noiDung ? 'rotate-180' : ''
                }`} />
              </button>

              {!collapsedSections.noiDung && (
                <div className="space-y-0.5">
                  <button onClick={() => handleTabClick('movies', '#/admin/movies')} className={getSubLinkClass('movies')}>
                    <Film className="w-4 h-4 shrink-0" />
                    <span>Phim & Điện ảnh</span>
                  </button>
                  <button onClick={() => handleTabClick('genres', '#/admin/genres')} className={getSubLinkClass('genres')}>
                    <Tags className="w-4 h-4 shrink-0" />
                    <span>Thể loại</span>
                  </button>
                  <button onClick={() => handleTabClick('events-promo', '#/admin/events')} className={getSubLinkClass('events-promo')}>
                    <Gift className="w-4 h-4 shrink-0" />
                    <span>Khuyến mãi & Sự kiện</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Section 3: Facilities & Showtimes */}
          {!isAccountantOnly && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('coSo')}
                className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
              >
                <span>Cơ sở & Lịch chiếu</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${
                  !collapsedSections.coSo ? 'rotate-180' : ''
                }`} />
              </button>

              {!collapsedSections.coSo && (
                <div className="space-y-0.5">
                  <button onClick={() => handleTabClick('clusters', '#/admin/cinemas')} className={getSubLinkClass('clusters')}>
                    <Building className="w-4 h-4 shrink-0" />
                    <span>Cụm rạp</span>
                  </button>
                  <button onClick={() => handleTabClick('rooms', '#/admin/rooms')} className={getSubLinkClass('rooms')}>
                    <DoorOpen className="w-4 h-4 shrink-0" />
                    <span>Phòng chiếu</span>
                  </button>
                  <button onClick={() => handleTabClick('seat-types', '#/admin/seat-types')} className={getSubLinkClass('seat-types')}>
                    <Armchair className="w-4 h-4 shrink-0" />
                    <span>Loại ghế</span>
                  </button>
                  <button onClick={() => handleTabClick('showtimes', '#/admin/showtimes')} className={getSubLinkClass('showtimes')}>
                    <Calendar className="w-4 h-4 shrink-0" />
                    <span>Lịch chiếu</span>
                  </button>
                  <button onClick={() => handleTabClick('pricing', '#/admin/pricing')} className={getSubLinkClass('pricing')}>
                    <BadgeDollarSign className="w-4 h-4 shrink-0" />
                    <span>Chính sách giá</span>
                  </button>
                  <button onClick={() => handleTabClick('auto-schedule-create', '#/admin/showtime-schedules/create')} className={getSubLinkClass('auto-schedule-create')}>
                    <Zap className="w-4 h-4 shrink-0" />
                    <span>Tạo lịch tự động</span>
                  </button>
                  <button onClick={() => handleTabClick('auto-schedule-history', '#/admin/showtime-schedules')} className={getSubLinkClass('auto-schedule-history')}>
                    <History className="w-4 h-4 shrink-0" />
                    <span>Lịch sử bản xem trước</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Section 4: Operations & Finance */}
          <div className="space-y-1">
            <button
              onClick={() => toggleSection('vanHanhTaiChinh')}
              className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
            >
              <span>Vận hành & Tài chính</span>
              <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${
                !collapsedSections.vanHanhTaiChinh ? 'rotate-180' : ''
              }`} />
            </button>

            {!collapsedSections.vanHanhTaiChinh && (
              <div className="space-y-0.5">
                <button onClick={() => handleTabClick('bookings', '#/admin/bookings')} className={getSubLinkClass('bookings')}>
                  <Ticket className="w-4 h-4 shrink-0" />
                  <span>Quản lý vé & Đơn hàng</span>
                </button>
                <button onClick={() => handleTabClick('finance', '#/admin/finance')} className={getSubLinkClass('finance')}>
                  <BarChart3 className="w-4 h-4 shrink-0" />
                  <span>Báo cáo doanh thu</span>
                </button>
                {!isAccountantOnly && (
                  <button onClick={() => handleTabClick('concessions', '#/admin/concessions')} className={getSubLinkClass('concessions')}>
                    <Coffee className="w-4 h-4 shrink-0" />
                    <span>Danh mục bắp nước</span>
                  </button>
                )}
                <button onClick={() => handleTabClick('concession-sales', '#/admin/concession-sales')} className={getSubLinkClass('concession-sales')}>
                  <Coins className="w-4 h-4 shrink-0" />
                  <span>Doanh thu bắp nước</span>
                </button>
              </div>
            )}
          </div>

          {/* Section 5: Users & HR */}
          <div className="space-y-1">
            <button
              onClick={() => toggleSection('nhanSu')}
              className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
            >
              <span>Nhân sự & Khách hàng</span>
              <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${
                !collapsedSections.nhanSu ? 'rotate-180' : ''
              }`} />
            </button>

            {!collapsedSections.nhanSu && (
              <div className="space-y-0.5">
                {!isAccountantOnly && (
                  <>
                    <button onClick={() => handleTabClick('customers', '#/admin/members')} className={getSubLinkClass('customers')}>
                      <Users className="w-4 h-4 shrink-0" />
                      <span>Khách hàng</span>
                    </button>
                    <button onClick={() => handleTabClick('scores-viewer', '#/admin/scores/viewer')} className={getSubLinkClass('scores-viewer')}>
                      <Award className="w-4 h-4 shrink-0" />
                      <span>Tra cứu Điểm thưởng</span>
                    </button>
                    <button onClick={() => handleTabClick('scores-tiers', '#/admin/scores/tiers')} className={getSubLinkClass('scores-tiers')}>
                      <Gift className="w-4 h-4 shrink-0" />
                      <span>Hạng thẻ thành viên</span>
                    </button>
                    <button onClick={() => handleTabClick('staff', '#/admin/staff')} className={getSubLinkClass('staff')}>
                      <Shield className="w-4 h-4 shrink-0" />
                      <span>Nhân viên</span>
                    </button>
                  </>
                )}
                <button onClick={() => handleTabClick('payroll', '#/admin/payroll')} className={getSubLinkClass('payroll')}>
                  <TrendingUp className="w-4 h-4 shrink-0" />
                  <span>Bảng lương</span>
                </button>
              </div>
            )}
          </div>

          {/* Section 6: Settings */}
          {!isAccountantOnly && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('cauHinh')}
                className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
              >
                <span>Hệ thống</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${
                  !collapsedSections.cauHinh ? 'rotate-180' : ''
                }`} />
              </button>

              {!collapsedSections.cauHinh && (
                <div className="space-y-0.5">
                  <button onClick={() => handleTabClick('settings', '#/admin/settings')} className={getSubLinkClass('settings')}>
                    <Sliders className="w-4 h-4 shrink-0" />
                    <span>Cấu hình chung</span>
                  </button>
                </div>
              )}
            </div>
          )}

        </nav>
      </div>

      {/* Pinned Bottom User Profile Card */}
      <div className="p-4 border-t border-zinc-800/60 bg-zinc-950 shrink-0">
        <div className="bg-zinc-900 border border-zinc-800 p-3.5 rounded-2xl flex flex-col gap-3 hover-scale">
          <div className="flex items-center justify-start gap-3">
            <div className="w-10 h-10 rounded-full bg-brand-orange/10 border border-brand-orange/20 flex items-center justify-center font-bold text-brand-orange text-sm shrink-0">
              {isAccountantOnly ? 'AC' : 'AD'}
            </div>
            <div className="truncate">
              <span className="text-[10px] text-zinc-500 font-bold block uppercase tracking-wider truncate">
                {isAccountantOnly ? 'Kế toán viên' : 'Quản Trị Viên'}
              </span>
              <span className="text-sm text-zinc-100 font-bold block truncate">
                {user?.fullName || 'Quản trị viên Lora'}
              </span>
            </div>
          </div>
          <div className="flex items-center justify-between border-t border-zinc-800 pt-3 mt-1">
            <button 
              onClick={onBackHome}
              className="flex items-center justify-start gap-1.5 text-xs font-medium text-zinc-400 hover:text-brand-orange transition-colors"
              title="Quay lại trang chủ"
            >
              <Home className="w-4 h-4" />
              <span>Trang chủ</span>
            </button>
            <button 
              onClick={handleLogout}
              className="flex items-center justify-start gap-1.5 text-xs font-bold text-red-500 hover:text-red-400 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Đăng xuất</span>
            </button>
          </div>
        </div>
      </div>

    </aside>
  );
}
