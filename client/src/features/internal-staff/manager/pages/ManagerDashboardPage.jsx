import { useEffect, useMemo, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { Building2, CalendarCheck2, CheckCircle2, Clock3, MapPin, RefreshCw } from 'lucide-react';
import managerCinemaService from '../services/managerCinemaService';
import { getShowtimeStatusPresentation } from '@/features/scheduling/admin/utils/schedulingPresentation';

const todayInputValue = () => {
  const now = new Date();
  const local = new Date(now.getTime() - (now.getTimezoneOffset() * 60000));
  return local.toISOString().slice(0, 10);
};

const formatTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date(value))
  : '—';

const SummaryCard = ({ icon: Icon, label, value, note, tone = 'orange' }) => (
  <article className="rounded-2xl border border-white/10 bg-white/[0.025] p-5">
    <div className="flex items-start justify-between gap-4">
      <div><p className="text-xs font-bold text-zinc-500">{label}</p><p className="mt-2 text-3xl font-black text-white">{value}</p></div>
      <span className={`grid h-10 w-10 place-items-center rounded-xl ${tone === 'green' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-brand-orange/10 text-brand-orange'}`}><Icon size={20} /></span>
    </div>
    <p className="mt-3 text-xs leading-5 text-zinc-600">{note}</p>
  </article>
);

export default function ManagerDashboardPage() {
  const { cinemas, selectedCinema, selectedCinemaId, cinemaState, reloadCinemas } = useOutletContext();
  const [showtimeState, setShowtimeState] = useState({ loading: true, error: '', data: [] });

  useEffect(() => {
    if (!selectedCinemaId) {
      return;
    }
    let active = true;
    managerCinemaService.getShowtimes({ cinemaPublicId: selectedCinemaId, date: todayInputValue(), page: 0, size: 100 })
      .then(response => {
        if (active) setShowtimeState({ loading: false, error: '', data: response.data || [] });
      })
      .catch(error => {
        if (active) setShowtimeState({ loading: false, error: error?.message || 'Không thể tải lịch chiếu hôm nay.', data: [] });
      });
    return () => { active = false; };
  }, [selectedCinemaId]);

  const showtimes = showtimeState.data;
  const upcoming = useMemo(() => [...showtimes]
    .filter(showtime => new Date(showtime.endTime) >= new Date())
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
    .slice(0, 6), [showtimes]);
  const activeRooms = selectedCinema?.activeAuditoriums?.filter(room => room.status === 'ACTIVE').length
    ?? selectedCinema?.activeAuditoriums?.length
    ?? 0;
  const openForBooking = showtimes.filter(showtime => showtime.status === 'OPEN_FOR_BOOKING').length;

  if (cinemaState.loading) {
    return <div className="grid min-h-[55vh] place-items-center text-sm font-bold text-zinc-500">Đang chuẩn bị dữ liệu rạp được phân công…</div>;
  }

  if (cinemaState.error) {
    return <div className="mx-auto max-w-xl rounded-2xl border border-red-500/20 bg-red-500/10 p-8 text-center"><h1 className="text-xl font-black">Không tải được phạm vi quản lý</h1><p className="mt-2 text-sm text-red-100/70">{cinemaState.error}</p><button type="button" onClick={reloadCinemas} className="mt-5 rounded-xl bg-white px-4 py-2 text-sm font-black text-black">Thử lại</button></div>;
  }

  if (!selectedCinema) {
    return <div className="mx-auto max-w-2xl rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center"><Building2 className="mx-auto text-amber-300" size={36} /><h1 className="mt-4 text-2xl font-black">Tài khoản chưa được phân công rạp</h1><p className="mt-2 text-sm leading-6 text-amber-100/70">Vui lòng liên hệ Quản trị viên để chọn rạp bạn phụ trách. Khi chưa được phân công, hệ thống sẽ không hiển thị dữ liệu vận hành của bất kỳ rạp nào.</p></div>;
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div><p className="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">Ca làm hôm nay</p><h1 className="mt-2 text-3xl font-black md:text-4xl">Tổng quan vận hành</h1><p className="mt-2 flex items-center gap-2 text-sm text-zinc-500"><MapPin size={15} /> {selectedCinema.name} · {selectedCinema.address}</p></div>
        <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-xs font-bold text-emerald-200"><span className="inline-flex items-center gap-2"><CheckCircle2 size={16} /> Chỉ dữ liệu của rạp được phân công</span></div>
      </header>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard icon={Building2} label="Rạp đang phụ trách" value={String(cinemas.length).padStart(2, '0')} note="Đổi rạp ở góc trên nếu bạn được phân công nhiều rạp." />
        <SummaryCard icon={Building2} label="Phòng đang hoạt động" value={activeRooms} note="Số phòng chiếu khả dụng tại rạp này." />
        <SummaryCard icon={CalendarCheck2} label="Suất chiếu hôm nay" value={showtimes.length} note="Tổng số suất theo ngày vận hành hiện tại." />
        <SummaryCard icon={CheckCircle2} label="Đang mở bán" value={openForBooking} note="Khách hàng có thể đặt vé ngay lúc này." tone="green" />
      </section>

      <section className="overflow-hidden rounded-2xl border border-white/10 bg-white/[0.02]">
        <header className="flex items-center justify-between gap-3 border-b border-white/10 p-5"><div><h2 className="font-black">Các suất sắp diễn ra</h2><p className="mt-1 text-xs text-zinc-600">Danh sách gần nhất trong ngày để quản lý dễ theo dõi.</p></div><Link to="/manager/showtimes" className="text-xs font-black text-brand-orange hover:underline">Xem toàn bộ lịch</Link></header>
        {showtimeState.loading ? <p className="p-8 text-center text-sm text-zinc-500">Đang tải lịch chiếu…</p> : showtimeState.error ? <div className="p-8 text-center"><p className="text-sm text-red-300">{showtimeState.error}</p><button type="button" onClick={() => window.location.reload()} className="mt-3 inline-flex items-center gap-2 text-xs font-bold text-brand-orange"><RefreshCw size={14} /> Tải lại</button></div> : upcoming.length ? (
          <div className="divide-y divide-white/5">
            {upcoming.map(showtime => <article key={showtime.showtimePublicId} className="grid gap-3 p-4 md:grid-cols-[110px_1fr_180px_150px] md:items-center md:px-5"><div className="flex items-center gap-2 text-xl font-black text-white"><Clock3 size={17} className="text-brand-orange" /> {formatTime(showtime.startTime)}</div><div><p className="font-bold text-white">{showtime.movie?.title || 'Chưa có tên phim'}</p><p className="mt-1 text-xs text-zinc-600">{showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Bản chiếu tiêu chuẩn'}</p></div><p className="text-sm font-semibold text-zinc-300">{showtime.auditorium?.name || 'Chưa xếp phòng'}</p><span className="w-fit rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-bold text-zinc-300">{getShowtimeStatusPresentation(showtime.status).label}</span></article>)}
          </div>
        ) : <p className="p-8 text-center text-sm text-zinc-500">Không còn suất chiếu nào sắp diễn ra hôm nay.</p>}
      </section>
    </div>
  );
}
