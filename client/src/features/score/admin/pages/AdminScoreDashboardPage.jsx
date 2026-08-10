import { useEffect } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import { Award, Users, TrendingUp, RefreshCw, ShieldAlert, Coins, Gift, Clock } from 'lucide-react';

const formatCount = value => Number(value ?? 0).toLocaleString('vi-VN');

export default function AdminScoreDashboardPage() {
  const {
    dashboardStats,
    fetchDashboardStats,
    isLoadingOperations
  } = useAdminScore();

  useEffect(() => {
    fetchDashboardStats();
  }, [fetchDashboardStats]);

  const stats = dashboardStats || {
    totalMembers: 0,
    totalPointsEarned: 0,
    totalPointsRedeemed: 0,
    totalPointsHeld: 0,
    totalPointsExpired: 0,
    silverMembers: 0,
    goldMembers: 0,
    diamondMembers: 0,
    pendingReconciliationMismatches: 0,
    lastReconciliationBatch: 'N/A',
    lastReconciliationTime: null
  };

  return (
    <div className="space-y-8 p-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-zinc-900/40 p-6 rounded-[2rem] border border-zinc-800/50 backdrop-blur-xl shadow-2xl shadow-black/20">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
            <Award className="w-7 h-7 text-brand-orange shrink-0" />
            <span>Tổng Quan Loyalty Program</span>
          </h1>
          <p className="text-xs text-zinc-400 mt-1 tracking-wide font-medium">
            Bảng điều khiển quản trị viên: giám sát số dư điểm, hạng thành viên và trạng thái đối soát.
          </p>
        </div>
        <button
          onClick={() => fetchDashboardStats()}
          disabled={isLoadingOperations}
          className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-2xl bg-white/5 hover:bg-white/10 font-black text-[11px] text-white transition-all border border-white/10 shadow-inner disabled:opacity-50 shrink-0 cursor-pointer uppercase tracking-widest"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoadingOperations ? 'animate-spin text-brand-orange' : ''}`} />
          <span>Làm mới dữ liệu</span>
        </button>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-zinc-900/40 backdrop-blur-md p-6 rounded-[2rem] border border-zinc-800/50 relative overflow-hidden group hover:border-brand-orange/40 transition-all shadow-xl shadow-black/10">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Tổng Thành Viên</span>
            <div className="w-12 h-12 rounded-2xl bg-amber-500/10 flex items-center justify-center text-amber-400 border border-amber-500/20 shadow-inner">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-5 flex items-baseline gap-1.5">
            <span className="text-4xl font-black text-white tracking-tighter">
              {formatCount(stats.totalMembers)}
            </span>
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-widest pb-1">tài khoản</span>
          </div>
        </div>

        <div className="bg-zinc-900/40 backdrop-blur-md p-6 rounded-[2rem] border border-zinc-800/50 relative overflow-hidden group hover:border-emerald-500/40 transition-all shadow-xl shadow-black/10">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Tổng Điểm Tích Lũy</span>
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 flex items-center justify-center text-emerald-400 border border-emerald-500/20 shadow-inner">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-5 flex items-baseline gap-1.5">
            <span className="text-4xl font-black text-emerald-400 tracking-tighter">
              {formatCount(stats.totalPointsEarned)}
            </span>
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-widest pb-1">pts</span>
          </div>
        </div>

        <div className="bg-zinc-900/40 backdrop-blur-md p-6 rounded-[2rem] border border-zinc-800/50 relative overflow-hidden group hover:border-blue-500/40 transition-all shadow-xl shadow-black/10">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Điểm Đang Giữ</span>
            <div className="w-12 h-12 rounded-2xl bg-blue-500/10 flex items-center justify-center text-blue-400 border border-blue-500/20 shadow-inner">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-5 flex items-baseline gap-1.5">
            <span className="text-4xl font-black text-blue-400 tracking-tighter">
              {formatCount(stats.totalPointsHeld)}
            </span>
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-widest pb-1">pts</span>
          </div>
        </div>

        <div className="bg-zinc-900/40 backdrop-blur-md p-6 rounded-[2rem] border border-zinc-800/50 relative overflow-hidden group hover:border-red-500/40 transition-all shadow-xl shadow-black/10">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Lệch Đối Soát</span>
            <div className="w-12 h-12 rounded-2xl bg-red-500/10 flex items-center justify-center text-red-400 border border-red-500/20 shadow-inner">
              <ShieldAlert className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-5 flex items-end justify-between">
            <div className="flex items-baseline gap-1.5">
              <span className={`text-4xl font-black tracking-tighter ${stats.pendingReconciliationMismatches > 0 ? 'text-red-500' : 'text-zinc-300'}`}>
                {formatCount(stats.pendingReconciliationMismatches)}
              </span>
              <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-widest pb-1">tài khoản</span>
            </div>
            <span className="text-[9px] font-black tracking-widest text-zinc-500 bg-white/5 px-2 py-1 rounded-md border border-white/5 uppercase shadow-inner">
              {stats.lastReconciliationBatch}
            </span>
          </div>
        </div>
      </div>

      {/* Tiers distribution */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-gradient-to-br from-zinc-800/30 via-zinc-900/40 to-zinc-950/40 backdrop-blur-md p-8 rounded-[2rem] border border-zinc-700/30 flex items-center gap-5 shadow-xl shadow-black/10 transition-all hover:-translate-y-1">
          <div className="w-14 h-14 rounded-2xl bg-white/5 flex items-center justify-center text-zinc-300 border border-white/10 shadow-inner">
            <Award className="w-7 h-7" />
          </div>
          <div>
            <div className="text-[10px] font-black uppercase text-zinc-500 tracking-widest mb-1">Hạng Silver</div>
            <div className="text-3xl font-black text-white tracking-tighter">{formatCount(stats.silverMembers)}</div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-amber-500/10 via-zinc-900/40 to-zinc-950/40 backdrop-blur-md p-8 rounded-[2rem] border border-amber-500/20 flex items-center gap-5 shadow-xl shadow-amber-900/10 transition-all hover:-translate-y-1">
          <div className="w-14 h-14 rounded-2xl bg-amber-500/10 flex items-center justify-center text-amber-400 border border-amber-500/20 shadow-inner">
            <Gift className="w-7 h-7" />
          </div>
          <div>
            <div className="text-[10px] font-black uppercase text-amber-500/80 tracking-widest mb-1">Hạng Gold</div>
            <div className="text-3xl font-black text-amber-400 tracking-tighter">{formatCount(stats.goldMembers)}</div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-cyan-500/10 via-zinc-900/40 to-zinc-950/40 backdrop-blur-md p-8 rounded-[2rem] border border-cyan-500/20 flex items-center gap-5 shadow-xl shadow-cyan-900/10 transition-all hover:-translate-y-1">
          <div className="w-14 h-14 rounded-2xl bg-cyan-500/10 flex items-center justify-center text-cyan-400 border border-cyan-500/20 shadow-inner">
            <Coins className="w-7 h-7" />
          </div>
          <div>
            <div className="text-[10px] font-black uppercase text-cyan-500/80 tracking-widest mb-1">Hạng Diamond</div>
            <div className="text-3xl font-black text-cyan-400 tracking-tighter">{formatCount(stats.diamondMembers)}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
