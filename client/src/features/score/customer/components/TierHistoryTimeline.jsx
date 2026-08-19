import { Award, TrendingUp, TrendingDown, Clock, ShieldCheck, ArrowRight } from 'lucide-react';

export default function TierHistoryTimeline({ tierHistory = [], isLoading = false }) {
  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getTierBadgeStyle = (code) => {
    switch (code?.toUpperCase()) {
      case 'DIAMOND':
        return 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30';
      case 'GOLD':
        return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
      case 'SILVER':
      default:
        return 'bg-zinc-700/40 text-zinc-300 border-zinc-600';
    }
  };

  const isUpgrade = (oldCode, newCode) => {
    const ranks = { SILVER: 1, GOLD: 2, DIAMOND: 3 };
    const oldRank = ranks[oldCode?.toUpperCase()] || 0;
    const newRank = ranks[newCode?.toUpperCase()] || 0;
    return newRank > oldRank;
  };

  return (
    <div className="rounded-[2rem] bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-8 text-white shadow-2xl shadow-black/20 relative overflow-hidden h-full flex flex-col">
      {/* Decorative Glow */}
      <div className="absolute -left-20 -top-20 h-64 w-64 rounded-full bg-indigo-500/5 blur-3xl pointer-events-none" />

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800/50 pb-5">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 shadow-inner">
            <Award className="h-6 w-6" />
          </div>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h3 className="text-lg font-black tracking-tight text-white">
                Lịch Sử Hạng Thành Viên
              </h3>
              <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[9px] font-black uppercase tracking-widest bg-white/5 text-indigo-300 border border-white/10 shadow-sm">
                Lịch sử bất biến
              </span>
            </div>
            <p className="text-[11px] text-zinc-400 font-medium tracking-wide">
              Toàn bộ lịch sử thăng hạng và giáng hạng đều được ghi nhận trọn đời.
            </p>
          </div>
        </div>

        {tierHistory.length > 0 && (
          <div className="text-[10px] uppercase font-bold tracking-widest text-zinc-500 bg-white/5 px-3 py-1.5 rounded-xl border border-white/5 shadow-inner">
            Tổng cộng: <strong className="text-white font-black">{tierHistory.length}</strong> lần đổi hạng
          </div>
        )}
      </div>

      {/* Content / Timeline */}
      <div className="mt-6 flex-grow">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center h-full py-12 space-y-3">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
            <span className="text-xs text-zinc-400 font-medium tracking-wide">Đang tải lịch sử hạng thành viên...</span>
          </div>
        ) : tierHistory.length === 0 ? (
          <div className="flex flex-col items-center justify-center text-center h-full py-12 px-4 rounded-3xl bg-zinc-950/40 border border-zinc-800/40">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-zinc-800/50 border border-zinc-700/50 text-zinc-500 mb-4 shadow-inner">
              <ShieldCheck className="h-7 w-7" />
            </div>
            <h4 className="text-sm font-black tracking-wide text-white mb-1.5 uppercase">
              Chưa có sự thay đổi hạng nào
            </h4>
            <p className="text-[11px] font-medium text-zinc-400 max-w-sm leading-relaxed">
              Hạng thành viên hiện tại của bạn được duy trì từ khi kích hoạt tài khoản. Hãy tiếp tục tích lũy điểm để thăng hạng!
            </p>
          </div>
        ) : (
          <div className="relative pl-7 border-l border-zinc-800/50 space-y-6 my-2">
            {tierHistory.map((item, idx) => {
              const upgrade = isUpgrade(item.oldTierCode, item.newTierCode);
              return (
                <div key={item.id || idx} className="relative group">
                  {/* Timeline Dot */}
                  <div
                    className={`absolute -left-[33px] top-4 h-2.5 w-2.5 rounded-full border border-zinc-900 transition-transform duration-300 group-hover:scale-150 ${
                      upgrade
                        ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.6)]'
                        : 'bg-amber-400 shadow-[0_0_8px_rgba(251,191,36,0.6)]'
                    }`}
                  />

                  {/* Card Item */}
                  <div className="p-4 rounded-3xl bg-zinc-950/40 border border-zinc-800/50 hover:border-zinc-700/80 hover:bg-zinc-900/40 transition-all duration-300 flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="space-y-2.5">
                      <div className="flex items-center gap-3 flex-wrap">
                        {upgrade ? (
                          <span className="inline-flex items-center gap-1.5 text-[9px] font-black uppercase tracking-widest text-emerald-400 bg-emerald-500/10 px-2 py-1 rounded-lg border border-emerald-500/20 shadow-inner">
                            <TrendingUp className="h-3 w-3" />
                            Thăng Hạng
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 text-[9px] font-black uppercase tracking-widest text-amber-400 bg-amber-500/10 px-2 py-1 rounded-lg border border-amber-500/20 shadow-inner">
                            <TrendingDown className="h-3 w-3" />
                            Điều Chỉnh
                          </span>
                        )}

                        <div className="flex items-center gap-2 font-bold text-[10px] tracking-widest uppercase">
                          <span className={`px-2 py-0.5 rounded-md border shadow-inner ${getTierBadgeStyle(item.oldTierCode)}`}>
                            {item.oldTierCode || 'N/A'}
                          </span>
                          <ArrowRight className="h-3.5 w-3.5 text-zinc-600" />
                          <span className={`px-2 py-0.5 rounded-md border shadow-inner ${getTierBadgeStyle(item.newTierCode)}`}>
                            {item.newTierCode || 'N/A'}
                          </span>
                        </div>
                      </div>

                      <p className="text-[11px] text-zinc-400 font-medium tracking-wide">
                        {item.reason || 'Sự kiện thay đổi hạng thành viên'}
                      </p>
                    </div>

                    <div className="shrink-0 flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-zinc-500 bg-black/20 px-3 py-1.5 rounded-xl border border-white/5 shadow-inner">
                      <Clock className="h-3.5 w-3.5 text-zinc-600" />
                      <span>{formatDateTime(item.createdAt)}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
