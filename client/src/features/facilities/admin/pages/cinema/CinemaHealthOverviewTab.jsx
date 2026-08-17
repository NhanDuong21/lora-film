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
  getCinemaStatus,
} from '../../utils/facilityPresentation';

export default function CinemaHealthOverviewTab({
  cinema,
  readiness,
  onOpenTab,
  onStatusChange,
}) {
  const { triggerConfirm } = useOutletContext() || {};
  const status = getCinemaStatus(cinema.status);
  const operationalChecks = readiness?.operationalChecks || [];
  const publicProfileChecks = readiness?.publicProfileChecks || [];
  const ready = readiness?.readyForActivation === true;
  const openDays = (cinema.operatingHours || []).filter((item) => !item.isClosed).length;
  const mediaCount = (cinema.gallery || []).length;

  const requestActivation = async () => {
    if (!ready) return;
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
                {ready ? 'Cụm rạp đã đủ điều kiện vận hành' : 'Cụm rạp còn điều kiện vận hành chưa hoàn tất'}
              </h2>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
                {ready
                  ? 'Backend đã xác nhận thông tin, giờ hoạt động và phòng có booking layout.'
                  : readiness
                    ? 'Hoàn thành từng blocker do backend trả về trước khi đưa cụm rạp vào vận hành.'
                    : 'Chưa tải được readiness authoritative. Thao tác kích hoạt đang được khóa an toàn.'}
              </p>
            </div>
            <span className={`rounded-xl border px-3 py-2 text-xs font-bold ${status.className}`}>
              {status.label}
            </span>
          </div>

          <div className="mt-6 h-2 overflow-hidden rounded-full bg-zinc-800">
            <div
              className="h-full rounded-full bg-orange-500 transition-all"
              style={{ width: `${readiness ? (readiness.completedOperationalChecks / readiness.totalOperationalChecks) * 100 : 0}%` }}
            />
          </div>
          <p className="mt-2 text-xs text-zinc-500">
            Hoàn thành {readiness?.completedOperationalChecks || 0}/{readiness?.totalOperationalChecks || 0} điều kiện vận hành
          </p>

          {cinema.status === 'DRAFT' && (
            <button
              type="button"
              disabled={!ready}
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
            <HealthRow icon={Film} label="Phòng sẵn sàng" value={`${readiness?.readyAuditoriums || 0}/${readiness?.totalAuditoriums || 0}`} />
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
          {!ready && <AlertTriangle className="h-5 w-5 text-amber-400" />}
        </div>

        <div className="mt-5 grid gap-3 md:grid-cols-2">
          {operationalChecks.map((check) => {
            return (
              <button
                type="button"
                key={check.id}
                onClick={() => onOpenTab(check.actionTab)}
                className="flex items-center gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 text-left transition hover:border-orange-500/40"
              >
                <CheckCircle2
                  className={`h-5 w-5 shrink-0 ${check.complete ? 'text-emerald-400' : 'text-zinc-600'}`}
                />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-bold text-zinc-100">{check.label}</p>
                  <p className="mt-1 text-xs text-zinc-500">
                    {check.complete ? 'Đã hoàn thành' : check.reason}
                  </p>
                </div>
                <ChevronRight className="h-4 w-4 text-zinc-600" />
              </button>
            );
          })}
        </div>
      </section>

      <section className="rounded-3xl border border-sky-500/20 bg-sky-500/5 p-6">
        <h2 className="text-sm font-black uppercase tracking-wider text-sky-200">
          Hồ sơ hiển thị cho khách hàng
        </h2>
        <p className="mt-2 text-xs leading-5 text-zinc-400">
          Các mục này giúp trang cụm rạp đầy đủ hơn, nhưng không phải điều kiện an toàn để kích hoạt vận hành.
        </p>
        <div className="mt-4 grid gap-3 md:grid-cols-3">
          {publicProfileChecks.map((check) => (
            <button
              type="button"
              key={check.id}
              onClick={() => onOpenTab(check.actionTab)}
              className="rounded-2xl border border-sky-500/10 bg-zinc-950/40 p-4 text-left"
            >
              <p className={`text-xs font-bold ${check.complete ? 'text-emerald-300' : 'text-sky-300'}`}>
                {check.complete ? 'Đã hoàn tất' : 'Nên bổ sung'}
              </p>
              <p className="mt-2 text-sm font-bold text-zinc-100">{check.label}</p>
              {!check.complete && <p className="mt-1 text-xs text-zinc-500">{check.reason}</p>}
            </button>
          ))}
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
