import { useState, useEffect } from 'react';
import { 
  Menu,
  CheckCircle,
  AlertCircle
} from 'lucide-react';

import { useAuth } from '../../contexts/AuthContext';
import { useData } from '../../contexts/DataContext';
import AdminSidebar from './AdminSidebar';
import AdminMovieView from '../../pages/admin/AdminMoviePage';
import AdminActorView from '../../pages/admin/AdminActorPage';
import AdminShowtimeView from '../../pages/admin/AdminShowtimePage';
import AdminEventView from '../../pages/admin/AdminEventPage';
import AdminCinemaView from '../../pages/admin/AdminCinemaPage';
import AdminFinanceView from '../../pages/admin/AdminFinancePage';
import AdminDashboardView from '../../pages/admin/AdminDashboardPage';
import AdminSettingsView from '../../pages/admin/AdminSettingsPage';
import AdminMembersView from '../../pages/admin/AdminMembersPage';
import AdminStaffView from '../../pages/admin/AdminStaffPage';
import AdminConcessionInventory from '../../pages/admin/AdminConcessionInventoryPage';

export default function AdminLayout({ initialTab = 'dashboard', onBackHome }) {
  const { user, logout } = useAuth();
  const { 
    movies, setMovies,
    actors, setActors,
    theaters, setTheaters,
    showtimes, setShowtimes,
    tickets,
    concessions, setConcessions,
    customers, setCustomers,
    employees, setEmployees,
    events, setEvents
  } = useData();

  const [activeTab, setActiveTab] = useState(() => {
    const hash = window.location.hash;
    const permissions = user?.permissions || [];
    const isAccountantOnly = permissions.includes('PERM_VIEW_FINANCE') && !permissions.includes('PERM_ROOT_ACCESS');
    const defaultTab = isAccountantOnly ? 'tickets' : 'dashboard';

    if (hash === '#/admin') return defaultTab;
    if (hash === '#/admin/movies') return 'movies';
    if (hash === '#/admin/actors') return 'actors';
    if (hash === '#/admin/showtimes') return 'showtimes';
    if (hash === '#/admin/events') return 'events-promo';
    if (hash === '#/admin/clusters') return 'clusters';
    if (hash === '#/admin/tickets') return 'tickets';
    if (hash === '#/admin/concessions') return 'concessions';
    if (hash === '#/admin/concession-sales') return 'concession-sales';
    if (hash === '#/admin/customers') return 'customers';
    if (hash === '#/admin/staff') return 'staff';
    if (hash === '#/admin/payroll') return 'payroll';
    if (hash === '#/admin/delays') return 'delays';
    if (hash === '#/admin/pricing') return 'pricing';
    if (hash === '#/admin/settings') return 'settings';
    return initialTab;
  });
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [timeFilter, setTimeFilter] = useState('today');

  // Sync hash routing
  useEffect(() => {
    const handleHashChange = () => {
      const hash = window.location.hash;
      const permissions = user?.permissions || [];
      const isAccountantOnly = permissions.includes('PERM_VIEW_FINANCE') && !permissions.includes('PERM_ROOT_ACCESS');
      const defaultTab = isAccountantOnly ? 'tickets' : 'dashboard';

      if (hash === '#/admin') setActiveTab(defaultTab);
      else if (hash === '#/admin/movies') setActiveTab('movies');
      else if (hash === '#/admin/actors') setActiveTab('actors');
      else if (hash === '#/admin/showtimes') setActiveTab('showtimes');
      else if (hash === '#/admin/events') setActiveTab('events-promo');
      else if (hash === '#/admin/clusters') setActiveTab('clusters');
      else if (hash === '#/admin/tickets') setActiveTab('tickets');
      else if (hash === '#/admin/concessions') setActiveTab('concessions');
      else if (hash === '#/admin/concession-sales') setActiveTab('concession-sales');
      else if (hash === '#/admin/customers') setActiveTab('customers');
      else if (hash === '#/admin/staff') setActiveTab('staff');
      else if (hash === '#/admin/payroll') setActiveTab('payroll');
      else if (hash === '#/admin/delays') setActiveTab('delays');
      else if (hash === '#/admin/pricing') setActiveTab('pricing');
      else if (hash === '#/admin/settings') setActiveTab('settings');
    };

    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [user]);

  const handleLogout = () => {
    logout();
    localStorage.removeItem('lora_session');
    sessionStorage.clear();
    window.location.hash = '#/';
    onBackHome();
  };

  // Toast System
  const [toast, setToast] = useState({ message: '', type: 'success', visible: false });
  const triggerToast = (message, type = 'success') => {
    setToast({ message, type, visible: true });
    setTimeout(() => {
      setToast(prev => ({ ...prev, visible: false }));
    }, 3000);
  };


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

      {/* Fixed Sidebar Column (Locked layout width w-64, stationary) */}
      <div className={`shrink-0 h-full fixed lg:static z-30 transition-transform duration-300 ${
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      } lg:translate-x-0`}>
        <AdminSidebar 
          activeTab={activeTab} 
          setActiveTab={(tab) => {
            setActiveTab(tab);
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
        <header className="w-full h-16 bg-zinc-900/50 backdrop-blur-md border-b border-zinc-800/60 flex items-center justify-between px-6 shrink-0 z-20">
          <div className="flex items-center gap-3">
            <button 
              onClick={() => setSidebarOpen(true)}
              className="lg:hidden p-2 text-zinc-400 hover:text-white"
            >
              <Menu className="w-5 h-5" />
            </button>
            <div className="flex flex-col md:flex-row md:items-center gap-1 md:gap-3">
              <h1 className="text-sm md:text-base font-bold uppercase tracking-wider text-zinc-50 select-none">
                {activeTab === 'dashboard' ? 'TỔNG QUAN HỆ THỐNG' : (
                  <>
                    {activeTab === 'movies' && 'DANH SÁCH BỘ PHIM'}
                    {activeTab === 'actors' && 'DANH MỤC DIỄN VIÊN'}
                    {activeTab === 'showtimes' && 'DANH SÁCH SUẤT CHIẾU'}
                    {activeTab === 'events-promo' && 'CHƯƠNG TRÌNH ƯU ĐÃI'}
                    {activeTab === 'clusters' && 'HỆ THỐNG CỤM RẠP'}
                    {activeTab === 'concessions' && 'DANH MỤC BẮP NƯỚC'}
                    {activeTab === 'concession-sales' && 'DOANH THU BẮP NƯỚC & COMBO'}
                    {activeTab === 'staff' && 'QUẢN LÝ NHÂN SỰ'}
                    {activeTab === 'customers' && 'DANH SÁCH HỘI VIÊN'}
                    {['tickets', 'payroll'].includes(activeTab) && 'LỊCH SỬ GIAO DỊCH'}
                    {['delays', 'pricing', 'settings'].includes(activeTab) && 'CẤU HÌNH & BẢO MẬT'}
                  </>
                )}
              </h1>
              {activeTab === 'dashboard' && (
                <p className="text-xs text-zinc-400 font-medium truncate hidden md:block max-w-xl border-l border-zinc-850 pl-3">
                  Hệ thống báo cáo hiệu suất kinh doanh và vận hành rạp phim LoraFilm
                </p>
              )}
            </div>
          </div>
          {activeTab === 'dashboard' ? (
            <div className="flex items-center bg-zinc-900 border border-zinc-800 rounded-xl p-1 gap-1 select-none">
              <button
                onClick={() => {
                  setTimeFilter('today');
                  triggerToast('Đã cập nhật dữ liệu báo cáo: Hôm nay');
                }}
                className={`${
                  timeFilter === 'today'
                    ? 'bg-amber-500 text-black font-semibold shadow-md'
                    : 'text-zinc-400 hover:text-zinc-200'
                } rounded-lg px-3 py-1.5 text-xs transition-all`}
              >
                Hôm nay
              </button>
              <button
                onClick={() => {
                  setTimeFilter('7days');
                  triggerToast('Đã cập nhật dữ liệu báo cáo: 7 ngày qua');
                }}
                className={`${
                  timeFilter === '7days'
                    ? 'bg-amber-500 text-black font-semibold shadow-md'
                    : 'text-zinc-400 hover:text-zinc-200'
                } rounded-lg px-3 py-1.5 text-xs transition-all`}
              >
                7 ngày qua
              </button>
              <button
                onClick={() => {
                  setTimeFilter('month');
                  triggerToast('Đã cập nhật dữ liệu báo cáo: Tháng này');
                }}
                className={`${
                  timeFilter === 'month'
                    ? 'bg-amber-500 text-black font-semibold shadow-md'
                    : 'text-zinc-400 hover:text-zinc-200'
                } rounded-lg px-3 py-1.5 text-xs transition-all`}
              >
                Tháng này
              </button>
              <button
                onClick={() => {
                  setTimeFilter('year');
                  triggerToast('Đã cập nhật dữ liệu báo cáo: Năm nay');
                }}
                className={`${
                  timeFilter === 'year'
                    ? 'bg-amber-500 text-black font-semibold shadow-md'
                    : 'text-zinc-400 hover:text-zinc-200'
                } rounded-lg px-3 py-1.5 text-xs transition-all`}
              >
                Năm nay
              </button>
            </div>
          ) : (
            <div className="text-[10px] text-zinc-400 font-bold uppercase tracking-widest bg-zinc-950 border border-zinc-800 px-3 py-1.5 rounded-full">
              HỆ THỐNG AN NINH LORAFILM
            </div>
          )}
        </header>

        {/* Dynamic View Body Content (ONLY scrollable container region) */}
        <main className="flex-1 overflow-y-auto p-6 space-y-6">
          
          {/* TAB: DASHBOARD OVERVIEW */}
          {activeTab === 'dashboard' && (
            <AdminDashboardView 
              timeFilter={timeFilter}
            />
          )}

          {/* TAB: MOVIES */}
          {activeTab === 'movies' && (
            <AdminMovieView 
              movies={movies} 
              updateMoviesState={setMovies} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: ACTORS */}
          {activeTab === 'actors' && (
            <AdminActorView 
              actors={actors} 
              updateActorsState={setActors} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: SHOWTIMES */}
          {activeTab === 'showtimes' && (
            <AdminShowtimeView 
              showtimes={showtimes} 
              movies={movies} 
              theaters={theaters} 
              updateShowtimesState={setShowtimes} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: EVENTS & PROMOS */}
          {activeTab === 'events-promo' && (
            <AdminEventView 
              events={events} 
              updateEventsState={setEvents} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: CLUSTERS & HALLS */}
          {activeTab === 'clusters' && (
            <AdminCinemaView 
              theaters={theaters} 
              updateTheatersState={setTheaters} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: MEMBERS */}
          {activeTab === 'customers' && (
            <AdminMembersView 
              customers={customers} 
              updateCustomersState={setCustomers} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: STAFF */}
          {activeTab === 'staff' && (
            <AdminStaffView 
              employees={employees} 
              updateEmployeesState={setEmployees} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: CONCESSION INVENTORY */}
          {activeTab === 'concessions' && (
            <AdminConcessionInventory 
              concessions={concessions} 
              updateConcessionsState={setConcessions} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: OPERATIONS / FINANCE VIEW MODULES */}
          {['tickets', 'concession-sales', 'payroll'].includes(activeTab) && (
            <AdminFinanceView 
              activeTab={activeTab === 'concession-sales' ? 'concessions' : activeTab} 
              tickets={tickets} 
              concessions={concessions} 
              customers={customers} 
              employees={employees} 
              updateCustomersState={setCustomers} 
              updateEmployeesState={setEmployees} 
              triggerToast={triggerToast} 
            />
          )}

          {/* TAB: SYSTEM SETTINGS */}
          {activeTab === 'settings' && (
            <AdminSettingsView 
              activeTab={activeTab}
              triggerToast={triggerToast} 
            />
          )}

        </main>
      </div>

    </div>
  );
}
