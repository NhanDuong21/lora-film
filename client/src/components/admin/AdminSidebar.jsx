import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Award,
  BadgePercent,
  BellRing,
  Building,
  Calendar,
  CalendarRange,
  CalendarClock,
  ChevronDown,
  Coffee,
  Coins,
  CreditCard,
  Calculator,
  DoorOpen,
  FileSearch,
  Film,
  Gift,
  History,
  Inbox,
  Home,
  LayoutDashboard,
  ListChecks,
  LogOut,
  Mail,
  Shield,
  Tags,
  Ticket,
  TrendingUp,
  UserCircle,
  UsersRound,
  Users,
  Zap,
  Armchair,
  Landmark,
  ClipboardCheck,
  FileClock,
} from 'lucide-react';
import { getAdminLandingPath, hasPermissionAccess } from '@/features/internal-staff/admin/permissionAccess';
import RoleAvatar from '@/components/common/RoleAvatar';

const sidebarStateStorageKey = 'lorafilm.admin.sidebar.sections.v1';

const readSidebarState = () => {
  if (typeof window === 'undefined') return {};

  try {
    const savedState = window.localStorage.getItem(sidebarStateStorageKey);
    const parsedState = savedState ? JSON.parse(savedState) : {};
    return parsedState && typeof parsedState === 'object' && !Array.isArray(parsedState)
      ? parsedState
      : {};
  } catch {
    return {};
  }
};

