// eslint-disable-next-line no-unused-vars
import React from 'react';

export default function StatsPanel({ stats }) {
  return (
    <div className="bg-zinc-950/50 rounded-2xl p-4 border border-zinc-850 space-y-3 mt-6">
      <span className="text-[10px] font-black text-zinc-500 uppercase tracking-widest block">Thống kê ghế ngồi</span>
      
      <div className="space-y-1.5 text-xs">
        <div className="flex justify-between">
          <span className="text-zinc-400">Ghế thường (Tím):</span>
          <span className="text-white font-bold">{stats.standard}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-zinc-400">Ghế VIP (Đỏ):</span>
          <span className="text-white font-bold">{stats.vip}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-zinc-400">Ghế đôi (Vàng):</span>
          <span className="text-white font-bold">{stats.coupleModules ?? Math.floor((stats.couple || 0) / 2)} module / {stats.couple || 0} người</span>
        </div>
        <div className="flex justify-between">
          <span className="text-zinc-400">Vị trí tiếp cận (Xanh):</span>
          <span className="text-white font-bold">{stats.disabled}</span>
        </div>
        <div className="flex justify-between border-t border-zinc-900 pt-1.5 mt-1.5 font-bold">
          <span className="text-zinc-300">Sức chứa tối đa:</span>
          <span className="text-brand-orange">{stats.activeSeats} người</span>
        </div>
        {stats.ticketingPositions != null && (
          <div className="flex justify-between">
            <span className="text-zinc-400">Vị trí bán vé:</span>
            <span className="font-bold text-white">{stats.ticketingPositions}</span>
          </div>
        )}
      </div>
    </div>
  );
}
