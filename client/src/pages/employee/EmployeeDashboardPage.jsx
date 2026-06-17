// TODO: Connect to Gateway API: GET /api/v1/employee/dashboard
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { 
  Ticket, 
  Coffee, 
  CheckSquare, 
  Calendar, 
  LogOut, 
  Home,
  AlertCircle
} from 'lucide-react';

export default function EmployeeDashboardView() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('ticketing');

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handleBackHome = () => {
    navigate('/');
  };

  return (
    <div className="bg-zinc-950 text-zinc-100 min-h-screen flex flex-col lg:flex-row relative">
      {/* Sidebar Operational Staff Panel */}
      <aside className="w-full lg:w-64 bg-zinc-900 border-r border-zinc-800 flex flex-col justify-between shrink-0">
        <div>
          {/* Box Office branding */}
          <div className="p-6 border-b border-zinc-800">
            <span className="text-brand-coral font-black tracking-widest text-lg uppercase block mb-1">
              Lora Film
            </span>
            <div className="flex items-center gap-1.5">
              <span className="text-[10px] uppercase font-black px-2 py-0.5 rounded bg-brand-coral text-white">
                NHÂN VIÊN
              </span>
              <span className="text-[9px] text-zinc-500 uppercase tracking-widest font-bold">
                Quầy Vé & Dịch Vụ
              </span>
            </div>
          </div>

          {/* Employee Navigation Links */}
          <nav className="p-4 space-y-1">
            <button
              onClick={() => setActiveTab('ticketing')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold uppercase transition-all duration-200 ${
                activeTab === 'ticketing'
                  ? 'bg-brand-coral/10 text-brand-coral border-l-4 border-brand-coral'
                  : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
              }`}
            >
              <Ticket className="w-4 h-4" />
              <span>Đặt Vé Tại Quầy</span>
            </button>

            <button
              onClick={() => setActiveTab('concessions')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold uppercase transition-all duration-200 ${
                activeTab === 'concessions'
                  ? 'bg-brand-coral/10 text-brand-coral border-l-4 border-brand-coral'
                  : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
              }`}
            >
              <Coffee className="w-4 h-4" />
              <span>Bán Kèm Bắp Nước</span>
            </button>

            <button
              onClick={() => setActiveTab('validation')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold uppercase transition-all duration-200 ${
                activeTab === 'validation'
                  ? 'bg-brand-coral/10 text-brand-coral border-l-4 border-brand-coral'
                  : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
              }`}
            >
              <CheckSquare className="w-4 h-4" />
              <span>Kiểm Tra Vé</span>
            </button>

            <button
              onClick={() => setActiveTab('schedules')}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold uppercase transition-all duration-200 ${
                activeTab === 'schedules'
                  ? 'bg-brand-coral/10 text-brand-coral border-l-4 border-brand-coral'
                  : 'text-zinc-400 hover:bg-zinc-800/50 hover:text-white'
              }`}
            >
              <Calendar className="w-4 h-4" />
              <span>Xem Lịch Chiếu</span>
            </button>
          </nav>
        </div>

        {/* User profile controls footer */}
        <div className="p-4 border-t border-zinc-800 space-y-2 mt-auto">
          <div className="px-4 py-2">
            <p className="text-xs text-zinc-500 font-bold uppercase">Nhân viên</p>
            <p className="text-sm font-bold text-white truncate">{user?.fullName || 'Frontline Staff'}</p>
          </div>

          <button
            onClick={handleBackHome}
            className="w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-xs font-bold text-zinc-400 hover:bg-zinc-800 hover:text-white transition-all cursor-pointer"
          >
            <Home className="w-4 h-4" />
            <span>Về Trang Chủ</span>
          </button>

          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-2.5 rounded-lg text-xs font-bold text-red-400 hover:bg-red-950/20 hover:text-red-300 transition-all cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>

      {/* Content Space */}
      <main className="flex-grow p-6 md:p-10 space-y-8 overflow-y-auto lg:max-h-screen flex flex-col">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800/80 pb-6 shrink-0">
          <div>
            <h1 className="text-2xl md:text-3xl font-black text-white tracking-wide uppercase">
              {activeTab === 'ticketing' && 'ĐẶT VÉ TẠI QUẦY'}
              {activeTab === 'concessions' && 'DỊCH VỤ BẮP NƯỚC'}
              {activeTab === 'validation' && 'QUÉT & KIỂM TRA VÉ'}
              {activeTab === 'schedules' && 'LỊCH CHIẾU HÔM NAY'}
            </h1>
            <p className="text-zinc-500 text-xs uppercase tracking-wider mt-1">
              Giao diện tác vụ trực tiếp hỗ trợ khách hàng tại quầy vé
            </p>
          </div>
          <div className="text-right text-xs text-zinc-400">
            Quầy hỗ trợ: <span className="text-emerald-500 font-black">BOX OFFICE 1</span>
          </div>
        </div>

        {/* Tab content body empty state */}
        <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 min-h-[400px] flex-grow flex items-center justify-center">
          <div className="flex flex-col items-center justify-center p-12 text-center max-w-xl mx-auto my-12 space-y-4 shadow-2xl">
            <div className="w-12 h-12 rounded-full bg-orange-500/10 flex items-center justify-center text-orange-500">
              <AlertCircle className="w-6 h-6 animate-pulse" />
            </div>
            <h2 className="text-lg font-bold text-zinc-100">Hệ thống đang được cập nhật</h2>
            <p className="text-xs text-zinc-400">
              No real data available yet. This module is waiting for backend API integration.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
