import React from 'react';
import { Award, ShieldCheck, Sparkles, TrendingUp, Lock } from 'lucide-react';

export default function MembershipCard({ scoreData, user }) {
  const currentTier = scoreData?.currentTier || {
    tierCode: 'SILVER',
    tierName: 'Silver Member',
    earningRate: 0.05
  };

  const currentPoints = scoreData?.currentPoints ?? 0;
  const heldPoints = scoreData?.heldPoints ?? 0;
  const accumulatedPoints = scoreData?.accumulatedPoints ?? 0;
  const earningRatePct = Math.round((currentTier.earningRate || 0.05) * 100);

  const getTierStyle = (code) => {
    switch (code?.toUpperCase()) {
      case 'DIAMOND':
        return {
          gradient: 'from-cyan-600 via-blue-700 to-indigo-900',
          border: 'border-cyan-500/30',
          badgeBg: 'bg-cyan-500/20 text-cyan-300 border-cyan-400/30',
          iconColor: 'text-cyan-400',
          shadow: 'shadow-cyan-500/10'
        };
      case 'GOLD':
        return {
          gradient: 'from-amber-600 via-yellow-700 to-amber-950',
          border: 'border-amber-500/30',
          badgeBg: 'bg-amber-500/20 text-amber-300 border-amber-400/30',
          iconColor: 'text-amber-400',
          shadow: 'shadow-amber-500/10'
        };
      case 'SILVER':
      default:
        return {
          gradient: 'from-zinc-700 via-zinc-800 to-zinc-950',
          border: 'border-zinc-700/50',
          badgeBg: 'bg-zinc-700/40 text-zinc-300 border-zinc-600',
          iconColor: 'text-zinc-400',
          shadow: 'shadow-zinc-500/10'
        };
    }
  };

  const style = getTierStyle(currentTier.tierCode);

  return (
    <div className={`relative overflow-hidden rounded-3xl bg-gradient-to-br ${style.gradient} p-8 text-white shadow-2xl border ${style.border} transition-all duration-300 hover:scale-[1.01]`}>
      {/* Decorative Glow */}
      <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/10 blur-3xl pointer-events-none" />
      <div className="absolute -bottom-16 -left-16 h-48 w-48 rounded-full bg-black/20 blur-2xl pointer-events-none" />

      {/* Card Header */}
      <div className="flex items-start justify-between relative z-10">
        <div className="flex items-center gap-3">
          <div className={`flex h-12 w-12 items-center justify-center rounded-2xl bg-black/30 backdrop-blur-md border ${style.border}`}>
            <Award className={`h-6 w-6 ${style.iconColor}`} />
          </div>
          <div>
            <span className={`inline-flex items-center gap-1 rounded-full border px-3 py-1 text-[10px] font-black uppercase tracking-wider ${style.badgeBg}`}>
              <Sparkles className="h-3 w-3" />
              {currentTier.tierName}
            </span>
            <p className="mt-1 text-xs text-zinc-300 font-medium">
              Tích lũy trọn đời: <span className="font-bold text-white">{accumulatedPoints.toLocaleString('vi-VN')}</span> điểm
            </p>
          </div>
        </div>
        <div className="text-right">
          <span className="text-[10px] font-black uppercase tracking-widest text-zinc-300 block">Tỷ lệ hoàn điểm</span>
          <span className="text-xl font-black text-white flex items-center justify-end gap-1">
            <TrendingUp className="h-4 w-4 text-emerald-400" />
            +{earningRatePct}%
          </span>
        </div>
      </div>

      {/* Card Body / Main Points Display */}
      <div className="mt-8 pt-6 border-t border-white/10 relative z-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
        <div>
          <span className="text-xs font-black uppercase tracking-wider text-zinc-300 block mb-1">Điểm khả dụng hiện tại</span>
          <div className="flex items-baseline gap-2">
            <span className="text-4xl md:text-5xl font-black tracking-tight text-white">
              {currentPoints.toLocaleString('vi-VN')}
            </span>
            <span className="text-sm font-bold text-zinc-300 uppercase">Điểm</span>
          </div>
        </div>

        {heldPoints > 0 && (
          <div className="flex items-center gap-2 rounded-xl bg-black/30 backdrop-blur-md px-4 py-2.5 border border-amber-500/30 text-amber-300">
            <Lock className="h-4 w-4 shrink-0" />
            <div className="text-xs">
              <span className="font-bold">{heldPoints.toLocaleString('vi-VN')}</span> điểm đang tạm giữ
            </div>
          </div>
        )}
      </div>

      {/* Card Footer */}
      <div className="mt-6 flex items-center justify-between text-[11px] text-zinc-300 font-medium relative z-10">
        <div className="flex items-center gap-1.5">
          <ShieldCheck className="h-4 w-4 text-emerald-400" />
          <span>Hội viên chính thức</span>
        </div>
        <div className="uppercase tracking-widest font-black text-zinc-300">
          {user?.fullName || 'Khách hàng thành viên'}
        </div>
      </div>
    </div>
  );
}
