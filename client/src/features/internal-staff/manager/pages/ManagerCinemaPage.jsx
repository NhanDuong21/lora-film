import { useMemo, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import {
  Building2,
  CalendarDays,
  CheckCircle2,
  Clock3,
  ExternalLink,
  Film,
  Info,
  MapPin,
  MoonStar,
  Phone,
  RefreshCw,
  ShieldCheck,
  TriangleAlert,
} from 'lucide-react';
import { EmptyWorkspace, HrHero } from '../../admin/components/HrWorkspace';
import { ConsolePanel, MetricStrip } from '../../admin/components/OperationsConsole';

const DAY_NAMES = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'];

const STATUS_LABELS = {
  ACTIVE: 'Đang hoạt động',
  DRAFT: 'Đang thiết lập',
  MAINTENANCE: 'Đang bảo trì',
  TEMPORARILY_CLOSED: 'Tạm đóng cửa',
  SUSPENDED: 'Tạm ngừng',
  ARCHIVED: 'Đã lưu trữ',
};

const LOCATION_REPLACEMENTS = [
  ['Ho Chi Minh City', 'Thành phố Hồ Chí Minh'],
  ['Thu Duc City', 'Thành phố Thủ Đức'],
  ['Binh Thanh', 'Bình Thạnh'],
  ['District 7', 'Quận 7'],
  ['Nguyen Huu Canh', 'Nguyễn Hữu Cảnh'],
  ['Ton Dat Tien', 'Tôn Dật Tiên'],
  ['Vo Van Ngan', 'Võ Văn Ngân'],
  ['Tan Phu Ward', 'Phường Tân Phú'],
  ['Linh Chieu Ward', 'Phường Linh Chiểu'],
];

const defaultHours = () => Array.from({ length: 7 }, (_, index) => ({
  dayOfWeek: index + 1,
  openTime: '08:00',
  closeTime: '23:00',
  isClosed: false,
}));

const normalizeTime = value => value ? String(value).slice(0, 5) : '';

const normalizeHours = operatingHours => {
  const byDay = new Map((operatingHours || []).map(item => [Number(item.dayOfWeek), item]));
  return defaultHours().map(fallback => {
    const source = byDay.get(fallback.dayOfWeek) || {};
    return {
      ...fallback,
      ...source,
      openTime: normalizeTime(source.openTime) || fallback.openTime,
      closeTime: normalizeTime(source.closeTime) || fallback.closeTime,
      isClosed: Boolean(source.isClosed),
    };
  });
};

const minutesOf = value => {
  const [hour, minute] = String(value || '').split(':').map(Number);
  return Number.isFinite(hour) && Number.isFinite(minute) ? hour * 60 + minute : null;
};

const isOvernight = item => {
  const open = minutesOf(item?.openTime);
  const close = minutesOf(item?.closeTime);
  return open != null && close != null && close < open;
};

const localClock = timezone => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone || 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date()).reduce((result, part) => ({ ...result, [part.type]: part.value }), {});
  const weekday = new Date(Date.UTC(Number(parts.year), Number(parts.month) - 1, Number(parts.day))).getUTCDay();
  return {
    dayOfWeek: weekday === 0 ? 7 : weekday,
    minutes: Number(parts.hour) * 60 + Number(parts.minute),
  };
};

const todayOperation = (hours, timezone) => {
  const clock = localClock(timezone);
  const todayIndex = clock.dayOfWeek - 1;
  const today = hours[todayIndex];
  const previous = hours[(todayIndex + 6) % 7];
  const previousClose = minutesOf(previous?.closeTime);

  if (!previous?.isClosed && isOvernight(previous) && previousClose != null && clock.minutes < previousClose) {
    return {
      value: 'Đang mở cửa',
      detail: `Ca từ ${DAY_NAMES[(todayIndex + 6) % 7]}, đóng lúc ${previous.closeTime} hôm nay`,
      tone: 'green',
    };
  }
  if (!today || today.isClosed) return { value: 'Đóng cửa hôm nay', detail: 'Hôm nay là ngày nghỉ theo lịch chuẩn', tone: 'amber' };
  const open = minutesOf(today.openTime);
  const close = minutesOf(today.closeTime);
  if (open == null || close == null) return { value: 'Chưa đủ giờ', detail: 'Quản trị viên cần hoàn thiện lịch chuẩn', tone: 'red' };
  if (isOvernight(today)) {
    if (clock.minutes >= open) return { value: 'Đang mở cửa', detail: `Đóng lúc ${today.closeTime} ngày mai`, tone: 'green' };
    return { value: `Mở lúc ${today.openTime}`, detail: `Đóng lúc ${today.closeTime} ngày mai`, tone: 'blue' };
  }
  if (clock.minutes < open) return { value: `Mở lúc ${today.openTime}`, detail: `Đóng lúc ${today.closeTime} hôm nay`, tone: 'blue' };
  if (clock.minutes < close) return { value: 'Đang mở cửa', detail: `Đóng lúc ${today.closeTime} hôm nay`, tone: 'green' };
  return { value: 'Đã đóng cửa', detail: `Giờ phục vụ hôm nay: ${today.openTime}–${today.closeTime}`, tone: 'amber' };
};

