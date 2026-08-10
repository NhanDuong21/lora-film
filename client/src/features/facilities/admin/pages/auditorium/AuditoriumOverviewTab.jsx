import { useMemo, useState } from 'react';
import { CheckCircle2, Edit3, Save, ShieldAlert, X } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import RoomForm from '@/features/facilities/admin/components/RoomForm';
import {
  getAuditoriumStatus,
  SCREEN_TYPE_LABELS,
  SOUND_TYPE_LABELS,
} from '@/features/facilities/admin/utils/facilityPresentation';

const STATUS_ACTIONS = {
  DRAFT: [
    {
      target: 'ACTIVE',
      label: 'Đưa vào phục vụ',
      description: 'Cho phép phòng tham gia xếp lịch và bán vé.',
      requiresLayout: true,
      tone: 'primary',
    },
    {
      target: 'INACTIVE',
      label: 'Tạm dừng thiết lập',
      description: 'Giữ lại dữ liệu nhưng không cho phòng tham gia vận hành.',
    },
  ],
  ACTIVE: [
    {
      target: 'MAINTENANCE',
      label: 'Chuyển sang bảo trì',
      description: 'Tạm ngừng phục vụ để xử lý thiết bị hoặc cơ sở vật chất.',
      tone: 'warning',
    },
    {
      target: 'INACTIVE',
      label: 'Tạm ngừng phòng',
      description: 'Ngừng xếp lịch và bán vé cho đến khi được mở lại.',
      tone: 'danger',
    },
  ],
  MAINTENANCE: [
    {
      target: 'ACTIVE',
      label: 'Hoàn tất bảo trì',
      description: 'Đưa phòng trở lại phục vụ.',
      requiresLayout: true,
      tone: 'primary',
    },
    {
      target: 'INACTIVE',
      label: 'Tạm ngừng dài hạn',
      description: 'Kết thúc bảo trì nhưng chưa mở lại phòng.',
    },
  ],
  INACTIVE: [
    {
      target: 'DRAFT',
      label: 'Chuyển về thiết lập',
      description: 'Cho phép chỉnh sửa lại sơ đồ ghế trước khi mở phòng.',
    },
    {
      target: 'ACTIVE',
      label: 'Mở lại phòng',
      description: 'Đưa phòng trở lại phục vụ ngay.',
      requiresLayout: true,
      tone: 'primary',
    },
  ],
};

