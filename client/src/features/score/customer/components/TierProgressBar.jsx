import { Crown, ArrowRight, CheckCircle2 } from 'lucide-react';

export default function TierProgressBar({ scoreData }) {
  const currentTier = scoreData?.currentTier || { tierName: 'Silver Member', minAccumulatedPoints: 0 };
  const nextTier = scoreData?.nextTier;
  const accumulatedPoints = scoreData?.accumulatedPoints ?? 0;

  const isMaxTier = !nextTier || nextTier.pointsRequired === 0 || nextTier.tierCode === currentTier.tierCode;

  if (isMaxTier) {
    return (
      <div className="rounded-3xl bg-zinc-900/80 border border-amber-500/30 p-6 shadow-xl relative overflow-hidden backdrop-blur-md">
        <div className="absolute top-0 right-0 w-32 h-32 bg-amber-500/10 rounded-full filter blur-2xl pointer-events-none" />
        <div className="flex items-center gap-4 relative z-10">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
            <Crown className="h-6 w-6" />
          </div>
          <div>
            <h4 className="text-base font-black text-white flex items-center gap-2">
              Đẳng cấp tối cao: {currentTier.tierName}
              <CheckCircle2 className="h-4 w-4 text-emerald-400 inline" />
            </h4>
            <p className="text-xs text-zinc-400 mt-0.5 leading-relaxed">
              Bạn đã đạt mức hạng thành viên cao nhất của hệ thống. Tận hưởng trọn vẹn đặc quyền và mức hoàn điểm tối đa cho mọi giao dịch!
            </p>
          </div>
        </div>
      </div>
    );
  }

  const currentMin = currentTier.minAccumulatedPoints || 0;
  const targetMin = nextTier.minAccumulatedPoints || (currentMin + 400);
  const totalRange = Math.max(1, targetMin - currentMin);
  const currentProgress = Math.max(0, accumulatedPoints - currentMin);
  const percentage = Math.min(100, Math.max(0, Math.round((currentProgress / totalRange) * 100)));

  return (
    <div className="rounded-3xl bg-zinc-900/80 border border-zinc-800 p-6 shadow-xl space-y-4 backdrop-blur-md">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="text-xs font-black uppercase tracking-wider text-zinc-400">Tiến độ thăng hạng:</span>
          <span className="text-xs font-bold text-white bg-zinc-800 px-2.5 py-1 rounded-lg border border-zinc-700/50 flex items-center gap-1.5">
            {currentTier.tierName}
            <ArrowRight className="h-3 w-3 text-zinc-500" />
            <span className="text-amber-400 font-black">{nextTier.tierName}</span>
          </span>
        </div>
        <div className="text-xs text-zinc-400 font-medium">
          Cần thêm <span className="font-black text-amber-400">{nextTier.pointsRequired.toLocaleString('vi-VN')}</span> điểm tích lũy
        </div>
      </div>

      {/* Bar */}
      <div className="relative h-3 w-full rounded-full bg-zinc-950 border border-zinc-800 overflow-hidden">
        <div
          className="absolute top-0 left-0 h-full rounded-full bg-gradient-to-r from-brand-orange via-amber-500 to-yellow-400 transition-all duration-700 ease-out shadow-lg shadow-amber-500/20"
          style={{ width: `${percentage}%` }}
        />
      </div>

      {/* Milestones Labels */}
      <div className="flex justify-between items-center text-[11px] font-bold text-zinc-500">
        <span>{currentMin.toLocaleString('vi-VN')} điểm</span>
        <span className="text-zinc-300 font-black">{accumulatedPoints.toLocaleString('vi-VN')} / {targetMin.toLocaleString('vi-VN')} điểm</span>
        <span>{targetMin.toLocaleString('vi-VN')} điểm</span>
      </div>
    </div>
  );
}
