// eslint-disable-next-line no-unused-vars
import React from 'react';
import { Calendar } from 'lucide-react';

export default function CinemaOperatingHours({ operatingHours, onHoursChange }) {
  return (
    <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-4">
      <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
        <Calendar className="w-4 h-4 text-orange-500" />
        <h2 className="text-sm font-bold uppercase tracking-wider text-white">Giờ Hoạt Động (Theo Thứ)</h2>
      </div>

      <div className="flex flex-col gap-3">
        {[
          'Thứ Hai',
          'Thứ Ba',
          'Thứ Tư',
          'Thứ Năm',
          'Thứ Sáu',
          'Thứ Bảy',
          'Chủ Nhật'
        ].map((dayName, idx) => {
          const oh = operatingHours[idx];
          if (!oh) return null;
          return (
            <div key={idx} className="flex items-center justify-between bg-zinc-950 p-3 rounded-xl border border-zinc-900 gap-4">
              <div className="flex items-center gap-3 shrink-0">
                <input
                  type="checkbox"
                  checked={!oh.isClosed}
                  onChange={e => onHoursChange(idx, 'isClosed', !e.target.checked)}
                  className="w-4 h-4 text-orange-500 bg-zinc-900 border-zinc-800 rounded focus:ring-orange-500/40 focus:ring-2 cursor-pointer"
                />
                <span className="text-xs font-bold text-zinc-300 w-16">{dayName}</span>
              </div>

              {!oh.isClosed ? (
                <div className="flex items-center gap-1.5 shrink-0">
                  <input
                    type="time"
                    max="23:59"
                    value={oh.openTime}
                    onChange={e => onHoursChange(idx, 'openTime', e.target.value)}
                    className="bg-zinc-900 border border-zinc-800 focus:border-orange-500/40 rounded-lg p-1.5 px-1 w-20 text-center text-[10px] text-zinc-100 focus:outline-none shrink-0"
                  />
                  <span className="text-[9px] text-zinc-600 font-bold uppercase shrink-0">đến</span>
                  <input
                    type="time"
                    max="23:59"
                    value={oh.closeTime}
                    onChange={e => onHoursChange(idx, 'closeTime', e.target.value)}
                    className="bg-zinc-900 border border-zinc-800 focus:border-orange-500/40 rounded-lg p-1.5 px-1 w-20 text-center text-[10px] text-zinc-100 focus:outline-none shrink-0"
                  />
                </div>
              ) : (
                <span className="text-[10px] font-black text-rose-500/80 bg-rose-500/5 border border-rose-500/10 px-3 py-1 rounded-lg uppercase tracking-wider">
                  Nghỉ / Đóng Cửa
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