export default function AuditoriumOverviewTab({
  auditorium,
  onUpdate,
  onChangeStatus,
}) {
  const { triggerConfirm } = useOutletContext() || {};
  const [isEditing, setIsEditing] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [roomName, setRoomName] = useState(auditorium?.auditoriumName || '');
  const [screenType, setScreenType] = useState(auditorium?.screenType || 'STANDARD');
  const [soundType, setSoundType] = useState(auditorium?.soundType || 'STANDARD');
  const [cleaningBuffer, setCleaningBuffer] = useState(
    auditorium?.cleaningBufferMinutes ?? 15,
  );

  const computedCapacity = useMemo(
    () => (auditorium?.rows || []).reduce(
      (total, row) => total + (row.seats || []).filter(
        (seat) => seat.status !== 'INACTIVE',
      ).length,
      0,
    ),
    [auditorium],
  );

  const status = auditorium?.auditoriumStatus || 'DRAFT';
  const statusPresentation = getAuditoriumStatus(status);
  const checks = [
    { label: 'Đã đặt tên phòng', complete: Boolean(roomName.trim()) },
    { label: 'Đã chọn công nghệ chiếu và âm thanh', complete: Boolean(screenType && soundType) },
    { label: 'Đã thiết lập sơ đồ ghế', complete: computedCapacity > 0 },
    { label: 'Đã có thời gian dọn phòng', complete: cleaningBuffer >= 0 },
  ];
  const completedChecks = checks.filter((item) => item.complete).length;

  const handleSave = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    const success = await onUpdate({
      name: roomName.trim(),
      screenType,
      soundType,
      capacity: computedCapacity,
      cleaningBufferMinutes: cleaningBuffer,
      status,
    }, 'Đã lưu cấu hình phòng chiếu');
    setIsSubmitting(false);
    if (success) setIsEditing(false);
  };

  const handleStatusAction = async (action) => {
    if (action.requiresLayout && computedCapacity === 0) return;
    const confirmed = await triggerConfirm?.({
      title: action.label,
      message:
        `${action.description} Hệ thống hiện chưa cung cấp số suất chiếu và đơn đặt vé bị ảnh hưởng tại màn hình này. ` +
        'Hãy kiểm tra Lịch vận hành và Đơn đặt vé trước khi xác nhận.',
      confirmLabel: action.label,
      cancelLabel: 'Quay lại kiểm tra',
      tone: action.tone === 'danger' ? 'danger' : 'warning',
    });
    if (!confirmed) return;
    setIsSubmitting(true);
    await onChangeStatus(action.target, `Đã cập nhật trạng thái: ${getAuditoriumStatus(action.target).label}`);
    setIsSubmitting(false);
  };

  return (
    <div className="mx-auto max-w-5xl space-y-6 pb-20">
      <section className="grid gap-4 md:grid-cols-3">
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5 md:col-span-2">
          <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
            Tình trạng vận hành
          </p>
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <span className={`rounded-lg border px-3 py-1.5 text-xs font-black ${statusPresentation.className}`}>
              {statusPresentation.label}
            </span>
            <p className="text-sm text-zinc-400">{statusPresentation.description}</p>
          </div>
        </div>
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5">
          <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
            Mức độ hoàn thiện
          </p>
          <p className="mt-2 text-2xl font-black text-white">
            {completedChecks}/{checks.length}
          </p>
          <p className="text-xs text-zinc-500">hạng mục đã sẵn sàng</p>
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/20 p-6">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-base font-black uppercase text-white">Thông tin phòng</h2>
            <p className="mt-1 text-xs text-zinc-500">
              {isEditing ? 'Đang chỉnh sửa cấu hình phục vụ.' : 'Chế độ xem an toàn, không làm thay đổi dữ liệu.'}
            </p>
          </div>
          {!isEditing && (
            <button
              type="button"
              onClick={() => setIsEditing(true)}
              className="flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-xs font-bold text-white hover:border-brand-orange"
            >
              <Edit3 className="h-4 w-4" />
              Chỉnh sửa thông tin
            </button>
          )}
        </div>

        {isEditing ? (
          <form onSubmit={handleSave} className="space-y-5">
            <RoomForm
              roomName={roomName}
              setRoomName={setRoomName}
              screenType={screenType}
              setScreenType={setScreenType}
              soundType={soundType}
              setSoundType={setSoundType}
              cleaningBuffer={cleaningBuffer}
              setCleaningBuffer={setCleaningBuffer}
              capacity={computedCapacity}
            />
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setIsEditing(false)}
                className="flex items-center gap-2 rounded-xl border border-zinc-700 px-5 py-3 text-xs font-bold text-zinc-300"
              >
                <X className="h-4 w-4" />
                Bỏ thay đổi
              </button>
              <button
                type="submit"
                disabled={isSubmitting || !roomName.trim()}
                className="flex items-center gap-2 rounded-xl bg-brand-orange px-6 py-3 text-xs font-black text-white disabled:opacity-50"
              >
                <Save className="h-4 w-4" />
                {isSubmitting ? 'Đang lưu...' : 'Lưu thông tin'}
              </button>
            </div>
          </form>
        ) : (
          <dl className="grid gap-4 md:grid-cols-2">
            {[
              ['Tên phòng', auditorium.auditoriumName],
              ['Công nghệ màn hình', SCREEN_TYPE_LABELS[auditorium.screenType] || 'Chưa xác định'],
              ['Hệ thống âm thanh', SOUND_TYPE_LABELS[auditorium.soundType] || 'Chưa xác định'],
              ['Thời gian dọn phòng', `${auditorium.cleaningBufferMinutes ?? 0} phút`],
              ['Sức chứa', `${computedCapacity} ghế`],
            ].map(([label, value]) => (
              <div key={label} className="rounded-xl border border-zinc-800 bg-zinc-950/50 p-4">
                <dt className="text-[10px] font-black uppercase tracking-widest text-zinc-500">{label}</dt>
                <dd className="mt-2 text-sm font-bold text-white">{value}</dd>
              </div>
            ))}
          </dl>
        )}
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/20 p-6">
          <h2 className="text-sm font-black uppercase text-white">Checklist trước khi mở phòng</h2>
          <div className="mt-4 space-y-3">
            {checks.map((item) => (
              <div key={item.label} className="flex items-center gap-3 text-sm">
                <CheckCircle2 className={`h-5 w-5 ${item.complete ? 'text-emerald-400' : 'text-zinc-700'}`} />
                <span className={item.complete ? 'text-zinc-200' : 'text-zinc-500'}>{item.label}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/20 p-6">
          <h2 className="text-sm font-black uppercase text-white">Tác vụ vận hành</h2>
          <div className="mt-3 flex gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-100">
            <ShieldAlert className="h-5 w-5 shrink-0 text-amber-400" />
            <p>Trước khi ngừng hoặc bảo trì phòng, hãy kiểm tra lịch chiếu và các đơn đã đặt.</p>
          </div>
          <div className="mt-4 space-y-3">
            {(STATUS_ACTIONS[status] || []).map((action) => {
              const disabled = isSubmitting || (action.requiresLayout && computedCapacity === 0);
              return (
                <button
                  key={action.target}
                  type="button"
                  disabled={disabled}
                  onClick={() => handleStatusAction(action)}
                  className={`w-full rounded-xl border p-4 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
                    action.tone === 'primary'
                      ? 'border-emerald-500/30 bg-emerald-500/5 hover:bg-emerald-500/10'
                      : action.tone === 'danger'
                        ? 'border-red-500/30 bg-red-500/5 hover:bg-red-500/10'
                        : 'border-zinc-700 bg-zinc-950/50 hover:border-brand-orange'
                  }`}
                >
                  <span className="text-sm font-black text-white">{action.label}</span>
                  <span className="mt-1 block text-xs text-zinc-500">
                    {action.requiresLayout && computedCapacity === 0
                      ? 'Cần hoàn thiện sơ đồ ghế trước.'
                      : action.description}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </section>
    </div>
  );
}
