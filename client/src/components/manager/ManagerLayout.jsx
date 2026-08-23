import { useCallback, useEffect, useMemo, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  Building2,
  BarChart3,
  CalendarDays,
  ChevronDown,
  CircleGauge,
  BadgePercent,
  CreditCard,
  ScanLine,
  LogOut,
  MapPin,
  ShieldCheck,
  Ticket,
  UsersRound,
  Wrench,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import managerCinemaService from '@/features/internal-staff/manager/services/managerCinemaService';
import RoleAvatar from '@/components/common/RoleAvatar';

const MANAGER_MENUS = [
  { path: '/manager', end: true, label: 'Việc cần xử lý hôm nay', icon: CircleGauge },
  { path: '/manager/showtimes', label: 'Điều phối suất chiếu', icon: CalendarDays },
  { path: '/manager/rooms', label: 'Phòng chiếu & bảo trì', icon: Wrench },
  { path: '/manager/bookings', label: 'Đơn đặt vé & giữ ghế', icon: Ticket },
  { path: '/manager/payments', label: 'Giao dịch tại rạp', icon: CreditCard },
  { path: '/manager/staff', label: 'Nhân sự & ca làm', icon: UsersRound },
  { path: '/manager/ticket-control', label: 'Soát vé & bàn giao', icon: ScanLine },
  { path: '/manager/reports', label: 'Báo cáo vận hành', icon: BarChart3 },
  { path: '/manager/cinema', label: 'Thông tin & giờ mở cửa', icon: Building2 },
];

export default function ManagerLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const managerMenus = user?.permissions?.includes('PROMOTION_VIEW')
    ? [...MANAGER_MENUS, { path: '/manager/promotions', label: 'Trung tâm khuyến mãi', icon: BadgePercent }]
    : MANAGER_MENUS;
  const [cinemas, setCinemas] = useState([]);
  const [selectedCinemaId, setSelectedCinemaId] = useState(
    () => window.localStorage.getItem('managerSelectedCinemaId') || '',
  );
  const [cinemaState, setCinemaState] = useState({ loading: true, error: '' });

  const loadCinemas = useCallback(async () => {
    setCinemaState({ loading: true, error: '' });
    try {
      const assignedCinemas = await managerCinemaService.getAssignedCinemas();
      setCinemas(assignedCinemas);
      setSelectedCinemaId(current => {
        const next = assignedCinemas.some(cinema => cinema.publicId === current)
          ? current
          : assignedCinemas[0]?.publicId || '';
        if (next) window.localStorage.setItem('managerSelectedCinemaId', next);
        else window.localStorage.removeItem('managerSelectedCinemaId');
        return next;
      });
      setCinemaState({ loading: false, error: '' });
    } catch (error) {
      setCinemaState({ loading: false, error: error?.message || 'Không thể tải rạp được phân công.' });
    }
  }, []);

  useEffect(() => {
    // Synchronize the signed-in manager scope once when entering the workspace.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadCinemas();
  }, [loadCinemas]);

  const selectedCinema = useMemo(
    () => cinemas.find(cinema => cinema.publicId === selectedCinemaId) || null,
    [cinemas, selectedCinemaId],
  );

  const changeCinema = publicId => {
    setSelectedCinemaId(publicId);
    window.localStorage.setItem('managerSelectedCinemaId', publicId);
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex min-h-screen bg-[#070709] text-white">
      <aside className="fixed inset-y-0 left-0 z-30 flex w-20 flex-col border-r border-white/10 bg-[#0b0b0e] lg:w-72">
        <div className="border-b border-white/10 px-4 py-5 lg:px-6">
          <button type="button" onClick={() => navigate('/manager')} className="flex w-full items-center justify-center gap-3 lg:justify-start">
            <img src="/images/main-logo.png" alt="LoraFilm" loading="eager" decoding="sync" className="h-10 w-10 shrink-0 rounded-xl object-contain" />
            <span className="hidden lg:block"><span className="block text-xl font-black">Lora<span className="text-brand-orange">Film</span></span><span className="block text-[10px] font-black uppercase tracking-[0.2em] text-zinc-600">Cổng quản lý rạp</span></span>
          </button>
        </div>

        <div className="hidden border-b border-white/10 p-4 lg:block">
          <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/[0.06] p-3">
            <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-wider text-emerald-300"><ShieldCheck size={14} /> Phạm vi an toàn</div>
            <p className="mt-2 text-xs leading-5 text-zinc-400">Chỉ hiển thị dữ liệu của rạp được Admin phân công.</p>
          </div>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-3" aria-label="Chức năng quản lý rạp">
          {managerMenus.map(menu => {
            const Icon = menu.icon;
            return (
              <NavLink key={menu.path} to={menu.path} end={menu.end} className={({ isActive }) => `flex min-h-12 items-center justify-center rounded-xl transition-colors lg:justify-start lg:px-4 ${isActive ? 'bg-brand-orange/10 font-bold text-brand-orange' : 'text-zinc-400 hover:bg-white/5 hover:text-white'}`}>
                <Icon className="h-5 w-5 shrink-0 lg:mr-3" />
                <span className="hidden text-sm lg:inline">{menu.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="space-y-2 border-t border-white/10 p-3 lg:p-4">
          <div className="hidden items-center gap-3 rounded-xl border border-white/10 bg-white/[0.025] p-3 lg:flex">
            <RoleAvatar
              user={user}
              className="h-9 w-9 border border-brand-orange/30"
            />
            <div className="min-w-0"><p className="text-[10px] font-black uppercase tracking-wide text-zinc-600">Quản lý rạp</p><p className="truncate text-xs font-bold text-white">{user?.fullName || user?.name || user?.email}</p></div>
          </div>
          <button type="button" onClick={handleLogout} className="flex min-h-11 w-full items-center justify-center rounded-xl text-red-400 hover:bg-red-500/10 lg:justify-start lg:px-4"><LogOut className="h-5 w-5 lg:mr-3" /><span className="hidden text-sm font-bold lg:inline">Đăng xuất</span></button>
        </div>
      </aside>

      <div className="min-w-0 flex-1 pl-20 lg:pl-72">
        <header className="sticky top-0 z-20 flex min-h-20 flex-col gap-3 border-b border-white/10 bg-[#09090b]/95 px-4 py-3 backdrop-blur md:flex-row md:items-center md:justify-between md:px-8">
          <div>
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Không gian làm việc</p>
            <p className="mt-1 text-sm font-bold text-zinc-300">Quản lý rạp được phân công</p>
          </div>
          {cinemas.length === 1 && !cinemaState.loading ? (
            <div className="flex min-h-11 w-full items-center gap-3 rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm font-bold text-white md:w-96">
              <MapPin className="h-4 w-4 shrink-0 text-brand-orange" />
              <span className="truncate">{cinemas[0].name}</span>
              <span className="ml-auto rounded-full bg-emerald-500/10 px-2 py-1 text-[9px] font-black uppercase text-emerald-300">Rạp được phân công</span>
            </div>
          ) : (
            <label className="relative block w-full md:w-96">
              <span className="sr-only">Chọn rạp đang vận hành</span>
              <MapPin className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-brand-orange" />
              <select
                value={selectedCinemaId}
                disabled={cinemaState.loading || !cinemas.length}
                onChange={event => changeCinema(event.target.value)}
                className="min-h-11 w-full appearance-none rounded-xl border border-white/10 bg-zinc-900 py-2 pl-10 pr-10 text-sm font-bold text-white outline-none focus:border-brand-orange/50 disabled:text-zinc-600"
              >
                <option value="">{cinemaState.loading ? 'Đang tải rạp được phân công…' : 'Chưa có rạp được phân công'}</option>
                {cinemas.map(cinema => <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>)}
              </select>
              <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
            </label>
          )}
        </header>

        <main className="p-4 md:p-8">
          <Outlet context={{
            cinemas,
            selectedCinema,
            selectedCinemaId,
            cinemaState,
            reloadCinemas: loadCinemas,
          }} />
        </main>
      </div>
    </div>
  );
}
