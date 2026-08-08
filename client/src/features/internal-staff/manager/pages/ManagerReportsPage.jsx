import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { AlertCircle, Banknote, BarChart3, RefreshCw, Ticket, Users } from 'lucide-react';
import managerCinemaService from '../services/managerCinemaService';

const inputDate = date => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
};

const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0,
}).format(Number(value || 0));

const percent = value => `${Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`;

const MetricCard = ({ icon: Icon, label, value, note }) => <article className="rounded-2xl border border-white/10 bg-white/[0.025] p-5"><div className="flex items-start justify-between"><div><p className="text-xs font-bold text-zinc-500">{label}</p><p className="mt-2 text-2xl font-black">{value}</p></div><span className="grid h-10 w-10 place-items-center rounded-xl bg-brand-orange/10 text-brand-orange"><Icon size={19} /></span></div><p className="mt-3 text-xs text-zinc-600">{note}</p></article>;

export default function ManagerReportsPage() {
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const today = useMemo(() => new Date(), []);
  const [startDate, setStartDate] = useState(inputDate(new Date(today.getTime() - 29 * 86400000)));
  const [endDate, setEndDate] = useState(inputDate(today));
  const [state, setState] = useState({ loading: true, error: '', report: null });

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState(current => ({ ...current, loading: true, error: '' }));
    try {
      const report = await managerCinemaService.getCinemaReport({ startDate, endDate, cinemaKey: selectedCinemaId });
      setState({ loading: false, error: '', report });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải báo cáo của rạp.', report: null });
    }
  }, [endDate, selectedCinemaId, startDate]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp…</p>;
  if (!selectedCinema) return <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center">Chưa có rạp được phân công.</div>;

  const summary = state.report?.summary || {};
  const daily = state.report?.daily || [];
  const maxRevenue = Math.max(...daily.map(item => Number(item.netRevenue || 0)), 1);

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between"><div><p className="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">Kết quả kinh doanh tại rạp</p><h1 className="mt-2 text-3xl font-black">Báo cáo vận hành</h1><p className="mt-2 text-sm text-zinc-500">Doanh thu, lượng vé và tỷ lệ lấp đầy chỉ của <strong className="text-zinc-300">{selectedCinema.name}</strong>.</p></div><div className="flex flex-wrap items-end gap-3"><label className="text-xs font-bold text-zinc-500">Từ ngày<input type="date" value={startDate} onChange={event => setStartDate(event.target.value)} className="mt-2 block min-h-10 rounded-xl border border-white/10 bg-zinc-900 px-3 text-white" /></label><label className="text-xs font-bold text-zinc-500">Đến ngày<input type="date" value={endDate} onChange={event => setEndDate(event.target.value)} className="mt-2 block min-h-10 rounded-xl border border-white/10 bg-zinc-900 px-3 text-white" /></label><button type="button" onClick={load} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-white px-4 text-sm font-black text-black"><RefreshCw size={16} /> Cập nhật</button></div></header>

      {state.error ? <p className="rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-200">{state.error}</p> : null}
      {state.loading ? <div className="grid min-h-[45vh] place-items-center text-sm font-bold text-zinc-500">Đang tổng hợp số liệu của rạp…</div> : <>
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><MetricCard icon={Banknote} label="Doanh thu thực nhận" value={money(summary.netRevenue)} note={`Tổng trước giảm giá: ${money(summary.grossRevenue)}`} /><MetricCard icon={Ticket} label="Số vé đã bán" value={Number(summary.ticketCount || 0).toLocaleString('vi-VN')} note={`${Number(summary.bookingCount || 0).toLocaleString('vi-VN')} đơn đặt vé`} /><MetricCard icon={BarChart3} label="Tỷ lệ lấp đầy" value={percent(summary.occupancyRate)} note="Tỷ lệ ghế bán được trên tổng ghế mở bán." /><MetricCard icon={Users} label="Giá trị đơn trung bình" value={money(summary.averageBookingValue)} note={`Tỷ lệ hoàn tiền: ${percent(summary.refundRate)}`} /></section>

        <section className="grid gap-5 xl:grid-cols-[1.5fr_1fr]"><article className="rounded-2xl border border-white/10 bg-white/[0.02] p-5"><div className="flex items-center justify-between"><div><h2 className="font-black">Doanh thu theo ngày</h2><p className="mt-1 text-xs text-zinc-600">So sánh nhanh để nhận biết ngày vận hành tốt hoặc bất thường.</p></div><BarChart3 className="text-brand-orange" size={20} /></div>{daily.length ? <div className="mt-6 space-y-3">{daily.slice(-14).map(item => <div key={item.statDate} className="grid grid-cols-[84px_1fr_130px] items-center gap-3"><span className="text-xs font-bold text-zinc-500">{new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(new Date(`${item.statDate}T00:00:00`))}</span><div className="h-3 overflow-hidden rounded-full bg-white/5"><div className="h-full rounded-full bg-brand-orange" style={{ width: `${Math.max(2, Number(item.netRevenue || 0) / maxRevenue * 100)}%` }} /></div><span className="text-right text-xs font-bold text-zinc-300">{money(item.netRevenue)}</span></div>)}</div> : <p className="mt-10 text-center text-sm text-zinc-500">Chưa có số liệu trong khoảng ngày này.</p>}</article><article className="rounded-2xl border border-white/10 bg-white/[0.02] p-5"><h2 className="font-black">Phim mang lại doanh thu cao</h2><p className="mt-1 text-xs text-zinc-600">Ưu tiên theo dõi lịch chiếu của các phim này.</p><div className="mt-5 space-y-3">{(state.report?.topMovies || []).slice(0, 6).map((movie, index) => <div key={movie.movieKey} className="flex items-center gap-3 rounded-xl bg-white/[0.025] p-3"><span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-brand-orange/10 text-xs font-black text-brand-orange">{index + 1}</span><div className="min-w-0 flex-1"><p className="truncate text-sm font-bold">{movie.movieTitle}</p><p className="mt-1 text-xs text-zinc-600">{movie.ticketCount || 0} vé</p></div><p className="text-xs font-black text-zinc-300">{money(movie.netRevenue)}</p></div>)}{!(state.report?.topMovies || []).length ? <p className="py-8 text-center text-sm text-zinc-500">Chưa có dữ liệu phim.</p> : null}</div></article></section>

        {(state.report?.alerts || []).length ? <section className="rounded-2xl border border-amber-500/20 bg-amber-500/[0.05] p-5"><div className="flex items-center gap-2 text-amber-300"><AlertCircle size={19} /><h2 className="font-black">Điểm cần quản lý chú ý</h2></div><div className="mt-4 grid gap-3 md:grid-cols-2">{state.report.alerts.filter(item => !item.resolved).slice(0, 4).map(item => <article key={item.id} className="rounded-xl border border-amber-500/10 bg-black/20 p-4"><p className="text-sm font-black text-amber-100">{item.title}</p><p className="mt-2 text-xs leading-5 text-amber-100/60">{item.message}</p></article>)}</div></section> : null}
      </>}
    </div>
  );
}
