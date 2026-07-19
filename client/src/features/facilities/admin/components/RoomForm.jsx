// eslint-disable-next-line no-unused-vars
import React from 'react';
import { Settings } from 'lucide-react';

export default function RoomForm({
  roomName,
  setRoomName,
  screenType,
  setScreenType,
  soundType,
  setSoundType,
  cleaningBuffer,
  setCleaningBuffer,
  status,
  setStatus,
  capacity,
  availableStatuses
}) {
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 border-b border-zinc-800 pb-2">
        <Settings className="w-4 h-4 text-brand-orange" />
        <h3 className="font-bold text-xs text-white uppercase tracking-wider">Cấu hình phòng</h3>
      </div>

      {/* Room Name */}
      <div className="flex flex-col gap-1.5">
        <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Tên phòng chiếu</label>
        <input
          type="text"
          placeholder="Ví dụ: Phòng Chiếu 1, Room 01..."
          value={roomName}
          onChange={(e) => setRoomName(e.target.value)}
          className="bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl px-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none transition-colors"
        />
      </div>

      {/* Screen Type */}
      <div className="flex flex-col gap-1.5">
        <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Định dạng màn hình</label>
        <select
          value={screenType}
          onChange={(e) => setScreenType(e.target.value)}
          className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none cursor-pointer"
        >
          <option value="STANDARD">STANDARD (Tiêu chuẩn)</option>
          <option value="IMAX">IMAX (Siêu cực đại)</option>
          <option value="4DX">4DX (Mô phỏng chuyển động)</option>
          <option value="SCREENX">SCREENX (Màn chiếu 3 mặt)</option>
        </select>
      </div>

      {/* Sound Type */}
      <div className="flex flex-col gap-1.5">
        <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Định dạng âm thanh</label>
        <select
          value={soundType}
          onChange={(e) => setSoundType(e.target.value)}
          className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none cursor-pointer"
        >
          <option value="STANDARD">STANDARD (Hệ thống thường)</option>
          <option value="DOLBY_ATMOS">DOLBY ATMOS (Âm thanh vòm)</option>
        </select>
      </div>

      {/* Cleaning Buffer */}
      <div className="flex flex-col gap-1.5">
        <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Thời gian dọn dẹp (phút)</label>
        <input
          type="number"
          min="0"
          max="120"
          value={cleaningBuffer}
          onChange={(e) => setCleaningBuffer(parseInt(e.target.value) || 0)}
          className="bg-zinc-950 border border-zinc-800 focus:border-brand-orange rounded-xl px-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none transition-colors"
        />
      </div>

      {/* Auto-Capacity display (Read-Only) */}
      <div className="flex flex-col gap-1.5">
        <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest flex items-center gap-1.5">
          <span>Tổng số ghế (Sức chứa)</span>
          <span className="bg-brand-orange/20 text-brand-orange px-1.5 py-0.5 rounded text-[8px] tracking-widest font-black uppercase">TỰ ĐỘNG</span>
        </label>
        <div className="bg-zinc-900/50 border border-zinc-800/80 rounded-xl px-4 py-2.5 text-xs font-semibold text-zinc-400 select-none flex justify-between items-center">
          <span>{capacity} ghế</span>
          <span className="text-[9px] text-zinc-500 font-medium">Được tính toán tự động từ Sơ đồ ghế</span>
        </div>
      </div>

      {/* Room Status */}
      <div className="flex flex-col gap-1.5">
        <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Trạng thái phát hành</label>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none cursor-pointer"
        >
          {availableStatuses.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
