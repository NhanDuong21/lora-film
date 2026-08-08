import { useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { CalendarDays, ChevronLeft, ChevronRight, Film, Search } from 'lucide-react';
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

const STATUS_OPTIONS = [
  ['', 'Tất cả trạng thái'],
  ['DRAFT', 'Bản nháp'],
  ['OPEN_FOR_BOOKING', 'Đang mở bán'],
  ['CLOSED', 'Đã đóng bán'],
  ['CANCELLED', 'Đã hủy'],
  ['FINISHED', 'Đã chiếu'],
];

export default function ManagerShowtimesPage() {
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const [date, setDate] = useState(todayInputValue());
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [state, setState] = useState({ loading: true, error: '', response: { data: [], totalElements: 0, totalPages: 0 } });

  useEffect(() => {
    if (!selectedCinemaId) return;
    let active = true;
    managerCinemaService.getShowtimes({ cinemaPublicId: selectedCinemaId, date, status: status || undefined, page, size: 25 })
      .then(response => { if (active) setState({ loading: false, error: '', response }); })
      .catch(error => { if (active) setState(current => ({ ...current, loading: false, error: error?.message || 'Không thể tải lịch chiếu.' })); });
    return () => { active = false; };
  }, [date, page, selectedCinemaId, status]);

  const showtimes = (state.response.data || []).filter(showtime => !keyword.trim()
    || `${showtime.movie?.title || ''} ${showtime.auditorium?.name || ''}`.toLocaleLowerCase('vi').includes(keyword.trim().toLocaleLowerCase('vi')));

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp…</p>;
  if (!selectedCinema) return <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center"><h1 className="text-xl font-black">Chưa có rạp để xem lịch chiếu</h1><p className="mt-2 text-sm text-amber-100/70">Quản trị viên cần phân công rạp cho tài khoản này trước.</p></div>;

  return (
    <div className="space-y-6">
      <header><p className="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">Điều phối tại rạp</p><h1 className="mt-2 text-3xl font-black">Lịch chiếu</h1><p className="mt-2 text-sm text-zinc-500">Theo dõi phim, phòng và trạng thái các suất tại <strong className="text-zinc-300">{selectedCinema.name}</strong>.</p></header>

      <section className="grid gap-3 rounded-2xl border border-white/10 bg-white/[0.025] p-4 md:grid-cols-3">
        <label className="block text-xs font-bold text-zinc-500">Ngày vận hành<input aria-label="Ngày vận hành" type="date" value={date} onChange={event => { setDate(event.target.value); setPage(0); }} className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm font-bold text-white outline-none focus:border-brand-orange/50" /></label>
        <label className="block text-xs font-bold text-zinc-500">Trạng thái<select aria-label="Trạng thái suất chiếu" value={status} onChange={event => { setStatus(event.target.value); setPage(0); }} className="mt-2 min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 px-3 text-sm font-bold text-white outline-none focus:border-brand-orange/50">{STATUS_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label className="block text-xs font-bold text-zinc-500">Tìm nhanh<span className="relative mt-2 block"><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" /><input aria-label="Tìm phim hoặc phòng" value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="Tên phim hoặc phòng chiếu…" className="min-h-11 w-full rounded-xl border border-white/10 bg-zinc-900 pl-10 pr-3 text-sm text-white outline-none placeholder:text-zinc-700 focus:border-brand-orange/50" /></span></label>
      </section>

      <section className="overflow-hidden rounded-2xl border border-white/10 bg-white/[0.02]">
        <header className="flex items-center justify-between border-b border-white/10 p-5"><div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-brand-orange/10 text-brand-orange"><CalendarDays size={19} /></span><div><h2 className="font-black">Danh sách suất chiếu</h2><p className="mt-0.5 text-xs text-zinc-600">{state.response.totalElements || 0} suất theo bộ lọc</p></div></div></header>
        {state.loading ? <p className="p-12 text-center text-sm text-zinc-500">Đang tải lịch chiếu…</p> : state.error ? <p className="p-12 text-center text-sm text-red-300">{state.error}</p> : showtimes.length ? (
          <div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-white/[0.035] text-[10px] uppercase tracking-wider text-zinc-600"><tr><th className="p-4">Thời gian</th><th className="p-4">Phim</th><th className="p-4">Phòng chiếu</th><th className="p-4">Phiên bản</th><th className="p-4">Trạng thái</th></tr></thead><tbody className="divide-y divide-white/5">{showtimes.map(showtime => <tr key={showtime.showtimePublicId} className="hover:bg-white/[0.025]"><td className="whitespace-nowrap p-4"><p className="text-lg font-black text-white">{formatTime(showtime.startTime)}</p><p className="mt-1 text-xs text-zinc-600">đến {formatTime(showtime.endTime)}</p></td><td className="p-4"><p className="flex items-center gap-2 font-bold text-white"><Film size={15} className="text-brand-orange" /> {showtime.movie?.title || 'Chưa có tên phim'}</p></td><td className="p-4 font-semibold text-zinc-300">{showtime.auditorium?.name || 'Chưa xếp phòng'}</td><td className="p-4 text-zinc-400">{showtime.movieVersion?.versionName || showtime.movieVersion?.format || 'Tiêu chuẩn'}</td><td className="p-4"><span className="inline-flex rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-bold text-zinc-300">{getShowtimeStatusPresentation(showtime.status).label}</span></td></tr>)}</tbody></table></div>
        ) : <div className="p-12 text-center"><CalendarDays className="mx-auto text-zinc-700" size={32} /><p className="mt-3 font-bold text-zinc-400">Không có suất chiếu phù hợp</p><p className="mt-1 text-xs text-zinc-600">Hãy chọn ngày khác hoặc bỏ bớt bộ lọc.</p></div>}
        <footer className="flex items-center justify-between gap-3 border-t border-white/10 p-4"><p className="text-xs text-zinc-600">Trang {Math.min(page + 1, Math.max(state.response.totalPages || 1, 1))}/{Math.max(state.response.totalPages || 1, 1)}</p><div className="flex gap-2"><button type="button" aria-label="Trang trước" disabled={page === 0 || state.loading} onClick={() => setPage(current => current - 1)} className="grid h-9 w-9 place-items-center rounded-lg border border-white/10 text-zinc-300 disabled:opacity-30"><ChevronLeft size={17} /></button><button type="button" aria-label="Trang sau" disabled={page >= (state.response.totalPages || 1) - 1 || state.loading} onClick={() => setPage(current => current + 1)} className="grid h-9 w-9 place-items-center rounded-lg border border-white/10 text-zinc-300 disabled:opacity-30"><ChevronRight size={17} /></button></div></footer>
      </section>
    </div>
  );
}