const hoursDescription = item => {
  if (item.isClosed) return 'Không phục vụ trong ngày';
  if (!item.openTime || !item.closeTime) return 'Chưa được thiết lập đầy đủ';
  return isOvernight(item)
    ? `${item.openTime} – ${item.closeTime} ngày hôm sau`
    : `${item.openTime} – ${item.closeTime} cùng ngày`;
};

const vietnameseLocation = cinema => {
  let result = [cinema?.address, cinema?.city].filter(Boolean).join(', ');
  LOCATION_REPLACEMENTS.forEach(([source, replacement]) => {
    result = result.split(source).join(replacement);
  });
  return result || 'Chưa cập nhật';
};

function StatusPill({ status }) {
  const active = status === 'ACTIVE';
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-black ${active ? 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300' : 'border-amber-500/25 bg-amber-500/10 text-amber-300'}`}>
      {active ? <CheckCircle2 size={13} /> : <TriangleAlert size={13} />}
      {STATUS_LABELS[status] || 'Chưa xác định'}
    </span>
  );
}

function OperatingHoursViewer({ hours }) {
  return (
    <ConsolePanel className="p-5 md:p-6">
      <div className="flex flex-col justify-between gap-4 border-b border-white/10 pb-5 sm:flex-row sm:items-start">
        <div>
          <p className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.18em] text-orange-400"><CalendarDays size={15} /> Lịch phục vụ chuẩn</p>
          <h2 className="mt-2 text-xl font-black text-white">Giờ mở cửa hằng tuần</h2>
          <p className="mt-1 max-w-2xl text-sm leading-6 text-zinc-500">Lịch này do Quản trị viên thiết lập và được dùng cho lịch chiếu, thông tin khách hàng và vận hành tại rạp.</p>
        </div>
        <span className="inline-flex items-center gap-2 rounded-xl border border-sky-500/20 bg-sky-500/5 px-3 py-2 text-xs font-black text-sky-300"><ShieldCheck size={15} /> Chỉ xem</span>
      </div>

      <div className="mt-5 grid gap-3 md:grid-cols-2">
        {hours.map((item, index) => (
          <article key={item.dayOfWeek} className={`rounded-2xl border p-4 ${item.isClosed ? 'border-white/5 bg-black/15' : 'border-white/10 bg-white/[0.025]'}`}>
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="text-sm font-black text-zinc-200">{DAY_NAMES[index]}</h3>
                <p className={`mt-1 text-xs ${isOvernight(item) ? 'text-amber-300' : 'text-zinc-500'}`}>{hoursDescription(item)}</p>
              </div>
              <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${item.isClosed ? 'border-white/10 bg-white/5 text-zinc-500' : 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300'}`}>{item.isClosed ? 'Ngày nghỉ' : 'Mở cửa'}</span>
            </div>
            {isOvernight(item) && !item.isClosed ? <p className="mt-3 flex items-center gap-2 text-[11px] font-bold text-amber-300"><MoonStar size={14} /> Ca phục vụ kéo dài sang ngày hôm sau</p> : null}
          </article>
        ))}
      </div>
    </ConsolePanel>
  );
}

