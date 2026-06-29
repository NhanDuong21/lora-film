import { useOutletContext } from 'react-router-dom';
import { 
  Ticket, 
  Users, 
  Activity, 
  Coins, 
  UserCheck, 
  Settings
} from 'lucide-react';

export default function AdminDashboardView() {
  useOutletContext();

  return (
    <div className="space-y-8 animate-fade-in">
      
      {/* SECTION 2: ROW OF 4 CORE KPI COUNTERS (SKELETONS) */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8 mt-2">
        
        {/* KPI 1: Tổng Doanh Thu */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="space-y-2">
            <span className="text-zinc-400 text-[10px] font-black uppercase tracking-wide block">
              TỔNG DOANH THU
            </span>
            <div className="h-8 w-32 bg-emerald-500/20 rounded-lg animate-pulse"></div>
          </div>
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
            <Coins className="w-5 h-5 text-emerald-400" />
          </div>
        </div>

        {/* KPI 2: Tổng Vé Đã Bán with Breakdown */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="space-y-2 w-full">
            <span className="text-zinc-400 text-[10px] font-black uppercase tracking-wide block">
              TỔNG VÉ ĐÃ BÁN
            </span>
            <div className="h-8 w-24 bg-zinc-800 rounded-lg animate-pulse"></div>
            <div className="flex items-center gap-2 pt-0.5 border-t border-zinc-800/60 mt-2 w-full">
              <div className="h-3 w-16 bg-zinc-800 rounded animate-pulse"></div>
              <span className="text-zinc-500 text-[10px]">|</span>
              <div className="h-3 w-16 bg-amber-500/20 rounded animate-pulse"></div>
            </div>
          </div>
          <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl self-start shrink-0 ml-2">
            <Ticket className="w-5 h-5 text-amber-500" />
          </div>
        </div>

        {/* KPI 3: Tổng Khách Hàng */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="space-y-2">
            <span className="text-zinc-400 text-[10px] font-black uppercase tracking-wide block">
              TỔNG KHÁCH HÀNG
            </span>
            <div className="h-8 w-28 bg-indigo-500/20 rounded-lg animate-pulse"></div>
          </div>
          <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl">
            <Users className="w-5 h-5 text-indigo-400" />
          </div>
        </div>

        {/* KPI 4: Tỷ Lệ Lấp Đầy */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="space-y-2">
            <span className="text-zinc-400 text-[10px] font-black uppercase tracking-wide block">
              TỶ LỆ LẤP ĐẦY PHÒNG
            </span>
            <div className="h-8 w-20 bg-indigo-500/20 rounded-lg animate-pulse"></div>
          </div>
          <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl">
            <Activity className="w-5 h-5 text-indigo-400" />
          </div>
        </div>

      </div>

      {/* SECTION 3: REVENUE & SALES CHART GRID (SKELETONS) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Left Panel: Doanh Thu 7 Ngày Qua */}
        <div className="bg-zinc-900/40 border border-zinc-800 rounded-2xl p-6 min-h-[320px] flex flex-col justify-between shadow-xl shadow-black/30 hover:border-zinc-750 transition-colors">
          <div className="flex items-center justify-between border-b border-zinc-800/60 pb-3 mb-2">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">
              BIỂU ĐỒ DOANH THU 7 NGÀY QUA
            </span>
            <span className="text-[10px] text-emerald-400 font-mono font-bold uppercase">
              Đang tải...
            </span>
          </div>
          
          <div className="flex-1 w-full relative min-h-[180px] mt-4 flex items-center justify-center border border-dashed border-zinc-800 rounded-xl">
             <div className="flex flex-col items-center gap-3 opacity-50">
                <Activity className="w-8 h-8 text-emerald-500 animate-pulse" />
                <span className="text-xs font-mono text-zinc-500">Waiting for backend data</span>
             </div>
          </div>
        </div>

        {/* Right Panel: Vé Bán Ra 7 Ngày Qua */}
        <div className="bg-zinc-900/40 border border-zinc-800 rounded-2xl p-6 min-h-[320px] flex flex-col justify-between shadow-xl shadow-black/30 hover:border-zinc-750 transition-colors">
          <div className="flex items-center justify-between border-b border-zinc-800/60 pb-3 mb-2">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">
              SẢN LƯỢNG VÉ BÁN 7 NGÀY QUA
            </span>
            <span className="text-[10px] text-amber-500 font-mono font-bold uppercase">
              Đang tải...
            </span>
          </div>

          <div className="flex-1 w-full mt-4 flex items-end justify-between gap-3 min-h-[180px] px-2">
            {[40, 70, 45, 90, 60, 100, 80].map((h, idx) => (
              <div key={idx} className="flex-1 flex flex-col items-center gap-2">
                <div className="w-full relative flex items-end justify-center h-40">
                  <div 
                    className="w-full bg-zinc-800/50 rounded-t-lg animate-pulse"
                    style={{ height: `${h}%` }}
                  />
                </div>
                <div className="h-2 w-8 bg-zinc-800/80 rounded animate-pulse"></div>
              </div>
            ))}
          </div>
        </div>

      </div>

      {/* SECTION 4: TOP MOVIES & RECENT ACTIVITIES GRID (SKELETONS) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Left Column: Top Phim Bán Chạy Nhất */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-6 shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="flex items-center justify-between border-b border-zinc-800/60 pb-3 mb-4">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">
              TOP PHIM BÁN CHẠY NHẤT
            </span>
            <span className="text-[10px] text-zinc-500 font-mono">
              Doanh thu tuần này
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-zinc-400">
              <thead className="text-[10px] text-zinc-500 font-black uppercase tracking-wider border-b border-zinc-800/60">
                <tr>
                  <th className="py-2.5 pb-3">Hạng</th>
                  <th className="py-2.5 pb-3">Tên Phim</th>
                  <th className="py-2.5 pb-3 text-center">Số Vé</th>
                  <th className="py-2.5 pb-3 text-right">Doanh Thu</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/40">
                {[1, 2, 3, 4].map((item) => (
                  <tr key={item}>
                    <td className="py-3.5">
                      <div className="w-5 h-5 rounded bg-zinc-800/80 animate-pulse"></div>
                    </td>
                    <td className="py-3.5">
                      <div className="h-4 w-32 bg-zinc-800/80 rounded animate-pulse"></div>
                    </td>
                    <td className="py-3.5 flex justify-center">
                      <div className="h-4 w-12 bg-zinc-800/60 rounded animate-pulse"></div>
                    </td>
                    <td className="py-3.5">
                      <div className="h-4 w-20 bg-emerald-500/10 rounded animate-pulse ml-auto"></div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right Column: Hoạt Động Gần Đây */}
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-6 shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div className="flex items-center justify-between border-b border-zinc-800/60 pb-3 mb-4">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">
              HOẠT ĐỘNG GẦN ĐÂY
            </span>
            <span className="text-[10px] text-zinc-500 font-mono">
              Đang tải...
            </span>
          </div>

          <div className="space-y-4">
            {[Ticket, UserCheck, Settings, Activity].map((Icon, idx) => (
              <div key={idx} className="flex items-start gap-3 border-b border-zinc-800/30 pb-3">
                <div className="p-1.5 rounded-lg bg-zinc-800 shrink-0 mt-0.5 animate-pulse">
                  <Icon className="w-3.5 h-3.5 text-zinc-600" />
                </div>
                <div className="flex-1 space-y-2">
                  <div className="h-3 w-full bg-zinc-800/80 rounded animate-pulse"></div>
                  <div className="h-3 w-2/3 bg-zinc-800/80 rounded animate-pulse"></div>
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>

    </div>
  );
}
