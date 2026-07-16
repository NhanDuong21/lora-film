import React from 'react';
import { AlertTriangle, Plus, Calendar, RefreshCcw, Power } from 'lucide-react';

export default function CinemaClosurePeriodsCard({
  closures,
  newClosure,
  setNewClosure,
  onCreateClosure,
  onCancelClosure
}) {

  // Helper to format ISO date string
  const formatDateTime = (isoStr) => {
    if (!isoStr) return '';
    const date = new Date(isoStr);
    return date.toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Helper to determine status styling
  const getStatusBadge = (c) => {
    const now = new Date();
    const start = new Date(c.startTime);
    const end = new Date(c.endTime);

    if (c.status === 'CANCELLED') {
      return (
        <span className="text-[9px] font-black text-zinc-500 bg-zinc-900 border border-zinc-800 px-2 py-0.5 rounded-md uppercase tracking-wider">
          Đã Hủy Đóng Cửa
        </span>
      );
    }

    if (now >= start && now <= end) {
      return (
        <span className="text-[9px] font-black text-red-500 bg-red-500/10 border border-red-500/20 px-2 py-0.5 rounded-md uppercase tracking-wider animate-pulse">
          Đang Đóng Cửa
        </span>
      );
    }

    if (now < start) {
      return (
        <span className="text-[9px] font-black text-amber-500 bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded-md uppercase tracking-wider">
          Lên Lịch Đóng Cửa
        </span>
      );
    }

    return (
      <span className="text-[9px] font-black text-zinc-500 bg-zinc-900 border border-zinc-800 px-2 py-0.5 rounded-md uppercase tracking-wider">
        Đã Kết Thúc
      </span>
    );
  };

  return (
    <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-6">
      
      {/* Header */}
      <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
        <AlertTriangle className="w-4 h-4 text-rose-500 animate-pulse" />
        <h2 className="text-sm font-bold uppercase tracking-wider text-white">Đóng Cửa Rạp Khẩn Cấp / Đột Xuất</h2>
      </div>

      <div className="text-xs text-zinc-400 leading-relaxed bg-rose-500/5 border border-rose-500/10 rounded-xl p-3.5 flex gap-2.5 items-start">
        <AlertTriangle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold text-white block mb-0.5">Quy tắc Đóng cửa khẩn cấp:</span>
          Lập lịch ngừng hoạt động rạp đột xuất khi có sự cố khẩn cấp (mất điện, cháy nổ, thiên tai). Trong thời gian đóng cửa này, tất cả các suất chiếu và lịch đặt vé thuộc cụm rạp sẽ bị tạm dừng.
        </div>
      </div>

      {/* Form: Add closure period */}
      <div className="flex flex-col gap-4 bg-zinc-950 p-4 rounded-xl border border-zinc-900">
        <h3 className="text-xs font-bold text-zinc-300 uppercase tracking-wider">Lập lịch dừng hoạt động mới</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Bắt đầu <span className="text-rose-500">*</span></label>
            <input
              type="datetime-local"
              value={newClosure.startTime}
              onChange={e => setNewClosure({ ...newClosure, startTime: e.target.value })}
              className="w-full bg-zinc-900 border border-zinc-850 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-orange-500/40"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Kết thúc <span className="text-rose-500">*</span></label>
            <input
              type="datetime-local"
              value={newClosure.endTime}
              onChange={e => setNewClosure({ ...newClosure, endTime: e.target.value })}
              className="w-full bg-zinc-900 border border-zinc-850 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-orange-500/40"
            />
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Lý do đóng cửa <span className="text-rose-500">*</span></label>
          <input
            type="text"
            value={newClosure.reason}
            onChange={e => setNewClosure({ ...newClosure, reason: e.target.value })}
            placeholder="Ví dụ: Sự cố mất điện lưới, cháy nổ, Lễ Tết..."
            className="w-full bg-zinc-900 border border-zinc-850 rounded-xl py-2 px-3 text-xs text-zinc-100 focus:outline-none focus:border-orange-500/40"
          />
        </div>

        <button
          type="button"
          onClick={onCreateClosure}
          className="self-end flex items-center gap-1.5 bg-rose-600 hover:bg-rose-500 text-white font-bold px-4 py-2 rounded-lg text-[10px] uppercase tracking-wider cursor-pointer transition-colors"
        >
          <Plus className="w-3.5 h-3.5" /> Kích hoạt dừng rạp
        </button>
      </div>

      {/* List of closures */}
      <div className="flex flex-col gap-3">
        <h3 className="text-xs font-bold text-zinc-300 uppercase tracking-wider">Lịch sử sự cố & ngừng hoạt động (5 lần gần nhất)</h3>
        
        {closures.length === 0 ? (
          <div className="text-center py-6 border border-dashed border-zinc-850 rounded-xl text-zinc-650 text-xs">
            Chưa ghi nhận sự cố hay đợt ngừng hoạt động khẩn cấp nào.
          </div>
        ) : (
          <div className="flex flex-col gap-2.5 max-h-60 overflow-y-auto pr-1">
            {closures.slice(0, 5).map((c) => (
              <div key={c.id} className="flex flex-col bg-zinc-950 p-3.5 rounded-xl border border-zinc-900 gap-2.5">
                <div className="flex justify-between items-start gap-2">
                  <div className="flex flex-col gap-1">
                    <span className="text-xs font-bold text-zinc-200">{c.reason}</span>
                    <span className="text-[10px] text-zinc-500 flex items-center gap-1">
                      <Calendar className="w-3 h-3 text-zinc-600" />
                      {formatDateTime(c.startTime)} - {formatDateTime(c.endTime)}
                    </span>
                  </div>
                  {getStatusBadge(c)}
                </div>

                {c.status === 'ACTIVE' && new Date(c.endTime) > new Date() && (
                  <button
                    type="button"
                    onClick={() => onCancelClosure(c.id)}
                    className="self-end flex items-center gap-1 text-[9px] font-black text-emerald-500 hover:text-emerald-400 bg-emerald-500/5 hover:bg-emerald-500/10 border border-emerald-500/10 hover:border-emerald-500/20 px-2.5 py-1.5 rounded-lg uppercase tracking-wider cursor-pointer transition-all"
                  >
                    <Power className="w-3 h-3" /> Mở lại rạp trước thời hạn
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

    </div>
  );
}