export default function ManagerCinemaPage() {
  const { selectedCinema, cinemaState, reloadCinemas } = useOutletContext();
  const [refreshState, setRefreshState] = useState({ loading: false, error: '', success: '' });
  const hours = useMemo(() => normalizeHours(selectedCinema?.operatingHours), [selectedCinema?.operatingHours]);
  const openDays = hours.filter(item => !item.isClosed).length;
  const overnightDays = hours.filter(item => !item.isClosed && isOvernight(item)).length;
  const today = todayOperation(hours, selectedCinema?.timezone);
  const rooms = selectedCinema?.activeAuditoriums || [];
  const readyRooms = rooms.filter(room => !room.status || room.status === 'ACTIVE').length;

  const refresh = async () => {
    setRefreshState({ loading: true, error: '', success: '' });
    try {
      await reloadCinemas();
      setRefreshState({ loading: false, error: '', success: 'Đã tải lịch mới nhất do Quản trị viên thiết lập.' });
    } catch (error) {
      setRefreshState({ loading: false, error: error?.message || 'Không thể tải lại thông tin rạp.', success: '' });
    }
  };

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải thông tin rạp…</p>;
  if (!selectedCinema) return <EmptyWorkspace title="Chưa được phân công rạp" description="Quản trị viên cần phân công rạp trước khi bạn có thể xem thông tin vận hành." />;

  return (
    <main className="space-y-5 pb-8 text-white">
      <HrHero
        context={`Vận hành rạp · ${selectedCinema.name}`}
        title="Trung tâm vận hành rạp"
        description="Kiểm tra tình trạng phục vụ, thông tin liên hệ và giờ mở cửa chuẩn của rạp được phân công. Việc thay đổi lịch tuần thuộc quyền Quản trị viên."
        actions={<><Link to="/manager/showtimes" className="inline-flex items-center gap-2 rounded-xl border border-white/10 px-4 py-2.5 text-sm font-black text-zinc-200 hover:bg-white/5"><Film size={17} /> Xem lịch chiếu</Link><button type="button" onClick={refresh} disabled={refreshState.loading} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-black text-black disabled:opacity-40"><RefreshCw size={17} className={refreshState.loading ? 'animate-spin' : ''} /> Tải lại lịch</button></>}
      />

      <MetricStrip items={[
        { icon: ShieldCheck, label: 'Tình trạng rạp', value: STATUS_LABELS[selectedCinema.status] || 'Chưa xác định', hint: selectedCinema.status === 'ACTIVE' ? 'Rạp có thể phục vụ khách hàng' : 'Cần kiểm tra trước khi vận hành', tone: selectedCinema.status === 'ACTIVE' ? 'green' : 'amber' },
        { icon: Clock3, label: 'Lịch phục vụ hôm nay', value: today.value, hint: today.detail, tone: today.tone },
        { icon: CalendarDays, label: 'Ngày mở cửa mỗi tuần', value: `${openDays}/7 ngày`, hint: overnightDays ? `${overnightDays} ngày có ca qua đêm` : 'Không có ca qua đêm', tone: 'orange' },
        { icon: Film, label: 'Phòng sẵn sàng', value: `${readyRooms}/${rooms.length || 0} phòng`, hint: rooms.length ? 'Xem chi tiết tại Phòng chiếu & bảo trì' : 'Chưa có phòng trong phạm vi rạp', tone: 'blue' },
      ]} />

      <section className="grid gap-5 xl:grid-cols-[1.35fr_1fr]">
        <ConsolePanel className="p-5 md:p-6">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
            <div className="flex items-start gap-4">
              <span className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-orange-500/10 text-orange-400"><Building2 size={23} /></span>
              <div><p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Rạp đang phụ trách</p><h2 className="mt-1 text-xl font-black text-zinc-100">{selectedCinema.name}</h2><div className="mt-2"><StatusPill status={selectedCinema.status} /></div></div>
            </div>
            <span className="inline-flex items-center gap-2 rounded-xl border border-sky-500/20 bg-sky-500/5 px-3 py-2 text-xs font-bold text-sky-300"><ShieldCheck size={15} /> Phạm vi được phân công</span>
          </div>
          <dl className="mt-6 grid gap-4 border-t border-white/10 pt-5 sm:grid-cols-2">
            <div className="flex items-start gap-3"><MapPin size={17} className="mt-0.5 shrink-0 text-orange-400" /><div><dt className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Địa chỉ rạp</dt><dd className="mt-1 text-sm leading-6 text-zinc-300">{vietnameseLocation(selectedCinema)}</dd></div></div>
            <div className="flex items-start gap-3"><Phone size={17} className="mt-0.5 shrink-0 text-orange-400" /><div><dt className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Điện thoại hỗ trợ</dt><dd className="mt-1 text-sm text-zinc-300">{selectedCinema.hotline ? <a href={`tel:${selectedCinema.hotline.replace(/\s/g, '')}`} className="font-black hover:text-orange-300">{selectedCinema.hotline}</a> : 'Chưa cập nhật'}</dd></div></div>
          </dl>
        </ConsolePanel>

        <ConsolePanel className="p-5 md:p-6">
          <p className="text-[10px] font-black uppercase tracking-[0.18em] text-orange-400">Đi đến công việc</p>
          <h2 className="mt-2 text-lg font-black">Thao tác thường dùng</h2>
          <div className="mt-4 space-y-2">
            <Link to="/manager/showtimes" className="flex items-center gap-3 rounded-xl border border-white/10 bg-white/[0.025] p-3 hover:border-orange-500/30"><span className="grid h-9 w-9 place-items-center rounded-lg bg-orange-500/10 text-orange-400"><Film size={17} /></span><span className="flex-1"><strong className="block text-sm text-zinc-200">Kiểm tra lịch chiếu</strong><span className="mt-0.5 block text-xs text-zinc-600">Xem các suất đang mở bán và cần xử lý</span></span><ExternalLink size={15} className="text-zinc-600" /></Link>
            <Link to="/manager/rooms" className="flex items-center gap-3 rounded-xl border border-white/10 bg-white/[0.025] p-3 hover:border-orange-500/30"><span className="grid h-9 w-9 place-items-center rounded-lg bg-sky-500/10 text-sky-400"><Building2 size={17} /></span><span className="flex-1"><strong className="block text-sm text-zinc-200">Phòng chiếu & bảo trì</strong><span className="mt-0.5 block text-xs text-zinc-600">Kiểm tra phòng sẵn sàng hoặc đang bảo trì</span></span><ExternalLink size={15} className="text-zinc-600" /></Link>
          </div>
        </ConsolePanel>
      </section>

      {refreshState.error ? <div className="rounded-xl border border-red-500/20 bg-red-500/5 p-4 text-sm text-red-200">{refreshState.error}</div> : null}
      {refreshState.success ? <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-sm text-emerald-200">{refreshState.success}</div> : null}

      <section className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
        <OperatingHoursViewer hours={hours} />

        <aside className="space-y-4 xl:sticky xl:top-24">
          <ConsolePanel className="p-5">
            <div className="flex items-start gap-3">
              <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-sky-500/10 text-sky-300"><ShieldCheck size={18} /></span>
              <div><p className="text-sm font-black text-zinc-200">Lịch do Quản trị viên quản lý</p><p className="mt-1 text-xs leading-5 text-zinc-600">Manager chỉ xem lịch chuẩn để phối hợp nhân sự, phòng chiếu và suất chiếu.</p></div>
            </div>
            <div className="mt-5 space-y-3 border-y border-white/10 py-4 text-xs">
              <div className="flex justify-between gap-3"><span className="text-zinc-600">Ngày phục vụ</span><strong className="text-zinc-300">{openDays}/7 ngày</strong></div>
              <div className="flex justify-between gap-3"><span className="text-zinc-600">Ca qua đêm</span><strong className="text-zinc-300">{overnightDays} ngày</strong></div>
              <div className="flex justify-between gap-3"><span className="text-zinc-600">Ngày nghỉ</span><strong className="text-zinc-300">{7 - openDays} ngày</strong></div>
            </div>
            <button type="button" onClick={refresh} disabled={refreshState.loading} className="mt-5 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-white/10 px-4 text-sm font-black text-zinc-300 hover:bg-white/5 disabled:opacity-40"><RefreshCw size={16} className={refreshState.loading ? 'animate-spin' : ''} /> Tải lịch mới nhất</button>
          </ConsolePanel>

          <div className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4 text-xs leading-5 text-amber-100/70">
            <p className="flex items-center gap-2 font-black text-amber-300"><Info size={15} /> Cần thay đổi giờ hoạt động?</p>
            <p className="mt-2">Hãy liên hệ Quản trị viên và cung cấp ngày áp dụng, giờ đề xuất cùng lý do thay đổi. Hệ thống hiện chưa có quy trình gửi yêu cầu trực tuyến.</p>
          </div>
        </aside>
      </section>
    </main>
  );
}
