import { useCallback, useEffect, useState } from 'react';
import {
  CalendarDays, CheckCircle2, Clapperboard, Clock3, LoaderCircle,
  MapPin, RefreshCw, Ticket, Users,
} from 'lucide-react';
import { getMyEmployeeCinemaContext } from '../services/employeeBoxOfficeService';
import { getTicketCheckerShowtimes } from '../services/employeeTicketCheckerService';
import { auditoriumLabel, clock, entryStatus } from '../employeePresentation';

const localDate = (date = new Date()) => new Date(
  date.getTime() - date.getTimezoneOffset() * 60_000,
).toISOString().slice(0, 10);

const statusClass = tone => ({
  emerald: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300',
  amber: 'border-amber-500/30 bg-amber-500/10 text-amber-300',
  zinc: 'border-zinc-700 bg-zinc-800 text-zinc-400',
}[tone]);

export default function EmployeeTicketShowtimesPage() {
  const [date, setDate] = useState(localDate());
  const [context, setContext] = useState(null);
  const [showtimes, setShowtimes] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [employee, rows] = await Promise.all([
        getMyEmployeeCinemaContext(),
        getTicketCheckerShowtimes(date),
      ]);
      setContext(employee);
      setShowtimes(rows);
    } catch (error) {
      setState({ loading: false, error: error?.response?.data?.message || 'Không tải được lịch suất chiếu.' });
      return;
    }
    setState({ loading: false, error: '' });
  }, [date]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const totals = showtimes.reduce((result, item) => ({
    tickets: result.tickets + Number(item.totalTickets || 0),
    admitted: result.admitted + Number(item.admittedTickets || 0),
    remaining: result.remaining + Number(item.remainingTickets || 0),
  }), { tickets: 0, admitted: 0, remaining: 0 });

  return (
    <section className="mx-auto w-full max-w-7xl space-y-6 text-white">
      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 md:p-8">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"><div><p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Điều phối khách vào phòng</p><h1 className="mt-2 text-3xl font-black">Suất chiếu & cửa phòng</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">Ưu tiên các suất đang đón khách, theo dõi số vé đã vào và số khách còn lại.</p></div><div className="rounded-2xl border border-emerald-500/25 bg-emerald-500/[0.06] px-4 py-3"><p className="flex items-center gap-2 text-[10px] font-black uppercase text-emerald-400"><MapPin size={14} /> Rạp được phân công</p><p className="mt-1 font-black">{context?.cinemaName || 'Đang xác định rạp'}</p></div></div>
      </header>

      <div className="flex flex-col gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4 sm:flex-row sm:items-end sm:justify-between"><label className="space-y-2"><span className="text-xs font-bold text-zinc-500">Ngày vận hành</span><div className="flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-950 px-3"><CalendarDays size={16} className="text-amber-400" /><input type="date" value={date} onChange={event => setDate(event.target.value)} className="bg-transparent py-3 text-sm font-bold outline-none" /></div></label><button type="button" onClick={load} disabled={state.loading} className="flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-3 text-sm font-black"><RefreshCw className={state.loading ? 'animate-spin' : ''} size={17} /> Làm mới</button></div>

      <div className="grid gap-4 sm:grid-cols-3">{[
        ['Tổng vé cần phục vụ', totals.tickets, Ticket, 'text-sky-300'],
        ['Khách đã vào', totals.admitted, CheckCircle2, 'text-emerald-300'],
        ['Khách chưa vào', totals.remaining, Users, 'text-amber-300'],
      ].map(([label, value, Icon, tone]) => <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5"><div className="flex items-center justify-between"><p className="text-xs font-bold text-zinc-500">{label}</p><Icon className={tone} size={19} /></div><p className={`mt-3 text-2xl font-black ${tone}`}>{value}</p></article>)}</div>

      {state.error ? <p className="rounded-xl border border-red-500/25 bg-red-500/10 p-4 text-sm text-red-200">{state.error}</p> : null}
      {state.loading ? <div className="flex min-h-64 items-center justify-center gap-3 text-zinc-500"><LoaderCircle className="animate-spin" /> Đang tải lịch cửa phòng…</div> : showtimes.length ? <div className="grid gap-4 lg:grid-cols-2">{showtimes.map(item => { const status = entryStatus(item.entryStatus); const progress = item.totalTickets ? Math.round((Number(item.admittedTickets || 0) / item.totalTickets) * 100) : 0; return <article key={`${item.showtimePublicId}-${item.showtimeStart}`} className={`rounded-3xl border p-6 ${item.entryStatus === 'OPEN' ? 'border-emerald-500/30 bg-emerald-500/[0.05]' : 'border-zinc-800 bg-zinc-900/60'}`}><div className="flex items-start justify-between gap-4"><div><p className="flex items-center gap-2 text-xs font-black uppercase text-zinc-500"><Clapperboard size={15} className="text-amber-400" /> {auditoriumLabel(item.auditoriumName)}</p><h2 className="mt-2 text-xl font-black">{item.movieTitle}</h2><p className="mt-2 flex items-center gap-2 text-sm text-zinc-400"><Clock3 size={15} /> {clock(item.showtimeStart)} – {clock(item.showtimeEnd)}</p></div><span className={`rounded-full border px-3 py-1 text-[11px] font-black ${statusClass(status.tone)}`}>{status.label}</span></div><div className="mt-6"><div className="flex items-center justify-between text-sm"><span className="text-zinc-500">Tiến độ đón khách</span><strong>{item.admittedTickets}/{item.totalTickets} vé</strong></div><div className="mt-2 h-2 overflow-hidden rounded-full bg-zinc-800"><div className="h-full rounded-full bg-emerald-400" style={{ width: `${Math.min(100, progress)}%` }} /></div><div className="mt-4 grid grid-cols-2 gap-3"><div className="rounded-xl bg-zinc-950/60 p-3"><p className="text-[10px] font-black uppercase text-zinc-600">Đã vào</p><p className="mt-1 text-lg font-black text-emerald-300">{item.admittedTickets}</p></div><div className="rounded-xl bg-zinc-950/60 p-3"><p className="text-[10px] font-black uppercase text-zinc-600">Còn lại</p><p className="mt-1 text-lg font-black text-amber-300">{item.remainingTickets}</p></div></div></div></article>; })}</div> : <div className="rounded-3xl border border-dashed border-zinc-800 py-20 text-center"><Clapperboard className="mx-auto text-zinc-700" size={44} /><h2 className="mt-4 font-black text-zinc-400">Chưa có vé cần soát trong ngày</h2><p className="mt-2 text-sm text-zinc-600">Khi có đơn đã thanh toán tại rạp này, suất chiếu sẽ xuất hiện ở đây.</p></div>}
    </section>
  );
}
