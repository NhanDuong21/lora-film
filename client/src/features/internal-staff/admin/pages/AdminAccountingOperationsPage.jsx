import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  AlertTriangle, Banknote, CalendarClock, CheckCircle2, ChevronRight,
  ClipboardCheck, FileClock, FileSpreadsheet, Landmark, LockKeyhole,
  Plus, RefreshCcw, Scale, ShieldCheck, Trash2, Upload,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  applyAccountingPeriodAction,
  createAccountingPeriod,
  getAccountingAuditEvents,
  getAccountingCashSessions,
  getAccountingOverview,
  getAccountingPeriods,
  getSettlementBatch,
  getSettlementBatches,
  importSettlementBatch,
  lockSettlementBatch,
  verifyAccountingCashSession,
} from '@/features/payment/services/paymentService';
import {
  ActionModal, ConsolePagination, ConsolePanel, DetailDrawer, DetailGrid,
  MetricStrip, OperationsHeader,
} from '../components/OperationsConsole';

const emptyPage = { content: [], totalPages: 0, totalElements: 0, number: 0 };
const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0,
}).format(Number(value) || 0);
const date = value => value ? new Date(value).toLocaleString('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
}) : 'Chưa ghi nhận';
const shortDate = value => value ? new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN') : '—';
const localDateValue = value => [
  value.getFullYear(),
  String(value.getMonth() + 1).padStart(2, '0'),
  String(value.getDate()).padStart(2, '0'),
].join('-');
const today = () => localDateValue(new Date());
const monthValue = () => today().slice(0, 7);
const monthRange = value => {
  const [year, month] = value.split('-').map(Number);
  return {
    periodCode: value,
    periodStart: `${value}-01`,
    periodEnd: localDateValue(new Date(year, month, 0)),
    cinemaPublicId: '',
    note: '',
  };
};
const apiMessage = error => error?.response?.data?.message
  || error?.response?.data?.error
  || error?.message
  || 'Không thể hoàn tất thao tác. Vui lòng thử lại.';

const SETTLEMENT_STATUS = {
  IMPORTED: ['Đã nhập', 'text-sky-300 border-sky-500/30 bg-sky-500/10'],
  NEEDS_REVIEW: ['Có chênh lệch', 'text-amber-300 border-amber-500/30 bg-amber-500/10'],
  RECONCILED: ['Đã đối soát', 'text-emerald-300 border-emerald-500/30 bg-emerald-500/10'],
  LOCKED: ['Đã khóa', 'text-violet-300 border-violet-500/30 bg-violet-500/10'],
};
const ENTRY_STATUS = {
  MATCHED: ['Khớp', 'text-emerald-300'],
  MISMATCH: ['Lệch', 'text-amber-300'],
  UNMATCHED: ['Không tìm thấy', 'text-red-300'],
};
const CASH_STATUS = {
  NOT_SUBMITTED: ['Chưa gửi', 'text-zinc-400 border-zinc-700'],
  PENDING_VERIFICATION: ['Chờ xác minh', 'text-sky-300 border-sky-500/30 bg-sky-500/10'],
  DISCREPANCY_REVIEW: ['Đang giải trình', 'text-amber-300 border-amber-500/30 bg-amber-500/10'],
  VERIFIED: ['Đã xác minh', 'text-emerald-300 border-emerald-500/30 bg-emerald-500/10'],
};
const PERIOD_STATUS = {
  OPEN: ['Đang mở', 'text-sky-300 border-sky-500/30 bg-sky-500/10'],
  RECONCILED: ['Đã đối soát', 'text-emerald-300 border-emerald-500/30 bg-emerald-500/10'],
  LOCKED: ['Đã khóa', 'text-violet-300 border-violet-500/30 bg-violet-500/10'],
  ADJUSTMENT: ['Đang điều chỉnh', 'text-amber-300 border-amber-500/30 bg-amber-500/10'],
};

function StatusPill({ map, value }) {
  const [label, classes] = map[value] || [value || 'Chưa rõ', 'text-zinc-400 border-zinc-700'];
  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase ${classes}`}>{label}</span>;
}

function DisabledReason({ children }) {
  if (!children) return null;
  return <p className="mt-2 flex items-start gap-2 text-xs leading-5 text-amber-300/80"><AlertTriangle size={14} className="mt-0.5 shrink-0" />{children}</p>;
}

const navItems = [
  ['/admin/settlements', 'Đối soát ngân hàng', Landmark],
  ['/admin/cash-control', 'Chốt ca & tiền mặt', Banknote],
  ['/admin/accounting-periods', 'Kỳ kế toán', CalendarClock],
  ['/admin/accounting-audit', 'Nhật ký kiểm soát', FileClock],
];

function AccountingNav() {
  const location = useLocation();
  const navigate = useNavigate();
  return (
    <nav className="grid gap-2 rounded-2xl border border-white/10 bg-white/[0.025] p-2 sm:grid-cols-2 xl:grid-cols-4" aria-label="Các nghiệp vụ kế toán">
      {navItems.map(([path, label, Icon]) => {
        const active = location.pathname === path;
        return <button key={path} type="button" onClick={() => navigate(path)} className={`flex items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-black transition ${active ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:bg-white/5 hover:text-white'}`}><Icon size={18} />{label}</button>;
      })}
    </nav>
  );
}

