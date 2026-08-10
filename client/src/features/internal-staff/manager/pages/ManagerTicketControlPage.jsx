import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  AlertTriangle, CheckCircle2, ClipboardCheck, Clock3, RefreshCw, ScanLine, TicketCheck,
} from 'lucide-react';
import managerCinemaService from '@/features/internal-staff/manager/services/managerCinemaService';
import managerTicketControlService from '@/features/internal-staff/manager/services/managerTicketControlService';
import {
  auditoriumLabel, clock, dateTime, entryStatus, ticketScanResult,
} from '@/features/internal-staff/employee/employeePresentation';

const operationalDate = () => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());
  const value = Object.fromEntries(parts.map(part => [part.type, part.value]));
  return `${value.year}-${value.month}-${value.day}`;
};

const toneClass = tone => ({
  emerald: 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300',
  amber: 'border-amber-500/25 bg-amber-500/10 text-amber-300',
  red: 'border-red-500/25 bg-red-500/10 text-red-300',
  zinc: 'border-white/10 bg-white/[0.04] text-zinc-400',
}[tone] || 'border-white/10 bg-white/[0.04] text-zinc-400');

function EmptyState({ children }) {
  return <div className="rounded-2xl border border-dashed border-white/10 px-5 py-10 text-center text-sm text-zinc-500">{children}</div>;
}

