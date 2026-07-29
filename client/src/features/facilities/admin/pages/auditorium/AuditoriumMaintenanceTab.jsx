import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  CalendarClock,
  CalendarX2,
  CheckCircle2,
  PlusCircle,
} from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import { EmptyState, ErrorState, LoadingState } from '@/components/common/ui/uiKit';

const STATUS_LABELS = {
  ACTIVE: 'Đã lên lịch',
  CANCELLED: 'Đã hủy',
};

function formatDateTime(value) {
  if (!value) return 'Chưa xác định';
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
}

export default function AuditoriumMaintenanceTab({
  roomId,
  auditorium,
  triggerToast,
}) {
  const { triggerConfirm } = useOutletContext() || {};
  const [windows, setWindows] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [acknowledged, setAcknowledged] = useState(false);
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    reason: 'Bảo trì thiết bị phòng chiếu',
  });

  const fetchWindows = useCallback(async () => {
    if (!roomId) return;
    setIsLoading(true);
    setError(null);
    try {
      const response = await adminRoomService.getMaintenanceWindows(roomId);
      setWindows(response?.success && Array.isArray(response.data) ? response.data : []);
    } catch (requestError) {
      setError(
        requestError.response?.data?.message
          || requestError.message
          || 'Không thể tải lịch đóng phòng',
      );
    } finally {
      setIsLoading(false);
    }
  }, [roomId]);

  useEffect(() => {
    // Load the remote maintenance calendar when the selected room changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchWindows();
  }, [fetchWindows]);

  const activeWindows = useMemo(
    () => windows.filter((window) => window.status === 'ACTIVE'),
    [windows],
  );

  const openForm = () => {
    setFormData({
      startTime: '',
      endTime: '',
      reason: 'Bảo trì thiết bị phòng chiếu',
    });
    setAcknowledged(false);
    setIsFormOpen(true);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const start = new Date(formData.startTime);
    const end = new Date(formData.endTime);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start >= end) {
      triggerToast?.('Thời gian kết thúc phải sau thời gian bắt đầu', 'error');
      return;
    }
    if (!acknowledged) {
      triggerToast?.('Vui lòng xác nhận đã kiểm tra phạm vi ảnh hưởng', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await adminRoomService.createMaintenanceWindow(roomId, {
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        reason: formData.reason.trim(),
      });
      if (response?.success) {
        triggerToast?.('Đã lên lịch đóng phòng để bảo trì');
        setIsFormOpen(false);
        await fetchWindows();
      }
    } catch (requestError) {
      triggerToast?.(
        requestError.response?.data?.message
          || requestError.message
          || 'Không thể tạo lịch bảo trì',
        'error',
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = async (window) => {
    const confirmed = await triggerConfirm?.({
      title: 'Hủy lịch đóng phòng?',
      message:
        `Lịch của ${auditorium?.auditoriumName || 'phòng chiếu'} từ ${formatDateTime(window.startTime)} ` +
        `đến ${formatDateTime(window.endTime)} sẽ bị hủy. Phòng không tự đổi trạng thái nếu bạn đã tạm ngừng thủ công.`,
      confirmLabel: 'Hủy lịch đóng phòng',
      cancelLabel: 'Giữ nguyên lịch',
      tone: 'danger',
    });
    if (!confirmed) return;

    setIsSubmitting(true);
    try {
      const response = await adminRoomService.cancelMaintenanceWindow(window.id);
      if (response?.success) {
        triggerToast?.('Đã hủy lịch đóng phòng');
        await fetchWindows();
      }
    } catch (requestError) {
      triggerToast?.(
        requestError.response?.data?.message
          || requestError.message
          || 'Không thể hủy lịch đóng phòng',
        'error',
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-20">
      <section className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5">
          <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
            Phòng áp dụng
          </p>
          <p className="mt-2 text-lg font-black text-white">
            {auditorium?.auditoriumName || 'Phòng chiếu'}
          </p>
        </div>
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5">
          <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
            Lịch sắp tới
          </p>
          <p className="mt-2 text-2xl font-black text-brand-orange">{activeWindows.length}</p>
        </div>
        <button
          type="button"
          onClick={openForm}
          className="flex min-h-28 items-center justify-center gap-2 rounded-2xl bg-brand-orange px-5 py-4 text-sm font-black text-white shadow-lg shadow-brand-orange/10"
        >
          <PlusCircle className="h-5 w-5" />
          Lên lịch đóng phòng
        </button>
      </section>

      <section className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-5">
        <div className="flex gap-3">
          <AlertTriangle className="h-5 w-5 shrink-0 text-amber-400" />
          <div>
            <h2 className="text-sm font-black text-amber-100">
              Kiểm tra ảnh hưởng trước khi đóng phòng
            </h2>
            <p className="mt-1 text-xs leading-5 text-amber-100/70">
              API hiện chưa trả số suất chiếu và đơn đặt vé bị ảnh hưởng. Hãy mở
              Lịch vận hành và Đơn đặt vé để xử lý khách hàng trước khi lưu lịch.
              Lịch bảo trì không tự thay thế bước chuyển trạng thái phòng.
            </p>
          </div>
        </div>
      </section>

      <section className="min-h-72 overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/20">
        <div className="border-b border-zinc-800 p-5">
          <h2 className="text-sm font-black uppercase text-white">Lịch đóng phòng & bảo trì</h2>
          <p className="mt-1 text-xs text-zinc-500">
            Theo dõi thời gian, lý do và trạng thái của từng lịch vận hành.
          </p>
        </div>
        {isLoading && <LoadingState message="Đang tải lịch đóng phòng..." />}
        {!isLoading && error && <ErrorState message={error} onRetry={fetchWindows} />}
        {!isLoading && !error && windows.length === 0 && (
          <EmptyState message="Chưa có lịch đóng phòng nào" actionLabel="Lên lịch" onAction={openForm} />
        )}
        {!isLoading && !error && windows.length > 0 && (
          <div className="divide-y divide-zinc-800">
            {windows.map((window) => (
              <article key={window.id} className="grid gap-4 p-5 lg:grid-cols-[1fr_1fr_1.5fr_auto] lg:items-center">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Bắt đầu</p>
                  <p className="mt-1 text-sm font-bold text-white">{formatDateTime(window.startTime)}</p>
                </div>
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Kết thúc</p>
                  <p className="mt-1 text-sm font-bold text-white">{formatDateTime(window.endTime)}</p>
                </div>
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Lý do</p>
                  <p className="mt-1 text-sm text-zinc-300">{window.reason}</p>
                </div>
                <div className="flex items-center justify-between gap-3 lg:justify-end">
                  <span className={`rounded-lg px-3 py-1.5 text-[10px] font-black ${
                    window.status === 'ACTIVE'
                      ? 'bg-emerald-500/10 text-emerald-400'
                      : 'bg-zinc-800 text-zinc-400'
                  }`}>
                    {STATUS_LABELS[window.status] || 'Chưa xác định'}
                  </span>
                  {window.status === 'ACTIVE' && (
                    <button
                      type="button"
                      disabled={isSubmitting}
                      onClick={() => handleCancel(window)}
                      className="rounded-xl border border-red-500/30 px-4 py-2 text-xs font-bold text-red-400 hover:bg-red-500/10 disabled:opacity-50"
                    >
                      Hủy lịch
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
          <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-3xl border border-zinc-800 bg-zinc-950 p-6 shadow-2xl">
            <div className="flex items-start gap-3">
              <CalendarX2 className="mt-1 h-5 w-5 text-brand-orange" />
              <div>
                <h2 className="text-lg font-black uppercase text-white">Lên lịch đóng phòng</h2>
                <p className="mt-1 text-xs text-zinc-500">
                  Áp dụng cho {auditorium?.auditoriumName || 'phòng chiếu này'}.
                </p>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="mt-6 space-y-5">
              <div className="grid gap-4 md:grid-cols-2">
                <label className="space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                  Thời gian bắt đầu
                  <input
                    type="datetime-local"
                    required
                    value={formData.startTime}
                    onChange={(event) => setFormData((current) => ({
                      ...current,
                      startTime: event.target.value,
                    }))}
                    className="w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm font-semibold normal-case text-white outline-none focus:border-brand-orange"
                  />
                </label>
                <label className="space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                  Thời gian kết thúc
                  <input
                    type="datetime-local"
                    required
                    value={formData.endTime}
                    onChange={(event) => setFormData((current) => ({
                      ...current,
                      endTime: event.target.value,
                    }))}
                    className="w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm font-semibold normal-case text-white outline-none focus:border-brand-orange"
                  />
                </label>
              </div>
              <label className="block space-y-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                Lý do vận hành
                <input
                  type="text"
                  required
                  value={formData.reason}
                  onChange={(event) => setFormData((current) => ({
                    ...current,
                    reason: event.target.value,
                  }))}
                  className="w-full rounded-xl border border-zinc-800 bg-zinc-900 px-4 py-3 text-sm font-semibold normal-case text-white outline-none focus:border-brand-orange"
                />
              </label>

              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4">
                <div className="flex gap-3">
                  <CalendarClock className="h-5 w-5 shrink-0 text-amber-400" />
                  <div className="text-xs leading-5 text-amber-100/80">
                    <p className="font-black text-amber-100">Phạm vi ảnh hưởng dự kiến</p>
                    <ul className="mt-2 list-inside list-disc">
                      <li>01 phòng: {auditorium?.auditoriumName || 'phòng hiện tại'}</li>
                      <li>Không thể xác định số suất chiếu từ API hiện tại</li>
                      <li>Không thể xác định số đơn/khách bị ảnh hưởng từ API hiện tại</li>
                    </ul>
                  </div>
                </div>
                <label className="mt-4 flex cursor-pointer items-start gap-3 rounded-xl border border-amber-500/20 p-3 text-xs text-zinc-200">
                  <input
                    type="checkbox"
                    checked={acknowledged}
                    onChange={(event) => setAcknowledged(event.target.checked)}
                    className="mt-0.5 h-4 w-4 accent-orange-500"
                  />
                  Tôi đã kiểm tra Lịch vận hành và Đơn đặt vé, đồng thời hiểu rằng
                  cần xử lý khách hàng bị ảnh hưởng trước khi đóng phòng.
                </label>
              </div>

              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsFormOpen(false)}
                  className="flex-1 rounded-xl border border-zinc-700 py-3 text-xs font-bold text-zinc-300"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || !acknowledged}
                  className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-brand-orange py-3 text-xs font-black text-white disabled:opacity-40"
                >
                  <CheckCircle2 className="h-4 w-4" />
                  {isSubmitting ? 'Đang lưu...' : 'Xác nhận lịch đóng phòng'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
