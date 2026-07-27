import { useEffect } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import { Award, Users, TrendingUp, RefreshCw, ShieldAlert, Coins, Gift, Clock } from 'lucide-react';

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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-zinc-900/60 p-6 rounded-2xl border border-zinc-800/80 backdrop-blur-md">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
            <Award className="w-7 h-7 text-amber-500 shrink-0" />
            <span>Tổng Quan Loyalty Program</span>
          </h1>
          <p className="text-sm text-zinc-400 mt-1">
            Bảng điều khiển quản trị viên: giám sát số dư điểm, hạng thành viên và trạng thái đối soát.
          </p>
        </div>
        <button
          onClick={() => fetchDashboardStats()}
          disabled={isLoadingOperations}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-zinc-800 hover:bg-zinc-700 font-bold text-xs text-white transition-all border border-zinc-700/60 shadow-sm disabled:opacity-50 shrink-0 cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoadingOperations ? 'animate-spin text-amber-400' : ''}`} />
          <span>Làm mới dữ liệu</span>
        </button>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-zinc-900/80 p-5 rounded-2xl border border-zinc-800/80 relative overflow-hidden group hover:border-amber-500/30 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Tổng Thành Viên</span>
            <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center text-amber-400 border border-amber-500/20">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <span className="text-3xl font-black text-white font-mono tracking-tight">
              {stats.totalMembers.toLocaleString('vi-VN')}
            </span>
            <span className="text-xs text-zinc-500 ml-1.5 font-medium">tài khoản</span>
          </div>
        </div>

        <div className="bg-zinc-900/80 p-5 rounded-2xl border border-zinc-800/80 relative overflow-hidden group hover:border-emerald-500/30 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Tổng Điểm Tích Lũy</span>
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center text-emerald-400 border border-emerald-500/20">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <span className="text-3xl font-black text-emerald-400 font-mono tracking-tight">
              {stats.totalPointsEarned.toLocaleString('vi-VN')}
            </span>
            <span className="text-xs text-zinc-500 ml-1.5 font-medium">điểm</span>
          </div>
        </div>

        <div className="bg-zinc-900/80 p-5 rounded-2xl border border-zinc-800/80 relative overflow-hidden group hover:border-blue-500/30 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Điểm Đang Giữ (Held)</span>
            <div className="w-10 h-10 rounded-xl bg-blue-500/10 flex items-center justify-center text-blue-400 border border-blue-500/20">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <span className="text-3xl font-black text-blue-400 font-mono tracking-tight">
              {stats.totalPointsHeld.toLocaleString('vi-VN')}
            </span>
            <span className="text-xs text-zinc-500 ml-1.5 font-medium">điểm</span>
          </div>
        </div>

        <div className="bg-zinc-900/80 p-5 rounded-2xl border border-zinc-800/80 relative overflow-hidden group hover:border-rose-500/30 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Lệch Đối Soát</span>
            <div className="w-10 h-10 rounded-xl bg-rose-500/10 flex items-center justify-center text-rose-400 border border-rose-500/20">
              <ShieldAlert className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4 flex items-baseline justify-between">
            <div>
              <span className={`text-3xl font-black font-mono tracking-tight ${stats.pendingReconciliationMismatches > 0 ? 'text-rose-500' : 'text-zinc-300'}`}>
                {stats.pendingReconciliationMismatches.toLocaleString('vi-VN')}
              </span>
              <span className="text-xs text-zinc-500 ml-1.5 font-medium">tài khoản lệch</span>
            </div>
            <span className="text-[10px] font-mono text-zinc-400 bg-zinc-800/80 px-2 py-1 rounded">
              {stats.lastReconciliationBatch}
            </span>
          </div>
        </div>
      </div>

      {/* Tiers distribution */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="bg-gradient-to-br from-zinc-900 via-zinc-900 to-zinc-800/80 p-6 rounded-2xl border border-zinc-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-zinc-700/30 flex items-center justify-center text-zinc-300 border border-zinc-600/40">
            <Award className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-bold uppercase text-zinc-400 tracking-wider">Hạng Silver</div>
            <div className="text-2xl font-black text-white font-mono mt-1">{stats.silverMembers.toLocaleString('vi-VN')}</div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-zinc-900 via-zinc-900 to-amber-950/20 p-6 rounded-2xl border border-amber-500/20 flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-500/10 flex items-center justify-center text-amber-400 border border-amber-500/30">
            <Gift className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-bold uppercase text-amber-400/80 tracking-wider">Hạng Gold</div>
            <div className="text-2xl font-black text-amber-300 font-mono mt-1">{stats.goldMembers.toLocaleString('vi-VN')}</div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-zinc-900 via-zinc-900 to-cyan-950/20 p-6 rounded-2xl border border-cyan-500/20 flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-cyan-500/10 flex items-center justify-center text-cyan-400 border border-cyan-500/30">
            <Coins className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-bold uppercase text-cyan-400/80 tracking-wider">Hạng Diamond</div>
            <div className="text-2xl font-black text-cyan-300 font-mono mt-1">{stats.diamondMembers.toLocaleString('vi-VN')}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
