import { useState } from 'react';
import { 
  LayoutDashboard, 
  Film, 
  Users, 
  Calendar, 
  Gift, 
  Database, 
  Ticket, 
  Coffee, 
  Clock, 
  Settings, 
  Home, 
  LogOut, 
  ChevronDown,
  TrendingUp,
  Building,
  Coins,
  Shield,
  Sliders
} from 'lucide-react';

export default function AdminSidebar({ 
  activeTab, 
  setActiveTab, 
  user, 
  onBackHome, 
  handleLogout 
}) {
  const permissions = user?.permissions || [];
  const isAccountantOnly = permissions.includes('PERM_VIEW_FINANCE') && !permissions.includes('PERM_ROOT_ACCESS');

  // Collapsible categories state (default false = expanded, true = collapsed)
  const [collapsedSections, setCollapsedSections] = useState({
    noiDung: false,
    coSo: false,
    vanHanh: false,
    nhanSu: false,
    cauHinh: false
  });

  const toggleSection = (section) => {
    setCollapsedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  const handleTabClick = (tabKey, hash) => {
    setActiveTab(tabKey);
    window.location.hash = hash;
  };

  // Helper function to return styling for nested child sub-links (strictly left-aligned and single-line)
  const getSubLinkClass = (tabKey) => {
    const isActive = activeTab === tabKey;
    if (isActive) {
      return "pl-9 pr-4 py-2.5 w-full flex items-center justify-start text-left gap-3 text-sm text-amber-400 bg-amber-500/10 border-l-4 border-amber-500 font-semibold rounded-xl transition-all whitespace-nowrap";
    }
    return "pl-9 pr-4 py-2.5 w-full flex items-center justify-start text-left gap-3 text-sm text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/40 rounded-xl transition-all whitespace-nowrap";
  };

  // Helper function to return styling for standalone top-level links (strictly left-aligned and single-line)
  const getTopLinkClass = (tabKey) => {
    const isActive = activeTab === tabKey;
    if (isActive) {
      return "w-full flex items-center justify-start text-left gap-3 px-4 py-2.5 text-sm font-semibold text-amber-400 bg-amber-500/10 rounded-xl border-l-4 border-amber-500 transition-all whitespace-nowrap";
    }
    return "w-full flex items-center justify-start text-left gap-3 px-4 py-2.5 text-sm font-medium text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/40 rounded-xl transition-all whitespace-nowrap";
  };

  return (
    <aside className="w-72 h-screen sticky top-0 bg-zinc-900 border-r border-zinc-800 flex flex-col justify-between shrink-0 z-30 select-none overflow-hidden">
      
      <div>
        {/* Brand Top Header */}
        <div className="px-6 py-6 border-b border-zinc-800/60 flex items-center justify-between shrink-0">
          <a href="#/" className="flex items-center gap-2.5 bg-transparent p-0 m-0 shadow-none border-none select-none decoration-none group">
            <img 
              src="/images/main-logo.png" 
              alt="LoraFilm Icon" 
              className="h-8 md:h-9 w-auto object-contain bg-transparent" 
            />
            <span className="text-lg md:text-xl font-black tracking-tight text-white font-sans leading-none">
              Lora<span className="text-amber-500 font-black ml-0.5">Film</span>
            </span>
          </a>
          <span className="bg-amber-500/10 border border-amber-500/30 text-amber-400 font-mono text-[9px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider select-none">
            {isAccountantOnly ? 'Finance' : 'Admin'}
          </span>
        </div>

        {/* Scrollable Navigation List */}
        <nav className="p-4 space-y-4 overflow-y-auto max-h-[calc(100vh-210px)] scrollbar-thin scrollbar-thumb-zinc-800 scrollbar-track-transparent">
          
          {/* Section 1: Bảng điều khiển */}
          {!isAccountantOnly && (
            <div>
              <button
                onClick={() => handleTabClick('dashboard', '#/admin')}
                className={getTopLinkClass('dashboard')}
              >
                <LayoutDashboard className="w-4 h-4 shrink-0" />
                <span>DASHBOARD</span>
              </button>
            </div>
          )}

          {/* Section 2: Quản Lý Nội Dung */}
          {!isAccountantOnly && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('noiDung')}
                className="w-full flex items-center justify-between px-4 py-3 text-sm font-bold uppercase tracking-wide text-zinc-300 hover:text-zinc-50 transition-colors select-none text-left whitespace-nowrap"
              >
                <div className="flex items-center justify-start text-left gap-2">
                  <Film className="w-3.5 h-3.5 shrink-0" />
                  <span>QUẢN LÝ NỘI DUNG</span>
                </div>
                <ChevronDown className={`w-3.5 h-3.5 text-zinc-500 transition-transform duration-200 ${
                  !collapsedSections.noiDung ? 'rotate-180' : ''
                }`} />
              </button>

              {!collapsedSections.noiDung && (
                <div className="space-y-1">
                  <button
                    onClick={() => handleTabClick('movies', '#/admin/movies')}
                    className={getSubLinkClass('movies')}
                  >
                    <Film className="w-4 h-4 shrink-0" />
                    <span>Quản lý phim</span>
                  </button>
                  <button
                    onClick={() => handleTabClick('actors', '#/admin/actors')}
                    className={getSubLinkClass('actors')}
                  >
                    <Users className="w-4 h-4 shrink-0" />
                    <span>Diễn viên</span>
                  </button>
                  <button
                    onClick={() => handleTabClick('events-promo', '#/admin/events')}
                    className={getSubLinkClass('events-promo')}
                  >
                    <Gift className="w-4 h-4 shrink-0" />
                    <span>Sự kiện & Khuyến mãi</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Section 3: Quản Lý Cơ Sở */}
          {!isAccountantOnly && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('coSo')}
                className="w-full flex items-center justify-between px-4 py-3 text-sm font-bold uppercase tracking-wide text-zinc-300 hover:text-zinc-50 transition-colors select-none text-left whitespace-nowrap"
              >
                <div className="flex items-center justify-start text-left gap-2">
                  <Building className="w-3.5 h-3.5 shrink-0" />
                  <span>QUẢN LÝ CƠ SỞ</span>
                </div>
                <ChevronDown className={`w-3.5 h-3.5 text-zinc-500 transition-transform duration-200 ${
                  !collapsedSections.coSo ? 'rotate-180' : ''
                }`} />
              </button>

              {!collapsedSections.coSo && (
                <div className="space-y-1">
                  <button
                    onClick={() => handleTabClick('clusters', '#/admin/clusters')}
                    className={getSubLinkClass('clusters')}
                  >
                    <Database className="w-4 h-4 shrink-0" />
                    <span>Cụm rạp & Phòng chiếu</span>
                  </button>
                  <button
                    onClick={() => handleTabClick('showtimes', '#/admin/showtimes')}
                    className={getSubLinkClass('showtimes')}
                  >
                    <Calendar className="w-4 h-4 shrink-0" />
                    <span>Quản lý suất chiếu</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Section 4: Vận Hành Kinh Doanh */}
          <div className="space-y-1">
            <button
              onClick={() => toggleSection('vanHanh')}
              className="w-full flex items-center justify-between px-4 py-3 text-sm font-bold uppercase tracking-wide text-zinc-300 hover:text-zinc-50 transition-colors select-none text-left whitespace-nowrap"
            >
              <div className="flex items-center justify-start text-left gap-2">
                <Coins className="w-3.5 h-3.5 shrink-0" />
                <span>VẬN HÀNH KINH DOANH</span>
              </div>
              <ChevronDown className={`w-3.5 h-3.5 text-zinc-500 transition-transform duration-200 ${
                !collapsedSections.vanHanh ? 'rotate-180' : ''
              }`} />
            </button>

            {!collapsedSections.vanHanh && (
              <div className="space-y-1">
                <button
                  onClick={() => handleTabClick('tickets', '#/admin/tickets')}
                  className={getSubLinkClass('tickets')}
                >
                  <Ticket className="w-4 h-4 shrink-0" />
                  <span>Quản lý vé bán</span>
                </button>
                <button
                  onClick={() => handleTabClick('concession-sales', '#/admin/concession-sales')}
                  className={getSubLinkClass('concession-sales')}
                >
                  <Coffee className="w-4 h-4 shrink-0" />
                  <span>Doanh thu bắp nước</span>
                </button>
                {!isAccountantOnly && (
                  <button
                    onClick={() => handleTabClick('concessions', '#/admin/concessions')}
                    className={getSubLinkClass('concessions')}
                  >
                    <Database className="w-4 h-4 shrink-0" />
                    <span>Danh mục bắp nước</span>
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Section 5: Nhân Sự & Khách Hàng */}
          <div className="space-y-1">
            <button
              onClick={() => toggleSection('nhanSu')}
              className="w-full flex items-center justify-between px-4 py-3 text-sm font-bold uppercase tracking-wide text-zinc-300 hover:text-zinc-50 transition-colors select-none text-left whitespace-nowrap"
            >
              <div className="flex items-center justify-start text-left gap-2">
                <Users className="w-3.5 h-3.5 shrink-0" />
                <span>NHÂN SỰ & KHÁCH HÀNG</span>
              </div>
              <ChevronDown className={`w-3.5 h-3.5 text-zinc-500 transition-transform duration-200 ${
                !collapsedSections.nhanSu ? 'rotate-180' : ''
              }`} />
            </button>

            {!collapsedSections.nhanSu && (
              <div className="space-y-1">
                {!isAccountantOnly && (
                  <>
                    <button
                      onClick={() => handleTabClick('customers', '#/admin/customers')}
                      className={getSubLinkClass('customers')}
                    >
                      <Users className="w-4 h-4 shrink-0" />
                      <span>Danh sách khách hàng</span>
                    </button>
                    <button
                      onClick={() => handleTabClick('staff', '#/admin/staff')}
                      className={getSubLinkClass('staff')}
                    >
                      <Users className="w-4 h-4 shrink-0" />
                      <span>Quản lý nhân sự</span>
                    </button>
                  </>
                )}
                <button
                  onClick={() => handleTabClick('payroll', '#/admin/payroll')}
                  className={getSubLinkClass('payroll')}
                >
                  <TrendingUp className="w-4 h-4 shrink-0" />
                  <span>Bảng lương nhân viên</span>
                </button>
              </div>
            )}
          </div>

          {/* Section 6: Cấu Hình & Bảo Mật */}
          {!isAccountantOnly && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('cauHinh')}
                className="w-full flex items-center justify-between px-4 py-3 text-sm font-bold uppercase tracking-wide text-zinc-300 hover:text-zinc-50 transition-colors select-none text-left whitespace-nowrap"
              >
                <div className="flex items-center justify-start text-left gap-2">
                  <Shield className="w-3.5 h-3.5 shrink-0" />
                  <span>CẤU HÌNH & BẢO MẬT</span>
                </div>
                <ChevronDown className={`w-3.5 h-3.5 text-zinc-500 transition-transform duration-200 ${
                  !collapsedSections.cauHinh ? 'rotate-180' : ''
                }`} />
              </button>

              {!collapsedSections.cauHinh && (
                <div className="space-y-1">
                  <button
                    onClick={() => handleTabClick('settings', '#/admin/settings')}
                    className={getSubLinkClass('settings')}
                  >
                    <Sliders className="w-4 h-4 shrink-0" />
                    <span className="truncate">Cấu hình hệ thống</span>
                  </button>
                </div>
              )}
            </div>
          )}

        </nav>
      </div>

      {/* Pinned Bottom User Profile Card */}
      <div className="p-4 border-t border-zinc-800 bg-zinc-950/80">
        <div className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl flex flex-col gap-3">
          <div className="flex items-center justify-start text-left gap-2.5">
            <div className="w-8 h-8 rounded-full bg-zinc-800 border border-zinc-700 flex items-center justify-center font-bold text-zinc-200 text-xs shrink-0">
              {isAccountantOnly ? 'AC' : 'AD'}
            </div>
            <div className="truncate">
              <span className="text-[10px] text-zinc-500 font-bold block uppercase tracking-wide truncate text-left">
                {isAccountantOnly ? 'Kế toán viên' : 'Quản Trị Viên'}
              </span>
              <span className="text-xs text-zinc-250 font-black block truncate text-left">
                {user?.fullName || 'Quản trị viên Lora'}
              </span>
            </div>
          </div>
          <div className="flex items-center justify-between border-t border-zinc-800 pt-2.5">
            <button 
              onClick={onBackHome}
              className="flex items-center justify-start text-left gap-1.5 text-xs text-zinc-400 hover:text-amber-500 transition-colors whitespace-nowrap"
              title="Quay lại trang chủ khách hàng"
            >
              <Home className="w-4 h-4" />
              <span>Trang chủ</span>
            </button>
            <button 
              onClick={handleLogout}
              className="flex items-center justify-start text-left gap-1 text-xs text-red-400 hover:text-red-300 font-bold transition-colors whitespace-nowrap"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Đăng xuất</span>
            </button>
          </div>
        </div>
      </div>

    </aside>
  );
}
