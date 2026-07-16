import React from 'react';

export default function StatsPanel({ stats }) {
  return (
    <div className="bg-zinc-950/50 rounded-2xl p-4 border border-zinc-850 space-y-3 mt-6">
      <span className="text-[10px] font-black text-zinc-500 uppercase tracking-widest block">Thống kê ghế ngồi</span>
      
      <div className="space-y-1.5 text-xs">
        <div className="flex justify-between">
          <span className="text-zinc-400">STANDARD (Tím):</span>
          <span className="text-white font-bold">{stats.standard}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-zinc-400">VIP (Đỏ):</span>
          <span className="text-white font-bold">{stats.vip}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-zinc-400">COUPLE / Đôi (Vàng):</span>
          <span className="text-white font-bold">{stats.couple}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-zinc-400">DISABLED (Xanh):</span>
          <span className="text-white font-bold">{stats.disabled}</span>
        </div>
        <div className="flex justify-between border-t border-zinc-900 pt-1.5 mt-1.5 font-bold">
          <span className="text-zinc-300">Tổng ghế hoạt động:</span>
          <span className="text-brand-coral">{stats.activeSeats} ghế</span>
        </div>
      </div>
    </div>
  );
}
