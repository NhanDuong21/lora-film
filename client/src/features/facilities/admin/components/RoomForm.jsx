import { Settings } from 'lucide-react';
import {
  SCREEN_TYPE_LABELS,
  SOUND_TYPE_LABELS,
} from '@/features/facilities/admin/utils/facilityPresentation';

export default function RoomForm({
  roomName,
  setRoomName,
  screenType,
  setScreenType,
  soundType,
  setSoundType,
  cleaningBuffer,
  setCleaningBuffer,
  capacity,
  isCreateMode = false,
}) {
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 border-b border-zinc-800 pb-3">
        <Settings className="h-4 w-4 text-brand-orange" />
        <div>
          <h3 className="text-sm font-black uppercase tracking-wider text-white">
            Cấu hình phục vụ
          </h3>
          <p className="mt-1 text-xs text-zinc-500">
            Các thông tin nhân viên vận hành cần biết về phòng chiếu.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <label className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
          Tên phòng chiếu
        </label>
        <input
          type="text"
          required
          placeholder="Ví dụ: Phòng 01, Phòng IMAX"
          value={roomName}
          onChange={(event) => setRoomName(event.target.value)}
          className="rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm font-semibold text-zinc-200 outline-none transition-colors focus:border-brand-orange"
        />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <label className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
            Công nghệ màn hình
          </label>
          <select
            value={screenType}
            onChange={(event) => setScreenType(event.target.value)}
            className="cursor-pointer rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm font-semibold text-zinc-200 outline-none focus:border-brand-orange"
          >
            {Object.entries(SCREEN_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
            Hệ thống âm thanh
          </label>
          <select
            value={soundType}
            onChange={(event) => setSoundType(event.target.value)}
            className="cursor-pointer rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm font-semibold text-zinc-200 outline-none focus:border-brand-orange"
          >
            {Object.entries(SOUND_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <label className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
          Thời gian dọn phòng giữa hai suất
        </label>
        <div className="relative">
          <input
            type="number"
            min="0"
            max="120"
            value={cleaningBuffer}
            onChange={(event) => setCleaningBuffer(Number.parseInt(event.target.value, 10) || 0)}
            className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 pr-16 text-sm font-semibold text-zinc-200 outline-none transition-colors focus:border-brand-orange"
          />
          <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs text-zinc-500">
            phút
          </span>
        </div>
        <p className="text-xs text-zinc-500">
          Khoảng thời gian này được chừa tự động khi xếp lịch.
        </p>
      </div>

      <div className="rounded-xl border border-zinc-800 bg-zinc-900/40 p-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
              Sức chứa hiện tại
            </p>
            <p className="mt-1 text-lg font-black text-white">{capacity} ghế</p>
          </div>
          <span className="rounded-lg bg-brand-orange/10 px-3 py-1.5 text-[10px] font-black uppercase tracking-wider text-brand-orange">
            Tính từ sơ đồ ghế
          </span>
        </div>
      </div>

      {isCreateMode && (
        <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-xs text-amber-200">
          Phòng mới được lưu ở trạng thái <strong>đang thiết lập</strong>. Hãy hoàn
          thiện sơ đồ ghế rồi dùng tác vụ “Đưa vào phục vụ”.
        </div>
      )}
    </div>
  );
}
