import { useState } from 'react';
import { DollarSign, Ticket, Eye, Users, ArrowUpRight, Activity } from 'lucide-react';

const KPI_CARDS = [
  { title: 'Doanh thu', value: '1.24B VNĐ', trend: '+14.5%', icon: DollarSign, color: 'text-emerald-500', bg: 'bg-emerald-500/10 border-emerald-500/20' },
  { title: 'Vé đã bán', value: '12,450', trend: '+8.2%', icon: Ticket, color: 'text-brand-orange', bg: 'bg-brand-orange/10 border-brand-orange/20' },
  { title: 'Lượt xem trang', value: '145.2K', trend: '+22.4%', icon: Eye, color: 'text-sky-500', bg: 'bg-sky-500/10 border-sky-500/20' },
  { title: 'Khách hàng mới', value: '890', trend: '+5.1%', icon: Users, color: 'text-purple-500', bg: 'bg-purple-500/10 border-purple-500/20' },
];

const RECENT_ACTIVITIES = [
  { id: 1, action: 'Khách hàng KH#1402 đã đặt 2 vé phim Mai', time: '5 phút trước' },
  { id: 2, action: 'Hệ thống đã đồng bộ 5 phim mới từ TMDB', time: '2 giờ trước' },
  { id: 3, action: 'Giao dịch hoàn tiền cho đơn #VN20412 thành công', time: '3 giờ trước' },
  { id: 4, action: 'Quản trị viên đã duyệt lịch chiếu tuần sau', time: 'Hôm qua' },
];

export default function AdminDashboardView() {
  const [timeFilter, setTimeFilter] = useState('today');

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-8">
      
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
                timeFilter === filter ? 'bg-brand-orange text-white font-semibold shadow-md' : 'text-zinc-400 hover:text-zinc-200'
              }`}
            >
              {filter === 'today' ? 'Hôm nay' : filter === '7days' ? '7 ngày qua' : filter === 'month' ? 'Tháng này' : 'Năm nay'}
            </button>
          ))}
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {KPI_CARDS.map((card, index) => {
          const Icon = card.icon;
          return (
            <div key={index} className="enterprise-card p-5 flex flex-col justify-between">
              <div className="flex justify-between items-start mb-4">
                <div className={`p-3 rounded-full border ${card.bg}`}>
                  <Icon className={`w-5 h-5 ${card.color}`} />
                </div>
                <div className="flex items-center gap-1 text-emerald-400 text-xs font-bold bg-emerald-500/10 px-2 py-1 rounded-md">
                  <ArrowUpRight className="w-3 h-3" />
                  {card.trend}
                </div>
              </div>
              <div>
                <h3 className="text-zinc-500 text-xs font-bold uppercase tracking-wider mb-1">{card.title}</h3>
                <p className="text-2xl font-black text-zinc-100">{card.value}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Main Content Area */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        
        {/* Placeholder for Chart */}
        <div className="xl:col-span-2 enterprise-card p-6 flex flex-col">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-sm font-black uppercase tracking-widest text-zinc-100">Biểu đồ doanh thu</h2>
          </div>
          <div className="flex-1 min-h-[300px] border border-zinc-800 border-dashed rounded-xl flex items-center justify-center text-zinc-500 text-sm">
            Khu vực hiển thị biểu đồ
          </div>
        </div>

        {/* Timeline / Activities */}
        <div className="enterprise-card p-6 flex flex-col">
          <div className="flex items-center justify-between mb-6 border-b border-zinc-800 pb-4">
            <h2 className="text-sm font-black uppercase tracking-widest text-zinc-100 flex items-center gap-2">
              <Activity className="w-4 h-4 text-brand-orange" />
              Hoạt động gần đây
            </h2>
          </div>
          <div className="flex-1 space-y-6">
            {RECENT_ACTIVITIES.map((activity, index) => (
              <div key={activity.id} className="relative flex gap-4">
                {/* Timeline Line */}
                {index !== RECENT_ACTIVITIES.length - 1 && (
                  <div className="absolute left-1.5 top-5 bottom-[-24px] w-px bg-zinc-800" />
                )}
                {/* Dot */}
                <div className="w-3 h-3 rounded-full bg-brand-orange mt-1 shrink-0 relative z-10 ring-4 ring-zinc-950" />
                {/* Content */}
                <div className="flex flex-col gap-1">
                  <p className="text-sm text-zinc-300 font-medium leading-snug">{activity.action}</p>
                  <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">{activity.time}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
        
      </div>
    </div>
  );
}