export default function AdminSidebar({
  activeTab,
  setActiveTab,
  user,
  onBackHome,
  handleLogout,
}) {
  const navigate = useNavigate();
  const permissions = user?.permissions || [];
  const normalizedRole = String(user?.role || '').replace(/^ROLE_/, '');
  const can = (...requiredPermissions) =>
    hasPermissionAccess(normalizedRole, permissions, ...requiredPermissions);
  const isFullAdmin = normalizedRole === 'ADMIN' || permissions.includes('PERM_ROOT_ACCESS');
  const canViewPayments = can('PERM_VIEW_FINANCE', 'PAYMENT_VIEW', 'PAYMENT_RECONCILE');
  const canReconcilePayments = can('PAYMENT_RECONCILE');
  const canViewAnalytics = can('PERM_VIEW_FINANCE', 'ANALYTICS_VIEW');
  // PAYMENT_VIEW is also required by box-office staff, so it cannot identify
  // an accounting workspace on its own.
  const hasAccountingAccess = can('PERM_VIEW_FINANCE', 'PAYMENT_RECONCILE', 'ANALYTICS_VIEW');
  const canViewSettlements = can('PAYMENT_RECONCILE', 'SETTLEMENT_IMPORT', 'SETTLEMENT_LOCK');
  const canViewCashControl = can('CASH_CLOSE_VERIFY', 'PAYMENT_VIEW');
  const canViewAccountingPeriods = can(
    'ACCOUNTING_PERIOD_VIEW', 'ACCOUNTING_PERIOD_CREATE',
    'ACCOUNTING_PERIOD_RECONCILE', 'ACCOUNTING_PERIOD_CLOSE',
  );
  const canViewAccountingAudit = can('AUDIT_VIEW');
  const isAccountantOnly = hasAccountingAccess && !isFullAdmin
    && ['PAYMENT_RECONCILE', 'ANALYTICS_VIEW', 'PERM_VIEW_FINANCE']
      .some(permission => permissions.includes(permission));
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
    ? 'Kế toán'
    : (isFullAdmin ? 'Quản trị viên' : 'Nhân viên');
  const adminHomePath = getAdminLandingPath(normalizedRole, permissions);

  const sections = [
    {
      key: 'content',
      label: 'Nội dung phim',
      visible: isFullAdmin,
      items: [
        { key: 'movies', label: 'Danh sách phim', path: '/admin/movies', icon: Film },
        { key: 'genres', label: 'Thể loại', path: '/admin/genres', icon: Tags },
      ],
    },
    {
      key: 'cinema',
      label: 'Rạp & lịch chiếu',
      visible: isFullAdmin,
      items: [
        { key: 'clusters', label: 'Cụm rạp & giờ hoạt động', path: '/admin/cinemas', icon: Building },
        { key: 'rooms', label: 'Phòng chiếu', path: '/admin/rooms', icon: DoorOpen },
        { key: 'seat-types', label: 'Loại ghế', path: '/admin/seat-types', icon: Armchair },
        { key: 'showtimes', label: 'Lịch chiếu', path: '/admin/showtimes', icon: Calendar },
        { key: 'auto-schedule-create', label: 'Tạo lịch tuần', path: '/admin/showtime-schedules/create', icon: Zap },
        { key: 'auto-schedule-history', label: 'Lịch tạo tự động', path: '/admin/showtime-schedules', icon: History },
        { key: 'pricing', label: 'Bảng giá', path: '/admin/pricing', icon: CalendarRange },
      ],
    },
    {
      key: 'sales',
      label: 'Bán vé & dịch vụ',
      visible: isFullAdmin,
      items: [
        { key: 'bookings', label: 'Đơn đặt vé & giữ ghế', path: '/admin/bookings', icon: Ticket },
        { key: 'concessions', label: 'Danh mục bắp nước', path: '/admin/concessions', icon: Coffee },
      ],
    },
    {
      key: 'marketing',
      label: 'Khuyến mãi',
      visible: isFullAdmin,
      items: [
        { key: 'promotions', label: 'Trung tâm khuyến mãi', path: '/admin/promotions', icon: BadgePercent },
      ],
    },
    {
      key: 'finance',
      label: 'Thanh toán & báo cáo',
      visible: isFullAdmin || hasAccountingAccess,
      items: [
        ...(hasAccountingAccess
          ? [{ key: 'accounting', label: 'Bàn làm việc kế toán', path: '/admin/accounting', icon: Calculator }]
          : []),
        ...(canViewPayments
          ? [{ key: 'payments', label: canReconcilePayments ? 'Giao dịch & đối soát' : 'Giao dịch thanh toán', path: '/admin/payments', icon: CreditCard }]
          : []),
        ...(canViewSettlements
          ? [{ key: 'settlements', label: 'Đối soát ngân hàng', path: '/admin/settlements', icon: Landmark }]
          : []),
        ...(canViewCashControl
          ? [{ key: 'cash-control', label: 'Chốt ca & tiền mặt', path: '/admin/cash-control', icon: ClipboardCheck }]
          : []),
        ...(canViewAccountingPeriods
          ? [{ key: 'accounting-periods', label: 'Kỳ kế toán', path: '/admin/accounting-periods', icon: CalendarClock }]
          : []),
        ...(canViewAccountingAudit
          ? [{ key: 'accounting-audit', label: 'Nhật ký kiểm soát', path: '/admin/accounting-audit', icon: FileClock }]
          : []),
        ...(canViewAnalytics
          ? [{ key: 'analytics', label: 'Báo cáo doanh thu', path: '/admin/analytics', icon: TrendingUp }]
          : []),
        ...(isFullAdmin || permissions.includes('PERM_VIEW_FINANCE')
          ? [{ key: 'concession-sales', label: 'Doanh thu bắp nước', path: '/admin/concession-sales', icon: Coins }]
          : []),
      ],
    },
    {
      key: 'customers',
      label: 'Khách hàng & tích điểm',
      visible: isFullAdmin || canManageCustomers,
      items: [
        ...(canManageCustomers
          ? [{ key: 'customers', label: 'Danh sách khách hàng', path: '/admin/members', icon: Users }]
          : []),
        ...(isFullAdmin
          ? [
              { key: 'scores-dashboard', label: 'Tổng quan tích điểm', path: '/admin/scores/dashboard', icon: Award },
              { key: 'scores-tiers', label: 'Hạng thành viên', path: '/admin/scores/tiers', icon: Award },
              { key: 'scores-viewer', label: 'Tra cứu điểm thưởng', path: '/admin/scores/viewer', icon: Gift },
              { key: 'scores-reconciliation', label: 'Đối soát điểm', path: '/admin/scores/reconciliation', icon: History },
            ]
          : []),
      ],
    },
    {
      key: 'people',
      label: 'Nhân sự & tiền lương',
      visible: hasHumanResourcesAccess,
      items: [
        ...(canManageEmployees ? [{ key: 'hr', label: 'Trung tâm nhân sự', path: '/admin/hr', icon: LayoutDashboard }] : []),
        ...(canManageEmployees ? [{ key: 'approvals', label: 'Việc chờ duyệt', path: '/admin/approvals', icon: Inbox }] : []),
        ...(canManageEmployees ? [{ key: 'staff', label: 'Hồ sơ nhân viên', path: '/admin/staff', icon: Shield }] : []),
        ...(canManageEmployees ? [{ key: 'workforce', label: 'Lịch ca & chấm công', path: '/admin/workforce', icon: CalendarClock }] : []),
        ...(canManagePayroll ? [{ key: 'payroll', label: 'Quy trình bảng lương', path: '/admin/payroll', icon: TrendingUp }] : []),
        ...(canManageDepartments || canManagePositions ? [{ key: 'organization', label: 'Sơ đồ tổ chức', path: '/admin/organization', icon: UsersRound }] : []),
      ],
    },
    {
      key: 'system',
      label: 'Hệ thống',
      visible: hasSystemAccess,
      items: [
        ...(hasSystemAccess ? [{ key: 'accounts', label: 'Tài khoản & phân quyền', path: '/admin/accounts', icon: UserCircle }] : []),
        ...(canConfigureSystem || canViewUserAudits ? [{ key: 'audits', label: 'Nhật ký hoạt động', path: '/admin/audits', icon: FileSearch }] : []),
      ],
    },
    {
      key: 'notifications',
      label: 'Thông báo',
      visible: isFullAdmin,
      items: [
        { key: 'notification-dashboard', label: 'Tổng quan thông báo', path: '/admin/notifications', icon: BellRing },
        { key: 'notification-templates', label: 'Mẫu thông báo', path: '/admin/notification-templates', icon: Mail },
        { key: 'notification-operations', label: 'Vận hành gửi thông báo', path: '/admin/notification-operations', icon: ListChecks },
      ],
    },
  ].filter(section => section.visible && section.items.length > 0);

  const activeSectionKey = sections.find(section =>
    section.items.some(item => item.key === activeTab),
  )?.key;
  const [collapsedSections, setCollapsedSections] = useState(readSidebarState);

  useEffect(() => {
    try {
      window.localStorage.setItem(
        sidebarStateStorageKey,
        JSON.stringify(collapsedSections),
      );
    } catch {
      // Sidebar state persistence is optional when storage is unavailable.
    }
  }, [collapsedSections]);

  const isSectionCollapsed = sectionKey => (
    Object.prototype.hasOwnProperty.call(collapsedSections, sectionKey)
      ? collapsedSections[sectionKey]
      : activeSectionKey !== sectionKey
  );

  const toggleSection = sectionKey => {
    setCollapsedSections(previous => ({
      ...previous,
      [sectionKey]: !isSectionCollapsed(sectionKey),
    }));
  };

  const handleTabClick = (tabKey, path) => {
    setActiveTab?.(tabKey);
    navigate(path);
  };

  const getLinkClass = (tabKey, nested = false) => {
    const isActive = activeTab === tabKey;
    const base = nested
      ? 'pl-11 pr-4 py-2.5'
      : 'px-6 py-3';
    if (isActive) {
      return `w-full flex items-center justify-start text-left gap-3 ${base} text-sm font-semibold text-brand-orange bg-brand-orange/10 border-l-[3px] border-brand-orange transition-colors whitespace-nowrap`;
    }
    return `w-full flex items-center justify-start text-left gap-3 ${base} text-sm ${nested ? 'text-zinc-400' : 'font-medium text-zinc-400'} hover:text-zinc-100 hover:bg-zinc-800/40 border-l-[3px] border-transparent transition-colors whitespace-nowrap`;
  };

  return (
    <aside className="w-[280px] h-screen h-[100dvh] sticky top-0 bg-zinc-950 border-r border-zinc-800 flex flex-col shrink-0 z-30 select-none overflow-hidden font-sans">
      <div className="flex min-h-0 flex-1 flex-col">
        <div className="px-6 py-6 border-b border-zinc-800/60 flex items-center justify-between shrink-0 h-[72px]">
          <Link to={adminHomePath} className="flex items-center gap-2.5 bg-transparent p-0 m-0 shadow-none border-none select-none decoration-none group">
            <img src="/images/main-logo.png" alt="LoraFilm Icon" className="h-8 w-auto object-contain bg-transparent" />
            <span className="text-xl font-black tracking-tight text-white leading-none">
              Lora<span className="text-brand-orange ml-0.5">Film</span>
            </span>
          </Link>
          <span className="bg-brand-orange/10 border border-brand-orange/30 text-brand-orange font-mono text-[9px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider select-none">
            {roleLabel}
          </span>
        </div>

        <nav className="min-h-0 flex-1 py-4 space-y-1 overflow-y-auto scrollbar-thin scrollbar-thumb-zinc-800 scrollbar-track-transparent">
          {can('DASHBOARD_VIEW') && (
            <button onClick={() => handleTabClick('dashboard', '/admin')} className={getLinkClass('dashboard')}>
              <LayoutDashboard className="w-4 h-4 shrink-0" />
              <span>Tổng quan hệ thống</span>
            </button>
          )}

          {isFullAdmin && (
            <button onClick={() => handleTabClick('movie-operations', '/admin/movie-operations')} className={getLinkClass('movie-operations')}>
              <ListChecks className="w-4 h-4 shrink-0" />
              <span>Trung tâm vận hành phim</span>
            </button>
          )}

          <div className="mt-3 space-y-1">
            {sections.map(section => (
              <div key={section.key} className="space-y-1">
                <button
                  type="button"
                  onClick={() => toggleSection(section.key)}
                  aria-expanded={!isSectionCollapsed(section.key)}
                  className="w-full flex items-center justify-between px-6 py-2.5 text-xs font-bold uppercase tracking-wider text-zinc-500 hover:text-zinc-300 transition-colors select-none text-left whitespace-nowrap"
                >
                  <span>{section.label}</span>
                  <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${!isSectionCollapsed(section.key) ? 'rotate-180' : ''}`} />
                </button>
                {!isSectionCollapsed(section.key) && (
                  <div className="space-y-0.5">
                    {section.items.map(item => {
                      const Icon = item.icon;
                      return (
                        <button key={item.key} onClick={() => handleTabClick(item.key, item.path)} className={getLinkClass(item.key, true)}>
                          <Icon className="w-4 h-4 shrink-0" />
                          <span>{item.label}</span>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            ))}
          </div>
        </nav>
      </div>

      <div className="p-4 border-t border-zinc-800/60 bg-zinc-950 shrink-0">
        <div className="bg-zinc-900 border border-zinc-800 p-3.5 rounded-2xl flex flex-col gap-3 hover-scale">
          <div className="flex items-center justify-start gap-3">
            <RoleAvatar
              user={user}
              alt={`Ảnh đại diện ${user?.fullName || 'tài khoản quản trị'}`}
              fallbackAlt={`Ảnh mặc định theo vai trò của ${user?.fullName || roleLabel}`}
              className="h-10 w-10 border border-brand-orange/30"
            />
            <div className="truncate">
              <span className="text-[10px] text-zinc-500 font-bold block uppercase tracking-wider truncate">{roleLabel}</span>
              <span className="text-sm text-zinc-100 font-bold block truncate">{user?.fullName || 'Quản trị viên Lora'}</span>
            </div>
          </div>
          <div className="flex flex-col gap-1 border-t border-zinc-800 pt-3 mt-1">
            <button
              onClick={() => handleTabClick('my-account', '/admin/me')}
              className={`flex items-center justify-start gap-2.5 rounded px-2 py-2 text-xs font-semibold transition-colors ${activeTab === 'my-account' ? 'bg-brand-orange/10 text-brand-orange' : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'}`}
            >
              <UserCircle className="w-3.5 h-3.5" />
              <span>Tài khoản của tôi</span>
            </button>
            <div className="flex items-center justify-between pt-2 mt-1 border-t border-zinc-800/50">
              <button onClick={onBackHome} className="flex items-center justify-start gap-1.5 px-2 py-1.5 text-xs font-medium text-zinc-400 hover:text-brand-orange transition-colors rounded" title="Quay lại trang chủ">
                <Home className="w-4 h-4" />
                <span>Trang chủ</span>
              </button>
              <button onClick={handleLogout} className="flex items-center justify-start gap-1.5 px-2 py-1.5 text-xs font-bold text-red-500 hover:text-red-400 transition-colors rounded">
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