function SettlementWorkspace({ overview, refreshOverview }) {
  const can = useAdminAccess();
  const { user } = useAuth();
  const canImport = can('SETTLEMENT_IMPORT');
  const canLock = can('SETTLEMENT_LOCK');
  const [query, setQuery] = useState({ status: '', page: 0, size: 15 });
  const [result, setResult] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [importOpen, setImportOpen] = useState(false);
  const [lockOpen, setLockOpen] = useState(false);
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState(() => ({
    providerCode: 'VNPAY', batchCode: '', cinemaPublicId: '',
    periodStart: today(), periodEnd: today(), sourceFileName: '', note: '',
    entries: [emptyEntry()],
  }));

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      setResult(await getSettlementBatches({ ...query, status: query.status || undefined }));
    } catch (requestError) { setError(apiMessage(requestError)); }
    finally { setLoading(false); }
  }, [query]);
  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const openDetail = async item => {
    setSelected(item);
    try { setSelected(await getSettlementBatch(item.publicId)); }
    catch (requestError) { setError(apiMessage(requestError)); }
  };

  const updateEntry = (index, key, value) => setForm(current => ({
    ...current,
    entries: current.entries.map((entry, entryIndex) => entryIndex === index
      ? { ...entry, [key]: value } : entry),
  }));
  const removeEntry = index => setForm(current => ({
    ...current, entries: current.entries.filter((_, entryIndex) => entryIndex !== index),
  }));

  const readCsv = async file => {
    if (!file) return;
    const text = await file.text();
    const lines = text.split(/\r?\n/).map(line => line.trim()).filter(Boolean);
    const rows = lines.slice(1).map(line => {
      const columns = line.split(',').map(value => value.trim());
      return {
        paymentTransactionCode: columns[0] || '', providerTransactionId: columns[1] || '',
        providerGrossAmount: columns[2] || '', providerFeeAmount: columns[3] || '0',
        providerNetAmount: columns[4] || '', bankCreditAmount: columns[5] || '',
        bankCreditReference: columns[6] || '',
      };
    }).filter(entry => entry.paymentTransactionCode);
    setForm(current => ({ ...current, sourceFileName: file.name, entries: rows.length ? rows : current.entries }));
  };

  const submitImport = async event => {
    event.preventDefault(); setSubmitting(true); setError('');
    try {
      const payload = {
        ...form,
        cinemaPublicId: form.cinemaPublicId || null,
        entries: form.entries.map(entry => ({
          ...entry,
          providerGrossAmount: Number(entry.providerGrossAmount),
          providerFeeAmount: Number(entry.providerFeeAmount),
          providerNetAmount: Number(entry.providerNetAmount),
          bankCreditAmount: Number(entry.bankCreditAmount),
        })),
      };
      const created = await importSettlementBatch(payload);
      setImportOpen(false); setForm({
        providerCode: 'VNPAY', batchCode: '', cinemaPublicId: '', periodStart: today(),
        periodEnd: today(), sourceFileName: '', note: '', entries: [emptyEntry()],
      });
      await load(); await refreshOverview(); await openDetail(created);
    } catch (requestError) { setError(apiMessage(requestError)); }
    finally { setSubmitting(false); }
  };

  const submitLock = async event => {
    event.preventDefault(); setSubmitting(true); setError('');
    try {
      const updated = await lockSettlementBatch(selected.publicId, {
        expectedVersion: selected.version, note,
      });
      setSelected(updated); setLockOpen(false); setNote('');
      await load(); await refreshOverview();
    } catch (requestError) { setError(apiMessage(requestError)); }
    finally { setSubmitting(false); }
  };

  const metrics = [
    { label: 'Lô cần kiểm tra', value: overview?.settlementBatchesNeedReview || 0, hint: 'Còn chênh lệch provider/ngân hàng', icon: AlertTriangle, tone: 'amber' },
    { label: 'Hồ sơ chưa đóng', value: overview?.reconciliationCasesOpen || 0, hint: 'Cần người phụ trách và kết luận', icon: ClipboardCheck, tone: 'red' },
    { label: 'Phạm vi số liệu', value: overview?.cinemaPublicId ? 'Theo rạp' : 'Toàn hệ thống', hint: overview?.cinemaPublicId || 'Quyền xem toàn chuỗi', icon: ShieldCheck, tone: 'blue' },
  ];

  return <div className="space-y-5">
    <OperationsHeader eyebrow="Thu tiền & đối soát" title="Đối soát cổng thanh toán và ngân hàng" description="Nhập lô settlement, để hệ thống tự ghép với giao dịch LoraFilm và chỉ tập trung xử lý những dòng bị lệch." actions={<><button type="button" disabled={!canImport} onClick={() => setImportOpen(true)} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black disabled:cursor-not-allowed disabled:opacity-40"><Upload size={17} /> Nhập lô settlement</button><button type="button" onClick={load} className="rounded-xl border border-white/10 p-2.5 text-zinc-300 hover:bg-white/5" aria-label="Làm mới"><RefreshCcw size={18} /></button></>} />
    {!canImport && <DisabledReason>Tài khoản này chỉ được xem. Cần quyền “Nhập lô settlement” để tải dữ liệu provider/ngân hàng.</DisabledReason>}
    <MetricStrip items={metrics} />
    {error && <div className="rounded-xl border border-red-500/25 bg-red-500/10 p-4 text-sm text-red-200">{error}</div>}
    <ConsolePanel>
      <header className="flex flex-col gap-3 border-b border-white/10 p-5 sm:flex-row sm:items-center sm:justify-between"><div><h2 className="font-black text-white">Các lô settlement</h2><p className="mt-1 text-xs text-zinc-500">Một lô là dữ liệu provider/ngân hàng của một ngày hoặc một kỳ.</p></div><select value={query.status} onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))} className="rounded-xl border border-white/10 bg-black/30 px-3 py-2.5 text-sm text-zinc-300"><option value="">Tất cả trạng thái</option><option value="NEEDS_REVIEW">Có chênh lệch</option><option value="RECONCILED">Đã đối soát</option><option value="LOCKED">Đã khóa</option></select></header>
      {loading ? <p className="p-12 text-center text-sm text-zinc-500">Đang tải lô đối soát…</p> : result.content?.length ? <div className="overflow-x-auto"><table className="w-full min-w-[980px] text-left text-sm"><thead className="sticky top-0 bg-[#0b0b0e] text-[10px] uppercase tracking-wider text-zinc-500"><tr><th className="p-4">Lô / provider</th><th>Kỳ dữ liệu</th><th>Kết quả ghép</th><th>Tiền provider</th><th>Ngân hàng ghi có</th><th>Trạng thái</th><th></th></tr></thead><tbody className="divide-y divide-white/5">{result.content.map(item => <tr key={item.publicId} className="hover:bg-white/[0.025]"><td className="p-4"><strong className="block text-zinc-200">{item.batchCode}</strong><span className="mt-1 block text-xs text-zinc-600">{item.providerCode} · {item.sourceFileName || 'Nhập thủ công'}</span></td><td className="text-zinc-400">{shortDate(item.periodStart)} – {shortDate(item.periodEnd)}</td><td><strong className="text-emerald-300">{item.matchedCount} khớp</strong><span className="ml-2 text-amber-300">{item.mismatchCount} lệch</span></td><td className="font-mono font-bold text-zinc-200">{money(item.providerNetAmount)}</td><td className="font-mono font-bold text-zinc-200">{money(item.bankCreditAmount)}</td><td><StatusPill map={SETTLEMENT_STATUS} value={item.status} /></td><td><button type="button" onClick={() => openDetail(item)} className="inline-flex items-center gap-1 text-xs font-black text-orange-400">Xem <ChevronRight size={14} /></button></td></tr>)}</tbody></table></div> : <p className="p-14 text-center text-sm text-zinc-500">Chưa có lô settlement trong phạm vi này.</p>}
      <ConsolePagination page={result.number ?? query.page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={page => setQuery(value => ({ ...value, page }))} />
    </ConsolePanel>

    <DetailDrawer open={Boolean(selected)} onClose={() => setSelected(null)} title={selected?.batchCode || 'Chi tiết lô settlement'} subtitle={selected ? `${selected.providerCode} · ${shortDate(selected.periodStart)} – ${shortDate(selected.periodEnd)}` : ''} footer={selected ? <div><button type="button" disabled={!canLock || !selected.canLock} onClick={() => { setNote(''); setLockOpen(true); }} className="w-full rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black disabled:cursor-not-allowed disabled:opacity-35"><LockKeyhole size={17} className="mr-2 inline" />Khóa lô settlement</button><DisabledReason>{!canLock ? 'Tài khoản không có quyền khóa lô. Cần người kiểm soát độc lập.' : selected.lockBlockedReason}</DisabledReason></div> : null}>
      {selected && <div className="space-y-5"><DetailGrid items={[{ label: 'Trạng thái', value: SETTLEMENT_STATUS[selected.status]?.[0] }, { label: 'Người nhập lô', value: selected.createdByAccountId === user?.id ? 'Bạn' : `Tài khoản #${selected.createdByAccountId}` }, { label: 'Tiền gộp provider', value: money(selected.grossAmount) }, { label: 'Phí thanh toán', value: money(selected.feeAmount) }, { label: 'Provider báo thực nhận', value: money(selected.providerNetAmount) }, { label: 'Ngân hàng ghi có', value: money(selected.bankCreditAmount) }]} />
        <section><h3 className="text-sm font-black text-zinc-200">Đối chiếu từng giao dịch</h3><div className="mt-3 space-y-3">{selected.entries?.map(entry => <article key={entry.id} className="rounded-2xl border border-white/10 bg-white/[0.025] p-4"><div className="flex items-start justify-between gap-3"><div><strong className="font-mono text-xs text-zinc-200">{entry.paymentTransactionCode}</strong><p className="mt-1 text-xs text-zinc-600">Provider: {entry.providerTransactionId}</p></div><span className={`text-xs font-black ${ENTRY_STATUS[entry.status]?.[1]}`}>{ENTRY_STATUS[entry.status]?.[0]}</span></div><div className="mt-4 grid gap-2 sm:grid-cols-3"><div className="rounded-xl bg-black/25 p-3"><p className="text-[10px] font-black uppercase text-zinc-600">LoraFilm</p><p className="mt-1 font-mono text-sm font-bold text-zinc-200">{entry.loraFilmAmount == null ? 'Không tìm thấy' : money(entry.loraFilmAmount)}</p><p className="mt-1 text-[10px] text-zinc-600">{entry.loraFilmPaymentStatus || 'Không có trạng thái'}</p></div><div className="rounded-xl bg-black/25 p-3"><p className="text-[10px] font-black uppercase text-zinc-600">Nhà cung cấp</p><p className="mt-1 font-mono text-sm font-bold text-zinc-200">{money(entry.providerGrossAmount)}</p><p className="mt-1 text-[10px] text-zinc-600">Phí {money(entry.providerFeeAmount)}</p></div><div className="rounded-xl bg-black/25 p-3"><p className="text-[10px] font-black uppercase text-zinc-600">Ngân hàng</p><p className="mt-1 font-mono text-sm font-bold text-zinc-200">{money(entry.bankCreditAmount)}</p><p className="mt-1 truncate text-[10px] text-zinc-600">{entry.bankCreditReference || 'Chưa có mã ghi có'}</p></div></div>{entry.mismatchReason && <p className="mt-3 rounded-xl border border-amber-500/20 bg-amber-500/[0.06] p-3 text-xs leading-5 text-amber-200">{entry.mismatchReason}</p>}</article>)}</div></section>
      </div>}
    </DetailDrawer>

    <ActionModal open={importOpen} onClose={() => setImportOpen(false)} title="Nhập lô settlement" description="Có thể chọn file CSV hoặc nhập từng dòng. Hệ thống chỉ đọc dữ liệu đối soát, không tự sửa trạng thái giao dịch." onSubmit={submitImport} submitLabel="Nhập và tự động đối chiếu" submitting={submitting} wide>
      <div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-black uppercase text-zinc-500">Nhà cung cấp *<select value={form.providerCode} onChange={event => setForm(value => ({ ...value, providerCode: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm text-zinc-200"><option value="VNPAY">VNPay</option><option value="MOMO">MoMo</option><option value="MOCK">Mô phỏng</option></select></label><label className="text-xs font-black uppercase text-zinc-500">Mã lô *<input required maxLength={100} value={form.batchCode} onChange={event => setForm(value => ({ ...value, batchCode: event.target.value }))} placeholder="VNPAY-2026-08-16" className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm" /></label><label className="text-xs font-black uppercase text-zinc-500">Từ ngày *<input required type="date" value={form.periodStart} onChange={event => setForm(value => ({ ...value, periodStart: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm" /></label><label className="text-xs font-black uppercase text-zinc-500">Đến ngày *<input required type="date" value={form.periodEnd} onChange={event => setForm(value => ({ ...value, periodEnd: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm" /></label></div>
      <label className="block rounded-xl border border-dashed border-white/15 p-4 text-sm text-zinc-400"><FileSpreadsheet size={18} className="mr-2 inline text-emerald-400" />Chọn CSV từ provider/ngân hàng<input type="file" accept=".csv,text/csv" onChange={event => readCsv(event.target.files?.[0])} className="mt-3 block w-full text-xs" /><span className="mt-2 block text-[11px] leading-5 text-zinc-600">Cột: mã giao dịch LoraFilm, mã provider, tiền gộp, phí, tiền net, ngân hàng ghi có, mã ghi có.</span></label>
      <div className="space-y-3"><div className="flex items-center justify-between"><h3 className="text-sm font-black text-zinc-200">Các dòng đối soát</h3><button type="button" onClick={() => setForm(value => ({ ...value, entries: [...value.entries, emptyEntry()] }))} className="inline-flex items-center gap-1 text-xs font-black text-orange-400"><Plus size={15} />Thêm dòng</button></div>{form.entries.map((entry, index) => <div key={index} className="rounded-xl border border-white/10 bg-black/20 p-3"><div className="grid gap-2 sm:grid-cols-2"><input required value={entry.paymentTransactionCode} onChange={event => updateEntry(index, 'paymentTransactionCode', event.target.value)} placeholder="Mã giao dịch LoraFilm" className="rounded-lg border border-white/10 bg-black/30 p-2.5 text-sm" /><div className="flex gap-2"><input required value={entry.providerTransactionId} onChange={event => updateEntry(index, 'providerTransactionId', event.target.value)} placeholder="Mã giao dịch provider" className="min-w-0 flex-1 rounded-lg border border-white/10 bg-black/30 p-2.5 text-sm" />{form.entries.length > 1 && <button type="button" onClick={() => removeEntry(index)} className="rounded-lg border border-red-500/20 p-2.5 text-red-400"><Trash2 size={16} /></button>}</div></div><div className="mt-2 grid gap-2 sm:grid-cols-4">{[['providerGrossAmount', 'Tiền gộp'], ['providerFeeAmount', 'Phí'], ['providerNetAmount', 'Tiền net'], ['bankCreditAmount', 'Ngân hàng ghi có']].map(([key, label]) => <label key={key} className="text-[10px] font-black uppercase text-zinc-600">{label}<input required type="number" min="0" value={entry[key]} onChange={event => updateEntry(index, key, event.target.value)} className="mt-1 w-full rounded-lg border border-white/10 bg-black/30 p-2 text-sm text-zinc-200" /></label>)}</div><input value={entry.bankCreditReference} onChange={event => updateEntry(index, 'bankCreditReference', event.target.value)} placeholder="Mã ghi có ngân hàng (nếu có)" className="mt-2 w-full rounded-lg border border-white/10 bg-black/30 p-2.5 text-sm" /></div>)}</div>
      <label className="block text-xs font-black uppercase text-zinc-500">Ghi chú nguồn dữ liệu<textarea rows={3} maxLength={1000} value={form.note} onChange={event => setForm(value => ({ ...value, note: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm font-normal normal-case" /></label>
    </ActionModal>

    <ActionModal open={lockOpen} onClose={() => setLockOpen(false)} title="Khóa lô settlement" description="Sau khi khóa, lô trở thành bằng chứng đối soát và không được sửa âm thầm." onSubmit={submitLock} submitLabel="Xác nhận khóa lô" submitting={submitting}><label className="text-xs font-black uppercase text-zinc-500">Căn cứ khóa lô *<textarea required minLength={5} maxLength={1000} rows={4} value={note} onChange={event => setNote(event.target.value)} placeholder="Đã kiểm tra tổng tiền, phí và mã ghi có ngân hàng…" className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm font-normal normal-case" /></label></ActionModal>
  </div>;
}

function emptyEntry() {
  return { paymentTransactionCode: '', providerTransactionId: '', bankCreditReference: '', providerGrossAmount: '', providerFeeAmount: '0', providerNetAmount: '', bankCreditAmount: '' };
}

function CashWorkspace({ overview, refreshOverview }) {
  const can = useAdminAccess();
  const canVerify = can('CASH_CLOSE_VERIFY');
  const [query, setQuery] = useState({ verificationStatus: '', page: 0, size: 15 });
  const [result, setResult] = useState(emptyPage);
  const [selected, setSelected] = useState(null);
  const [note, setNote] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setResult(await getAccountingCashSessions({ ...query, verificationStatus: query.verificationStatus || undefined })); }
    catch (requestError) { setError(apiMessage(requestError)); }
    finally { setLoading(false); }
  }, [query]);
  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);
  const submit = async event => {
    event.preventDefault(); setSubmitting(true); setError('');
    try { const updated = await verifyAccountingCashSession(selected.publicId, { note }); setSelected(updated); setModalOpen(false); setNote(''); await load(); await refreshOverview(); }
    catch (requestError) { setError(apiMessage(requestError)); }
    finally { setSubmitting(false); }
  };
  return <div className="space-y-5"><OperationsHeader eyebrow="Tiền mặt tại quầy" title="Chốt ca & kiểm kê tiền mặt" description="Đối chiếu tiền hệ thống kỳ vọng với tiền nhân viên thực đếm. Ca có thừa hoặc thiếu luôn cần ghi rõ căn cứ trước khi xác minh." actions={<button type="button" onClick={load} className="rounded-xl border border-white/10 p-2.5 text-zinc-300"><RefreshCcw size={18} /></button>} /><MetricStrip items={[{ label: 'Ca chờ xác minh', value: overview?.cashSessionsNeedVerification || 0, hint: 'Bao gồm ca đang giải trình', icon: ClipboardCheck, tone: 'amber' }, { label: 'Tổng chênh lệch', value: money(overview?.cashVarianceNeedReview), hint: 'Giá trị tuyệt đối cần kiểm tra', icon: Scale, tone: 'red' }, { label: 'Nguyên tắc', value: '2 người', hint: 'Người thu tiền không tự xác minh', icon: ShieldCheck, tone: 'blue' }]} />{error && <div className="rounded-xl border border-red-500/25 bg-red-500/10 p-4 text-sm text-red-200">{error}</div>}<ConsolePanel><header className="flex flex-col gap-3 border-b border-white/10 p-5 sm:flex-row sm:items-center sm:justify-between"><div><h2 className="font-black text-white">Biên bản chốt ca</h2><p className="mt-1 text-xs text-zinc-500">Ưu tiên ca có chênh lệch và ca chờ lâu.</p></div><select value={query.verificationStatus} onChange={event => setQuery(value => ({ ...value, verificationStatus: event.target.value, page: 0 }))} className="rounded-xl border border-white/10 bg-black/30 p-2.5 text-sm text-zinc-300"><option value="">Tất cả ca đã chốt</option><option value="PENDING_VERIFICATION">Chờ xác minh</option><option value="DISCREPANCY_REVIEW">Có chênh lệch</option><option value="VERIFIED">Đã xác minh</option></select></header>{loading ? <p className="p-12 text-center text-sm text-zinc-500">Đang tải biên bản…</p> : result.content?.length ? <div className="overflow-x-auto"><table className="w-full min-w-[900px] text-left text-sm"><thead className="sticky top-0 bg-[#0b0b0e] text-[10px] uppercase tracking-wider text-zinc-500"><tr><th className="p-4">Ca / nhân viên</th><th>Rạp</th><th>Hệ thống kỳ vọng</th><th>Thực đếm</th><th>Chênh lệch</th><th>Kiểm soát</th><th></th></tr></thead><tbody className="divide-y divide-white/5">{result.content.map(item => <tr key={item.publicId} className="hover:bg-white/[0.025]"><td className="p-4"><strong className="block text-zinc-200">Tài khoản #{item.employeeAccountId}</strong><span className="mt-1 block text-xs text-zinc-600">Chốt {date(item.closedAt)}</span></td><td className="font-mono text-xs text-zinc-500">{item.cinemaPublicId}</td><td className="font-mono font-bold text-zinc-200">{money(item.expectedCash)}</td><td className="font-mono font-bold text-zinc-200">{money(item.countedCash)}</td><td className={`font-mono font-black ${Number(item.varianceAmount) ? 'text-amber-300' : 'text-emerald-300'}`}>{Number(item.varianceAmount) > 0 ? '+' : ''}{money(item.varianceAmount)}</td><td><StatusPill map={CASH_STATUS} value={item.verificationStatus} /></td><td><button type="button" onClick={() => setSelected(item)} className="text-xs font-black text-orange-400">Kiểm tra</button></td></tr>)}</tbody></table></div> : <p className="p-14 text-center text-sm text-zinc-500">Chưa có ca tiền mặt đã chốt.</p>}<ConsolePagination page={result.number ?? query.page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={page => setQuery(value => ({ ...value, page }))} /></ConsolePanel><DetailDrawer open={Boolean(selected)} onClose={() => setSelected(null)} title="Biên bản chốt ca" subtitle={selected ? `Nhân viên #${selected.employeeAccountId} · ${date(selected.closedAt)}` : ''} footer={selected ? <div><button type="button" disabled={!canVerify || !selected.canVerify} onClick={() => { setNote(''); setModalOpen(true); }} className="w-full rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black disabled:cursor-not-allowed disabled:opacity-35"><CheckCircle2 size={17} className="mr-2 inline" />Xác minh biên bản</button><DisabledReason>{!canVerify ? 'Tài khoản không có quyền xác minh chốt ca.' : selected.verifyBlockedReason}</DisabledReason></div> : null}>{selected && <div className="space-y-5"><DetailGrid items={[{ label: 'Tiền đầu ca', value: money(selected.openingFloat) }, { label: 'Thu tiền mặt', value: `${money(selected.cashSales)} · ${selected.cashTransactionCount || 0} giao dịch` }, { label: 'Hoàn tiền mặt', value: `${money(selected.cashRefunds)} · ${selected.cashRefundCount || 0} khoản` }, { label: 'Hệ thống kỳ vọng', value: money(selected.expectedCash) }, { label: 'Nhân viên thực đếm', value: money(selected.countedCash) }, { label: 'Chênh lệch', value: money(selected.varianceAmount) }]} /><section className={`rounded-2xl border p-4 ${Number(selected.varianceAmount) ? 'border-amber-500/25 bg-amber-500/[0.06]' : 'border-emerald-500/25 bg-emerald-500/[0.06]'}`}><h3 className="text-sm font-black text-zinc-200">Giải trình của nhân viên</h3><p className="mt-2 text-sm leading-6 text-zinc-400">{selected.closingNote || 'Không có ghi chú.'}</p></section>{selected.verificationNote && <section className="rounded-2xl border border-white/10 p-4"><h3 className="text-sm font-black text-zinc-200">Kết luận kế toán</h3><p className="mt-2 text-sm leading-6 text-zinc-400">{selected.verificationNote}</p><p className="mt-2 text-xs text-zinc-600">Tài khoản #{selected.verifiedByAccountId} · {date(selected.verifiedAt)}</p></section>}</div>}</DetailDrawer><ActionModal open={modalOpen} onClose={() => setModalOpen(false)} title="Xác minh chốt ca" description={Number(selected?.varianceAmount) ? `Ca đang chênh lệch ${money(selected?.varianceAmount)}. Hãy ghi rõ chứng từ hoặc căn cứ chấp nhận.` : 'Tiền thực đếm đã khớp với hệ thống. Vẫn cần lưu căn cứ kiểm tra.'} onSubmit={submit} submitLabel="Xác nhận biên bản" submitting={submitting}><label className="text-xs font-black uppercase text-zinc-500">Kết luận kiểm tra *<textarea required minLength={5} maxLength={1000} rows={4} value={note} onChange={event => setNote(event.target.value)} placeholder="Đã đối chiếu biên bản bàn giao và số tiền thực tế…" className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm font-normal normal-case" /></label></ActionModal></div>;
}

function PeriodWorkspace({ overview, refreshOverview }) {
  const can = useAdminAccess();
  const canCreate = can('ACCOUNTING_PERIOD_CREATE');
  const canReconcile = can('ACCOUNTING_PERIOD_RECONCILE');
  const canLock = can('ACCOUNTING_PERIOD_CLOSE');
  const [result, setResult] = useState(emptyPage);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [actionOpen, setActionOpen] = useState(false);
  const [action, setAction] = useState('RECONCILE');
  const [note, setNote] = useState('');
  const [periodMonth, setPeriodMonth] = useState(monthValue());
  const [form, setForm] = useState(() => monthRange(monthValue()));
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const load = useCallback(async () => {
    setError('');
    try { setResult(await getAccountingPeriods({ page, size: 15 })); }
    catch (requestError) { setError(apiMessage(requestError)); }
  }, [page]);
  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);
  const changeMonth = value => { setPeriodMonth(value); setForm(current => ({ ...current, ...monthRange(value) })); };
  const create = async event => {
    event.preventDefault(); setSubmitting(true); setError('');
    try { await createAccountingPeriod({ ...form, cinemaPublicId: form.cinemaPublicId || null }); setCreateOpen(false); await load(); await refreshOverview(); }
    catch (requestError) { setError(apiMessage(requestError)); }
    finally { setSubmitting(false); }
  };
  const submitAction = async event => {
    event.preventDefault(); setSubmitting(true); setError('');
    try { const updated = await applyAccountingPeriodAction(selected.publicId, { action, note, expectedVersion: selected.version }); setSelected(updated); setActionOpen(false); setNote(''); await load(); await refreshOverview(); }
    catch (requestError) { setError(apiMessage(requestError)); }
    finally { setSubmitting(false); }
  };
  const startAction = type => { setAction(type); setNote(''); setActionOpen(true); };
  return <div className="space-y-5"><OperationsHeader eyebrow="Doanh thu & chứng từ" title="Kỳ kế toán" description="Kỳ chỉ được đánh dấu đã đối soát khi không còn lô lệch, hồ sơ mở hoặc ca tiền mặt chưa xác minh. Khóa kỳ cần một người kiểm soát độc lập." actions={<button type="button" disabled={!canCreate} onClick={() => setCreateOpen(true)} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black disabled:cursor-not-allowed disabled:opacity-35"><Plus size={17} /> Mở kỳ kế toán</button>} />{!canCreate && <DisabledReason>Bạn không có quyền mở kỳ mới. Kỳ được người lập chuẩn bị trước khi chuyển cho người kiểm soát.</DisabledReason>}<MetricStrip items={[{ label: 'Kỳ đang mở', value: overview?.accountingPeriodsOpen || 0, hint: 'Số liệu vẫn có thể thay đổi', icon: CalendarClock, tone: 'blue' }, { label: 'Hồ sơ chặn chốt kỳ', value: overview?.reconciliationCasesOpen || 0, hint: 'Phải có kết luận trước khi chốt', icon: AlertTriangle, tone: 'red' }, { label: 'Ca tiền mặt chưa xong', value: overview?.cashSessionsNeedVerification || 0, hint: 'Cần xác minh biên bản', icon: Banknote, tone: 'amber' }]} />{error && <div className="rounded-xl border border-red-500/25 bg-red-500/10 p-4 text-sm text-red-200">{error}</div>}<ConsolePanel><header className="border-b border-white/10 p-5"><h2 className="font-black text-white">Danh sách kỳ kế toán</h2><p className="mt-1 text-xs text-zinc-500">Số quá khứ đã khóa không thay đổi âm thầm; điều chỉnh phải mở luồng riêng.</p></header>{result.content?.length ? <div className="divide-y divide-white/5">{result.content.map(item => <button key={item.publicId} type="button" onClick={() => setSelected(item)} className="grid w-full gap-3 p-5 text-left hover:bg-white/[0.025] sm:grid-cols-[1fr_auto_auto] sm:items-center"><span><strong className="block text-zinc-200">Kỳ {item.periodCode}</strong><span className="mt-1 block text-xs text-zinc-600">{shortDate(item.periodStart)} – {shortDate(item.periodEnd)} · {item.cinemaPublicId || 'Toàn hệ thống'}</span></span><span className="text-xs text-zinc-500">{item.blockers?.length ? `${item.blockers.length} điều kiện chưa đạt` : 'Đủ điều kiện'}</span><StatusPill map={PERIOD_STATUS} value={item.status} /></button>)}</div> : <p className="p-14 text-center text-sm text-zinc-500">Chưa mở kỳ kế toán.</p>}<ConsolePagination page={result.number ?? page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={setPage} /></ConsolePanel><DetailDrawer open={Boolean(selected)} onClose={() => setSelected(null)} title={`Kỳ ${selected?.periodCode || ''}`} subtitle={selected ? `${shortDate(selected.periodStart)} – ${shortDate(selected.periodEnd)}` : ''} footer={selected ? <div className="grid gap-2 sm:grid-cols-2"><button type="button" disabled={!canReconcile || !selected.canReconcile} onClick={() => startAction('RECONCILE')} className="rounded-xl border border-emerald-500/30 px-4 py-3 text-sm font-black text-emerald-300 disabled:cursor-not-allowed disabled:opacity-30">Xác nhận đã đối soát</button><button type="button" disabled={!canLock || !selected.canLock} onClick={() => startAction('LOCK')} className="rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black disabled:cursor-not-allowed disabled:opacity-30">Khóa kỳ</button>{!canReconcile && selected.status === 'OPEN' && <DisabledReason>Tài khoản không có quyền xác nhận đối soát kỳ.</DisabledReason>}{!canLock && selected.status === 'RECONCILED' && <DisabledReason>Cần tài khoản kế toán kiểm soát để khóa kỳ.</DisabledReason>}</div> : null}>{selected && <div className="space-y-5"><DetailGrid items={[{ label: 'Trạng thái', value: PERIOD_STATUS[selected.status]?.[0] }, { label: 'Phạm vi', value: selected.cinemaPublicId || 'Toàn hệ thống' }, { label: 'Người mở kỳ', value: `Tài khoản #${selected.createdByAccountId}` }, { label: 'Người xác nhận đối soát', value: selected.reconciledByAccountId ? `Tài khoản #${selected.reconciledByAccountId}` : 'Chưa có' }, { label: 'Người khóa kỳ', value: selected.lockedByAccountId ? `Tài khoản #${selected.lockedByAccountId}` : 'Chưa có' }, { label: 'Phiên bản dữ liệu', value: selected.version }]} /><section className="rounded-2xl border border-white/10 p-4"><h3 className="text-sm font-black text-zinc-200">Điều kiện chốt kỳ</h3>{selected.blockers?.length ? <ul className="mt-3 space-y-2">{selected.blockers.map(blocker => <li key={blocker} className="flex gap-2 text-sm text-amber-300"><AlertTriangle size={16} className="mt-0.5 shrink-0" />{blocker}</li>)}</ul> : <p className="mt-3 flex gap-2 text-sm text-emerald-300"><CheckCircle2 size={16} />Không còn công việc mở trong phạm vi kỳ.</p>}</section></div>}</DetailDrawer><ActionModal open={createOpen} onClose={() => setCreateOpen(false)} title="Mở kỳ kế toán" description="Kỳ mới bắt đầu ở trạng thái đang mở. Không thể khóa nếu còn chênh lệch hoặc biên bản chưa xác minh." onSubmit={create} submitLabel="Mở kỳ" submitting={submitting}><label className="text-xs font-black uppercase text-zinc-500">Tháng kế toán *<input required type="month" value={periodMonth} onChange={event => changeMonth(event.target.value)} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm" /></label><div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-black uppercase text-zinc-500">Từ ngày<input required type="date" value={form.periodStart} onChange={event => setForm(value => ({ ...value, periodStart: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm" /></label><label className="text-xs font-black uppercase text-zinc-500">Đến ngày<input required type="date" value={form.periodEnd} onChange={event => setForm(value => ({ ...value, periodEnd: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm" /></label></div><label className="text-xs font-black uppercase text-zinc-500">Mục tiêu kiểm soát<textarea rows={3} maxLength={1000} value={form.note} onChange={event => setForm(value => ({ ...value, note: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm font-normal normal-case" /></label></ActionModal><ActionModal open={actionOpen} onClose={() => setActionOpen(false)} title={action === 'LOCK' ? 'Khóa kỳ kế toán' : 'Xác nhận kỳ đã đối soát'} description={action === 'LOCK' ? 'Sau khi khóa, số liệu không được cập nhật âm thầm. Người mở kỳ không được tự khóa.' : 'Hệ thống sẽ kiểm tra lại toàn bộ điều kiện backend trước khi ghi nhận.'} onSubmit={submitAction} submitLabel={action === 'LOCK' ? 'Khóa kỳ' : 'Xác nhận đối soát'} submitting={submitting}><label className="text-xs font-black uppercase text-zinc-500">Căn cứ thực hiện *<textarea required minLength={5} maxLength={1000} rows={4} value={note} onChange={event => setNote(event.target.value)} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm font-normal normal-case" /></label></ActionModal></div>;
}

function AuditWorkspace() {
  const can = useAdminAccess();
  const canView = can('AUDIT_VIEW');
  const [result, setResult] = useState(emptyPage);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  useEffect(() => {
    if (!canView) return;
    getAccountingAuditEvents({ page, size: 50 }).then(setResult).catch(requestError => setError(apiMessage(requestError)));
  }, [canView, page]);
  return <div className="space-y-5"><OperationsHeader eyebrow="Kiểm soát nội bộ" title="Nhật ký kế toán bất biến" description="Theo dõi ai đã nhập settlement, xác minh tiền mặt hoặc thay đổi trạng thái kỳ. Nhật ký chỉ được đọc, không có thao tác sửa hoặc xóa." />{!canView ? <div className="rounded-2xl border border-amber-500/25 bg-amber-500/[0.06] p-6 text-sm text-amber-200">Tài khoản không có quyền xem nhật ký kế toán.</div> : <ConsolePanel>{error ? <p className="p-5 text-red-300">{error}</p> : result.content?.length ? <div className="divide-y divide-white/5">{result.content.map(item => <article key={item.id} className="grid gap-3 p-5 sm:grid-cols-[1fr_auto]"><div><strong className="text-sm text-zinc-200">{auditLabel(item.actionCode)}</strong><p className="mt-1 text-xs text-zinc-600">{item.aggregateType} · {item.aggregatePublicId}</p><p className="mt-2 text-sm leading-6 text-zinc-400">{item.detailSanitized || 'Không có ghi chú bổ sung.'}</p></div><div className="text-right text-xs text-zinc-600"><p>Tài khoản #{item.actorAccountId}</p><p className="mt-1">{date(item.createdAt)}</p></div></article>)}</div> : <p className="p-14 text-center text-sm text-zinc-500">Chưa có hoạt động kế toán được ghi nhận.</p>}<ConsolePagination page={result.number ?? page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={setPage} /></ConsolePanel>}</div>;
}

const auditLabel = code => ({
  SETTLEMENT_IMPORTED: 'Đã nhập lô settlement',
  SETTLEMENT_LOCKED: 'Đã khóa lô settlement',
  CASH_SESSION_VERIFIED: 'Đã xác minh chốt ca tiền mặt',
  ACCOUNTING_PERIOD_CREATED: 'Đã mở kỳ kế toán',
  ACCOUNTING_PERIOD_RECONCILE: 'Đã xác nhận kỳ được đối soát',
  ACCOUNTING_PERIOD_LOCK: 'Đã khóa kỳ kế toán',
  ACCOUNTING_PERIOD_REOPEN: 'Đã mở lại kỳ để điều chỉnh',
}[code] || code?.toLowerCase().replaceAll('_', ' '));

export default function AdminAccountingOperationsPage() {
  const location = useLocation();
  const [overview, setOverview] = useState(null);
  const [overviewError, setOverviewError] = useState('');
  const loadOverview = useCallback(async () => {
    try { setOverview(await getAccountingOverview({})); setOverviewError(''); }
    catch (error) { setOverviewError(apiMessage(error)); }
  }, []);
  useEffect(() => {
    const timer = window.setTimeout(loadOverview, 0);
    return () => window.clearTimeout(timer);
  }, [loadOverview]);
  const mode = useMemo(() => {
    if (location.pathname.includes('cash-control')) return 'cash';
    if (location.pathname.includes('accounting-periods')) return 'periods';
    if (location.pathname.includes('accounting-audit')) return 'audit';
    return 'settlements';
  }, [location.pathname]);
  return <main className="mx-auto w-full max-w-[1600px] space-y-6 pb-16 text-white"><AccountingNav />{overviewError && <div className="rounded-xl border border-amber-500/25 bg-amber-500/[0.06] p-4 text-sm text-amber-200">Không tải được số tổng hợp: {overviewError}</div>}{mode === 'settlements' && <SettlementWorkspace overview={overview} refreshOverview={loadOverview} />}{mode === 'cash' && <CashWorkspace overview={overview} refreshOverview={loadOverview} />}{mode === 'periods' && <PeriodWorkspace overview={overview} refreshOverview={loadOverview} />}{mode === 'audit' && <AuditWorkspace />}</main>;
}
