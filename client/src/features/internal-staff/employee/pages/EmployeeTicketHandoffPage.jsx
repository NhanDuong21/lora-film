import { useCallback, useEffect, useState } from 'react';
import {
  CheckCircle2, ClipboardCheck, History, LoaderCircle, MapPin,
  RefreshCw, ShieldAlert, Ticket, Users,
} from 'lucide-react';
import { getMyEmployeeCinemaContext } from '../services/employeeBoxOfficeService';
import {
  getTicketCheckerSummary, getTicketGateHandoffs, saveTicketGateHandoff,
} from '../services/employeeTicketCheckerService';
import { dateTime } from '../employeePresentation';

export default function EmployeeTicketHandoffPage() {
  const [context, setContext] = useState(null);
  const [summary, setSummary] = useState(null);
  const [history, setHistory] = useState([]);
  const [form, setForm] = useState({ gateLabel: 'Cửa phòng 01', unresolvedIncidents: 0, note: '' });
  const [state, setState] = useState({ loading: true, saving: false, error: '', success: '' });

  const load = useCallback(async () => {
    setState(value => ({ ...value, loading: true, error: '' }));
    try {
      const [employee, current, rows] = await Promise.all([
        getMyEmployeeCinemaContext(), getTicketCheckerSummary(), getTicketGateHandoffs(),
      ]);
      setContext(employee);
      setSummary(current);
      setHistory(rows);
      if (current?.handoff) setForm({
        gateLabel: current.handoff.gateLabel || 'Cửa phòng 01',
        unresolvedIncidents: current.handoff.unresolvedIncidents || 0,
        note: current.handoff.note || '',
      });
      setState(value => ({ ...value, loading: false }));
    } catch (error) {
      setState(value => ({ ...value, loading: false, error: error?.response?.data?.message || 'Không tải được dữ liệu bàn giao.' }));
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const submit = async event => {
    event.preventDefault();
    if (!form.gateLabel.trim()) {
      setState(value => ({ ...value, error: 'Vui lòng nhập cửa hoặc khu vực đang trực.', success: '' }));
      return;
    }
    setState(value => ({ ...value, saving: true, error: '', success: '' }));
    try {
      await saveTicketGateHandoff({
        gateLabel: form.gateLabel.trim(),
        unresolvedIncidents: Number(form.unresolvedIncidents || 0),
        note: form.note.trim(),
      });
      setState(value => ({ ...value, saving: false, success: 'Đã lưu biên bản bàn giao ca soát vé.' }));
      await load();
    } catch (error) {
      setState(value => ({ ...value, saving: false, error: error?.response?.data?.message || 'Không lưu được bàn giao ca.', success: '' }));
    }
  };

  return (
    <section className="mx-auto w-full max-w-7xl space-y-6 text-white">
      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 md:p-8"><div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"><div><p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Kết thúc nhiệm vụ tại cửa</p><h1 className="mt-2 text-3xl font-black">Bàn giao ca soát vé</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">Xác nhận số lượt đã xử lý, ghi rõ sự cố còn tồn và bàn giao cho quản lý hoặc ca tiếp theo.</p></div><div className="rounded-2xl border border-emerald-500/25 bg-emerald-500/[0.06] px-4 py-3"><p className="flex items-center gap-2 text-[10px] font-black uppercase text-emerald-400"><MapPin size={14} /> Rạp đang làm việc</p><p className="mt-1 font-black">{context?.cinemaName || 'Đang xác định rạp'}</p></div></div></header>

      {state.loading ? <div className="flex min-h-64 items-center justify-center gap-3 text-zinc-500"><LoaderCircle className="animate-spin" /> Đang tổng hợp ca…</div> : <>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[
          ['Tổng lượt quét', summary?.totalScans || 0, Ticket, 'text-sky-300'],
          ['Khách đã vào', summary?.admittedScans || 0, CheckCircle2, 'text-emerald-300'],
          ['Lượt bị từ chối', summary?.rejectedScans || 0, ShieldAlert, 'text-red-300'],
          ['Khách còn chưa vào', summary?.remainingTickets || 0, Users, 'text-amber-300'],
        ].map(([label, value, Icon, tone]) => <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5"><div className="flex items-center justify-between"><p className="text-xs font-bold text-zinc-500">{label}</p><Icon className={tone} size={19} /></div><p className={`mt-3 text-2xl font-black ${tone}`}>{value}</p></article>)}</div>

        <div className="grid gap-6 xl:grid-cols-[1fr_0.8fr]">
          <form onSubmit={submit} className="rounded-3xl border border-amber-500/25 bg-amber-500/[0.04] p-6"><div className="flex items-center gap-2"><ClipboardCheck className="text-amber-400" /><h2 className="font-black">Biên bản bàn giao hôm nay</h2></div><p className="mt-2 text-sm leading-6 text-zinc-500">Kiểm tra số liệu phía trên trước khi lưu. Bạn có thể cập nhật lại nếu phát hiện thông tin chưa chính xác.</p><div className="mt-6 grid gap-5 sm:grid-cols-2"><label className="space-y-2"><span className="text-xs font-bold text-zinc-400">Cửa hoặc khu vực đã trực *</span><input value={form.gateLabel} onChange={event => setForm(value => ({ ...value, gateLabel: event.target.value }))} placeholder="Ví dụ: Cửa phòng 01" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none focus:border-amber-500" /></label><label className="space-y-2"><span className="text-xs font-bold text-zinc-400">Sự cố chưa xử lý xong</span><input type="number" min="0" max="999" value={form.unresolvedIncidents} onChange={event => setForm(value => ({ ...value, unresolvedIncidents: event.target.value }))} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none focus:border-amber-500" /></label></div><label className="mt-5 block space-y-2"><span className="text-xs font-bold text-zinc-400">Ghi chú cho quản lý hoặc ca sau</span><textarea rows="5" maxLength="1000" value={form.note} onChange={event => setForm(value => ({ ...value, note: event.target.value }))} placeholder="Nêu mã vé, tình huống và việc cần tiếp tục xử lý…" className="w-full resize-none rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm leading-6 outline-none focus:border-amber-500" /><span className="block text-right text-[11px] text-zinc-600">{form.note.length}/1000 ký tự</span></label>{state.error ? <p className="mt-4 rounded-xl border border-red-500/25 bg-red-500/10 p-3 text-sm text-red-200">{state.error}</p> : null}{state.success ? <p className="mt-4 rounded-xl border border-emerald-500/25 bg-emerald-500/10 p-3 text-sm text-emerald-200">{state.success}</p> : null}<button type="submit" disabled={state.saving} className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-amber-500 py-3 text-sm font-black text-black disabled:opacity-50">{state.saving ? <LoaderCircle className="animate-spin" size={18} /> : <ClipboardCheck size={18} />} Lưu bàn giao ca</button></form>

          <article className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6"><div className="flex items-center justify-between"><div className="flex items-center gap-2"><History className="text-sky-400" /><h2 className="font-black">Lịch sử bàn giao gần đây</h2></div><button type="button" onClick={load} className="rounded-lg border border-zinc-700 p-2" aria-label="Làm mới"><RefreshCw size={16} /></button></div>{history.length ? <div className="mt-5 space-y-3">{history.map(item => <div key={item.publicId} className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4"><div className="flex items-start justify-between gap-3"><div><p className="font-black">{item.gateLabel}</p><p className="mt-1 text-xs text-zinc-600">Bàn giao lúc {dateTime(item.handedOffAt)}</p></div><span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${item.unresolvedIncidents ? 'bg-amber-500/10 text-amber-300' : 'bg-emerald-500/10 text-emerald-300'}`}>{item.unresolvedIncidents ? `${item.unresolvedIncidents} việc còn lại` : 'Đã bàn giao đủ'}</span></div><div className="mt-4 grid grid-cols-3 gap-2 text-center"><div className="rounded-lg bg-zinc-900 p-2"><p className="text-[10px] text-zinc-600">Tổng</p><strong>{item.totalScans}</strong></div><div className="rounded-lg bg-zinc-900 p-2"><p className="text-[10px] text-zinc-600">Hợp lệ</p><strong className="text-emerald-300">{item.successfulScans}</strong></div><div className="rounded-lg bg-zinc-900 p-2"><p className="text-[10px] text-zinc-600">Từ chối</p><strong className="text-red-300">{item.rejectedScans}</strong></div></div>{item.note ? <p className="mt-3 text-xs leading-5 text-zinc-500">{item.note}</p> : null}</div>)}</div> : <p className="py-14 text-center text-sm text-zinc-600">Chưa có biên bản bàn giao.</p>}</article>
        </div>
      </>}
    </section>
  );
}
