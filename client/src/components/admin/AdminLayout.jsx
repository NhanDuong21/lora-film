import { useState } from 'react';
import { 
  Menu,
  CheckCircle,
  AlertCircle
} from 'lucide-react';
import { Outlet, useLocation } from 'react-router-dom';

import { useAuth } from '../../contexts/AuthContext';
import AdminSidebar from './AdminSidebar';

export default function AdminLayout({ onBackHome }) {
  const { user, logout } = useAuth();
  const location = useLocation();

  // Derive activeTab from pathname
  const activeTab = (() => {
    const path = location.pathname;
    if (path.endsWith('/movies')) return 'movies';
    if (path.endsWith('/genres')) return 'genres';
    if (path.endsWith('/actors')) return 'actors';
    if (path.endsWith('/showtimes')) return 'showtimes';
    if (path.endsWith('/events')) return 'events-promo';
    if (path.endsWith('/cinemas')) return 'clusters';
    if (path.endsWith('/tickets')) return 'tickets';
    if (path.endsWith('/concessions')) return 'concessions';
    if (path.endsWith('/concession-sales')) return 'concession-sales';
    if (path.endsWith('/customers')) return 'customers';
    if (path.endsWith('/staff')) return 'staff';
    if (path.endsWith('/payroll')) return 'payroll';
    if (path.endsWith('/settings')) return 'settings';
    return 'dashboard';
  })();

  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [timeFilter, setTimeFilter] = useState('today');

  const handleLogout = () => {
    logout();
    localStorage.removeItem('lora_session');
    sessionStorage.clear();
    window.location.hash = '#/';
    if (onBackHome) onBackHome();
  };

  // Toast System (can be passed down via context if needed, but currently passed via Outlet context or props?)
  // Actually, wait. React Router Outlet doesn't pass props directly like `<AdminGenrePage triggerToast={triggerToast} />`. 
  // It passes context via `<Outlet context={{ triggerToast }} />`.
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
                    {activeTab === 'genres' && 'DANH MỤC THỂ LOẠI'}
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

        {/* Dynamic View Body Content */}
        <main className="flex-1 overflow-y-auto p-6 space-y-6">
          <Outlet context={{ triggerToast, timeFilter }} />
        </main>
      </div>

    </div>
  );
}
