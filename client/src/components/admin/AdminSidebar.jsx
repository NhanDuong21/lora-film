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
  Award,
  Key,
  ShieldAlert,
  UserCircle,
  FileSearch,
  CreditCard,
  BarChart3,
  BellRing,
  Mail,
  ListChecks,
  CalendarRange
} from 'lucide-react';
import { getAdminLandingPath, hasPermissionAccess } from '@/features/internal-staff/admin/permissionAccess';

export default function AdminSidebar({
  activeTab,
  setActiveTab,
  user,
  onBackHome,
  handleLogout
}) {
  const navigate = useNavigate();
  const permissions = user?.permissions || [];
  const normalizedRole = String(user?.role || '').replace(/^ROLE_/, '');
  const can = (...requiredPermissions) =>
    hasPermissionAccess(normalizedRole, permissions, ...requiredPermissions);
  const isFullAdmin = normalizedRole === 'ADMIN' || permissions.includes('PERM_ROOT_ACCESS');
  const isAccountantOnly = can('PERM_VIEW_FINANCE') && !isFullAdmin;
  const canManageCustomers = can('CUSTOMER_VIEW');
  const canManageEmployees = can('EMPLOYEE_VIEW');
  const canManageDepartments = can('DEPARTMENT_VIEW');
  const canManagePositions = can('POSITION_VIEW');
  const canManagePayroll = can('PAYROLL_VIEW');
  const canManageRoles = can('ROLE_VIEW');
  const canManagePermissions = can('PERMISSION_VIEW');
  const canConfigureSystem = can('SYSTEM_CONFIGURATION');
  const canViewUserAudits = can('USER_AUDIT_VIEW', 'SYSTEM_CONFIGURATION');
  const hasHumanResourcesAccess = canManageEmployees || canManageDepartments
    || canManagePositions || canManagePayroll;
  const hasSystemAccess = canManageRoles || canManagePermissions
    || canConfigureSystem || canViewUserAudits;
  const roleLabel = isAccountantOnly
    ? 'Finance'
    : (isFullAdmin ? 'Admin' : normalizedRole.replaceAll('_', ' ') || 'Staff');
  const adminHomePath = getAdminLandingPath(normalizedRole, permissions);

  // Collapsible categories state (default false = expanded, true = collapsed)
  const [collapsedSections, setCollapsedSections] = useState({
    noiDung: false,
    coSo: false,
    lichGia: false,
    vanHanhDatVe: false,
    thanhToan: false,
    baoCao: false,
    khachHang: false,
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
    <aside className="w-[280px] h-screen h-[100dvh] sticky top-0 bg-zinc-950 border-r border-zinc-800 flex flex-col shrink-0 z-30 select-none overflow-hidden font-sans">

      <div className="flex min-h-0 flex-1 flex-col">
        {/* Brand Top Header */}
        <div className="px-6 py-6 border-b border-zinc-800/60 flex items-center justify-between shrink-0 h-[72px]">
          <Link to={adminHomePath} className="flex items-center gap-2.5 bg-transparent p-0 m-0 shadow-none border-none select-none decoration-none group">
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
            {roleLabel}
          </span>
        </div>

        {/* Scrollable Navigation List */}
        <nav className="min-h-0 flex-1 py-4 space-y-1 overflow-y-auto scrollbar-thin scrollbar-thumb-zinc-800 scrollbar-track-transparent">

          {/* Section 1: Dashboard */}
          {can('DASHBOARD_VIEW') && (
            <div className="mb-2">
              <button
                onClick={() => handleTabClick('dashboard', '#/admin')}
                className={getTopLinkClass('dashboard')}
              >
                <LayoutDashboard className="w-4 h-4 shrink-0" />
                <span>Tổng quan hệ thống</span>
              </button>
            </div>
          )}

          {isFullAdmin && (
            <div className="mb-2">
              <button
                onClick={() => handleTabClick('movie-operations', '#/admin/movie-operations')}
                className={getTopLinkClass('movie-operations')}
              >
                <ListChecks className="w-4 h-4 shrink-0" />
                <span>Trung tâm vận hành phim</span>
              </button>
            </div>
          )}

          {/* Section 2: Content Management */}
          {isFullAdmin && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('noiDung')}
                className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
              >
                <span>Nội dung & phát hành</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.noiDung ? 'rotate-180' : ''
                  }`} />
              </button>

              {!collapsedSections.noiDung && (
                <div className="space-y-0.5">
                  <button onClick={() => handleTabClick('movies', '#/admin/movies')} className={getSubLinkClass('movies')}>
                    <Film className="w-4 h-4 shrink-0" />
                    <span>Danh sách phim</span>
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

          {/* Section 3: Facilities */}
          {isFullAdmin && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('coSo')}
                className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
              >
                <span>Cơ sở rạp</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.coSo ? 'rotate-180' : ''
                  }`} />
              </button>

              {!collapsedSections.coSo && (
                <div className="space-y-0.5">
                  <button onClick={() => handleTabClick('clusters', '#/admin/cinemas')} className={getSubLinkClass('clusters')}>
                    <Building className="w-4 h-4 shrink-0" />
                    <span>Cụm rạp & giờ hoạt động</span>
                  </button>
                  <button onClick={() => handleTabClick('rooms', '#/admin/rooms')} className={getSubLinkClass('rooms')}>
                    <DoorOpen className="w-4 h-4 shrink-0" />
                    <span>Phòng chiếu</span>
                  </button>
                  <button onClick={() => handleTabClick('seat-types', '#/admin/seat-types')} className={getSubLinkClass('seat-types')}>
                    <Armchair className="w-4 h-4 shrink-0" />
                    <span>Cấu hình loại ghế</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Section 4: Scheduling and pricing */}
          {isFullAdmin && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('lichGia')}
                className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
              >
                <span>Lịch chiếu & giá vé</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.lichGia ? 'rotate-180' : ''
                  }`} />
              </button>

              {!collapsedSections.lichGia && (
                <div className="space-y-0.5">
                  <button onClick={() => handleTabClick('showtimes', '#/admin/showtimes')} className={getSubLinkClass('showtimes')}>
                    <Calendar className="w-4 h-4 shrink-0" />
                    <span>Lịch chiếu</span>
                  </button>
                  <button onClick={() => handleTabClick('auto-schedule-create', '#/admin/showtime-schedules/create')} className={getSubLinkClass('auto-schedule-create')}>
                    <Zap className="w-4 h-4 shrink-0" />
                    <span>Tạo lịch tuần</span>
                  </button>
                  <button onClick={() => handleTabClick('auto-schedule-history', '#/admin/showtime-schedules')} className={getSubLinkClass('auto-schedule-history')}>
                    <History className="w-4 h-4 shrink-0" />
                    <span>Lịch đang soạn</span>
                  </button>
                  <button onClick={() => handleTabClick('pricing', '#/admin/pricing')} className={getSubLinkClass('pricing')}>
                    <CalendarRange className="w-4 h-4 shrink-0" />
                    <span>Bảng giá</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Section 5: Booking operations */}
          {isFullAdmin && <div className="space-y-1">
            <button
              onClick={() => toggleSection('vanHanhDatVe')}
              className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
            >
              <span>Vận hành đặt vé</span>
              <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.vanHanhDatVe ? 'rotate-180' : ''
                }`} />
            </button>

            {!collapsedSections.vanHanhDatVe && (
              <div className="space-y-0.5">
                <button onClick={() => handleTabClick('bookings', '#/admin/bookings')} className={getSubLinkClass('bookings')}>
                  <Ticket className="w-4 h-4 shrink-0" />
                  <span>Đơn đặt vé & giữ ghế</span>
                </button>
                <button onClick={() => handleTabClick('concessions', '#/admin/concessions')} className={getSubLinkClass('concessions')}>
                  <Coffee className="w-4 h-4 shrink-0" />
                  <span>Danh mục bắp nước</span>
                </button>
              </div>
            )}
          </div>}

          {/* Section 5: Payment operations */}
          {(isFullAdmin || isAccountantOnly) && <div className="space-y-1">
            <button
              onClick={() => toggleSection('thanhToan')}
              className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
            >
              <span>Thanh toán</span>
              <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.thanhToan ? 'rotate-180' : ''
                }`} />
            </button>

            {!collapsedSections.thanhToan && (
              <div className="space-y-0.5">
                <button onClick={() => handleTabClick('payments', '#/admin/payments')} className={getSubLinkClass('payments')}>
                  <CreditCard className="w-4 h-4 shrink-0" />
                  <span>Giao dịch & Đối soát</span>
                </button>
              </div>
            )}
          </div>}

          {/* Section 6: Reports */}
          {(isFullAdmin || isAccountantOnly) && <div className="space-y-1">
            <button
              onClick={() => toggleSection('baoCao')}
              className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
            >
              <span>Báo cáo & phân tích</span>
              <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.baoCao ? 'rotate-180' : ''
                }`} />
            </button>

            {!collapsedSections.baoCao && (
              <div className="space-y-0.5">
                <button onClick={() => handleTabClick('analytics', '#/admin/analytics')} className={getSubLinkClass('analytics')}>
                  <TrendingUp className="w-4 h-4 shrink-0" />
                  <span>Báo cáo doanh thu</span>
                </button>
                <button onClick={() => handleTabClick('finance', '#/admin/finance')} className={getSubLinkClass('finance')}>
                  <BarChart3 className="w-4 h-4 shrink-0" />
                  <span>Doanh thu tổng hợp</span>
                </button>
                <button onClick={() => handleTabClick('concession-sales', '#/admin/concession-sales')} className={getSubLinkClass('concession-sales')}>
                  <Coins className="w-4 h-4 shrink-0" />
                  <span>Doanh thu bắp nước</span>
                </button>
              </div>
            )}
          </div>}

          {/* Section 7: Customers */}
          {(isFullAdmin || canManageCustomers) && <div className="space-y-1">
            <button
              onClick={() => toggleSection('khachHang')}
              className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
            >
              <span>Khách hàng</span>
              <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.khachHang ? 'rotate-180' : ''
                }`} />
            </button>

            {!collapsedSections.khachHang && (
              <div className="space-y-0.5">
                {canManageCustomers && (
                  <>
                    <button onClick={() => handleTabClick('customers', '#/admin/members')} className={getSubLinkClass('customers')}>
                      <Users className="w-4 h-4 shrink-0" />
                      <span>Danh sách Khách hàng</span>
                    </button>
                    {isFullAdmin && <div className="px-6 py-2 mt-2">
                      <span className="text-[10px] uppercase font-black tracking-widest text-zinc-600">Loyalty Program</span>
                    </div>}
                    {isFullAdmin && <button onClick={() => handleTabClick('scores-tiers', '#/admin/scores/tiers')} className={getSubLinkClass('scores-tiers')}>
                      <Award className="w-4 h-4 shrink-0" />
                      <span>Hạng thẻ thành viên</span>
                    </button>}
                    {isFullAdmin && <button onClick={() => handleTabClick('scores-viewer', '#/admin/scores/viewer')} className={getSubLinkClass('scores-viewer')}>
                      <Gift className="w-4 h-4 shrink-0" />
                      <span>Tra cứu Điểm thưởng</span>
                    </button>}
                  </>
                )}
              </div>
            )}
          </div>}

          {/* Section 8: Human Resources */}
          {hasHumanResourcesAccess && <div className="space-y-1">
            <button
              onClick={() => toggleSection('nhanSu')}
              className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
            >
              <span>Nhân sự</span>
              <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.nhanSu ? 'rotate-180' : ''
                }`} />
            </button>

            {!collapsedSections.nhanSu && (
              <div className="space-y-0.5">
                {canManageEmployees && <button onClick={() => handleTabClick('staff', '#/admin/staff')} className={getSubLinkClass('staff')}>
                  <Shield className="w-4 h-4 shrink-0" />
                  <span>Nhân viên</span>
                </button>}
                {canManageDepartments && <button onClick={() => handleTabClick('departments', '#/admin/departments')} className={getSubLinkClass('departments')}>
                  <Building className="w-4 h-4 shrink-0" />
                  <span>Phòng ban</span>
                </button>}
                {canManagePositions && <button onClick={() => handleTabClick('positions', '#/admin/positions')} className={getSubLinkClass('positions')}>
                  <BadgeDollarSign className="w-4 h-4 shrink-0" />
                  <span>Vị trí</span>
                </button>}
                {canManagePayroll && <button onClick={() => handleTabClick('payroll', '#/admin/payroll')} className={getSubLinkClass('payroll')}>
                  <TrendingUp className="w-4 h-4 shrink-0" />
                  <span>Bảng lương</span>
                </button>}
              </div>
            )}
          </div>}

          {/* Section 9: Settings */}
          {hasSystemAccess && (
            <div className="space-y-1">
              <button
                onClick={() => toggleSection('cauHinh')}
                className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap mt-4 mb-1"
              >
                <span>Hệ thống</span>
                <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!collapsedSections.cauHinh ? 'rotate-180' : ''
                  }`} />
              </button>

              {!collapsedSections.cauHinh && (
                <div className="space-y-0.5">
                  {canManageRoles && <button onClick={() => handleTabClick('roles', '#/admin/roles')} className={getSubLinkClass('roles')}>
                    <ShieldAlert className="w-4 h-4 shrink-0" />
                    <span>Quản lý vai trò (Role)</span>
                  </button>}
                  {canConfigureSystem && <button onClick={() => handleTabClick('accounts', '#/admin/accounts')} className={getSubLinkClass('accounts')}>
                    <UserCircle className="w-4 h-4 shrink-0" />
                    <span>Tài khoản đăng nhập</span>
                  </button>}
                  {canManagePermissions && <button onClick={() => handleTabClick('permissions', '#/admin/permissions')} className={getSubLinkClass('permissions')}>
                    <Key className="w-4 h-4 shrink-0" />
                    <span>Quản lý quyền hạn (Permission)</span>
                  </button>}
                  {canConfigureSystem && <button onClick={() => handleTabClick('settings', '#/admin/settings')} className={getSubLinkClass('settings')}>
                    <Sliders className="w-4 h-4 shrink-0" />
                    <span>Cấu hình chung</span>
                  </button>}
                  {canConfigureSystem && <button onClick={() => handleTabClick('audits', '#/admin/audits')} className={getSubLinkClass('audits')}>
                    <FileSearch className="w-4 h-4 shrink-0" />
                    <span>Nhật ký truy cập (Audit)</span>
                  </button>}
                  {canViewUserAudits && <button onClick={() => handleTabClick('user-audits', '#/admin/user-audits')} className={getSubLinkClass('user-audits')}>
                    <FileSearch className="w-4 h-4 shrink-0" />
                    <span>Nhật ký nghiệp vụ</span>
                  </button>}
                  {isFullAdmin && <button onClick={() => handleTabClick('notification-dashboard', '#/admin/notifications')} className={getSubLinkClass('notification-dashboard')}>
                    <BellRing className="w-4 h-4 shrink-0" />
                    <span>Tổng quan thông báo</span>
                  </button>}
                  {isFullAdmin && <button onClick={() => handleTabClick('notification-templates', '#/admin/notification-templates')} className={getSubLinkClass('notification-templates')}>
                    <Mail className="w-4 h-4 shrink-0" />
                    <span>Mẫu thông báo</span>
                  </button>}
                  {isFullAdmin && <button onClick={() => handleTabClick('notification-operations', '#/admin/notification-operations')} className={getSubLinkClass('notification-operations')}>
                    <ListChecks className="w-4 h-4 shrink-0" />
                    <span>Vận hành gửi thông báo</span>
                  </button>}
                </div>
              )}
            </div>
          )
          }

        </nav>
      </div>

      {/* Pinned Bottom User Profile Card */}
      <div className="p-4 border-t border-zinc-800/60 bg-zinc-950 shrink-0">
        <div className="bg-zinc-900 border border-zinc-800 p-3.5 rounded-2xl flex flex-col gap-3 hover-scale">
          <div className="flex items-center justify-start gap-3">
            <div className="w-10 h-10 rounded-full bg-brand-orange/10 border border-brand-orange/20 flex items-center justify-center font-bold text-brand-orange text-sm shrink-0">
              {roleLabel.slice(0, 2).toUpperCase()}
            </div>
            <div className="truncate">
              <span className="text-[10px] text-zinc-500 font-bold block uppercase tracking-wider truncate">
                {roleLabel}
              </span>
              <span className="text-sm text-zinc-100 font-bold block truncate">
                {user?.fullName || 'Quản trị viên Lora'}
              </span>
            </div>
          </div>
          <div className="flex flex-col gap-1 border-t border-zinc-800 pt-3 mt-1">
            <button
              onClick={() => handleTabClick('my-account', '#/admin/me')}
              className={`flex items-center justify-start gap-2.5 rounded px-2 py-2 text-xs font-semibold transition-colors ${activeTab === 'my-account'
                ? 'bg-brand-orange/10 text-brand-orange'
                : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
                }`}
            >
              <UserCircle className="w-3.5 h-3.5" />
              <span>Tài khoản của tôi</span>
            </button>
            <div className="flex items-center justify-between pt-2 mt-1 border-t border-zinc-800/50">
              <button
                onClick={onBackHome}
                className="flex items-center justify-start gap-1.5 px-2 py-1.5 text-xs font-medium text-zinc-400 hover:text-brand-orange transition-colors rounded"
                title="Quay lại trang chủ"
              >
                <Home className="w-4 h-4" />
                <span>Trang chủ</span>
              </button>
              <button
                onClick={handleLogout}
                className="flex items-center justify-start gap-1.5 px-2 py-1.5 text-xs font-bold text-red-500 hover:text-red-400 transition-colors rounded"
              >
                <LogOut className="w-4 h-4" />
                <span>Đăng xuất</span>
              </button>
            </div>
          </div>
        </div>
      </div>

    </aside>
  );
}