export default function ManagerTicketControlPage() {
  const { selectedCinema, selectedCinemaId } = useOutletContext();
  const [date, setDate] = useState(operationalDate);
  const [state, setState] = useState({
    loading: true, error: '', summary: null, history: [], handoffs: [], staff: [],
  });

  const load = useCallback(async () => {
    if (!selectedCinemaId) return;
    setState(value => ({ ...value, loading: true, error: '' }));
    try {
      const [summary, history, handoffs, staff] = await Promise.all([
        managerTicketControlService.getSummary(selectedCinemaId, date),
        managerTicketControlService.getHistory(selectedCinemaId, date),
        managerTicketControlService.getHandoffs(selectedCinemaId, date),
        managerCinemaService.getStaff(selectedCinemaId),
      ]);
      setState({ loading: false, error: '', summary, history, handoffs, staff });
    } catch (error) {
      setState(value => ({
        ...value,
        loading: false,
        error: error?.message || 'Không thể tải dữ liệu soát vé tại rạp.',
      }));
    }
  }, [date, selectedCinemaId]);

  useEffect(() => {
    load();
  }, [load]);

  const staffNames = useMemo(() => new Map(
    state.staff.map(employee => [Number(employee.accountId), employee.fullName]),
  ), [state.staff]);
  const incidents = state.history.filter(item => item.result !== 'ADMITTED');
  const summary = state.summary || {};

  if (!selectedCinemaId) {
    return <EmptyState>Quản trị viên cần phân công rạp trước khi bạn theo dõi soát vé.</EmptyState>;
  }

  return (
    <section className="space-y-6">
      <header className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.22em] text-brand-orange">Giám sát lối vào tại rạp</p>
          <h1 className="mt-2 text-3xl font-black md:text-4xl">Soát vé & bàn giao</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-500">Theo dõi tiến độ đón khách, các lượt bị từ chối và biên bản bàn giao của nhân viên tại {selectedCinema?.name}.</p>
        </div>
        <div className="flex flex-col gap-3 sm:flex-row">
          <label className="text-xs font-bold text-zinc-500">Ngày vận hành
            <input type="date" aria-label="Ngày vận hành" value={date} onChange={event => setDate(event.target.value)} className="mt-2 block min-h-11 rounded-xl border border-white/10 bg-zinc-900 px-4 text-sm font-bold text-white outline-none focus:border-brand-orange/50" />
          </label>
          <button type="button" onClick={load} disabled={state.loading} className="mt-auto inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-white/10 px-4 text-sm font-black text-zinc-200 hover:bg-white/5 disabled:opacity-50"><RefreshCw size={16} className={state.loading ? 'animate-spin' : ''} /> Lấy dữ liệu mới</button>
        </div>
      </header>

      {state.error ? <p className="rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-200">{state.error}</p> : null}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Tổng lượt quét', summary.totalScans || 0, ScanLine, 'text-sky-300'],
          ['Khách đã vào', summary.admittedTickets || 0, CheckCircle2, 'text-emerald-300'],
          ['Lượt cần kiểm tra', summary.rejectedScans || 0, AlertTriangle, 'text-red-300'],
          ['Khách còn chưa vào', summary.remainingTickets || 0, TicketCheck, 'text-amber-300'],
        ].map(([label, value, Icon, tone]) => (
          <article key={label} className="rounded-2xl border border-white/10 bg-white/[0.025] p-5">
            <div className="flex items-center justify-between"><p className="text-xs font-bold text-zinc-500">{label}</p><Icon size={18} className={tone} /></div>
            <p className={`mt-4 text-3xl font-black ${tone}`}>{value}</p>
          </article>
        ))}
      </div>

      <article className="rounded-2xl border border-white/10 bg-white/[0.02] p-5 md:p-6">
        <div className="mb-5 flex items-start gap-3">
          <span className="rounded-xl bg-brand-orange/10 p-2 text-brand-orange"><Clock3 size={20} /></span>
          <div><h2 className="text-lg font-black">Tiến độ đón khách theo suất</h2><p className="mt-1 text-sm text-zinc-500">Ưu tiên suất đang mở cửa và còn nhiều khách chưa vào.</p></div>
        </div>
        {summary.showtimes?.length ? (
          <div className="grid gap-3 lg:grid-cols-2">
            {summary.showtimes.map(showtime => {
              const status = entryStatus(showtime.entryStatus);
              const progress = showtime.totalTickets
                ? Math.round((showtime.admittedTickets / showtime.totalTickets) * 100) : 0;
              return (
                <div key={`${showtime.showtimePublicId}-${showtime.showtimeStart}`} className="rounded-xl border border-white/10 bg-black/20 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div><p className="font-black text-white">{showtime.movieTitle}</p><p className="mt-1 text-xs text-zinc-500">{clock(showtime.showtimeStart)} – {clock(showtime.showtimeEnd)} · {auditoriumLabel(showtime.auditoriumName)}</p></div>
                    <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${toneClass(status.tone)}`}>{status.label}</span>
                  </div>
                  <div className="mt-4 h-2 overflow-hidden rounded-full bg-white/5"><div className="h-full rounded-full bg-emerald-400" style={{ width: `${progress}%` }} /></div>
                  <p className="mt-2 text-xs text-zinc-500"><strong className="text-zinc-200">{showtime.admittedTickets}/{showtime.totalTickets}</strong> vé đã vào · còn {showtime.remainingTickets}</p>
                </div>
              );
            })}
          </div>
        ) : <EmptyState>Chưa có vé phát hành cần soát trong ngày này.</EmptyState>}
      </article>

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <article className="rounded-2xl border border-white/10 bg-white/[0.02] p-5 md:p-6">
          <div className="mb-5"><h2 className="text-lg font-black">Lượt quét cần quản lý chú ý</h2><p className="mt-1 text-sm text-zinc-500">Vé trùng, sai rạp, quá sớm hoặc không còn hiệu lực.</p></div>
          {incidents.length ? (
            <div className="space-y-3">
              {incidents.slice(0, 20).map(item => {
                const result = ticketScanResult(item.result);
                return (
                  <div key={item.eventPublicId} className="rounded-xl border border-white/10 bg-black/20 p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div><p className="font-black text-zinc-100">{item.ticketCode || 'Mã không xác định'}</p><p className="mt-1 text-xs text-zinc-500">{staffNames.get(Number(item.employeeAccountId)) || `Nhân viên #${item.employeeAccountId}`} · {item.gateLabel || 'Chưa ghi cửa'} · {dateTime(item.scannedAt)}</p></div>
                      <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${toneClass(result.tone)}`}>{result.shortLabel}</span>
                    </div>
                    <p className="mt-3 text-sm leading-6 text-zinc-400">{item.message}</p>
                  </div>
                );
              })}
            </div>
          ) : <EmptyState>Không có lượt quét bị từ chối trong ngày.</EmptyState>}
        </article>

        <article className="rounded-2xl border border-white/10 bg-white/[0.02] p-5 md:p-6">
          <div className="mb-5 flex items-start gap-3">
            <span className="rounded-xl bg-amber-500/10 p-2 text-amber-300"><ClipboardCheck size={20} /></span>
            <div><h2 className="text-lg font-black">Biên bản bàn giao</h2><p className="mt-1 text-sm text-zinc-500">Nội dung nhân viên chuyển cho quản lý hoặc ca sau.</p></div>
          </div>
          {state.handoffs.length ? (
            <div className="space-y-3">
              {state.handoffs.map(item => (
                <div key={item.publicId} className="rounded-xl border border-white/10 bg-black/20 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div><p className="font-black text-zinc-100">{staffNames.get(Number(item.employeeAccountId)) || `Nhân viên #${item.employeeAccountId}`}</p><p className="mt-1 text-xs text-zinc-500">{item.gateLabel || 'Chưa ghi cửa'} · {dateTime(item.handedOffAt)}</p></div>
                    {item.unresolvedIncidents ? <span className="rounded-full border border-red-500/20 bg-red-500/10 px-2.5 py-1 text-[10px] font-black text-red-300">{item.unresolvedIncidents} sự cố còn tồn</span> : <span className="rounded-full border border-emerald-500/20 bg-emerald-500/10 px-2.5 py-1 text-[10px] font-black text-emerald-300">Đã bàn giao đủ</span>}
                  </div>
                  <p className="mt-3 text-sm leading-6 text-zinc-400">{item.note || 'Không có ghi chú bổ sung.'}</p>
                  <p className="mt-3 text-xs text-zinc-600">{item.totalScans} lượt quét · {item.successfulScans} đã vào · {item.rejectedScans} bị từ chối</p>
                </div>
              ))}
            </div>
          ) : <EmptyState>Chưa có biên bản bàn giao trong ngày.</EmptyState>}
        </article>
      </div>
    </section>
  );
}
