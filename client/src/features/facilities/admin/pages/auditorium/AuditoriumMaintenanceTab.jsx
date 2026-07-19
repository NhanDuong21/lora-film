import { useState } from 'react';
import { PlusCircle, CalendarX2, Info } from 'lucide-react';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';

export default function AuditoriumMaintenanceTab({ roomId, triggerToast }) {
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    reason: 'Phòng chiếu bảo trì thiết bị'
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const openAddForm = () => {
    setFormData({ startTime: '', endTime: '', reason: 'Phòng chiếu bảo trì thiết bị' });
    setIsFormOpen(true);
  };

  const closeForm = () => {
    setIsFormOpen(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    
    try {
      const start = new Date(formData.startTime);
      const end = new Date(formData.endTime);
      
      const res = await adminRoomService.createMaintenanceWindow(roomId, {
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        reason: formData.reason
      });
      
      if (res?.success) {
        triggerToast?.('Thêm lịch bảo trì thành công!');
        closeForm();
      }
    } catch (err) {
      triggerToast?.(err.response?.data?.message || err.message || 'Thêm lịch bảo trì thất bại', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 pb-20">
      <div className="bg-amber-500/10 border border-amber-500/30 text-amber-400 p-4 rounded-2xl flex items-start gap-3 shadow-xl shadow-black/20 select-none">
        <Info className="w-5 h-5 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <h4 className="font-extrabold uppercase">CONTRACT GAP</h4>
          <p className="font-semibold text-zinc-300">
            Backend hiện hỗ trợ tạo khoảng bảo trì nhưng chưa cung cấp API <code>GET /api/admin/auditoriums/&#123;id&#125;/maintenance-windows</code> để tải danh sách. 
          </p>
          <p className="font-semibold text-zinc-300">
            Do đó, danh sách bảo trì chưa thể hiển thị đầy đủ tại đây. Bạn vẫn có thể tạo mới lịch bảo trì, nhưng sau khi tạo sẽ không thể xem lại hoặc hủy từ giao diện này.
          </p>
        </div>
      </div>

      <div className="flex justify-between items-center bg-zinc-900/30 border border-zinc-800 p-5 rounded-2xl">
        <div>
          <h2 className="text-sm font-black text-zinc-50 uppercase tracking-wider">LỊCH BẢO TRÌ PHÒNG CHIẾU</h2>
          <p className="text-xs text-zinc-500 mt-1">Tạo mới lịch bảo trì, sửa chữa định kỳ hoặc đột xuất cho phòng này.</p>
        </div>
        <button
          onClick={openAddForm}
          className="flex items-center gap-2 bg-zinc-800 hover:bg-zinc-700 text-white px-4 py-2 rounded-xl text-xs font-bold transition-colors border border-zinc-700"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Thêm Lịch Bảo Trì</span>
        </button>
      </div>

      <div className="bg-zinc-900/20 border border-zinc-900 rounded-3xl overflow-hidden shadow-2xl relative">
        <div className="flex flex-col items-center justify-center py-16 text-center space-y-3">
          <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-850 flex items-center justify-center">
            <CalendarX2 className="w-6 h-6 text-zinc-650" />
          </div>
          <div className="text-xs font-bold text-zinc-500 uppercase tracking-widest max-w-sm">
            Danh sách không khả dụng do thiếu API Backend (Contract Gap).
          </div>
        </div>
      </div>

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-zinc-950 border border-zinc-800 rounded-3xl w-full max-w-lg shadow-2xl p-6">
            <h2 className="text-lg font-black uppercase tracking-wider mb-6 text-zinc-50 flex items-center gap-2">
              <CalendarX2 className="w-5 h-5 text-brand-orange" />
              Thêm Lịch Bảo Trì
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                    Thời Gian Bắt Đầu <span className="text-brand-orange">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    required
                    value={formData.startTime}
                    onChange={(e) => setFormData({...formData, startTime: e.target.value})}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-orange outline-none transition-colors text-zinc-200"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                    Thời Gian Kết Thúc <span className="text-brand-orange">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    required
                    value={formData.endTime}
                    onChange={(e) => setFormData({...formData, endTime: e.target.value})}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-orange outline-none transition-colors text-zinc-200"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                  Lý Do
                </label>
                <input
                  type="text"
                  value={formData.reason}
                  onChange={(e) => setFormData({...formData, reason: e.target.value})}
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-orange outline-none transition-colors text-zinc-200"
                />
              </div>

              <div className="flex gap-3 pt-6">
                <button
                  type="button"
                  onClick={closeForm}
                  className="flex-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-bold py-3 rounded-xl uppercase tracking-wider text-xs transition-colors"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-1 bg-brand-orange hover:bg-opacity-90 text-white font-bold py-3 rounded-xl uppercase tracking-wider text-xs transition-colors disabled:opacity-50"
                >
                  {isSubmitting ? 'ĐANG LƯU...' : 'LƯU LỊCH BẢO TRÌ'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
