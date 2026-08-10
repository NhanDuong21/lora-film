import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle, Banknote, CheckCircle2, Clock3, History, LoaderCircle,
  LockKeyhole, Printer, RefreshCw, ShieldCheck, WalletCards,
} from 'lucide-react';
import { dateTime, money } from '../employeePresentation';
import { getMyEmployeeCinemaContext } from '../services/employeeBoxOfficeService';
import {
  closeCounterSession,
  getCounterSessionHistory,
  getCurrentCounterSession,
  openCounterSession,
} from '../services/employeeOperationsService';

const amountValue = value => Number(String(value || '').replace(/[^0-9]/g, ''));

export default function EmployeeCashSessionPage() {
  const [context, setContext] = useState(null);
  const [session, setSession] = useState(null);
  const [history, setHistory] = useState([]);
  const [state, setState] = useState({ loading: true, error: '', message: '' });
  const [openingFloat, setOpeningFloat] = useState('1000000');
  const [openingNote, setOpeningNote] = useState('Nhận két và kiểm đếm tiền đầu ca');
  const [countedCash, setCountedCash] = useState('');
  const [closingNote, setClosingNote] = useState('');
  const [working, setWorking] = useState(false);
  const [closedReceipt, setClosedReceipt] = useState(null);

  const load = useCallback(async () => {
    setState(current => ({ ...current, loading: true, error: '' }));
    try {
      const [workContext, currentSession, sessionHistory] = await Promise.all([
        getMyEmployeeCinemaContext(),
        getCurrentCounterSession(),
        getCounterSessionHistory(),
      ]);
      setContext(workContext);
      setSession(currentSession || null);
      setHistory(sessionHistory || []);
      setState(current => ({ ...current, loading: false, error: '' }));
    } catch (error) {
      setState(current => ({
        ...current,
        loading: false,
        error: error?.message === 'Network Error'
          ? 'Không thể kết nối dịch vụ ca thu ngân. Vui lòng thử lại sau ít phút.'
          : error?.message || 'Chưa thể tải thông tin ca thu ngân. Vui lòng thử lại.',
      }));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const counted = amountValue(countedCash);
  const expected = Number(session?.expectedCash || 0);
  const previewVariance = countedCash === '' ? null : counted - expected;
  const closeNoteRequired = previewVariance !== null && previewVariance !== 0;

  const openShift = async event => {
    event.preventDefault();
    setWorking(true);
    setState(current => ({ ...current, error: '', message: '' }));
    try {
      const result = await openCounterSession({
        openingFloat: amountValue(openingFloat),
        note: openingNote.trim() || null,
      });
      setSession(result);
      setState(current => ({ ...current, message: 'Đã mở ca thu ngân. Bạn có thể bắt đầu bán vé và thu tiền.' }));
      await load();
    } catch (error) {
      setState(current => ({ ...current, error: error?.message || 'Không thể mở ca thu ngân.' }));
    } finally {
      setWorking(false);
    }
  };

  const closeShift = async event => {
    event.preventDefault();
    if (closeNoteRequired && closingNote.trim().length < 5) {
      setState(current => ({ ...current, error: 'Có chênh lệch tiền. Vui lòng ghi rõ nguyên nhân hoặc tình trạng bàn giao.' }));
      return;
    }
    setWorking(true);
    setState(current => ({ ...current, error: '', message: '' }));
    try {
      const result = await closeCounterSession(session.publicId, {
        countedCash: counted,
        note: closingNote.trim() || 'Đã kiểm đếm và bàn giao đủ tiền',
      });
      setClosedReceipt(result);
      setSession(null);
      setCountedCash('');
      setClosingNote('');
      setState(current => ({ ...current, message: 'Đã chốt ca và lưu biên bản bàn giao.' }));
      await load();
    } catch (error) {
      setState(current => ({ ...current, error: error?.message || 'Không thể chốt ca thu ngân.' }));
    } finally {
      setWorking(false);
    }
  };

  const printHandover = () => {
    const className = 'printing-cash-handover';
    const cleanup = () => document.body.classList.remove(className);
    document.body.classList.add(className);
    window.addEventListener('afterprint', cleanup, { once: true });
    window.print();
    window.setTimeout(cleanup, 1000);
  };

  const latestClosed = useMemo(() => closedReceipt || history.find(item => item.status === 'CLOSED'), [closedReceipt, history]);

  if (state.loading) return <div className="flex min-h-[60vh] items-center justify-center gap-3 text-zinc-500"><LoaderCircle className="animate-spin" /> Đang tải ca thu ngân…</div>;

  return (
    <section className="mx-auto w-full max-w-7xl space-y-6 text-white">
      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 md:p-8">
        <p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Đối chiếu tiền mặt</p>
        <div className="mt-2 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between"><div><h1 className="text-3xl font-black">Chốt ca & bàn giao</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">Mở két đầu ca, theo dõi số tiền hệ thống dự kiến và ghi nhận tiền thực tế khi bàn giao.</p></div><div className="rounded-2xl border border-emerald-500/25 bg-emerald-500/[0.06] px-4 py-3"><p className="text-[10px] font-black uppercase text-emerald-400">Rạp đang làm việc</p><p className="mt-1 font-black">{context?.cinemaName || 'Rạp được phân công'}</p></div></div>
      </header>

      {state.message ? <p className="rounded-xl border border-emerald-500/25 bg-emerald-500/10 p-4 text-sm font-bold text-emerald-200">{state.message}</p> : null}
      {state.error ? <p className="rounded-xl border border-red-500/25 bg-red-500/10 p-4 text-sm font-bold text-red-200">{state.error}</p> : null}

      {!session ? (
        <div className="grid gap-6 xl:grid-cols-[1fr_.8fr]">
          <form onSubmit={openShift} className="rounded-3xl border border-amber-500/25 bg-zinc-900/70 p-6 md:p-8">
            <div className="flex items-start gap-3"><span className="grid h-11 w-11 place-items-center rounded-2xl bg-amber-500/10 text-amber-400"><LockKeyhole size={21} /></span><div><h2 className="text-xl font-black">Mở ca thu ngân</h2><p className="mt-1 text-sm text-zinc-500">Kiểm đếm tiền có sẵn trong két trước giao dịch đầu tiên.</p></div></div>
            <label className="mt-6 block text-xs font-black text-zinc-300">Tiền đầu ca *<input required inputMode="numeric" value={openingFloat} onChange={event => setOpeningFloat(event.target.value)} className="mt-2 h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-lg font-black outline-none focus:border-amber-500" /></label>
            <div className="mt-3 flex flex-wrap gap-2">{[500000, 1000000, 2000000].map(value => <button key={value} type="button" onClick={() => setOpeningFloat(String(value))} className="rounded-lg border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-400 hover:border-amber-500 hover:text-amber-300">{money(value)}</button>)}</div>
            <label className="mt-5 block text-xs font-black text-zinc-300">Ghi chú nhận két<textarea value={openingNote} onChange={event => setOpeningNote(event.target.value)} maxLength={500} className="mt-2 min-h-24 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-4 text-sm font-normal outline-none focus:border-amber-500" /></label>
            <button disabled={working} className="mt-5 w-full rounded-xl bg-amber-500 py-3.5 font-black text-black disabled:opacity-50">{working ? 'Đang mở ca…' : 'Xác nhận mở ca thu ngân'}</button>
          </form>
          <article className="rounded-3xl border border-sky-500/20 bg-sky-500/[0.05] p-6 md:p-8"><ShieldCheck className="text-sky-300" size={30} /><h2 className="mt-4 text-lg font-black text-sky-100">Quy trình an toàn</h2><ol className="mt-4 space-y-4 text-sm leading-6 text-sky-100/70"><li><strong className="text-sky-100">1. Kiểm đếm đầu ca:</strong> chỉ nhập số tiền thực tế đang có trong két.</li><li><strong className="text-sky-100">2. Bán hàng trong ca:</strong> hệ thống tự cộng các khoản thu và trừ hoàn tiền mặt do chính bạn thực hiện.</li><li><strong className="text-sky-100">3. Chốt ca:</strong> kiểm đếm lại, ghi nguyên nhân nếu có chênh lệch và bàn giao cho quản lý.</li></ol></article>
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
            {[
              ['Tiền đầu ca', money(session.openingFloat), WalletCards, 'text-sky-300'],
              ['Tiền bán vé', money(session.cashSales), Banknote, 'text-emerald-300'],
              ['Tiền đã hoàn', money(session.cashRefunds), RefreshCw, 'text-red-300'],
              ['Giao dịch đã thu', session.cashTransactionCount, CheckCircle2, 'text-violet-300'],
              ['Hệ thống dự kiến', money(session.expectedCash), ShieldCheck, 'text-amber-300'],
            ].map(([label, value, Icon, tone]) => <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-5"><div className="flex items-center justify-between"><p className="text-xs font-bold text-zinc-500">{label}</p><Icon size={18} className={tone} /></div><p className={`mt-3 text-xl font-black ${tone}`}>{value}</p></article>)}
          </div>

          <form onSubmit={closeShift} className="grid gap-6 rounded-3xl border border-amber-500/25 bg-zinc-900/70 p-6 md:p-8 xl:grid-cols-[1fr_.8fr]">
            <div><div className="flex items-center gap-2"><Clock3 className="text-amber-400" size={20} /><h2 className="text-xl font-black">Kiểm đếm và chốt ca</h2></div><p className="mt-2 text-sm text-zinc-500">Ca được mở lúc {dateTime(session.openedAt)}. Hệ thống không tính tiền thối vào doanh thu.</p><label className="mt-6 block text-xs font-black text-zinc-300">Tiền thực tế trong két *<input required inputMode="numeric" value={countedCash} onChange={event => setCountedCash(event.target.value)} placeholder="Nhập số tiền đã kiểm đếm" className="mt-2 h-12 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 text-lg font-black outline-none focus:border-amber-500" /></label><label className="mt-5 block text-xs font-black text-zinc-300">Ghi chú bàn giao {closeNoteRequired ? '*' : ''}<textarea required={closeNoteRequired} minLength={closeNoteRequired ? 5 : undefined} value={closingNote} onChange={event => setClosingNote(event.target.value)} placeholder={closeNoteRequired ? 'Ghi rõ nguyên nhân hoặc tình trạng đang kiểm tra…' : 'Ví dụ: Đã kiểm đếm và bàn giao trực tiếp cho quản lý'} className="mt-2 min-h-28 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-4 text-sm font-normal outline-none focus:border-amber-500" /></label></div>
            <aside className={`rounded-2xl border p-5 ${previewVariance === null ? 'border-zinc-700 bg-zinc-950/70' : previewVariance === 0 ? 'border-emerald-500/30 bg-emerald-500/[0.07]' : 'border-red-500/30 bg-red-500/[0.07]'}`}><p className="text-xs font-black uppercase text-zinc-500">Đối chiếu trước khi chốt</p><div className="mt-5 space-y-3 text-sm"><div className="flex justify-between"><span className="text-zinc-400">Hệ thống dự kiến</span><strong>{money(expected)}</strong></div><div className="flex justify-between"><span className="text-zinc-400">Bạn đã kiểm đếm</span><strong>{countedCash === '' ? 'Chưa nhập' : money(counted)}</strong></div><div className="flex justify-between border-t border-zinc-700 pt-3 text-lg"><span className="font-black">Chênh lệch</span><strong className={previewVariance === 0 ? 'text-emerald-300' : previewVariance == null ? 'text-zinc-400' : 'text-red-300'}>{previewVariance === null ? '—' : money(previewVariance)}</strong></div></div>{previewVariance !== null && previewVariance !== 0 ? <p className="mt-4 flex gap-2 rounded-xl bg-red-500/10 p-3 text-xs leading-5 text-red-200"><AlertTriangle className="mt-0.5 shrink-0" size={15} /> Chỉ chốt sau khi đã kiểm đếm lại và ghi rõ tình trạng chênh lệch.</p> : null}<button disabled={working || countedCash === ''} className="mt-6 w-full rounded-xl bg-amber-500 py-3.5 font-black text-black disabled:opacity-40">{working ? 'Đang chốt ca…' : 'Chốt ca và lưu bàn giao'}</button></aside>
          </form>
        </div>
      )}

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/50">
        <div className="flex items-center justify-between border-b border-zinc-800 p-5"><div className="flex items-center gap-2"><History className="text-zinc-400" size={19} /><h2 className="font-black">Lịch sử bàn giao gần đây</h2></div><button type="button" onClick={load} className="text-xs font-black text-amber-400">Làm mới</button></div>
        {history.length ? <div className="divide-y divide-zinc-800">{history.map(item => <article key={item.publicId} className="grid gap-3 p-5 text-sm md:grid-cols-[1fr_.8fr_.8fr_.8fr] md:items-center"><div><p className="font-black">{item.status === 'OPEN' ? 'Ca đang mở' : `Ca ${dateTime(item.openedAt)}`}</p><p className="mt-1 text-xs text-zinc-500">{item.closedAt ? `Chốt lúc ${dateTime(item.closedAt)}` : 'Chưa chốt ca'}</p></div><div><p className="text-xs text-zinc-500">Hệ thống dự kiến</p><p className="mt-1 font-bold">{money(item.expectedCash)}</p></div><div><p className="text-xs text-zinc-500">Tiền kiểm đếm</p><p className="mt-1 font-bold">{item.countedCash == null ? '—' : money(item.countedCash)}</p></div><div><p className="text-xs text-zinc-500">Chênh lệch</p><p className={`mt-1 font-black ${Number(item.varianceAmount || 0) === 0 ? 'text-emerald-300' : 'text-red-300'}`}>{item.varianceAmount == null ? '—' : money(item.varianceAmount)}</p></div></article>)}</div> : <p className="py-10 text-center text-sm text-zinc-500">Chưa có ca thu ngân nào được bàn giao.</p>}
      </section>

      {latestClosed ? <section id="cash-handover-receipt" className="hidden space-y-4 bg-white p-6 text-black print:block"><div className="text-center"><p className="text-xl font-black tracking-widest">LORAFILM</p><p className="font-black uppercase">Biên bản chốt ca và bàn giao tiền mặt</p></div><div className="border-y border-black py-3 text-sm"><p><strong>Rạp:</strong> {context?.cinemaName}</p><p><strong>Mở ca:</strong> {dateTime(latestClosed.openedAt)}</p><p><strong>Chốt ca:</strong> {dateTime(latestClosed.closedAt)}</p></div><div className="space-y-2 text-sm"><p className="flex justify-between"><span>Tiền đầu ca</span><strong>{money(latestClosed.openingFloat)}</strong></p><p className="flex justify-between"><span>Tiền bán vé</span><strong>{money(latestClosed.cashSales)}</strong></p><p className="flex justify-between"><span>Tiền đã hoàn</span><strong>-{money(latestClosed.cashRefunds)}</strong></p><p className="flex justify-between border-t border-black pt-2"><span>Hệ thống dự kiến</span><strong>{money(latestClosed.expectedCash)}</strong></p><p className="flex justify-between"><span>Tiền kiểm đếm</span><strong>{money(latestClosed.countedCash)}</strong></p><p className="flex justify-between"><span>Chênh lệch</span><strong>{money(latestClosed.varianceAmount)}</strong></p></div><p className="text-sm"><strong>Ghi chú:</strong> {latestClosed.closingNote || 'Đã kiểm đếm và bàn giao đủ tiền'}</p><div className="grid grid-cols-2 gap-10 pt-10 text-center text-sm"><p>Nhân viên bàn giao<br /><br /><br />________________</p><p>Quản lý nhận bàn giao<br /><br /><br />________________</p></div></section> : null}
      {latestClosed ? <button type="button" onClick={printHandover} className="flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-sm font-black"><Printer size={17} /> In biên bản bàn giao gần nhất</button> : null}
    </section>
  );
}
