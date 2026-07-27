import React from 'react';
import { Award, Sparkles, TrendingUp, TrendingDown, Clock, ShieldCheck, ArrowRight } from 'lucide-react';

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
    <div className="rounded-3xl bg-zinc-900/60 backdrop-blur-xl border border-zinc-800/80 p-6 md:p-8 text-white shadow-2xl relative overflow-hidden">
      {/* Decorative Glow */}
      <div className="absolute -left-20 -top-20 h-64 w-64 rounded-full bg-blue-500/5 blur-3xl pointer-events-none" />

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800 pb-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
            <Award className="h-6 w-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-lg font-black tracking-tight text-white">
                Lịch Sử Hạng Thành Viên
              </h3>
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">
                Append Only
              </span>
            </div>
            <p className="text-xs text-zinc-400 mt-0.5">
              Toàn bộ lịch sử thăng hạng và giáng hạng đều được ghi nhận trọn đời, không tẩy xóa.
            </p>
          </div>
        </div>

        {tierHistory.length > 0 && (
          <div className="text-xs text-zinc-400 bg-zinc-950/60 px-3.5 py-1.5 rounded-xl border border-zinc-800">
            Tổng cộng: <strong className="text-white font-bold">{tierHistory.length}</strong> lần đổi hạng
          </div>
        )}
      </div>

      {/* Content / Timeline */}
      <div className="mt-6">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-12 space-y-3">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
            <span className="text-xs text-zinc-400 font-medium">Đang tải lịch sử hạng thành viên...</span>
          </div>
        ) : tierHistory.length === 0 ? (
          <div className="flex flex-col items-center justify-center text-center py-12 px-4 rounded-2xl bg-zinc-950/40 border border-zinc-800/60">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-zinc-800/50 border border-zinc-700/50 text-zinc-400 mb-3">
              <ShieldCheck className="h-7 w-7" />
            </div>
            <h4 className="text-base font-bold text-white mb-1">
              Chưa có sự thay đổi hạng nào
            </h4>
            <p className="text-xs text-zinc-400 max-w-md">
              Hạng thành viên hiện tại của bạn được duy trì từ khi kích hoạt tài khoản. Hãy tiếp tục tích lũy điểm để thăng lên các hạng thẻ cao hơn!
            </p>
          </div>
        ) : (
          <div className="relative pl-6 border-l-2 border-zinc-800 space-y-6 my-2">
            {tierHistory.map((item, idx) => {
              const upgrade = isUpgrade(item.oldTierCode, item.newTierCode);
              return (
                <div key={item.id || idx} className="relative group">
                  {/* Timeline Dot */}
                  <div
                    className={`absolute -left-[31px] top-1.5 h-3.5 w-3.5 rounded-full border-2 transition-transform duration-300 group-hover:scale-125 ${
                      upgrade
                        ? 'bg-emerald-500 border-zinc-900 shadow-md shadow-emerald-500/50'
                        : 'bg-amber-500 border-zinc-900 shadow-md shadow-amber-500/50'
                    }`}
                  />

                  {/* Card Item */}
                  <div className="p-4 rounded-2xl bg-zinc-950/60 border border-zinc-800/80 hover:border-zinc-700 transition-all duration-300 flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="space-y-2">
                      <div className="flex items-center gap-2 flex-wrap">
                        {upgrade ? (
                          <span className="inline-flex items-center gap-1 text-xs font-black text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-md border border-emerald-500/20">
                            <TrendingUp className="h-3 w-3" />
                            Thăng Hạng
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs font-black text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-md border border-amber-500/20">
                            <TrendingDown className="h-3 w-3" />
                            Điều Chỉnh Hạng
                          </span>
                        )}

                        <div className="flex items-center gap-1.5 font-bold text-xs">
                          <span className={`px-2 py-0.5 rounded border text-[10px] uppercase ${getTierBadgeStyle(item.oldTierCode)}`}>
                            {item.oldTierCode || 'N/A'}
                          </span>
                          <ArrowRight className="h-3 w-3 text-zinc-500" />
                          <span className={`px-2 py-0.5 rounded border text-[10px] uppercase ${getTierBadgeStyle(item.newTierCode)}`}>
                            {item.newTierCode || 'N/A'}
                          </span>
                        </div>
                      </div>

                      <p className="text-xs text-zinc-300 font-medium">
                        {item.reason || 'Sự kiện thay đổi hạng thành viên'}
                      </p>
                    </div>

                    <div className="shrink-0 flex items-center gap-1.5 text-[11px] text-zinc-400 bg-black/40 px-3 py-1.5 rounded-xl border border-zinc-800/60">
                      <Clock className="h-3.5 w-3.5 text-zinc-500" />
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
