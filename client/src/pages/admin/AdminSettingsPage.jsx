import { useState } from 'react';
import { Save, Clock, Settings, ShieldAlert } from 'lucide-react';
import { SYSTEM_SETTINGS } from '../../mocks/mockData';

export default function AdminSettingsView({ activeTab, triggerToast }) {
  // Load initial configurations from localStorage or mockData SYSTEM_SETTINGS fallback
  const [maxDelay, setMaxDelay] = useState(() => {
    const saved = localStorage.getItem('lora_max_delay');
    return saved ? parseInt(saved, 10) : SYSTEM_SETTINGS.maxDelay;
  });

  const [autoClose, setAutoClose] = useState(() => {
    const saved = localStorage.getItem('lora_auto_close');
    return saved ? saved === 'true' : SYSTEM_SETTINGS.autoClose;
  });

  const [imaxSurcharge, setImaxSurcharge] = useState(() => {
    const saved = localStorage.getItem('lora_imax_surcharge');
    return saved ? parseInt(saved, 10) : SYSTEM_SETTINGS.imaxSurcharge;
  });

  const [weekendMultiplier, setWeekendMultiplier] = useState(() => {
    const saved = localStorage.getItem('lora_weekend_multiplier');
    return saved ? parseFloat(saved) : SYSTEM_SETTINGS.weekendMultiplier;
  });

  const handleSaveAll = () => {
    // Write directly back to SYSTEM_SETTINGS store matrix
    SYSTEM_SETTINGS.maxDelay = maxDelay;
    SYSTEM_SETTINGS.autoClose = autoClose;
    SYSTEM_SETTINGS.imaxSurcharge = imaxSurcharge;
    SYSTEM_SETTINGS.weekendMultiplier = weekendMultiplier;

    // Persist in localStorage for simulation resilience
    localStorage.setItem('lora_max_delay', maxDelay.toString());
    localStorage.setItem('lora_auto_close', autoClose.toString());
    localStorage.setItem('lora_imax_surcharge', imaxSurcharge.toString());
    localStorage.setItem('lora_weekend_multiplier', weekendMultiplier.toString());

    triggerToast('Cập nhật toàn bộ cấu hình hệ thống thành công!', 'success');
  };

  return (
    <div className="space-y-6">
      {/* 1. Global Navigation Title Banner */}
      <div className="mb-6 animate-fade-in">
        <h1 className="text-xl md:text-2xl font-black text-zinc-50 uppercase tracking-tight">THIẾT LẬP HỆ THỐNG</h1>
        <p className="text-xs text-zinc-400 mt-1 font-medium">Quản lý các ngưỡng thời gian vận hành tự động và ma trận hệ số phụ thu giá vé toàn chuỗi rạp LoraFilm.</p>
      </div>

      {/* 2. Card Grid Assembly Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
        
        {/* CARD A: Cấu Hình Vận Hành Lịch Chiếu */}
        <div className="bg-zinc-900/60 border border-zinc-800 p-6 rounded-2xl shadow-xl flex flex-col gap-5">
          <div className="flex items-center gap-2.5 pb-2 border-b border-zinc-800/60">
            <Clock className="w-5 h-5 text-amber-500" />
            <h2 className="text-sm font-bold text-zinc-200 uppercase tracking-wider">Cấu Hình Vận Hành Lịch Chiếu</h2>
          </div>

          <div className="space-y-1.5">
            <label className="text-zinc-400 text-[10px] font-black uppercase tracking-wider block">Thời gian trễ tối đa cho phép</label>
            <div className="relative flex items-center">
              <input
                type="number"
                value={maxDelay}
                onChange={(e) => setMaxDelay(parseInt(e.target.value) || 0)}
                className="w-full bg-zinc-950 border border-zinc-800 focus:border-amber-500 rounded-xl py-3 pl-4 pr-16 text-xs text-zinc-100 outline-none transition-colors font-mono"
              />
              <span className="absolute right-4 text-xs font-bold text-zinc-500 border-l border-zinc-800/60 pl-3">phút</span>
            </div>
          </div>

          <div className="space-y-1.5 flex items-center justify-between bg-zinc-950/40 p-4 rounded-xl border border-zinc-800/50 mt-1.5">
            <div>
              <label className="text-zinc-300 text-xs font-bold block">Tự động đóng suất chiếu</label>
              <span className="text-[10px] text-zinc-500 block mt-0.5">Đóng phòng vé sau khi đạt giới hạn thời gian trễ</span>
            </div>
            <button
              type="button"
              onClick={() => setAutoClose(!autoClose)}
              className={`w-11 h-6 flex items-center rounded-full p-1 cursor-pointer transition-colors duration-300 outline-none ${
                autoClose ? 'bg-amber-500' : 'bg-zinc-800'
              }`}
            >
              <div
                className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform duration-300 ${
                  autoClose ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>
        </div>

        {/* CARD B: Ma Trận Phụ Thu & Hệ Số Giá Vé */}
        <div className="bg-zinc-900/60 border border-zinc-800 p-6 rounded-2xl shadow-xl flex flex-col gap-5">
          <div className="flex items-center gap-2.5 pb-2 border-b border-zinc-800/60">
            <Settings className="w-5 h-5 text-amber-500" />
            <h2 className="text-sm font-bold text-zinc-200 uppercase tracking-wider">Ma Trận Phụ Thu & Hệ Số Giá Vé</h2>
          </div>

          <div className="space-y-1.5">
            <label className="text-zinc-400 text-[10px] font-black uppercase tracking-wider block">Phụ thu suất chiếu đặc biệt (IMAX)</label>
            <div className="relative flex items-center">
              <input
                type="number"
                value={imaxSurcharge}
                onChange={(e) => setImaxSurcharge(parseInt(e.target.value) || 0)}
                className="w-full bg-zinc-950 border border-zinc-800 focus:border-amber-500 rounded-xl py-3 pl-4 pr-16 text-xs text-zinc-100 outline-none transition-colors font-mono"
              />
              <span className="absolute right-4 text-[10px] font-black text-zinc-500 border-l border-zinc-800/60 pl-3">VNĐ</span>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-zinc-400 text-[10px] font-black uppercase tracking-wider block">Hệ số vé cuối tuần (T6-CN)</label>
            <div className="relative flex items-center">
              <input
                type="number"
                step="0.1"
                value={weekendMultiplier}
                onChange={(e) => setWeekendMultiplier(parseFloat(e.target.value) || 0)}
                className="w-full bg-zinc-950 border border-zinc-800 focus:border-amber-500 rounded-xl py-3 pl-4 pr-12 text-xs text-zinc-100 outline-none transition-colors font-mono"
              />
              <span className="absolute right-4 text-xs font-bold text-zinc-500 border-l border-zinc-800/60 pl-3">x</span>
            </div>
          </div>

          {/* Pricing Simulator */}
          <div className="bg-zinc-950/80 border border-zinc-800/60 rounded-xl p-4 mt-2 select-none">
            <span className="text-[10px] font-mono font-bold tracking-wider text-zinc-500 uppercase block mb-2">BẢNG GIẢ LẬP GIÁ VÉ THỰC TẾ (PREVIEW)</span>
            <div className="flex justify-between items-center text-xs text-zinc-400 py-1.5 border-b border-zinc-900">
              <span>Vé Standard ngày thường:</span>
              <span className="font-mono text-zinc-200 font-semibold">80.000 đ</span>
            </div>
            <div className="flex justify-between items-center text-xs text-zinc-400 py-1.5">
              <span>Vé VIP Cuối Tuần + Phòng IMAX:</span>
              <span className="font-mono text-amber-400 font-bold text-sm">
                {((100000 * parseFloat(weekendMultiplier || 1)) + parseFloat(imaxSurcharge || 0)).toLocaleString('vi-VN')} đ
              </span>
            </div>
          </div>
        </div>

      </div>

      {/* 3. Master Submission Ribbon Layer */}
      <div className="flex justify-end pt-4 border-t border-zinc-900">
        <button
          onClick={handleSaveAll}
          className="bg-gradient-to-r from-orange-400 to-amber-500 hover:opacity-95 text-zinc-950 font-black px-6 py-3 rounded-xl text-xs uppercase tracking-widest transition-all shadow-lg shadow-amber-500/10 cursor-pointer mt-4 flex items-center gap-2"
        >
          <Save className="w-4 h-4 shrink-0" />
          <span>CẬP NHẬT TOÀN BỘ CẤU HÌNH</span>
        </button>
      </div>
    </div>
  );
}
