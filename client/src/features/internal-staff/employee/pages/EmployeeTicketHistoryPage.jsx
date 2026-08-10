import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  CalendarDays, CheckCircle2, CircleAlert, Clock3, History,
  LoaderCircle, RefreshCw, Search, Ticket, XCircle,
} from 'lucide-react';
import { getTicketScanHistory } from '../services/employeeTicketCheckerService';
import { auditoriumLabel, clock, dateTime, TICKET_SCAN_RESULTS, ticketScanResult } from '../employeePresentation';

const localDate = (date = new Date()) => new Date(
  date.getTime() - date.getTimezoneOffset() * 60_000,
).toISOString().slice(0, 10);

const toneClass = tone => ({
  emerald: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300',
  amber: 'border-amber-500/30 bg-amber-500/10 text-amber-300',
  red: 'border-red-500/30 bg-red-500/10 text-red-300',
}[tone] || 'border-zinc-700 bg-zinc-800 text-zinc-300');

export default function EmployeeTicketHistoryPage() {
  const [date, setDate] = useState(localDate());
  const [result, setResult] = useState('');
  const [keyword, setKeyword] = useState('');
  const [rows, setRows] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      setRows(await getTicketScanHistory({ date, result }));
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.response?.data?.message || 'Không tải được lịch sử soát vé.' });
    }
  }, [date, result]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const filtered = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    if (!term) return rows;
    return rows.filter(item => [item.ticketCode, item.bookingCode, item.movieTitle, item.seatLabel, item.message]
      .some(value => String(value || '').toLowerCase().includes(term)));
  }, [keyword, rows]);

  const admitted = rows.filter(item => item.result === 'ADMITTED').length;
  const duplicates = rows.filter(item => item.result === 'ALREADY_USED').length;
  const rejected = rows.length - admitted;

  return (
    <section className="mx-auto w-full max-w-7xl space-y-6 text-white">
      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 md:p-8"><p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Theo dõi và xử lý ngoại lệ</p><h1 className="mt-2 text-3xl font-black">Lịch sử soát & sự cố</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">Tra cứu các lượt quét của chính bạn, nhận biết vé trùng và bàn giao sự cố cho quản lý.</p></header>

      <div className="grid gap-4 sm:grid-cols-3">{[
        ['Tổng lượt quét', rows.length, History, 'text-sky-300'],
        ['Đã cho khách vào', admitted, CheckCircle2, 'text-emerald-300'],
        ['Bị từ chối / cần kiểm tra', rejected, duplicates ? CircleAlert : XCircle, 'text-red-300'],
      ].map(([label, value, Icon, tone]) => <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5"><div className="flex items-center justify-between"><p className="text-xs font-bold text-zinc-500">{label}</p><Icon className={tone} size={19} /></div><p className={`mt-3 text-2xl font-black ${tone}`}>{value}</p></article>)}</div>

      <div className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4 md:grid-cols-[1fr_190px_220px_auto] md:items-end"><label className="space-y-2"><span className="text-xs font-bold text-zinc-500">Tìm nhanh</span><div className="flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-950 px-3"><Search size={16} className="text-zinc-500" /><input value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="Mã vé, phim, ghế hoặc nội dung lỗi" className="w-full bg-transparent py-3 text-sm outline-none" /></div></label><label className="space-y-2"><span className="text-xs font-bold text-zinc-500">Ngày soát vé</span><div className="flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-950 px-3"><CalendarDays size={16} className="text-amber-400" /><input type="date" value={date} onChange={event => setDate(event.target.value)} className="w-full bg-transparent py-3 text-sm outline-none" /></div></label><label className="space-y-2"><span className="text-xs font-bold text-zinc-500">Kết quả</span><select value={result} onChange={event => setResult(event.target.value)} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 text-sm outline-none"><option value="">Tất cả kết quả</option>{Object.entries(TICKET_SCAN_RESULTS).map(([value, item]) => <option key={value} value={value}>{item.label}</option>)}</select></label><button type="button" onClick={load} className="flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-3 text-sm font-black"><RefreshCw className={state.loading ? 'animate-spin' : ''} size={17} /> Làm mới</button></div>

      {state.error ? <p className="rounded-xl border border-red-500/25 bg-red-500/10 p-4 text-sm text-red-200">{state.error}</p> : null}
      <article className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/60">
        {state.loading ? <div className="flex min-h-72 items-center justify-center gap-3 text-zinc-500"><LoaderCircle className="animate-spin" /> Đang tải lịch sử…</div> : filtered.length ? <div className="overflow-x-auto"><table className="min-w-[980px] w-full text-left text-sm"><thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-wider text-zinc-600"><tr><th className="p-4">Thời gian</th><th className="p-4">Vé / kết quả</th><th className="p-4">Phim và suất</th><th className="p-4">Phòng / ghế</th><th className="p-4">Hướng xử lý</th></tr></thead><tbody className="divide-y divide-white/5">{filtered.map(item => { const presentation = ticketScanResult(item.result); return <tr key={item.eventPublicId} className="align-top hover:bg-white/[0.02]"><td className="p-4"><p className="font-bold">{dateTime(item.scannedAt)}</p><p className="mt-1 text-xs text-zinc-600">{item.gateLabel || 'Chưa ghi cửa soát'}</p></td><td className="p-4"><span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black ${toneClass(presentation.tone)}`}>{presentation.shortLabel}</span><p className="mt-2 max-w-48 break-all font-mono text-[11px] text-zinc-500">{item.ticketCode || 'Không nhận diện được mã vé'}</p></td><td className="p-4"><p className="max-w-64 font-bold">{item.movieTitle || 'Chưa xác định phim'}</p><p className="mt-1 flex items-center gap-1 text-xs text-zinc-600"><Clock3 size={13} /> {item.showtimeStart ? clock(item.showtimeStart) : 'Chưa rõ giờ'}</p></td><td className="p-4"><p className="font-bold">{item.auditoriumName ? auditoriumLabel(item.auditoriumName) : 'Chưa xác định'}</p><p className="mt-1 text-xs text-zinc-600">{item.seatLabel ? `Ghế ${item.seatLabel}` : 'Chưa có ghế'}</p></td><td className="p-4"><p className={`max-w-80 text-sm leading-6 ${item.admitted ? 'text-emerald-300' : 'text-zinc-300'}`}>{item.message}</p>{!item.admitted ? <p className="mt-2 text-[11px] font-bold text-amber-300">Không tự ý cho khách vào; chuyển quầy vé hoặc quản lý khi cần.</p> : null}</td></tr>; })}</tbody></table></div> : <div className="py-20 text-center"><Ticket className="mx-auto text-zinc-700" size={42} /><h2 className="mt-4 font-black text-zinc-400">Không có lượt quét phù hợp</h2><p className="mt-2 text-sm text-zinc-600">Thử đổi ngày, kết quả hoặc từ khóa tìm kiếm.</p></div>}
      </article>
    </section>
  );
}
