import { useAuth } from '@/contexts/AuthContext';
import { 
  Ticket, 
  CheckSquare, 
  Calendar, 
  LogOut, 
  Home,
  User,
  DollarSign
} from 'lucide-react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';

const EMPLOYEE_MENUS = [
  {
    id: 'pos',
    path: '/employee/pos',
    label: 'Đặt Vé Tại Quầy',
    icon: Ticket,
    permission: 'PERM_POS_ACCESS'
  },
  {
    id: 'checkin',
    path: '/employee/checkin',
    label: 'Kiểm Tra Vé',
    icon: CheckSquare,
    permission: 'PERM_CHECKIN'
  },
  {
    id: 'schedules',
    path: '/employee/schedules',
    label: 'Xem Lịch Chiếu',
    icon: Calendar,
    permission: 'PERM_VIEW_SCHEDULE'
  },
  {
    id: 'payroll',
    path: '/employee/payroll',
    label: 'Lương Của Tôi',
    icon: DollarSign,
    permission: 'PERM_VIEW_PAYROLL'
  }
];

export default function EmployeeLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const activeTab = (() => {
    const path = location.pathname;
    if (path.endsWith('/pos')) return 'pos';
    if (path.endsWith('/checkin')) return 'checkin';
    if (path.endsWith('/schedules')) return 'schedules';
    if (path.endsWith('/payroll')) return 'payroll';
    return 'pos';
  })();

  return (
    <div className="w-full h-screen overflow-hidden bg-zinc-950 flex select-none">
      {/* ➊ Stationary Employee Sidebar */}
      <aside className="w-72 h-full bg-zinc-900 border-r border-zinc-800 flex flex-col justify-between shrink-0 z-30">
        <div>
          {/* Corporate Identity Header */}
          <div className="p-6 border-b border-zinc-800">
            <span className="text-amber-500 font-black tracking-widest text-lg uppercase block mb-1">
              LORA FILM
            </span>
            <div className="flex items-center gap-1.5">
              <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded bg-amber-500 text-black">
                NHÂN VIÊN
              </span>
              <span className="text-[9px] text-zinc-500 uppercase tracking-widest font-bold">
                Quầy Vé & Dịch Vụ
              </span>
            </div>
          </div>

          {/* Navigation Links List */}
          <nav className="p-4 space-y-1">
            {EMPLOYEE_MENUS.map((menu) => {
              // ROOT_ACCESS implies all permissions, or check explicit permission
              const hasPerm = user?.permissions?.includes('PERM_ROOT_ACCESS') || 
                              user?.permissions?.includes(menu.permission) ||
                              // Fallback if permissions aren't properly configured yet
                              !user?.permissions || user.permissions.length === 0;
                              
              if (!hasPerm) return null;

              const Icon = menu.icon;
              const isActive = activeTab === menu.id;

              return (
                <button
                  key={menu.id}
                  onClick={() => navigate(menu.path)}
                  className={`w-full text-left justify-start items-center flex pl-9 py-2.5 text-sm rounded-xl transition-all duration-200 ${
                    isActive
                      ? 'text-amber-400 bg-amber-500/10 border-l-4 border-amber-500 font-semibold'
                      : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
                  }`}
                >
                  <Icon className="w-4 h-4 mr-3 shrink-0" />
                  <span>{menu.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* User Identity Info & Logout Footer */}
        <div className="p-4 border-t border-zinc-800 space-y-2 mt-auto">
          <div className="px-4 py-2 flex items-center gap-3 bg-zinc-950/40 rounded-xl border border-zinc-800/40">
            <div className="w-8 h-8 rounded-full bg-zinc-800 flex items-center justify-center text-amber-500 shrink-0">
              <User className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              {user?.role === 'ROLE_SUPERVISOR' ? (
                <span className="bg-amber-500/10 border border-amber-500/30 text-amber-400 font-mono text-[10px] font-bold px-2 py-0.5 rounded uppercase inline-block mb-1">
                  GIÁM SÁT CA
                </span>
              ) : (
                <p className="text-[10px] text-zinc-500 font-black uppercase">Quầy Vé</p>
              )}
              <p className="text-xs font-bold text-white truncate">{user?.fullName || user?.name || 'Staff'}</p>
            </div>
          </div>

          <button
            onClick={() => navigate('/')}
            className="w-full text-left justify-start items-center flex pl-9 py-2.5 text-xs text-zinc-400 hover:bg-zinc-800 hover:text-white transition-all rounded-xl"
          >
            <Home className="w-4 h-4 mr-3" />
            <span>Về Trang Chủ</span>
          </button>

          <button
            onClick={handleLogout}
            className="w-full text-left justify-start items-center flex pl-9 py-2.5 text-xs text-red-400 hover:bg-red-950/20 hover:text-red-300 transition-all rounded-xl"
          >
            <LogOut className="w-4 h-4 mr-3" />
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>

      {/* ➋ Scrollable Workspace Panels */}
      <main className="flex-grow h-full overflow-y-auto bg-zinc-950 p-6 md:p-10 flex flex-col space-y-6">
        <Outlet />
      </main>
    </div>
  );
}
