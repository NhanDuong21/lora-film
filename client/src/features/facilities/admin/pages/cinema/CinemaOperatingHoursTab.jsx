import { useState, useEffect } from 'react';
import { Save, Clock } from 'lucide-react';

const DAYS_OF_WEEK = [
  { id: 1, name: 'Thứ Hai', code: 'T2' },
  { id: 2, name: 'Thứ Ba', code: 'T3' },
  { id: 3, name: 'Thứ Tư', code: 'T4' },
  { id: 4, name: 'Thứ Năm', code: 'T5' },
  { id: 5, name: 'Thứ Sáu', code: 'T6' },
  { id: 6, name: 'Thứ Bảy', code: 'T7' },
  { id: 7, name: 'Chủ Nhật', code: 'CN' },
];

const normalizeTimeForApi = (value) => {
  if (!value) return null;
  if (value === "24:00" || value === "24:00:00") {
    return "23:59:59";
  }
  return value.length === 5 ? `${value}:00` : value;
};

export default function CinemaOperatingHoursTab({ cinema, onUpdate, triggerToast }) {
  const [hours, setHours] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    // Initialize hours, either from cinema data or defaults
    const currentHours = cinema?.operatingHours || [];
    
    const initializedHours = DAYS_OF_WEEK.map(day => {
      const existing = currentHours.find(h => h.dayOfWeek === day.id);
      return {
        dayOfWeek: day.id,
        openTime: existing?.openTime ? existing.openTime.substring(0, 5) : '08:00',
        closeTime: existing?.closeTime ? existing.closeTime.substring(0, 5) : '23:30',
        isClosed: existing ? existing.isClosed : false
      };
    });
    
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setHours(initializedHours);
  }, [cinema]);

  const handleChange = (dayId, field, value) => {
    setHours(prev => prev.map(h => {
      if (h.dayOfWeek === dayId) {
        return { ...h, [field]: value };
      }
      return h;
    }));
  };

  const applyMonToFri = () => {
    const monday = hours.find(h => h.dayOfWeek === 1);
    if (!monday) return;
    
    setHours(prev => prev.map(h => {
      if (h.dayOfWeek >= 2 && h.dayOfWeek <= 5) {
        return {
          ...h,
          openTime: monday.openTime,
          closeTime: monday.closeTime,
          isClosed: monday.isClosed
        };
      }
      return h;
    }));
    triggerToast?.('Đã sao chép lịch Thứ Hai cho T2-T6');
  };

  const applyMonToAll = () => {
    const monday = hours.find(h => h.dayOfWeek === 1);
    if (!monday) return;
    
    setHours(prev => prev.map(h => {
      if (h.dayOfWeek !== 1) {
        return {
          ...h,
          openTime: monday.openTime,
          closeTime: monday.closeTime,
          isClosed: monday.isClosed
        };
      }
      return h;
    }));
    triggerToast?.('Đã sao chép lịch Thứ Hai cho tất cả các ngày');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    
    // Format payload
    const payload = hours.map(h => ({
      dayOfWeek: h.dayOfWeek,
      openTime: h.isClosed ? null : normalizeTimeForApi(h.openTime),
      closeTime: h.isClosed ? null : normalizeTimeForApi(h.closeTime),
      isClosed: h.isClosed
    }));
    
    await onUpdate(payload);
    setIsSubmitting(false);
  };

  return (
    <div className="max-w-4xl space-y-6 pb-20">
      <div className="bg-zinc-900/30 border border-zinc-800 rounded-2xl p-6">
        <div className="flex justify-between items-start mb-6">
          <div>
            <h2 className="text-sm font-black text-brand-coral uppercase tracking-wider mb-2">Giờ Hoạt Động Hàng Tuần</h2>
            <p className="text-xs text-zinc-500">Thiết lập thời gian mở và đóng cửa cho từng ngày trong tuần.</p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={applyMonToFri}
              className="text-[10px] bg-zinc-950 hover:bg-zinc-800 text-brand-coral border border-zinc-800 px-4 py-2 rounded-xl font-bold uppercase tracking-wider transition-colors"
            >
              Áp Dụng T2-T6
            </button>
            <button
              type="button"
              onClick={applyMonToAll}
              className="text-[10px] bg-zinc-950 hover:bg-zinc-800 text-brand-coral border border-zinc-800 px-4 py-2 rounded-xl font-bold uppercase tracking-wider transition-colors"
            >
              Áp Dụng Tất Cả
            </button>
          </div>
        </div>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            {hours.map((h) => {
              const day = DAYS_OF_WEEK.find(d => d.id === h.dayOfWeek);
              return (
                <div 
                  key={h.dayOfWeek}
                  className={`flex flex-col md:flex-row items-center gap-4 p-4 rounded-xl border transition-colors ${
                    h.isClosed 
                      ? 'bg-zinc-900/20 border-zinc-800/50' 
                      : 'bg-zinc-900/50 border-zinc-800'
                  }`}
                >
                  <div className="w-full md:w-32 shrink-0">
                    <span className={`text-sm font-black uppercase tracking-widest ${
                      h.dayOfWeek === 7 ? 'text-amber-500' : 
                      h.dayOfWeek === 6 ? 'text-brand-coral' : 'text-zinc-300'
                    }`}>
                      {day?.name}
                    </span>
                  </div>
                  
                  <div className="flex items-center gap-4 flex-1 w-full">
                    <div className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        checked={h.isClosed}
                        onChange={(e) => handleChange(h.dayOfWeek, 'isClosed', e.target.checked)}
                        className="w-4 h-4 bg-zinc-900 border-zinc-700 rounded accent-brand-coral"
                      />
                      <span className="text-xs font-bold text-zinc-400 uppercase tracking-widest">Đóng Cửa</span>
                    </div>

                    {!h.isClosed && (
                      <div className="flex items-center gap-3 flex-1 justify-end md:justify-start">
                        <div className="flex items-center gap-2">
                          <Clock className="w-4 h-4 text-zinc-500 hidden md:block" />
                          <input
                            type="time"
                            max="23:59"
                            value={h.openTime}
                            onChange={(e) => handleChange(h.dayOfWeek, 'openTime', e.target.value)}
                            required={!h.isClosed}
                            className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-xs text-zinc-200 outline-none focus:border-brand-coral font-mono"
                          />
                        </div>
                        <span className="text-zinc-600 font-bold">-</span>
                        <div className="flex items-center gap-2">
                          <input
                            type="time"
                            max="23:59"
                            value={h.closeTime}
                            onChange={(e) => handleChange(h.dayOfWeek, 'closeTime', e.target.value)}
                            required={!h.isClosed}
                            className="bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-xs text-zinc-200 outline-none focus:border-brand-coral font-mono"
                          />
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
          
          <div className="flex justify-end pt-6">
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-2 bg-brand-coral hover:bg-opacity-90 text-white px-8 py-3 rounded-xl font-bold uppercase tracking-wider text-xs transition-colors shadow-lg shadow-brand-coral/20 disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              {isSubmitting ? 'ĐANG LƯU...' : 'LƯU THAY ĐỔI'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
