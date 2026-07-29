import { useEffect, useState } from 'react';
import { AlertTriangle, Building2, CalendarX2, Film, PlusCircle } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import { EmptyState, ErrorState, LoadingState } from '@/components/common/ui/uiKit';
import { formatDateTime } from '@/utils/formatters';
import useClosurePeriods from '../../hooks/useClosurePeriods';

const REASONS = {
  MAINTENANCE: 'Bảo trì hoặc sửa chữa',
  HOLIDAY: 'Nghỉ lễ',
  EMERGENCY: 'Sự cố khẩn cấp',
  PRIVATE_EVENT: 'Sự kiện riêng',
};

export default function CinemaClosurePeriodsTab({ cinema, triggerToast, onOpenRooms }) {
  const { triggerConfirm } = useOutletContext() || {};
  const {
    closurePeriods,
    isLoading,
    error,
    fetchClosurePeriods,
    createClosurePeriod,
    cancelClosurePeriod,
  } = useClosurePeriods(cinema.publicId, triggerToast);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [acceptedImpact, setAcceptedImpact] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    reason: 'MAINTENANCE',
  });

  useEffect(() => {
    fetchClosurePeriods();
  }, [fetchClosurePeriods]);

  const openForm = () => {
    setFormData({ startTime: '', endTime: '', reason: 'MAINTENANCE' });
    setAcceptedImpact(false);
    setIsFormOpen(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    const start = new Date(formData.startTime);
    const end = new Date(formData.endTime);
    if (end <= start) {
      triggerToast?.('Thời gian kết thúc phải sau thời gian bắt đầu.', 'error');
      return;
    }
    if (!acceptedImpact) {
      triggerToast?.('Vui lòng xác nhận đã kiểm tra phạm vi ảnh hưởng.', 'warning');
      return;
    }
    setIsSubmitting(true);
    const success = await createClosurePeriod({
      startTime: start.toISOString(),
      endTime: end.toISOString(),
      reason: formData.reason,
    });
    setIsSubmitting(false);
    if (success) setIsFormOpen(false);
  };

  const cancel = async (id) => {
    const confirmed = await triggerConfirm?.({
      title: 'Hủy lịch đóng cửa này?',
      message:
        'Cụm rạp có thể quay lại vận hành trong khoảng thời gian này. Các lịch chiếu không tự động được khôi phục.',
      confirmLabel: 'Hủy lịch đóng cửa',
      tone: 'danger',
    });
    if (confirmed) await cancelClosurePeriod(id);
  };

  return (
    <div className="space-y-6 pb-20">
      <section className="grid gap-4 md:grid-cols-2">
        <button
          type="button"
          onClick={openForm}
          className="rounded-3xl border border-orange-500/30 bg-orange-500/5 p-6 text-left transition hover:border-orange-500/60"
        >
          <Building2 className="h-6 w-6 text-orange-400" />
          <h2 className="mt-4 text-base font-black text-white">Đóng toàn bộ cụm rạp</h2>
          <p className="mt-2 text-xs leading-5 text-zinc-400">
            Dùng khi toàn bộ cụm rạp nghỉ lễ, gặp sự cố hoặc bảo trì diện rộng.
          </p>
          <span className="mt-5 inline-flex items-center gap-2 text-xs font-black text-orange-400">
            <PlusCircle className="h-4 w-4" />
            Tạo lịch đóng cửa
          </span>
        </button>
        <button
          type="button"
          onClick={onOpenRooms}
          className="rounded-3xl border border-zinc-800 bg-zinc-900/30 p-6 text-left transition hover:border-zinc-600"
        >
          <Film className="h-6 w-6 text-sky-400" />
          <h2 className="mt-4 text-base font-black text-white">Bảo trì một phòng chiếu</h2>
          <p className="mt-2 text-xs leading-5 text-zinc-400">
            Chọn phòng cần bảo trì để không làm gián đoạn các phòng vẫn hoạt động.
          </p>
          <span className="mt-5 inline-flex items-center gap-2 text-xs font-black text-sky-400">
            Mở danh sách phòng chiếu
          </span>
        </button>
      </section>

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/20">
        <div className="border-b border-zinc-800 p-5">
          <h2 className="text-sm font-black uppercase tracking-wider text-white">
            Lịch đóng cửa toàn cụm
          </h2>
          <p className="mt-1 text-xs text-zinc-500">
            Lịch đang áp dụng hoặc đã hủy vẫn được giữ để tra cứu.
          </p>
        </div>
        {isLoading && <LoadingState message="Đang tải lịch đóng cửa..." />}
        {!isLoading && error && <ErrorState message={error} onRetry={fetchClosurePeriods} />}
        {!isLoading && !error && closurePeriods.length === 0 && (
          <EmptyState message="Chưa có lịch đóng cửa toàn cụm" />
        )}
        {!isLoading && !error && closurePeriods.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-xs">
              <thead className="border-b border-zinc-800 text-[10px] uppercase tracking-wider text-zinc-500">
                <tr>
                  <th className="px-5 py-4">Thời gian</th>
                  <th className="px-5 py-4">Lý do</th>
                  <th className="px-5 py-4">Tình trạng</th>
                  <th className="px-5 py-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/70">
                {closurePeriods.map((period) => (
                  <tr key={period.id}>
                    <td className="px-5 py-4 text-zinc-300">
                      <p>{formatDateTime(period.startTime)}</p>
                      <p className="mt-1 text-zinc-500">đến {formatDateTime(period.endTime)}</p>
                    </td>
                    <td className="px-5 py-4 text-zinc-300">
                      {REASONS[period.reason] || 'Lý do vận hành khác'}
                    </td>
                    <td className="px-5 py-4">
                      <span className={period.status === 'ACTIVE' ? 'text-amber-400' : 'text-zinc-500'}>
                        {period.status === 'ACTIVE' ? 'Đã lên lịch' : 'Đã hủy'}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-right">
                      {period.status === 'ACTIVE' && (
                        <button
                          type="button"
                          onClick={() => cancel(period.id)}
                          className="rounded-xl border border-zinc-700 px-3 py-2 font-bold text-zinc-300 hover:border-red-500/50 hover:text-red-300"
                        >
                          Hủy lịch đóng cửa
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
          <form
            onSubmit={submit}
            className="w-full max-w-xl rounded-3xl border border-zinc-800 bg-zinc-950 p-6 shadow-2xl"
          >
            <div className="flex items-center gap-3">
              <CalendarX2 className="h-6 w-6 text-orange-400" />
              <div>
                <h2 className="text-lg font-black text-white">Lên lịch đóng toàn bộ cụm rạp</h2>
                <p className="mt-1 text-xs text-zinc-500">{cinema.name}</p>
              </div>
            </div>

            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              <DateField
                label="Bắt đầu"
                value={formData.startTime}
                onChange={(value) => setFormData({ ...formData, startTime: value })}
              />
              <DateField
                label="Kết thúc"
                value={formData.endTime}
                onChange={(value) => setFormData({ ...formData, endTime: value })}
              />
            </div>
            <label className="mt-4 block">
              <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Lý do</span>
              <select
                value={formData.reason}
                onChange={(event) => setFormData({ ...formData, reason: event.target.value })}
                className="mt-2 w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-500"
              >
                {Object.entries(REASONS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>

            <div className="mt-5 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4">
              <div className="flex gap-3">
                <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-400" />
                <div>
                  <p className="text-sm font-bold text-amber-200">Phạm vi ảnh hưởng</p>
                  <p className="mt-1 text-xs leading-5 text-zinc-400">
                    Toàn bộ {cinema.activeAuditoriums?.length || 0} phòng của cụm sẽ ngừng phục vụ
                    trong thời gian trên. API chưa cung cấp số suất chiếu và đơn đặt vé bị ảnh hưởng;
                    cần kiểm tra Lịch vận hành và Đơn đặt vé trước khi lưu.
                  </p>
                </div>
              </div>
              <label className="mt-4 flex cursor-pointer items-start gap-3 text-xs text-zinc-300">
                <input
                  type="checkbox"
                  checked={acceptedImpact}
                  onChange={(event) => setAcceptedImpact(event.target.checked)}
                  className="mt-0.5"
                />
                Tôi đã kiểm tra lịch chiếu và các đơn cần xử lý.
              </label>
            </div>

            <div className="mt-6 flex gap-3">
              <button
                type="button"
                onClick={() => setIsFormOpen(false)}
                className="flex-1 rounded-xl border border-zinc-800 py-3 text-xs font-black text-zinc-300"
              >
                Quay lại
              </button>
              <button
                type="submit"
                disabled={isSubmitting || !acceptedImpact}
                className="flex-1 rounded-xl bg-orange-500 py-3 text-xs font-black text-white disabled:opacity-40"
              >
                {isSubmitting ? 'Đang lưu...' : 'Xác nhận lịch đóng cửa'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

function DateField({ label, value, onChange }) {
  return (
    <label>
      <span className="text-[10px] font-black uppercase tracking-wider text-zinc-500">{label}</span>
      <input
        type="datetime-local"
        required
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition focus:border-orange-500"
      />
    </label>
  );
}
