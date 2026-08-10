import {
  AlertTriangle,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Film,
  Image as ImageIcon,
  MapPin,
  Power,
} from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import {
  getCinemaReadiness,
  getCinemaStatus,
} from '../../utils/facilityPresentation';

const CHECK_ACTIONS = {
  basic: { tab: 'overview', label: 'Bổ sung thông tin' },
  hours: { tab: 'operating-hours', label: 'Thiết lập giờ mở cửa' },
  media: { tab: 'media', label: 'Thêm hình ảnh' },
  rooms: { tab: 'auditoriums', label: 'Thiết lập phòng chiếu' },
};

export default function CinemaHealthOverviewTab({
  cinema,
  onOpenTab,
  onStatusChange,
}) {
  const { triggerConfirm } = useOutletContext() || {};
  const readiness = getCinemaReadiness(cinema);
  const status = getCinemaStatus(cinema.status);
  const openDays = (cinema.operatingHours || []).filter((item) => !item.isClosed).length;
  const mediaCount = (cinema.gallery || []).length;

  const requestActivation = async () => {
    if (!readiness.ready) return;
    const confirmed = await triggerConfirm?.({
      title: 'Đưa cụm rạp vào hoạt động?',
      message:
        'Cụm rạp sẽ có thể tham gia xếp lịch và phục vụ khách hàng. Hãy chắc chắn các phòng và giờ hoạt động đã được kiểm tra.',
      confirmLabel: 'Đưa vào hoạt động',
    });
    if (confirmed) await onStatusChange('ACTIVE');
  };

  return (
    <div className="space-y-6 pb-20">
      <section className="grid gap-4 lg:grid-cols-[1.4fr_1fr]">
        <div className="rounded-3xl border border-zinc-800 bg-zinc-900/40 p-6">
          <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">
                Sức khỏe vận hành
              </p>
              <h2 className="mt-2 text-xl font-black text-white">
                {readiness.ready ? 'Cụm rạp đã đủ điều kiện cơ bản' : 'Cụm rạp còn việc cần hoàn thiện'}
              </h2>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
                {readiness.ready
                  ? 'Thông tin, giờ mở cửa, hình ảnh và ít nhất một phòng phục vụ đã sẵn sàng.'
                  : 'Hoàn thành các mục bên dưới trước khi đưa cụm rạp vào vận hành chính thức.'}
              </p>
            </div>
            <span className={`rounded-xl border px-3 py-2 text-xs font-bold ${status.className}`}>
              {status.label}
            </span>
          </div>

          <div className="mt-6 h-2 overflow-hidden rounded-full bg-zinc-800">
            <div
              className="h-full rounded-full bg-orange-500 transition-all"
              style={{ width: `${(readiness.completed / readiness.total) * 100}%` }}
            />
          </div>
          <p className="mt-2 text-xs text-zinc-500">
            Hoàn thành {readiness.completed}/{readiness.total} hạng mục thiết lập
          </p>

          {cinema.status === 'DRAFT' && (
            <button
              type="button"
              disabled={!readiness.ready}
              onClick={requestActivation}
              className="mt-6 inline-flex items-center gap-2 rounded-xl bg-orange-500 px-5 py-3 text-xs font-black uppercase tracking-wider text-white disabled:cursor-not-allowed disabled:opacity-40"
            >
              <Power className="h-4 w-4" />
              Đưa cụm rạp vào hoạt động
            </button>
          )}
        </div>

        <div className="rounded-3xl border border-zinc-800 bg-zinc-900/40 p-6">
          <h3 className="text-sm font-black uppercase tracking-wider text-white">
            Tình trạng hiện tại
          </h3>
          <div className="mt-5 space-y-4 text-sm">
            <HealthRow icon={Film} label="Phòng sẵn sàng" value={`${readiness.readyRooms}/${readiness.totalRooms}`} />
            <HealthRow icon={Clock3} label="Ngày mở cửa mỗi tuần" value={`${openDays}/7`} />
            <HealthRow icon={ImageIcon} label="Hình ảnh đang quản lý" value={`${mediaCount}`} />
            <HealthRow icon={MapPin} label="Vị trí" value={cinema.city || 'Chưa hoàn thiện'} />
          </div>
        </div>
      </section>

      <section className="rounded-3xl border border-zinc-800 bg-zinc-900/30 p-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-sm font-black uppercase tracking-wider text-white">Việc cần làm</h2>
            <p className="mt-1 text-xs text-zinc-500">
              Chọn một việc để đi thẳng đến khu vực cần xử lý.
            </p>
          </div>
          {!readiness.ready && <AlertTriangle className="h-5 w-5 text-amber-400" />}
        </div>

        <div className="mt-5 grid gap-3 md:grid-cols-2">
          {readiness.checks.map((check) => {
            const action = CHECK_ACTIONS[check.id];
            return (
              <button
                type="button"
                key={check.id}
                onClick={() => onOpenTab(action.tab)}
                className="flex items-center gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 text-left transition hover:border-orange-500/40"
              >
                <CheckCircle2
                  className={`h-5 w-5 shrink-0 ${check.complete ? 'text-emerald-400' : 'text-zinc-600'}`}
                />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-bold text-zinc-100">{check.label}</p>
                  <p className="mt-1 text-xs text-zinc-500">
                    {check.complete ? 'Đã hoàn thành' : action.label}
                  </p>
                </div>
                <ChevronRight className="h-4 w-4 text-zinc-600" />
              </button>
            );
          })}
        </div>
      </section>

      <section className="rounded-2xl border border-sky-500/20 bg-sky-500/5 p-5">
        <p className="text-sm font-bold text-sky-300">Dữ liệu lịch vận hành</p>
        <p className="mt-1 text-xs leading-5 text-zinc-400">
          API hiện chưa cung cấp suất chiếu kế tiếp hoặc số đơn bị ảnh hưởng theo cụm rạp.
          Giao diện không tự ước lượng các số liệu này; hãy kiểm tra tại Lịch vận hành và Đơn đặt vé.
        </p>
      </section>
    </div>
  );
}

function HealthRow({ icon: Icon, label, value }) {
  return (
    <div className="flex items-center gap-3 border-b border-zinc-800 pb-3 last:border-0 last:pb-0">
      <Icon className="h-4 w-4 text-orange-400" />
      <span className="flex-1 text-zinc-400">{label}</span>
      <strong className="text-zinc-100">{value}</strong>
    </div>
  );
}
