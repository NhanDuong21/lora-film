import { useMemo, useState } from 'react';
import { Armchair, CalendarClock, CheckCircle2, Clock3, Edit3, Save, ShieldAlert, Wrench, X } from 'lucide-react';
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
      description: 'Cho phép chọn phòng khi tạo suất chiếu; mở bán vẫn là tác vụ của từng suất.',
      requiresLayout: true,
      tone: 'primary',
    },
    {
      target: 'INACTIVE',
      label: 'Tạm dừng thiết lập',
      description: 'Giữ lại dữ liệu nhưng không cho phòng tham gia vận hành.',
    },
  ],
  ACTIVE: [],
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
      target: 'ACTIVE',
      label: 'Mở lại phòng',
      description: 'Đưa phòng trở lại khả dụng cho lịch chiếu; không tự mở bán suất.',
      requiresLayout: true,
      tone: 'primary',
    },
  ],
};

const OPERATIONAL_TONE_CLASSES = {
  red: 'text-red-300',
  amber: 'text-amber-300',
  orange: 'text-brand-orange',
  violet: 'text-violet-300',
  sky: 'text-sky-300',
  emerald: 'text-emerald-300',
  zinc: 'text-zinc-300',
};

export default function AuditoriumOverviewTab({
  auditorium,
  operationalState,
  maintenanceWindows = [],
  onUpdate,
  onChangeStatus,
  onOpenMaintenance,
}) {
  const { triggerConfirm, triggerToast } = useOutletContext() || {};
  const [isEditing, setIsEditing] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [roomName, setRoomName] = useState(auditorium?.auditoriumName || '');
  const [screenType, setScreenType] = useState(auditorium?.screenType || 'STANDARD');
  const [soundType, setSoundType] = useState(auditorium?.soundType || 'STANDARD');
  const [cleaningBuffer, setCleaningBuffer] = useState(
    auditorium?.cleaningBufferMinutes ?? 15,
  );
  const [approvedCapacity, setApprovedCapacity] = useState(auditorium?.capacity ?? 1);

  const seatMetrics = useMemo(() => {
    const seats = (auditorium?.rows || []).flatMap(row => row.seats || [])
      .filter(seat => seat.status !== 'INACTIVE');
    const byType = code => seats.filter(seat => seat.seatType?.code === code);
    const coupleSeats = byType('COUPLE');
    const pairGroups = new Set(coupleSeats.map(seat => seat.pairGroup).filter(Boolean));
    const coupleModules = pairGroups.size || Math.floor(coupleSeats.length / 2);
    return {
      standard: byType('STANDARD').length,
      vip: byType('VIP').length,
      accessible: byType('DISABLED').length,
      coupleSeats: coupleSeats.length,
      coupleModules,
      capacity: seats.length,
      ticketingPositions: seats.length - coupleModules,
    };
  }, [auditorium]);
  const computedCapacity = seatMetrics.capacity;

  const status = auditorium?.auditoriumStatus || 'DRAFT';
  const statusPresentation = getAuditoriumStatus(status);
  const checks = [
    { label: 'Đã đặt tên phòng', complete: Boolean(roomName.trim()) },
    { label: 'Đã chọn công nghệ chiếu và âm thanh', complete: Boolean(screenType && soundType) },
    { label: 'Đã thiết lập sơ đồ ghế', complete: computedCapacity > 0 },
    { label: 'Đã có thời gian dọn phòng', complete: cleaningBuffer >= 0 },
  ];
  const completedChecks = checks.filter((item) => item.complete).length;
  const isDraft = status === 'DRAFT';
  const nextMaintenance = operationalState?.upcomingMaintenance;
  const lastMaintenance = [...maintenanceWindows]
    .filter(item => item.status === 'RESOLVED')
    .sort((left, right) => new Date(right.actualEndTime || right.endTime) - new Date(left.actualEndTime || left.endTime))[0];
  const formatDateTime = value => value
    ? new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
      }).format(new Date(value))
    : 'Không có';

  const handleSave = async (event) => {
    event.preventDefault();
    if (approvedCapacity < computedCapacity) {
      triggerToast?.('Số vị trí trong sơ đồ không được vượt sức chứa theo hồ sơ.', 'error');
      return;
    }
    setIsSubmitting(true);
    const success = await onUpdate({
      name: roomName.trim(),
      screenType,
      soundType,
      capacity: approvedCapacity,
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
      {isDraft ? (
        <section className="grid gap-4 md:grid-cols-3">
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5 md:col-span-2">
            <p className="text-[10px] font-black uppercase tracking-widest text-zinc-400">Tình trạng cấu hình</p>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <span className={`rounded-lg border px-3 py-1.5 text-xs font-black ${statusPresentation.className}`}>{statusPresentation.label}</span>
              <p className="text-sm text-zinc-300">{statusPresentation.description}</p>
            </div>
          </div>
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5">
            <p className="text-[10px] font-black uppercase tracking-widest text-zinc-400">Mức độ hoàn thiện</p>
            <p className="mt-2 text-2xl font-black text-white">{completedChecks}/{checks.length}</p>
            <p className="text-xs text-zinc-400">hạng mục đã sẵn sàng</p>
          </div>
        </section>
      ) : (
        <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {[
            ['Trạng thái hiện tại', operationalState?.label || 'Đang trống', Clock3, OPERATIONAL_TONE_CLASSES[operationalState?.tone] || 'text-emerald-300'],
            ['Suất tiếp theo', operationalState?.nextShowtime ? formatDateTime(operationalState.nextShowtime.startTime) : 'Không có', CalendarClock, 'text-sky-300'],
            ['Ghế tạm khóa', String(auditorium.maintenanceSeats || 0), Armchair, Number(auditorium.maintenanceSeats || 0) > 0 ? 'text-amber-300' : 'text-emerald-300'],
            ['Bảo trì sắp tới', nextMaintenance ? formatDateTime(nextMaintenance.startTime) : 'Không có', Wrench, nextMaintenance ? 'text-amber-300' : 'text-zinc-300'],
          ].map(([label, value, Icon, tone]) => (
            <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/25 p-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-400">{label}</p>
                  <p className={`mt-2 text-base font-black ${tone}`}>{value}</p>
                </div>
                <Icon className="h-4 w-4 text-zinc-500" />
              </div>
            </article>
          ))}
        </section>
      )}

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
              approvedCapacity={approvedCapacity}
              setApprovedCapacity={setApprovedCapacity}
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
              ['Vị trí bán vé', seatMetrics.ticketingPositions],
              ['Ghế đôi', `${seatMetrics.coupleModules} module / ${seatMetrics.coupleSeats} người`],
              ['Vị trí xe lăn', seatMetrics.accessible],
              ['Sức chứa tối đa', `${auditorium.capacity ?? computedCapacity} người`],
            ].map(([label, value]) => (
              <div key={label} className="rounded-xl border border-zinc-800 bg-zinc-950/50 p-4">
                <dt className="text-[10px] font-black uppercase tracking-widest text-zinc-500">{label}</dt>
                <dd className="mt-2 text-sm font-bold text-white">{value}</dd>
              </div>
            ))}
          </dl>
        )}
      </section>

      <section className={`grid gap-6 ${isDraft ? 'lg:grid-cols-2' : ''}`}>
        {isDraft && <div className="rounded-2xl border border-zinc-800 bg-zinc-900/20 p-6">
          <h2 className="text-sm font-black uppercase text-white">Checklist trước khi mở phòng</h2>
          <div className="mt-4 space-y-3">
            {checks.map((item) => (
              <div key={item.label} className="flex items-center gap-3 text-sm">
                <CheckCircle2 className={`h-5 w-5 ${item.complete ? 'text-emerald-400' : 'text-zinc-700'}`} />
                <span className={item.complete ? 'text-zinc-200' : 'text-zinc-500'}>{item.label}</span>
              </div>
            ))}
          </div>
        </div>}

        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/20 p-6">
          <h2 className="text-sm font-black uppercase text-white">{isDraft ? 'Tác vụ hoàn thiện' : 'Tác vụ vận hành'}</h2>
          <div className="mt-3 flex gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-100">
            <ShieldAlert className="h-5 w-5 shrink-0 text-amber-400" />
            <p>{status === 'ACTIVE'
              ? 'Mọi thao tác đóng phòng được thực hiện trong luồng bảo trì để hệ thống kiểm tra suất chiếu, ghế đã bán và phạm vi ảnh hưởng trước khi xác nhận.'
              : 'Trước khi đổi trạng thái phòng, hãy kiểm tra lịch chiếu và các đơn đã đặt.'}</p>
          </div>
          <div className="mt-4 space-y-3">
            {status === 'ACTIVE' && (
              <button
                type="button"
                onClick={onOpenMaintenance}
                className="w-full rounded-xl border border-amber-500/30 bg-amber-500/5 p-4 text-left transition-colors hover:bg-amber-500/10"
              >
                <span className="text-sm font-black text-white">Mở trung tâm đóng phòng & bảo trì</span>
                <span className="mt-1 block text-xs text-zinc-400">
                  Lập lịch bảo trì hoặc đóng khẩn cấp với bước xem trước tác động đến lịch chiếu và đơn đặt vé.
                </span>
              </button>
            )}
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

      {!isDraft && lastMaintenance && (
        <p className="text-xs text-zinc-500">Bảo trì gần nhất hoàn tất lúc {formatDateTime(lastMaintenance.actualEndTime || lastMaintenance.endTime)}.</p>
      )}
    </div>
  );
}
