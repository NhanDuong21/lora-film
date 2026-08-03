import { AlertCircle, ShieldAlert } from 'lucide-react';

export default function OutstandingBadge({ outstandingPoints = 0, variant = 'banner' }) {
  if (!outstandingPoints || outstandingPoints <= 0) {
    return null;
  }

  if (variant === 'badge') {
    return (
      <div className="inline-flex items-center gap-2 rounded-xl bg-red-500/20 backdrop-blur-md px-3.5 py-2 border border-red-500/40 text-red-300 animate-pulse shadow-lg shadow-red-500/10">
        <ShieldAlert className="h-4 w-4 shrink-0 text-red-400" />
        <div className="text-xs font-bold tracking-wide">
          <span>{outstandingPoints.toLocaleString('vi-VN')}</span> điểm nợ (Outstanding)
        </div>
      </div>
    );
  }

  return (
    <div className="relative overflow-hidden rounded-[2rem] bg-gradient-to-r from-red-950/40 via-red-900/20 to-orange-950/40 p-8 text-white shadow-2xl shadow-red-900/10 border border-red-500/20 backdrop-blur-xl transition-all duration-300 hover:border-red-500/40">
      {/* Decorative background glow */}
      <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-red-500/10 blur-3xl pointer-events-none" />
      <div className="absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-orange-500/10 blur-3xl pointer-events-none" />

      <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="flex items-start gap-5">
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-red-500/10 border border-red-500/20 text-red-400 shadow-inner">
            <AlertCircle className="h-8 w-8 animate-pulse" />
          </div>
          <div className="space-y-1.5">
            <div className="flex items-center gap-2 mb-1">
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-md text-[9px] font-black uppercase tracking-widest bg-red-500/10 text-red-300 border border-red-500/20 shadow-sm">
                Cần chú ý
              </span>
            </div>
            <h3 className="text-xl font-black tracking-tight text-white">
              Tài khoản đang có số dư nợ điểm
            </h3>
            <p className="text-[11px] text-zinc-300 max-w-2xl leading-relaxed tracking-wide">
              Bạn hiện có <strong className="text-red-400 font-bold">{outstandingPoints.toLocaleString('vi-VN')} điểm nợ</strong> phát sinh từ giao dịch hoàn/hủy đặt chỗ khi số dư khả dụng không đủ. Số điểm tích lũy trong các giao dịch tiếp theo sẽ được hệ thống tự động cấn trừ vào khoản nợ này.
            </p>
          </div>
        </div>

        <div className="shrink-0 flex flex-col items-start md:items-end bg-black/20 px-6 py-4 rounded-3xl border border-red-500/10 shadow-inner">
          <span className="text-[10px] font-black uppercase tracking-widest text-red-400/80 mb-1">Tổng điểm ghi nợ</span>
          <div className="flex items-end gap-1.5">
            <span className="text-3xl md:text-4xl font-black text-red-400 tracking-tighter">
              -{outstandingPoints.toLocaleString('vi-VN')}
            </span>
            <span className="text-[10px] font-bold text-red-400/60 uppercase tracking-widest pb-1">pts</span>
          </div>
        </div>
      </div>
    </div>
  );
}
