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
  const outstandingPoints = scoreData?.outstandingPoints ?? 0;
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
    <div className={`relative overflow-hidden rounded-[2rem] bg-gradient-to-br ${style.gradient} p-8 text-white shadow-2xl shadow-black/40 border-0 transition-all duration-300 hover:-translate-y-1 hover:shadow-black/60`}>
      {/* Decorative Glow */}
      <div className="absolute -right-20 -top-20 h-72 w-72 rounded-full bg-white/5 blur-3xl pointer-events-none" />
      <div className="absolute -bottom-24 -left-24 h-64 w-64 rounded-full bg-black/40 blur-3xl pointer-events-none" />

      {/* Inner glass border */}
      <div className={`absolute inset-0 rounded-[2rem] border ${style.border} opacity-50 pointer-events-none`} />

      {/* Card Header */}
      <div className="flex items-start justify-between relative z-10">
        <div className="flex items-center gap-4">
          <div className={`flex h-14 w-14 items-center justify-center rounded-2xl bg-white/5 backdrop-blur-xl border ${style.border} shadow-inner`}>
            <Award className={`h-7 w-7 ${style.iconColor}`} />
          </div>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[10px] font-black uppercase tracking-widest backdrop-blur-md ${style.badgeBg}`}>
                <Sparkles className="h-3 w-3" />
                {currentTier.tierName}
              </span>
            </div>
            <p className="text-xs text-zinc-400 font-medium tracking-wide">
              Tích lũy: <span className="font-bold text-white">{accumulatedPoints.toLocaleString('vi-VN')}</span> điểm
            </p>
          </div>
        </div>
        <div className="text-right">
          <span className="text-[9px] font-black uppercase tracking-widest text-zinc-500 block mb-1">Tỷ lệ hoàn</span>
          <span className="text-lg font-black text-white flex items-center justify-end gap-1.5 bg-black/20 px-2 py-1 rounded-xl backdrop-blur-sm border border-white/5">
            <TrendingUp className="h-4 w-4 text-emerald-400" />
            {earningRatePct}%
          </span>
        </div>
      </div>

      {/* Card Body / Main Points Display */}
      <div className="mt-10 mb-8 relative z-10 flex flex-col gap-6">
        <div>
          <span className="text-[10px] font-black uppercase tracking-widest text-zinc-400 block mb-2">Điểm khả dụng</span>
          <div className="flex items-end gap-3">
            <span className="text-5xl md:text-6xl font-black tracking-tighter text-white drop-shadow-lg">
              {currentPoints.toLocaleString('vi-VN')}
            </span>
            <span className="text-sm font-bold text-zinc-400 tracking-widest pb-1.5">điểm</span>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {heldPoints > 0 && (
            <div className="flex items-center gap-2 rounded-xl bg-amber-950/40 backdrop-blur-md px-3.5 py-2 border border-amber-500/20 text-amber-300">
              <Lock className="h-3.5 w-3.5 shrink-0" />
              <div className="text-[11px] font-medium tracking-wide">
                <span className="font-black">{heldPoints.toLocaleString('vi-VN')}</span> tạm giữ
              </div>
            </div>
          )}
          {outstandingPoints > 0 && (
            <div className="flex items-center gap-2 rounded-xl bg-red-950/40 backdrop-blur-md px-3.5 py-2 border border-red-500/20 text-red-400">
              <span className="h-1.5 w-1.5 rounded-full bg-red-500 animate-pulse" />
              <div className="text-[11px] font-medium tracking-wide">
                <span className="font-black">{outstandingPoints.toLocaleString('vi-VN')}</span> nợ
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Card Footer */}
      <div className="flex items-center justify-between text-[11px] text-zinc-400 font-medium relative z-10 pt-5 border-t border-white/10">
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-emerald-500" />
          <span className="tracking-wide">Thành viên chính thức</span>
        </div>
        <div className="uppercase tracking-widest font-black text-white/90">
          {user?.fullName || 'LORA MEMBER'}
        </div>
      </div>
    </div>
  );
}
