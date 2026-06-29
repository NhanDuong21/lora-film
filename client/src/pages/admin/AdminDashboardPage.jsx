import { useState } from 'react';
import SystemUpdating from '../../components/common/SystemUpdating';

export default function AdminDashboardView() {
  const [timeFilter, setTimeFilter] = useState('today');

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6">
      
      {/* Page Header with Time Filters */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 border-b border-zinc-800 pb-4">
        <div className="flex flex-col">
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">TỔNG QUAN HỆ THỐNG</h1>
          <p className="text-xs text-zinc-400 mt-1">Hệ thống báo cáo hiệu suất kinh doanh và vận hành rạp phim LoraFilm</p>
        </div>
        
        <div className="flex items-center bg-zinc-900 border border-zinc-800 rounded-xl p-1 gap-1 select-none">
          {['today', '7days', 'month', 'year'].map(filter => (
            <button
              key={filter}
              onClick={() => setTimeFilter(filter)}
              className={`rounded-lg px-3 py-1.5 text-xs transition-all ${
                timeFilter === filter ? 'bg-amber-500 text-black font-semibold shadow-md' : 'text-zinc-400 hover:text-zinc-200'
              }`}
            >
              {filter === 'today' ? 'Hôm nay' : filter === '7days' ? '7 ngày qua' : filter === 'month' ? 'Tháng này' : 'Năm nay'}
            </button>
          ))}
        </div>
      </div>

      <SystemUpdating />
    </div>
  );
}
