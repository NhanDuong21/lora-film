import React from 'react';
import { AlertCircle, ArrowRight, ShieldAlert } from 'lucide-react';

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
    <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-red-950/80 via-red-900/60 to-orange-950/80 p-6 md:p-8 text-white shadow-2xl border border-red-500/30 backdrop-blur-xl transition-all duration-300 hover:border-red-500/50">
      {/* Decorative background glow */}
      <div className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-red-500/10 blur-3xl pointer-events-none" />
      <div className="absolute -bottom-10 -left-10 h-40 w-40 rounded-full bg-orange-500/10 blur-3xl pointer-events-none" />

      <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="flex items-start gap-4">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-red-500/20 border border-red-500/30 text-red-400 shadow-inner">
            <AlertCircle className="h-7 w-7 animate-pulse" />
          </div>
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-red-500/20 text-red-300 border border-red-500/30">
                Cần chú ý
              </span>
              <h3 className="text-lg font-black tracking-tight text-white">
                Tài khoản đang có số dư nợ điểm
              </h3>
            </div>
            <p className="text-sm text-zinc-300 max-w-2xl leading-relaxed">
              Bạn hiện có <strong className="text-red-400 font-bold">{outstandingPoints.toLocaleString('vi-VN')} điểm nợ</strong> phát sinh từ giao dịch hoàn/hủy đặt chỗ khi số dư khả dụng không đủ. Số điểm tích lũy trong các giao dịch tiếp theo sẽ được hệ thống tự động cấn trừ vào khoản nợ này cho đến khi hết.
            </p>
          </div>
        </div>

        <div className="shrink-0 flex flex-col items-start md:items-end bg-black/40 px-5 py-3 rounded-2xl border border-red-500/20">
          <span className="text-[10px] font-black uppercase tracking-widest text-zinc-400">Tổng điểm ghi nợ</span>
          <span className="text-2xl md:text-3xl font-black text-red-400">
            -{outstandingPoints.toLocaleString('vi-VN')} <span className="text-xs font-bold text-zinc-400 uppercase">pts</span>
          </span>
        </div>
      </div>
    </div>
  );
}
