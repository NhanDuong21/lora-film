import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useOutletContext, useSearchParams } from 'react-router-dom';
import {
  AlertTriangle, CheckCircle2, ChevronDown, Clock3, Database,
  FileClock, ListChecks, Play, RefreshCw, ShieldAlert,
} from 'lucide-react';
import useAdminScore from '../hooks/useAdminScore';
import scoreAdminService from '../services/scoreAdminService';
import { getCustomers } from '@/features/internal-staff/admin/services/userAdminService';

const n = value => Number(value ?? 0).toLocaleString('vi-VN');
const when = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa có dữ liệu';
const content = page => page?.content || [];

function SummaryCard({ label, value, hint, tone = 'text-white', icon: Icon }) {
  return <article className="rounded-2xl border border-white/10 bg-zinc-900/40 p-4"><div className="flex items-start justify-between gap-3"><div><p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">{label}</p><p className={`mt-2 text-2xl font-black ${tone}`}>{value}</p></div><Icon className="text-zinc-600" size={18} /></div><p className="mt-2 text-xs leading-5 text-zinc-500">{hint}</p></article>;
}

export default function AdminScoreReconciliationPage() {
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const confirm = outlet?.triggerConfirm || (async () => false);
  const [params, setParams] = useSearchParams();
  const view = params.get('view') || 'control';
  const {
    reconciliationRuns, fetchReconciliationRuns, runReconciliation,
    isLoadingOperations, dashboardStats, fetchDashboardStats,
  } = useAdminScore();
  const [selectedRun, setSelectedRun] = useState(null);
  const [details, setDetails] = useState([]);
  const [scoreAccounts, setScoreAccounts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loadingCoverage, setLoadingCoverage] = useState(true);
  const [coverageError, setCoverageError] = useState('');
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [remark, setRemark] = useState('');

  const loadCoverage = useCallback(async () => {
    setLoadingCoverage(true);
    setCoverageError('');
    const results = await Promise.allSettled([
      fetchDashboardStats({ forceRefresh: true }),
      scoreAdminService.getScoreAccounts({ page: 0, size: 200 }),
      getCustomers({ page: 0, size: 200 }),
    ]);
    if (results[1].status === 'fulfilled') setScoreAccounts(content(results[1].value));
    if (results[2].status === 'fulfilled') setCustomers(content(results[2].value));
    if (results.some(result => result.status === 'rejected')) setCoverageError('Một nguồn dữ liệu chưa tải được; chưa thể kết luận độ phủ toàn hệ thống.');
    setLoadingCoverage(false);
  }, [fetchDashboardStats]);

  const loadDetails = useCallback(async run => {
    if (!run) { setDetails([]); return; }
    const page = await scoreAdminService.getReconciliationDetails({ runId: run.id, page: 0, size: 200 });
    setDetails(content(page));
  }, []);

  useEffect(() => {
    fetchReconciliationRuns({ page: 0, size: 20 });
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadCoverage();
  }, [fetchReconciliationRuns, loadCoverage]);

  useEffect(() => {
    const latest = content(reconciliationRuns)[0];
    if (latest && !selectedRun) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedRun(latest);
      loadDetails(latest).catch(() => setCoverageError('Không thể tải chi tiết lần đối soát gần nhất.'));
    }
  }, [loadDetails, reconciliationRuns, selectedRun]);

  const selectRun = async run => {
    setSelectedRun(run);
    try { await loadDetails(run); } catch { notify('Không thể tải chi tiết lần đối soát.', 'error'); }
  };

  const setView = nextView => setParams(current => {
    const next = new URLSearchParams(current);
    next.set('view', nextView);
    return next;
  });

  const execute = async event => {
    event.preventDefault();
    if (remark.trim().length < 5) return;
    const batchCode = `RECON-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`;
    const accepted = await confirm({ title: 'Chạy đối soát toàn bộ', message: `Đối chiếu toàn bộ tài khoản điểm hiện có với sổ giao dịch. Mã lần chạy ${batchCode}; mục đích: ${remark.trim()}.`, confirmLabel: 'Chạy đối soát' });
    if (!accepted) return;
    try {
      const run = await runReconciliation({ batchCode, remark: remark.trim() });
      notify(`Đã hoàn tất ${run.batchCode}: ${run.mismatchedUsers} tài khoản chênh lệch.`);
      setRemark(''); setAdvancedOpen(false); setSelectedRun(run); setView('control');
      await Promise.all([fetchReconciliationRuns({ page: 0, size: 20 }, { forceRefresh: true }), loadCoverage(), loadDetails(run)]);
    } catch (error) { notify(error?.response?.data?.message || 'Không thể chạy đối soát.', 'error'); }
  };

  const runs = content(reconciliationRuns);
  const scoreIds = useMemo(() => new Set(scoreAccounts.map(item => Number(item.userId))), [scoreAccounts]);
  const customerAccountIds = useMemo(() => new Set(customers.map(item => Number(item.accountId)).filter(Number.isFinite)), [customers]);
  const checkedIds = useMemo(() => new Set(details.map(item => Number(item.userId))), [details]);
  const mismatches = details.filter(item => item.status === 'MISMATCH');
  const matched = details.filter(item => item.status === 'MATCHED');
  const unreconciled = scoreAccounts.filter(item => !checkedIds.has(Number(item.userId)));
  const scoreWithoutCustomer = scoreAccounts.filter(item => !customerAccountIds.has(Number(item.userId)));
  const customerWithoutScore = customers.filter(item => Number.isFinite(Number(item.accountId)) && !scoreIds.has(Number(item.accountId)));
  const systemTotal = Number(dashboardStats?.totalMembers ?? scoreAccounts.length);
  const checkedTotal = Number(selectedRun?.totalUsers ?? details.length);
  const runProcessed = Number(selectedRun?.matchedUsers ?? 0) + Number(selectedRun?.mismatchedUsers ?? 0);
  const runProgress = checkedTotal ? Math.round(runProcessed / checkedTotal * 100) : 0;
  const systemCoverage = systemTotal ? Math.min(100, Math.round(checkedTotal / systemTotal * 100)) : 0;
  const uncheckedTotal = Math.max(0, systemTotal - checkedTotal);
  const populationGap = scoreAccounts.length - customers.length;
  const selectedRows = view === 'mismatch' ? mismatches : matched;
  const tabs = [
    ['control', 'Tổng quan kiểm soát', ListChecks],
    ['mismatch', `Chênh lệch (${mismatches.length})`, AlertTriangle],
    ['unreconciled', `Chưa kiểm tra (${uncheckedTotal})`, FileClock],
    ['population', `Độ phủ tài khoản (${Math.abs(populationGap)})`, Database],
    ['runs', 'Lịch sử chạy', Clock3],
  ];

  return <section className="mx-auto max-w-7xl space-y-6 text-white">
    <header className="flex flex-col gap-4 rounded-3xl border border-white/10 bg-zinc-900/50 p-6 lg:flex-row lg:items-center lg:justify-between"><div><p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Kiểm soát tính toàn vẹn</p><h1 className="mt-2 text-2xl font-black">Kiểm soát và đối soát điểm thưởng</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">Tách rõ tiến độ của một lần chạy, độ phủ toàn hệ thống, tài khoản chưa kiểm tra và số dư thực sự chênh lệch.</p></div><button type="button" onClick={() => Promise.all([fetchReconciliationRuns({ page: 0, size: 20 }, { forceRefresh: true }), loadCoverage(), loadDetails(selectedRun)])} disabled={isLoadingOperations || loadingCoverage} className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-xs font-black hover:bg-white/10 disabled:opacity-50"><RefreshCw size={15} className={isLoadingOperations || loadingCoverage ? 'animate-spin' : ''} /> Làm mới</button></header>

    {coverageError ? <div className="rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 text-sm text-amber-200">{coverageError}</div> : null}

    <div className={`flex flex-col gap-3 rounded-2xl border p-5 md:flex-row md:items-center md:justify-between ${systemCoverage === 100 && mismatches.length === 0 && populationGap === 0 ? 'border-emerald-500/25 bg-emerald-500/[0.06]' : 'border-amber-500/30 bg-amber-500/[0.07]'}`}><div className="flex gap-3"><ShieldAlert className={systemCoverage === 100 ? 'text-emerald-400' : 'text-amber-400'} size={21} /><div><p className="font-black">{runProgress === 100 ? 'Lần chạy đã hoàn tất' : 'Lần chạy chưa hoàn tất'}{systemCoverage < 100 ? ', nhưng chưa phủ toàn hệ thống' : ''}</p><p className="mt-1 text-xs leading-5 text-zinc-400">Đã xử lý {n(runProcessed)}/{n(checkedTotal)} trong lần chạy ({runProgress}%). Độ phủ hệ thống {n(checkedTotal)}/{n(systemTotal)} ({systemCoverage}%); còn {n(uncheckedTotal)} tài khoản chưa được kiểm tra.</p></div></div><span className={`shrink-0 rounded-full px-3 py-1.5 text-[10px] font-black ${systemCoverage === 100 ? 'bg-emerald-500/10 text-emerald-300' : 'bg-amber-500/10 text-amber-300'}`}>{systemCoverage === 100 ? 'ĐỦ ĐỘ PHỦ' : 'CHƯA ĐỦ ĐỘ PHỦ'}</span></div>

    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4"><SummaryCard label="Tiến độ lần chạy" value={`${runProgress}%`} hint={`${n(runProcessed)}/${n(checkedTotal)} tài khoản đã có kết quả.`} icon={CheckCircle2} tone={runProgress === 100 ? 'text-emerald-400' : 'text-amber-400'} /><SummaryCard label="Độ phủ toàn hệ thống" value={`${systemCoverage}%`} hint={`${n(checkedTotal)}/${n(systemTotal)} tài khoản điểm trong phạm vi.`} icon={Database} tone={systemCoverage === 100 ? 'text-emerald-400' : 'text-amber-400'} /><SummaryCard label="Chưa kiểm tra" value={n(uncheckedTotal)} hint="Không được coi là đã khớp cho đến khi có kết quả đối soát." icon={FileClock} tone={uncheckedTotal ? 'text-amber-400' : 'text-zinc-200'} /><SummaryCard label="Số dư chênh lệch" value={n(mismatches.length)} hint={`Trong ${n(checkedTotal)} tài khoản đã kiểm tra.`} icon={AlertTriangle} tone={mismatches.length ? 'text-red-400' : 'text-emerald-400'} /></div>

    <nav className="flex gap-2 overflow-x-auto rounded-2xl border border-white/10 bg-zinc-900/40 p-2" aria-label="Bộ lọc kiểm soát điểm">{tabs.map(([id, label, Icon]) => <button type="button" key={id} onClick={() => setView(id)} className={`inline-flex shrink-0 items-center gap-2 rounded-xl px-3 py-2 text-xs font-black ${view === id ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:bg-white/5 hover:text-white'}`}><Icon size={14} />{label}</button>)}</nav>

    {view === 'control' ? <div className="grid gap-5 lg:grid-cols-2"><article className="rounded-3xl border border-white/10 bg-zinc-900/40 p-6"><h2 className="font-black">Quyết định vận hành</h2><div className="mt-4 space-y-3 text-sm"><QueueButton onClick={() => setView('mismatch')} title={`${n(mismatches.length)} số dư chênh lệch`} hint="Điều tra số dư hiện tại so với sổ giao dịch." /><QueueButton onClick={() => setView('unreconciled')} title={`${n(uncheckedTotal)} tài khoản chưa kiểm tra`} hint="Khoảng trống độ phủ, không phải kết quả đã khớp." /><QueueButton onClick={() => setView('population')} title={`${n(Math.abs(populationGap))} hồ sơ cần xác minh liên kết`} hint="So sánh hồ sơ khách hàng và tài khoản điểm." /></div></article><article className="rounded-3xl border border-white/10 bg-zinc-900/40 p-6"><h2 className="font-black">Lần chạy đang xem</h2><p className="mt-2 font-mono text-xs text-zinc-500">{selectedRun?.batchCode || 'Chưa có lần chạy'}</p><dl className="mt-5 grid grid-cols-2 gap-4 text-sm"><div><dt className="text-zinc-500">Bắt đầu</dt><dd className="mt-1 font-bold">{when(selectedRun?.startedAt)}</dd></div><div><dt className="text-zinc-500">Hoàn tất</dt><dd className="mt-1 font-bold">{when(selectedRun?.finishedAt)}</dd></div><div><dt className="text-zinc-500">Đã khớp</dt><dd className="mt-1 font-bold text-emerald-400">{n(selectedRun?.matchedUsers)}</dd></div><div><dt className="text-zinc-500">Chênh lệch</dt><dd className="mt-1 font-bold text-red-400">{n(selectedRun?.mismatchedUsers)}</dd></div></dl><p className="mt-5 rounded-xl bg-black/25 p-3 text-xs leading-5 text-zinc-500">{selectedRun?.remark || 'Không có ghi chú vận hành.'}</p><Link to="/admin/scores/audit-logs" className="mt-4 inline-block text-xs font-black text-brand-orange">Mở nhật ký quản trị →</Link></article></div> : null}

    {view === 'population' ? <div className="grid gap-5 lg:grid-cols-2"><PopulationList title={`Có tài khoản điểm, thiếu hồ sơ khách hàng (${scoreWithoutCustomer.length})`} items={scoreWithoutCustomer.map(item => ({ id: item.userId, primary: `Tài khoản #${item.userId}`, secondary: `${n(item.availablePoints)} điểm khả dụng · ${item.status === 'LOCKED' ? 'Đã khóa' : 'Đang hoạt động'}` }))} /><PopulationList title={`Có hồ sơ khách hàng, thiếu tài khoản điểm (${customerWithoutScore.length})`} items={customerWithoutScore.map(item => ({ id: item.accountId, primary: item.fullName || item.email || `Tài khoản #${item.accountId}`, secondary: `${item.customerCode || 'Chưa có mã khách'} · ${item.email || 'Chưa có email'}` }))} /></div> : null}

    {view === 'unreconciled' ? <PopulationList title={`Tài khoản chưa có kết quả trong lần chạy (${unreconciled.length || uncheckedTotal})`} emptyText={uncheckedTotal ? 'API chi tiết chưa trả đủ danh sách tài khoản chưa kiểm tra; không được xem số này là đã khớp.' : 'Lần chạy đã phủ toàn bộ tài khoản điểm hiện có.'} items={unreconciled.map(item => ({ id: item.userId, primary: `Tài khoản #${item.userId}`, secondary: `${n(item.availablePoints)} điểm khả dụng · ${item.tierName || item.tierCode || 'Chưa có hạng'}` }))} /> : null}

    {view === 'mismatch' ? <ReconciliationTable rows={selectedRows} empty={`Không có số dư chênh lệch trong ${n(checkedTotal)} tài khoản đã kiểm tra. Vẫn còn ${n(uncheckedTotal)} tài khoản chưa kiểm tra và ${n(Math.abs(populationGap))} hồ sơ cần xác minh liên kết.`} /> : null}

    {view === 'runs' ? <article className="overflow-hidden rounded-3xl border border-white/10 bg-zinc-900/40"><div className="border-b border-white/10 p-5"><h2 className="font-black">Lịch sử các lần đối soát</h2><p className="mt-1 text-xs text-zinc-500">Chọn một lần để cập nhật các số liệu và hàng đợi phía trên.</p></div><div className="divide-y divide-white/5">{runs.length ? runs.map(run => <button type="button" key={run.id} onClick={() => selectRun(run)} className={`grid w-full gap-2 p-4 text-left hover:bg-white/[0.03] md:grid-cols-[1.2fr_.8fr_.6fr_.6fr] ${selectedRun?.id === run.id ? 'bg-white/[0.05]' : ''}`}><span className="font-mono text-xs font-black text-zinc-200">{run.batchCode}</span><span className="text-xs text-zinc-500">{when(run.startedAt)}</span><span className="text-xs text-emerald-400">{n(run.matchedUsers)} khớp</span><span className={Number(run.mismatchedUsers) ? 'text-xs text-red-400' : 'text-xs text-zinc-500'}>{n(run.mismatchedUsers)} chênh lệch</span></button>) : <p className="p-8 text-center text-sm text-zinc-500">Chưa có lần đối soát.</p>}</div></article> : null}

    <article className="rounded-3xl border border-white/10 bg-zinc-900/40"><button type="button" onClick={() => setAdvancedOpen(value => !value)} className="flex w-full items-center justify-between p-5 text-left"><div><h2 className="font-black">Chạy thủ công (nâng cao)</h2><p className="mt-1 text-xs text-zinc-500">Dùng sau triển khai, chuyển đổi dữ liệu, sự cố hoặc khi lần chạy tự động quá hạn.</p></div><ChevronDown size={18} className={`text-zinc-500 transition ${advancedOpen ? 'rotate-180' : ''}`} /></button>{advancedOpen ? <form onSubmit={execute} className="border-t border-white/10 p-5"><div className="flex gap-3 rounded-xl border border-amber-500/20 bg-amber-500/[0.06] p-4 text-xs leading-5 text-zinc-400"><AlertTriangle size={17} className="shrink-0 text-amber-400" />Lần chạy kiểm toàn bộ tài khoản điểm hiện có, so sánh số dư khả dụng, điểm tạm giữ và điểm xét hạng với sổ giao dịch. Thao tác không tự sửa dữ liệu.</div><div className="mt-4 flex flex-col gap-3 md:flex-row"><label className="flex-1 text-xs font-bold text-zinc-400">Mục đích chạy *<input required minLength={5} maxLength={500} value={remark} onChange={event => setRemark(event.target.value)} placeholder="Ví dụ: Kiểm tra sau đợt triển khai #142" className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-black/30 px-4 text-sm text-white outline-none focus:border-brand-orange" /></label><button disabled={isLoadingOperations || remark.trim().length < 5} className="mt-auto h-11 rounded-xl bg-brand-orange px-5 text-xs font-black text-black disabled:opacity-40"><Play size={14} className="mr-2 inline" />Xem lại và chạy</button></div></form> : null}</article>

    <div className="flex gap-3 rounded-2xl border border-sky-500/20 bg-sky-500/[0.05] p-4 text-xs leading-5 text-zinc-400"><ShieldAlert size={18} className="shrink-0 text-sky-400" /><p>Đối soát hiện chứng minh sổ giao dịch nội bộ khớp số dư tổng hợp và điểm tạm giữ. Nó chưa thay thế đối chiếu chéo sự kiện gốc từ Đặt vé, Thanh toán và Hoàn tiền; đây vẫn là hạng mục UAT tích hợp bắt buộc trước production.</p></div>
  </section>;
}

