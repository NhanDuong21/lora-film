import { useAuth } from '@/contexts/AuthContext';
import {
  Banknote, CalendarDays, ClipboardList, Clock3, Home, LayoutDashboard,
  LogOut, RotateCcw, Ticket, User, WalletCards,
} from 'lucide-react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  EMPLOYEE_PERMISSIONS,
  hasEmployeeAccess,
} from '@/features/internal-staff/employee/employeeAccess';

const EMPLOYEE_MENU_GROUPS = [
  {
    id: 'operations',
    label: 'Vận hành tại quầy',
    items: [
      {
        id: 'dashboard', path: '/employee/dashboard', label: 'Tổng quan ca', icon: LayoutDashboard,
        permissions: [EMPLOYEE_PERMISSIONS.DASHBOARD_VIEW],
      },
      {
        id: 'box-office', path: '/employee/box-office', label: 'Bán vé tại quầy', icon: Ticket,
        permissions: [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE, EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT],
        requireAll: true,
      },
      {
        id: 'orders', path: '/employee/orders', label: 'Đơn tại quầy', icon: ClipboardList,
        permissions: [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE],
      },
      {
        id: 'cash-payment', path: '/employee/payments/cash', label: 'Thu tiền tại quầy', icon: Banknote,
        permissions: [EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT],
        hiddenWhenGranted: [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE],
      },
      {
        id: 'refund-request', path: '/employee/payments/refunds', label: 'Hỗ trợ & hoàn tiền', icon: RotateCcw,
        permissions: [EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT],
      },
      {
        id: 'cash-session', path: '/employee/cash-session', label: 'Chốt ca & bàn giao', icon: Banknote,
        permissions: [EMPLOYEE_PERMISSIONS.BOOKING_MANAGE, EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT],
        requireAll: true,
      },
    ],
  },
  {
    id: 'personal',
    label: 'Cá nhân',
    items: [
      {
        id: 'schedules', path: '/employee/schedules', label: 'Lịch làm & nghỉ phép', icon: CalendarDays,
        permissions: [EMPLOYEE_PERMISSIONS.SCHEDULE_VIEW],
      },
      {
        id: 'checkin', path: '/employee/checkin', label: 'Chấm công', icon: Clock3,
        permissions: [EMPLOYEE_PERMISSIONS.ATTENDANCE_VIEW, EMPLOYEE_PERMISSIONS.ATTENDANCE_UPDATE],
        requireAll: true,
      },
      {
        id: 'payroll', path: '/employee/payroll', label: 'Phiếu lương', icon: WalletCards,
        permissions: [EMPLOYEE_PERMISSIONS.PAYROLL_VIEW],
      },
    ],
  },
];

export default function EmployeeLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const permissions = user?.permissions || [];
  const visibleGroups = EMPLOYEE_MENU_GROUPS.map(group => ({
    ...group,
    items: group.items.filter(menu => (
      !menu.hiddenWhenGranted?.some(permission => permissions.includes(permission))
      && hasEmployeeAccess(
        user?.role,
        permissions,
        menu.permissions,
        menu.requireAll,
      )
    )),
  })).filter(group => group.items.length);
  const visibleMenus = visibleGroups.flatMap(group => group.items);
  const activeTab = visibleMenus.find(menu => location.pathname.startsWith(menu.path))?.id;

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex h-screen w-full overflow-hidden bg-zinc-950">
      <aside className="z-30 flex h-full w-20 shrink-0 flex-col justify-between border-r border-zinc-800 bg-zinc-900 md:w-72">
        <div>
          <div className="border-b border-zinc-800 p-4 text-center md:p-6 md:text-left">
            <span className="hidden text-lg font-black tracking-widest text-amber-500 md:block">LORAFILM</span>
            <span className="block text-lg font-black text-amber-500 md:hidden">LF</span>
            <p className="mt-1 hidden text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500 md:block">Cổng nhân viên</p>
          </div>
          <nav className="space-y-5 p-3">
            {visibleGroups.map(group => <section key={group.id}>
              <p className="mb-2 hidden px-5 text-[10px] font-black uppercase tracking-[0.18em] text-zinc-600 md:block">{group.label}</p>
              <div className="space-y-1">{group.items.map(menu => {
                const Icon = menu.icon;
                const active = activeTab === menu.id;
                return <button key={menu.id} type="button" onClick={() => navigate(menu.path)} className={`flex w-full items-center justify-center rounded-xl py-3 text-sm transition md:justify-start md:px-5 ${active ? 'bg-amber-500/10 font-bold text-amber-400' : 'text-zinc-400 hover:bg-zinc-800/70 hover:text-white'}`}>
                  <Icon className="h-4 w-4 shrink-0 md:mr-3" />
                  <span className="hidden md:inline">{menu.label}</span>
                </button>;
              })}</div>
            </section>)}
            {!visibleMenus.length && (
              <p className="px-3 py-4 text-center text-xs text-zinc-500 md:text-left">
                Chưa có chức năng được cấp quyền.
              </p>
            )}
          </nav>
        </div>
        <div className="mt-auto space-y-2 border-t border-zinc-800 p-4">
          <div className="hidden items-center gap-3 rounded-xl border border-zinc-800 bg-zinc-950/40 p-3 md:flex">
            <span className="grid h-8 w-8 place-items-center rounded-full bg-zinc-800 text-amber-500"><User size={16} /></span>
            <div className="min-w-0"><p className="text-[10px] font-black uppercase text-zinc-500">Nhân viên</p><p className="truncate text-xs font-bold text-white">{user?.fullName || user?.name || 'Employee'}</p></div>
          </div>
          <button type="button" onClick={() => navigate('/')} className="flex w-full items-center justify-center rounded-xl py-2.5 text-xs text-zinc-400 hover:bg-zinc-800 md:justify-start md:px-5"><Home className="h-4 w-4 md:mr-3" /><span className="hidden md:inline">Trang chủ</span></button>
          <button type="button" onClick={handleLogout} className="flex w-full items-center justify-center rounded-xl py-2.5 text-xs text-red-400 hover:bg-red-950/20 md:justify-start md:px-5"><LogOut className="h-4 w-4 md:mr-3" /><span className="hidden md:inline">Đăng xuất</span></button>
        </div>
      </aside>
      <main className="h-full min-w-0 flex-grow overflow-y-auto bg-zinc-950 p-4 md:p-9"><Outlet /></main>
    </div>
  );
}
