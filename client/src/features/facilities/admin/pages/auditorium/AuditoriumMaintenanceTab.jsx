import { useState, useEffect, useCallback } from 'react';
import { PlusCircle, CalendarX2, AlertTriangle, XCircle } from 'lucide-react';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import { LoadingState, ErrorState, EmptyState } from '@/components/common/ui/uiKit';

export default function AuditoriumMaintenanceTab({ roomId, triggerToast }) {
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    reason: 'Phòng chiếu bảo trì thiết bị'
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [windows, setWindows] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelingId, setCancelingId] = useState(null);

  const fetchWindows = useCallback(async (isInitialLoad = false) => {
    if (!roomId) return;
    if (!isInitialLoad) {
        setIsLoading(true);
        setError(null);
    }
    try {
      const res = await adminRoomService.getMaintenanceWindows(roomId);
      if (res?.success) {
        setWindows(res.data);
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Không thể tải danh sách bảo trì');
    } finally {
      setIsLoading(false);
    }
  }, [roomId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setError(null);
    fetchWindows(true);
  }, [fetchWindows]);

  const openAddForm = () => {
    setFormData({ startTime: '', endTime: '', reason: 'Phòng chiếu bảo trì thiết bị' });
    setIsFormOpen(true);
  };

  const closeForm = () => {
    setIsFormOpen(false);
  };

  const confirmCancel = (id) => {
    setCancelingId(id);
  };

  const handleCancelWindow = async () => {
    if (!cancelingId) return;
    setIsSubmitting(true);
    try {
      const res = await adminRoomService.cancelMaintenanceWindow(cancelingId);
      if (res?.success) {
        triggerToast?.('Hủy lịch bảo trì thành công!');
        fetchWindows();
      }
    } catch (err) {
      triggerToast?.(err.response?.data?.message || err.message || 'Hủy lịch thất bại', 'error');
    } finally {
      setIsSubmitting(false);
      setCancelingId(null);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Intl.DateTimeFormat('vi-VN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit'
    }).format(new Date(dateStr));
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
        fetchWindows();
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

      <div className="bg-zinc-900/20 border border-zinc-900 rounded-3xl overflow-hidden shadow-2xl relative min-h-[300px]">
        {isLoading && <LoadingState message="Đang tải lịch bảo trì..." />}
        {!isLoading && error && <ErrorState message={error} onRetry={fetchWindows} />}
        {!isLoading && !error && windows.length === 0 ? (
          <EmptyState message="Không có lịch bảo trì nào" actionLabel="Thêm mới" onAction={openAddForm} />
        ) : (
          !isLoading && !error && (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-zinc-900 text-[10px] font-black text-zinc-500 uppercase tracking-wider bg-zinc-950/40">
                    <th className="py-4 px-6">ID</th>
                    <th className="py-4 px-6">Bắt Đầu</th>
                    <th className="py-4 px-6">Kết Thúc</th>
                    <th className="py-4 px-6">Lý Do</th>
                    <th className="py-4 px-6">Trạng Thái</th>
                    <th className="py-4 px-6 text-right">Thao Tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-900/60 text-xs font-semibold">
                  {windows.map((w) => (
                    <tr key={w.id} className="hover:bg-zinc-900/10 transition-colors">
                      <td className="py-4 px-6 text-zinc-500 font-mono">#{w.id}</td>
                      <td className="py-4 px-6 text-zinc-300">{formatDate(w.startTime)}</td>
                      <td className="py-4 px-6 text-zinc-300">{formatDate(w.endTime)}</td>
                      <td className="py-4 px-6 text-zinc-400">{w.reason}</td>
                      <td className="py-4 px-6">
                        {w.status === 'ACTIVE' && <span className="text-emerald-400 bg-emerald-400/10 px-2 py-1 rounded text-[10px]">Đang Áp Dụng</span>}
                        {w.status === 'CANCELLED' && <span className="text-zinc-500 bg-zinc-500/10 px-2 py-1 rounded text-[10px]">Đã Hủy</span>}
                      </td>
                      <td className="py-4 px-6 text-right">
                        {w.status === 'ACTIVE' && (
                          <button
                            onClick={() => confirmCancel(w.id)}
                            className="p-2 bg-zinc-950 border border-zinc-800 hover:border-red-500/50 text-zinc-400 hover:text-red-500 rounded-xl transition-all"
                            title="Hủy lịch"
                          >
                            <XCircle className="w-4 h-4" />
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

      {cancelingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-zinc-950 border border-zinc-800 rounded-3xl w-full max-w-sm shadow-2xl p-6 text-center space-y-4">
            <div className="w-16 h-16 mx-auto bg-red-500/10 border border-red-500/30 rounded-full flex items-center justify-center mb-2">
              <AlertTriangle className="w-8 h-8 text-red-500" />
            </div>
            <h2 className="text-lg font-black uppercase text-zinc-50">Xác Nhận Hủy Lịch</h2>
            <p className="text-sm text-zinc-400">
              Bạn có chắc chắn muốn hủy lịch bảo trì (ID: {cancelingId}) này không? Hành động này không thể hoàn tác.
            </p>
            <div className="flex gap-3 pt-4">
              <button
                onClick={() => setCancelingId(null)}
                className="flex-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-bold py-2.5 rounded-xl uppercase tracking-wider text-xs transition-colors"
              >
                Không
              </button>
              <button
                onClick={handleCancelWindow}
                disabled={isSubmitting}
                className="flex-1 bg-red-600 hover:bg-red-500 text-white font-bold py-2.5 rounded-xl uppercase tracking-wider text-xs transition-colors disabled:opacity-50 flex justify-center items-center gap-2"
              >
                {isSubmitting ? 'Đang Hủy...' : 'Có, Hủy Ngay'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