function QueueButton({ onClick, title, hint }) {
  return <button type="button" onClick={onClick} className="flex w-full items-center justify-between rounded-2xl bg-black/25 p-4 text-left hover:bg-black/40"><span><b>{title}</b><small className="mt-1 block text-zinc-500">{hint}</small></span><span className="text-brand-orange">Mở →</span></button>;
}

function PopulationList({ title, items, emptyText = 'Không có hồ sơ trong nhóm này.' }) {
  return <article className="overflow-hidden rounded-3xl border border-white/10 bg-zinc-900/40"><div className="border-b border-white/10 p-5"><h2 className="font-black">{title}</h2></div>{items.length ? <div className="divide-y divide-white/5">{items.map(item => <div key={item.id} className="flex items-center justify-between gap-4 p-4"><div><p className="text-sm font-bold">{item.primary}</p><p className="mt-1 text-xs text-zinc-500">{item.secondary}</p></div><Link to={`/admin/scores/viewer?accountId=${item.id}`} className="shrink-0 text-xs font-black text-brand-orange">Kiểm tra →</Link></div>)}</div> : <p className="p-8 text-center text-sm leading-6 text-zinc-500">{emptyText}</p>}</article>;
}

function ReconciliationTable({ rows, empty }) {
  if (!rows.length) return <div className="rounded-3xl border border-white/10 bg-zinc-900/40 p-10 text-center"><CheckCircle2 className="mx-auto text-emerald-400" size={28} /><p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-zinc-400">{empty}</p></div>;
  return <article className="overflow-hidden rounded-3xl border border-white/10 bg-zinc-900/40"><div className="overflow-x-auto"><table className="min-w-full text-left text-xs"><thead className="bg-white/[0.025] text-[10px] uppercase tracking-wider text-zinc-600"><tr><th className="px-5 py-3">Tài khoản</th><th className="px-5 py-3">Số dư khả dụng</th><th className="px-5 py-3">Điểm tạm giữ</th><th className="px-5 py-3">Điểm xét hạng</th><th className="px-5 py-3">Kết quả</th><th className="px-5 py-3" /></tr></thead><tbody className="divide-y divide-white/5">{rows.map(item => <tr key={item.id} className="align-top hover:bg-white/[0.02]"><td className="px-5 py-4"><p className="font-mono font-black">Tài khoản #{item.userId}</p><p className="mt-1 text-[10px] text-zinc-600">Kết quả #{item.id}</p></td><td className="px-5 py-4"><p className="text-zinc-400">Hiện tại {n(item.currentBalance)}</p><p className="mt-1 text-zinc-600">Theo sổ {n(item.ledgerBalance)}</p><b className={Number(item.balanceDifference) ? 'text-red-400' : 'text-emerald-400'}>Chênh {n(item.balanceDifference)}</b></td><td className="px-5 py-4"><p className="text-zinc-400">Hiện tại {n(item.currentHeldPoints)}</p><p className="mt-1 text-zinc-600">Theo lệnh giữ {n(item.ledgerHeldPoints)}</p><b className={Number(item.heldDifference) ? 'text-red-400' : 'text-emerald-400'}>Chênh {n(item.heldDifference)}</b></td><td className="px-5 py-4"><p className="text-zinc-400">Hiện tại {n(item.currentAccumulated)}</p><p className="mt-1 text-zinc-600">Theo sổ {n(item.ledgerAccumulated)}</p><b className={Number(item.accumulatedDifference) ? 'text-red-400' : 'text-emerald-400'}>Chênh {n(item.accumulatedDifference)}</b></td><td className="max-w-xs px-5 py-4"><span className={`rounded-full px-2 py-1 text-[10px] font-black ${item.status === 'MATCHED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'}`}>{item.status === 'MATCHED' ? 'ĐÃ KHỚP' : 'CẦN KIỂM TRA'}</span><p className="mt-2 text-[10px] leading-5 text-zinc-500">{item.remark}</p></td><td className="px-5 py-4 text-right"><Link to={`/admin/scores/viewer?accountId=${item.userId}`} className="text-xs font-black text-brand-orange">Điều tra →</Link></td></tr>)}</tbody></table></div></article>;
}
