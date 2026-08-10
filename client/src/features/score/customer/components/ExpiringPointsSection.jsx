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
    <div className="rounded-[2rem] bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-8 text-white shadow-2xl shadow-black/20 relative overflow-hidden h-full flex flex-col">
      {/* Subtle Background Glow */}
      <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-amber-500/5 blur-3xl pointer-events-none" />
      
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800/50 pb-5">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-400 shadow-inner">
            <Clock className="h-6 w-6" />
          </div>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h3 className="text-lg font-black tracking-tight text-white">
                Điểm Sắp Hết Hạn
              </h3>
              <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[9px] font-black uppercase tracking-widest bg-white/5 text-amber-300 border border-white/10 shadow-sm">
                FIFO Policy
              </span>
            </div>
            <p className="text-[11px] text-zinc-400 font-medium tracking-wide">
              Thời hạn sử dụng 12 tháng kể từ ngày tích lũy. Ưu tiên trừ điểm cũ trước (FIFO).
            </p>
          </div>
        </div>

        {activeBuckets.length > 0 && (
          <div className="flex items-center gap-2 bg-amber-500/10 border border-amber-500/30 px-3 py-1.5 rounded-xl text-amber-300 text-[10px] font-black uppercase tracking-widest shadow-inner">
            <Flame className="h-3.5 w-3.5 shrink-0 text-amber-400 animate-pulse" />
            <span>{activeBuckets.length} lô điểm cần lưu ý</span>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="mt-6 flex-grow">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center h-full py-12 space-y-3">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-amber-500 border-t-transparent" />
            <span className="text-xs text-zinc-400 font-medium tracking-wide">Đang kiểm tra dữ liệu điểm hết hạn...</span>
          </div>
        ) : activeBuckets.length === 0 ? (
          <div className="flex flex-col items-center justify-center text-center h-full py-12 px-4 rounded-3xl bg-zinc-950/40 border border-zinc-800/40">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 mb-4 shadow-inner">
              <ShieldCheck className="h-7 w-7" />
            </div>
            <h4 className="text-sm font-black tracking-wide text-white mb-1.5 uppercase">
              Tất cả điểm khả dụng đều an toàn
            </h4>
            <p className="text-[11px] font-medium text-zinc-400 max-w-sm leading-relaxed">
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
                  className={`p-5 rounded-3xl border transition-all duration-300 flex flex-col justify-between gap-4 ${
                    isUrgent
                      ? 'bg-red-950/20 border-red-500/30 shadow-lg shadow-red-500/5 hover:border-red-500/50'
                      : 'bg-zinc-950/40 border-zinc-800/50 hover:border-zinc-700/80 hover:bg-zinc-900/40'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-1.5">
                      <div className="flex items-center gap-2">
                        <span className="text-3xl font-black tracking-tighter text-white">
                          {bucket.remainingPoints.toLocaleString('vi-VN')}
                        </span>
                        <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-widest pb-1">PTS</span>
                      </div>
                      <div className="text-[10px] text-zinc-400 font-medium tracking-wide flex flex-col gap-0.5">
                        <span>Ban đầu: <strong className="text-zinc-300">{bucket.earnedPoints.toLocaleString('vi-VN')}</strong></span>
                        {bucket.bookingId && (
                          <span>Mã đơn: <strong className="text-zinc-300">#{bucket.bookingId}</strong></span>
                        )}
                      </div>
                    </div>

                    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl text-[9px] font-black uppercase tracking-widest border shadow-inner ${
                      isUrgent
                        ? 'bg-red-500/10 text-red-400 border-red-500/20 animate-pulse'
                        : 'bg-white/5 text-zinc-300 border-white/10'
                    }`}>
                      {isUrgent ? (
                        <>
                          <AlertTriangle className="h-3 w-3" />
                          <span>Gấp: {daysLeft} ngày</span>
                        </>
                      ) : (
                        <>
                          <Calendar className="h-3 w-3 text-amber-400" />
                          <span>{daysLeft > 0 ? `Còn ${daysLeft} ngày` : 'Sắp hết hạn'}</span>
                        </>
                      )}
                    </span>
                  </div>

                  <div className="pt-3 border-t border-zinc-800/40 flex items-center justify-between text-[11px] font-medium tracking-wide">
                    <span className="text-zinc-500">Ngày hết hạn</span>
                    <span className={`font-black ${isUrgent ? 'text-red-400' : 'text-amber-300'}`}>
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
      <div className="mt-6 pt-5 border-t border-zinc-800/50 flex items-start gap-2.5 text-[10px] text-zinc-400 font-medium tracking-wide leading-relaxed">
        <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
        <span>Khi bạn thực hiện đổi điểm (Redeem), hệ thống sẽ tự động khấu trừ từ những lô điểm có ngày hết hạn gần nhất.</span>
      </div>
    </div>
  );
}
