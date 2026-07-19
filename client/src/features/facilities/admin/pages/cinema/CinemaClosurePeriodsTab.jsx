import { useState, useEffect } from 'react';
import { PlusCircle, CalendarX2 } from 'lucide-react';
import useClosurePeriods from '../../hooks/useClosurePeriods';
import { LoadingState, ErrorState, EmptyState } from '@/components/common/ui/uiKit';
import { formatDateTime } from '@/utils/formatters';
import { useOutletContext } from 'react-router-dom';

export default function CinemaClosurePeriodsTab({ cinemaPublicId, triggerToast }) {
  const { triggerConfirm } = useOutletContext() || {};
  const {
    closurePeriods,
    isLoading,
    error,
    fetchClosurePeriods,
    createClosurePeriod,
    cancelClosurePeriod
  } = useClosurePeriods(cinemaPublicId, triggerToast);

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    reason: 'MAINTENANCE'
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchClosurePeriods();
  }, [fetchClosurePeriods]);

  const openAddForm = () => {
    setFormData({ startTime: '', endTime: '', reason: 'MAINTENANCE' });
    setIsFormOpen(true);
  };

  const closeForm = () => {
    setIsFormOpen(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate end > start
    const start = new Date(formData.startTime);
    const end = new Date(formData.endTime);
    if (end <= start) {
      triggerToast?.('Thời gian kết thúc phải lớn hơn thời gian bắt đầu', 'error');
      return;
    }

    setIsSubmitting(true);
    const success = await createClosurePeriod({
      startTime: start.toISOString(),
      endTime: end.toISOString(),
      reason: formData.reason
    });
    
    setIsSubmitting(false);
    if (success) closeForm();
  };

  const handleCancel = async (id) => {
    const shouldCancel = triggerConfirm
      ? await triggerConfirm('Bạn có chắc muốn hủy lịch đóng cửa này? Rạp sẽ có thể mở cửa lại trong khoảng thời gian này.')
      : window.confirm('Bạn có chắc muốn hủy lịch đóng cửa này? Rạp sẽ có thể mở cửa lại trong khoảng thời gian này.');
      
    if (shouldCancel) {
      await cancelClosurePeriod(id);
    }
  };

  return (
    <div className="space-y-6 pb-20">
      <div className="flex justify-between items-center bg-zinc-900/30 border border-zinc-800 p-5 rounded-2xl">
        <div>
          <h2 className="text-sm font-black text-zinc-50 uppercase tracking-wider">LỊCH ĐÓNG CỬA</h2>
          <p className="text-xs text-zinc-500 mt-1">Đóng cửa toàn bộ rạp để bảo trì, nghỉ lễ, hoặc sự cố.</p>
        </div>
        <button
          onClick={openAddForm}
          className="flex items-center gap-2 bg-zinc-800 hover:bg-zinc-700 text-white px-4 py-2 rounded-xl text-xs font-bold transition-colors border border-zinc-700"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Thêm Lịch Đóng Cửa</span>
        </button>
      </div>

      <div className="bg-zinc-900/20 border border-zinc-900 rounded-3xl overflow-hidden shadow-2xl relative">
        {isLoading && <LoadingState message="Đang tải danh sách đóng cửa..." />}
        {!isLoading && error && <ErrorState message={error} onRetry={fetchClosurePeriods} />}
        
        {!isLoading && !error && closurePeriods.length === 0 ? (
          <EmptyState message="Không có lịch đóng cửa nào" onAction={openAddForm} actionLabel="Thêm mới" />
        ) : (
          !isLoading && !error && (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-zinc-900 text-[10px] font-black text-zinc-500 uppercase tracking-wider bg-zinc-950/40">
                    <th className="py-4 px-6">Bắt đầu</th>
                    <th className="py-4 px-6">Kết thúc</th>
                    <th className="py-4 px-6">Lý do</th>
                    <th className="py-4 px-6">Trạng thái</th>
                    <th className="py-4 px-6 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-900/60 text-xs font-semibold">
                  {closurePeriods.map((cp) => (
                    <tr key={cp.id} className="hover:bg-zinc-900/10 transition-colors">
                      <td className="py-4 px-6 font-mono text-zinc-300">
                        {formatDateTime(cp.startTime)}
                      </td>
                      <td className="py-4 px-6 font-mono text-zinc-300">
                        {formatDateTime(cp.endTime)}
                      </td>
                      <td className="py-4 px-6 font-mono text-amber-500">{cp.reason}</td>
                      <td className="py-4 px-6">
                        {cp.status === 'ACTIVE' 
                          ? <span className="text-emerald-400 bg-emerald-400/10 px-2 py-1 rounded text-[10px] uppercase font-bold tracking-wider border border-emerald-500/20">Hoạt động</span>
                          : <span className="text-zinc-500 bg-zinc-800 px-2 py-1 rounded text-[10px] uppercase font-bold tracking-wider border border-zinc-700">Đã hủy</span>}
                      </td>
                      <td className="py-4 px-6 text-right">
                        {cp.status === 'ACTIVE' && (
                          <button
                            onClick={() => handleCancel(cp.id)}
                            className="text-[10px] bg-zinc-950 border border-zinc-800 hover:border-red-500 text-zinc-400 hover:text-red-400 px-3 py-1.5 rounded-lg uppercase tracking-wider font-bold transition-colors"
                          >
                            Hủy Bỏ
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
      </div>

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-zinc-950 border border-zinc-800 rounded-3xl w-full max-w-lg shadow-2xl p-6">
            <h2 className="text-lg font-black uppercase tracking-wider mb-6 text-zinc-50 flex items-center gap-2">
              <CalendarX2 className="w-5 h-5 text-brand-orange" />
              Thêm Lịch Đóng Cửa
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
                  Lý Do Đóng Cửa
                </label>
                <select
                  value={formData.reason}
                  onChange={(e) => setFormData({...formData, reason: e.target.value})}
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-orange outline-none transition-colors cursor-pointer text-zinc-200"
                >
                  <option value="MAINTENANCE">Bảo Trì / Sửa Chữa</option>
                  <option value="HOLIDAY">Nghỉ Lễ</option>
                  <option value="EMERGENCY">Sự Cố Khẩn Cấp</option>
                  <option value="PRIVATE_EVENT">Sự Kiện Riêng Tư</option>
                </select>
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
                  {isSubmitting ? 'ĐANG LƯU...' : 'LƯU LỊCH TRÌNH'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
