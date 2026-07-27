import React from 'react';
import { Clock, AlertTriangle, CheckCircle2, Calendar, ShieldCheck, Flame } from 'lucide-react';

export default function ExpiringPointsSection({ expiringPoints = [], isLoading = false }) {
  // Filter out consumed or zero remaining buckets for active display
  const activeBuckets = expiringPoints.filter(b => b.remainingPoints > 0);

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  const getDaysRemaining = (dateStr) => {
    if (!dateStr) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const expDate = new Date(dateStr);
    expDate.setHours(0, 0, 0, 0);
    const diffTime = expDate - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  return (
    <div className="rounded-3xl bg-zinc-900/60 backdrop-blur-xl border border-zinc-800/80 p-6 md:p-8 text-white shadow-2xl relative overflow-hidden">
      {/* Subtle Background Glow */}
      <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-amber-500/5 blur-3xl pointer-events-none" />
      
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800 pb-6">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
            <Clock className="h-6 w-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-lg font-black tracking-tight text-white">
                Điểm Sắp Hết Hạn
              </h3>
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-amber-500/10 text-amber-300 border border-amber-500/20">
                FIFO Policy
              </span>
            </div>
            <p className="text-xs text-zinc-400 mt-0.5">
              Thời hạn sử dụng 12 tháng kể từ ngày tích lũy. Ưu tiên trừ điểm cũ trước (FIFO).
            </p>
          </div>
        </div>

        {activeBuckets.length > 0 && (
          <div className="flex items-center gap-2 bg-amber-500/10 border border-amber-500/30 px-4 py-2 rounded-xl text-amber-300 text-xs font-bold">
            <Flame className="h-4 w-4 shrink-0 text-amber-400 animate-pulse" />
            <span>{activeBuckets.length} lô điểm cần lưu ý</span>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="mt-6">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-12 space-y-3">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-amber-500 border-t-transparent" />
            <span className="text-xs text-zinc-400 font-medium">Đang kiểm tra dữ liệu điểm hết hạn...</span>
          </div>
        ) : activeBuckets.length === 0 ? (
          <div className="flex flex-col items-center justify-center text-center py-12 px-4 rounded-2xl bg-zinc-950/40 border border-zinc-800/60">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 mb-3">
              <ShieldCheck className="h-7 w-7" />
            </div>
            <h4 className="text-base font-bold text-white mb-1">
              Tất cả điểm khả dụng đều an toàn
            </h4>
            <p className="text-xs text-zinc-400 max-w-md">
              Hiện không có lô điểm nào sắp hết hạn trong thời gian gần. Bạn có thể thoải mái sử dụng điểm tích lũy cho các dịch vụ tiếp theo.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {activeBuckets.map((bucket) => {
              const daysLeft = getDaysRemaining(bucket.expirationDate);
              const isUrgent = daysLeft !== null && daysLeft <= 30;

              return (
                <div
                  key={bucket.id}
                  className={`p-5 rounded-2xl border transition-all duration-300 flex flex-col justify-between gap-4 ${
                    isUrgent
                      ? 'bg-gradient-to-br from-red-950/30 via-zinc-900/90 to-zinc-950 border-red-500/40 shadow-lg shadow-red-500/5'
                      : 'bg-zinc-950/50 border-zinc-800/80 hover:border-zinc-700'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-2xl font-black text-white">
                          {bucket.remainingPoints.toLocaleString('vi-VN')}
                        </span>
                        <span className="text-xs font-bold text-zinc-400 uppercase">điểm</span>
                      </div>
                      <div className="text-[11px] text-zinc-400 flex items-center gap-1.5">
                        <span>Ban đầu: <strong className="text-zinc-300">{bucket.earnedPoints.toLocaleString('vi-VN')}</strong></span>
                        {bucket.bookingId && (
                          <>
                            <span>•</span>
                            <span>Mã đơn: <strong className="text-zinc-300">#{bucket.bookingId}</strong></span>
                          </>
                        )}
                      </div>
                    </div>

                    <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider border ${
                      isUrgent
                        ? 'bg-red-500/20 text-red-300 border-red-500/30 animate-pulse'
                        : 'bg-zinc-800 text-zinc-300 border-zinc-700'
                    }`}>
                      {isUrgent ? (
                        <>
                          <AlertTriangle className="h-3 w-3 text-red-400" />
                          <span>Gấp: còn {daysLeft} ngày</span>
                        </>
                      ) : (
                        <>
                          <Calendar className="h-3 w-3 text-amber-400" />
                          <span>{daysLeft > 0 ? `Còn ${daysLeft} ngày` : 'Sắp hết hạn'}</span>
                        </>
                      )}
                    </span>
                  </div>

                  <div className="pt-3 border-t border-zinc-800/60 flex items-center justify-between text-xs">
                    <span className="text-zinc-400 font-medium">Ngày hết hạn:</span>
                    <span className={`font-bold ${isUrgent ? 'text-red-400' : 'text-amber-300'}`}>
                      {formatDate(bucket.expirationDate)}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Footer Info */}
      <div className="mt-6 pt-4 border-t border-zinc-800/60 flex items-center gap-2 text-[11px] text-zinc-400">
        <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
        <span>Khi bạn thực hiện đổi điểm (Redeem), hệ thống sẽ tự động khấu trừ từ những lô điểm có ngày hết hạn gần nhất.</span>
      </div>
    </div>
  );
}
