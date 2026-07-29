import { useState, useCallback, useMemo } from 'react';
import { 
  Menu,
  CheckCircle,
  AlertCircle,
  LogOut
} from 'lucide-react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { X } from 'lucide-react';

import { useAuth } from '@/contexts/AuthContext';
import AdminSidebar from './AdminSidebar';
import Breadcrumbs from '@/components/common/Breadcrumbs';

export default function AdminLayout({ onBackHome }) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  // Derive activeTab from pathname
  const activeTab = (() => {
    const path = location.pathname;
    if (path.endsWith('/me')) return 'my-account';
    if (path.endsWith('/movies') || path.includes('/movies/')) return 'movies';
    if (path.endsWith('/genres')) return 'genres';
    if (path.includes('/showtimes')) return 'showtimes';
    if (path.includes('/pricing')) return 'pricing';
    if (path === '/admin/showtime-schedules/create') return 'auto-schedule-create';
    if (path.includes('/showtime-schedules')) return 'auto-schedule-history';
    if (path.endsWith('/events') || path.includes('/events')) return 'events-promo';
    if (path.endsWith('/cinemas') || path.includes('/cinemas')) return 'clusters';
    if (path.endsWith('/rooms') || path.includes('/rooms')) return 'rooms';
    if (path.endsWith('/seat-types') || path.includes('/seat-types')) return 'seat-types';
    if (path.endsWith('/finance')) return 'finance';
    if (path.endsWith('/payments') || path.includes('/payments/')) return 'payments';
    if (path.endsWith('/bookings') || path.includes('/bookings/')) return 'bookings';
    if (path.endsWith('/concessions')) return 'concessions';
    if (path.endsWith('/concession-sales')) return 'concession-sales';
    if (path.endsWith('/members')) return 'customers';
    if (path.endsWith('/staff')) return 'staff';
    if (path.endsWith('/payroll')) return 'payroll';
    if (path.endsWith('/settings')) return 'settings';
    if (path.endsWith('/accounts')) return 'accounts';
    if (path.endsWith('/roles')) return 'roles';
    if (path.endsWith('/permissions')) return 'permissions';
    if (path.endsWith('/user-audits')) return 'user-audits';
    if (path.endsWith('/audits')) return 'audits';
    if (path.endsWith('/departments')) return 'departments';
    if (path.endsWith('/positions')) return 'positions';
    return 'dashboard';
  })();

  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    localStorage.removeItem('lora_session');
    navigate('/', { replace: true });
    if (onBackHome) onBackHome();
  };

  // Toast System
  const [toast, setToast] = useState({ message: '', type: 'success', visible: false });
  const triggerToast = useCallback((message, type = 'success') => {
    setToast({ message, type, visible: true });
    setTimeout(() => {
      setToast(prev => ({ ...prev, visible: false }));
    }, 3000);
  }, []);

  // Confirm Modal System
  const [confirmModal, setConfirmModal] = useState({ visible: false, message: '', resolve: null });
  const triggerConfirm = useCallback((message) => {
    return new Promise((resolve) => {
      setConfirmModal({ visible: true, message, resolve });
    });
  }, []);

  const handleConfirmAccept = () => {
    if (confirmModal.resolve) confirmModal.resolve(true);
    setConfirmModal({ visible: false, message: '', resolve: null });
  };

  const handleConfirmCancel = () => {
    if (confirmModal.resolve) confirmModal.resolve(false);
    setConfirmModal({ visible: false, message: '', resolve: null });
  };

  // Alert Modal System
  const [alertModal, setAlertModal] = useState({ visible: false, message: '' });
  const triggerAlert = useCallback((message) => {
    setAlertModal({ visible: true, message });
  }, []);

  const handleAlertClose = () => {
    setAlertModal({ visible: false, message: '' });
  };


  const outletContext = useMemo(() => ({ triggerToast, triggerConfirm, triggerAlert }), [triggerToast, triggerConfirm, triggerAlert]);

  return (
    <div className="w-full h-screen overflow-hidden bg-zinc-950 flex font-sans relative">
      
      {/* Toast Notification */}
      {toast.visible && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 bg-zinc-900 border border-zinc-800 rounded-2xl py-4 px-5 shadow-2xl animate-slide-in">
          {toast.type === 'success' ? (
            <CheckCircle className="w-5 h-5 text-emerald-500" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-500" />
          )}
          <span className="text-xs font-bold text-zinc-200">{toast.message}</span>
        </div>
      )}

      {/* Confirm Modal */}
      {confirmModal.visible && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 max-w-sm w-full shadow-2xl animate-fade-in-up">
            <h3 className="text-lg font-bold text-zinc-100 mb-2">Xác nhận</h3>
            <p className="text-sm text-zinc-400 mb-6">{confirmModal.message}</p>
            <div className="flex gap-3 justify-end">
              <button 
                onClick={handleConfirmCancel}
                className="px-4 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 rounded-xl text-xs font-bold uppercase transition-colors"
              >
                Hủy
              </button>
              <button 
                onClick={handleConfirmAccept}
                className="px-4 py-2 bg-brand-orange hover:bg-opacity-90 text-white rounded-xl text-xs font-bold uppercase shadow-lg shadow-brand-orange/20 transition-all"
              >
                Đồng ý
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Alert Modal */}
      {alertModal.visible && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 max-w-sm w-full shadow-2xl animate-fade-in-up">
            <div className="flex justify-between items-center mb-2">
              <h3 className="text-lg font-bold text-amber-500 flex items-center gap-2">
                <AlertCircle className="w-5 h-5" /> Thông báo
              </h3>
              <button onClick={handleAlertClose} className="text-zinc-500 hover:text-zinc-300">
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-sm text-zinc-400 mb-6">{alertModal.message}</p>
            <div className="flex justify-end">
              <button 
                onClick={handleAlertClose}
                className="px-6 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 rounded-xl text-xs font-bold uppercase transition-colors"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Fixed Sidebar Column */}
      <div className={`shrink-0 h-full fixed lg:static z-30 transition-transform duration-300 ${
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      } lg:translate-x-0`}>
        <AdminSidebar 
          activeTab={activeTab} 
          setActiveTab={() => {
            setSidebarOpen(false); // Close on mobile navigation
          }} 
          user={user} 
          onBackHome={onBackHome} 
          handleLogout={handleLogout} 
        />
      </div>

      {/* Mobile Sidebar overlay backdrop */}
      {sidebarOpen && (
        <div 
          onClick={() => setSidebarOpen(false)}
          className="fixed inset-0 bg-black/60 z-20 lg:hidden"
        />
      )}

      {/* Right Column Workspace (Fluid layout) */}
      <div className="flex-1 h-full flex flex-col overflow-hidden">
        
        {/* Sticky top Navigation Bar */}
        <header className="w-full h-[72px] bg-zinc-950/80 backdrop-blur-xl border-b border-zinc-800 flex items-center justify-between px-4 sm:px-8 shrink-0 z-20">
          <div className="flex items-center gap-4">
            <button 
              onClick={() => setSidebarOpen(true)}
              aria-label="Mở menu quản trị"
              aria-expanded={sidebarOpen}
              className="lg:hidden p-2 -ml-2 text-zinc-400 hover:text-white transition-colors"
            >
              <Menu className="w-5 h-5" />
            </button>
            <Breadcrumbs />
          </div>
          <div className="flex items-center gap-4">
            <div className="text-[10px] text-brand-orange font-bold uppercase tracking-widest bg-brand-orange/10 border border-brand-orange/20 px-3 py-1.5 rounded-full hidden md:block">
              Hệ thống an ninh LoraFilm
            </div>
            <div className="h-6 w-px bg-zinc-800 hidden md:block"></div>
            <button className="text-zinc-400 hover:text-white transition-colors p-2" title="Notifications">
              <div className="relative">
                <AlertCircle className="w-5 h-5" />
                <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-brand-orange rounded-full border-2 border-zinc-950"></span>
              </div>
            </button>
            <div className="h-6 w-px bg-zinc-800 hidden md:block"></div>
            <button onClick={handleLogout} className="text-zinc-400 hover:text-red-500 transition-colors p-2 flex items-center gap-2" title="Đăng xuất">
              <LogOut className="w-5 h-5" />
              <span className="hidden md:block text-sm font-bold">Đăng xuất</span>
            </button>
          </div>
        </header>

        {/* Dynamic View Body Content */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6">
          <Outlet context={outletContext} />
        </main>
      </div>

    </div>
  );
}
