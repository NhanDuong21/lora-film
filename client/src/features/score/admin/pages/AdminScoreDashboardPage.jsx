import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle, ArrowRight, Award, CheckCircle2, Clock3, Database,
  RefreshCw, ShieldAlert, Users, WalletCards
} from 'lucide-react';
import useAdminScore from '../hooks/useAdminScore';
import { getDashboard as getCustomerDashboard } from '@/features/internal-staff/admin/services/userAdminService';

const number = value => Number(value ?? 0).toLocaleString('vi-VN');
const PAGE_LOADED_AT = Date.now();
const dateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa có lần chạy';

function Metric({ label, value, unit = 'điểm', hint, icon: Icon, tone = 'text-white' }) {
  return (
    <article className="rounded-2xl border border-white/10 bg-zinc-900/50 p-5">
      <div className="flex items-start justify-between gap-3">
        <div><p className="text-[10px] font-black uppercase tracking-[0.16em] text-zinc-500">{label}</p><p className={`mt-3 text-3xl font-black tracking-tight ${tone}`}>{value}</p><p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-zinc-600">{unit}</p></div>
        <span className="rounded-xl border border-white/10 bg-white/5 p-2.5 text-zinc-400"><Icon size={19} /></span>
      </div>
      <p className="mt-4 text-xs leading-5 text-zinc-500">{hint}</p>
    </article>
  );
}

export default function AdminScoreDashboardPage() {
  const { dashboardStats, fetchDashboardStats } = useAdminScore();
  const [customerStats, setCustomerStats] = useState(null);
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    const [scoreResult, customerResult] = await Promise.allSettled([
      fetchDashboardStats({ forceRefresh: true }), getCustomerDashboard(),
    ]);
    if (customerResult.status === 'fulfilled') setCustomerStats(customerResult.value);
    setState({ loading: false, error: scoreResult.status === 'rejected' ? 'Không thể tải dữ liệu vận hành điểm thưởng.' : '' });
  }, [fetchDashboardStats]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const stats = dashboardStats || {};
  const totalCustomers = Number(customerStats?.totalCustomers ?? 0);
  const populationGap = Number(stats.totalMembers ?? 0) - totalCustomers;
  const reconTotal = Number(stats.lastReconciliationTotalUsers ?? 0);
  const reconCoverage = Number(stats.totalMembers) > 0 ? Math.round((reconTotal / Number(stats.totalMembers)) * 100) : 0;
  const unreconciledTotal = Math.max(0, Number(stats.totalMembers ?? 0) - reconTotal);
  const reconAgeHours = stats.lastReconciliationFinishedAt ? (PAGE_LOADED_AT - new Date(stats.lastReconciliationFinishedAt).getTime()) / 3_600_000 : Infinity;
  const isReconStale = reconAgeHours > 24;
  const attentionItems = [
    populationGap !== 0 && { text: `${Math.abs(populationGap)} hồ sơ lệch giữa khách hàng và tài khoản điểm`, view: 'population' },
    unreconciledTotal > 0 && { text: `${unreconciledTotal} tài khoản chưa có kết quả đối soát`, view: 'unreconciled' },
    Number(stats.pendingReconciliationMismatches) > 0 && { text: `${stats.pendingReconciliationMismatches} tài khoản có số dư chênh lệch`, view: 'mismatch' },
    isReconStale && { text: 'Lần đối soát gần nhất đã quá 24 giờ hoặc chưa tồn tại', view: 'runs' },
  ].filter(Boolean);
  const hasAttention = attentionItems.length > 0;
  const tierRows = [
    ['Silver', stats.silverMembers, 'bg-zinc-300'], ['Gold', stats.goldMembers, 'bg-amber-400'], ['Diamond', stats.diamondMembers, 'bg-cyan-400'],
  ];

  return (
    <section className="mx-auto max-w-7xl space-y-6 p-5 text-white md:p-8">
      <header className="flex flex-col gap-4 rounded-3xl border border-white/10 bg-zinc-900/50 p-6 lg:flex-row lg:items-center lg:justify-between">
        <div><p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Vận hành điểm thưởng</p><h1 className="mt-2 text-2xl font-black">Bàn điều hành điểm thưởng</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">Theo dõi độ phủ tài khoản, nghĩa vụ điểm, ngoại lệ đối soát và tình trạng chính sách trên cùng một màn hình.</p></div>
        <button type="button" onClick={load} disabled={state.loading} className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-xs font-black hover:bg-white/10 disabled:opacity-50"><RefreshCw size={16} className={state.loading ? 'animate-spin' : ''} /> Làm mới</button>
      </header>

      {state.error ? <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">{state.error}</div> : null}

      <div className={`rounded-2xl border p-5 ${hasAttention ? 'border-amber-500/30 bg-amber-500/[0.07]' : 'border-emerald-500/25 bg-emerald-500/[0.06]'}`}>
        <div className="flex gap-3">{hasAttention ? <AlertTriangle className="mt-0.5 shrink-0 text-amber-400" size={21} /> : <CheckCircle2 className="mt-0.5 shrink-0 text-emerald-400" size={21} />}<div className="min-w-0 flex-1"><p className="font-black">{hasAttention ? 'Có tín hiệu cần kiểm tra' : 'Các kiểm soát hiện tại không ghi nhận ngoại lệ'}</p>{hasAttention ? <div className="mt-3 grid gap-2 md:grid-cols-2">{attentionItems.map(item => <Link key={item.view} to={`/admin/scores/reconciliation?view=${item.view}`} className="flex items-center justify-between rounded-xl border border-amber-500/15 bg-black/15 px-3 py-2 text-xs text-zinc-300 hover:bg-black/30"><span>{item.text}</span><ArrowRight className="shrink-0 text-amber-300" size={14} /></Link>)}</div> : <p className="mt-1 text-xs leading-5 text-zinc-400">Không có chênh lệch, khoảng trống độ phủ hoặc lần chạy quá hạn trong dữ liệu vừa tải.</p>}</div></div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric label="Điểm khả dụng" value={number(stats.totalAvailablePoints)} hint="Nghĩa vụ khách hàng có thể dùng ngay; đã loại phần tạm giữ." icon={WalletCards} tone="text-emerald-400" />
        <Metric label="Đang tạm giữ" value={number(stats.totalPointsHeld)} hint="Điểm đã dành cho vé đang xử lý, chưa ghi nhận sử dụng hoặc hoàn lại." icon={Clock3} tone="text-sky-400" />
        <Metric label="Điểm hạng" value={number(stats.totalAccumulatedPoints)} hint="Chỉ dùng xét hạng; không đồng nghĩa số dư có thể chi tiêu." icon={Award} tone="text-violet-300" />
        <Metric label="Dư nợ điểm" value={number(stats.totalOutstandingPoints)} hint="Phần phải thu hồi sau hoàn/hủy khi số dư không đủ." icon={ShieldAlert} tone={Number(stats.totalOutstandingPoints) > 0 ? 'text-red-400' : 'text-zinc-300'} />
      </div>

      <div className="grid gap-5 lg:grid-cols-[1.15fr_.85fr]">
        <article className="rounded-3xl border border-white/10 bg-zinc-900/40 p-6">
          <div className="flex items-center justify-between"><div><h2 className="font-black">Độ phủ tài khoản</h2><p className="mt-1 text-xs text-zinc-500">Hai nguồn phải liên kết một-một trước khi kết luận hệ thống ổn định.</p></div><Database className="text-zinc-600" size={21} /></div>
          <div className="mt-5 grid gap-3 sm:grid-cols-3">
            <div className="rounded-2xl bg-black/30 p-4"><p className="text-xs text-zinc-500">Hồ sơ khách hàng</p><p className="mt-2 text-2xl font-black">{state.loading && !customerStats ? '—' : number(totalCustomers)}</p></div>
            <div className="rounded-2xl bg-black/30 p-4"><p className="text-xs text-zinc-500">Tài khoản điểm</p><p className="mt-2 text-2xl font-black">{state.loading && !dashboardStats ? '—' : number(stats.totalMembers)}</p><p className="mt-1 text-[11px] text-zinc-600">{number(stats.activeMembers)} đang hoạt động · {number(stats.lockedMembers)} đã khóa</p></div>
            <div className={`rounded-2xl p-4 ${populationGap === 0 ? 'bg-emerald-500/10' : 'bg-amber-500/10'}`}><p className="text-xs text-zinc-500">Hồ sơ cần xác minh</p><p className={`mt-2 text-2xl font-black ${populationGap === 0 ? 'text-emerald-400' : 'text-amber-400'}`}>{number(Math.abs(populationGap))}</p><p className="mt-1 text-[11px] text-zinc-600">Thiếu hoặc thừa liên kết giữa hai nguồn</p></div>
          </div>
          <div className="mt-4 flex flex-wrap gap-3 text-xs"><Link to="/admin/members" className="font-bold text-zinc-300 hover:text-white">Mở trung tâm khách hàng →</Link><Link to="/admin/scores/viewer" className="font-bold text-zinc-300 hover:text-white">Tra cứu tài khoản điểm →</Link></div>
        </article>

        <article className="rounded-3xl border border-white/10 bg-zinc-900/40 p-6">
          <div className="flex items-center justify-between"><div><h2 className="font-black">Đối soát gần nhất</h2><p className="mt-1 text-xs text-zinc-500">{stats.lastReconciliationBatch || 'Chưa có batch'}</p></div><span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${isReconStale ? 'bg-amber-500/10 text-amber-300' : 'bg-emerald-500/10 text-emerald-300'}`}>{isReconStale ? 'QUÁ HẠN' : 'MỚI'}</span></div>
          <div className="mt-5 flex items-end justify-between"><div><p className="text-3xl font-black">{reconCoverage}%</p><p className="mt-1 text-xs text-zinc-500">{number(reconTotal)}/{number(stats.totalMembers)} tài khoản toàn hệ thống</p><p className="mt-1 text-[11px] text-amber-400">{number(unreconciledTotal)} chưa được kiểm tra</p></div><p className="text-right text-xs leading-5 text-zinc-500">{dateTime(stats.lastReconciliationFinishedAt || stats.lastReconciliationTime)}<br />{number(stats.lastReconciliationMismatchedUsers)} chênh lệch trong nhóm đã kiểm tra</p></div>
          <div className="mt-4 h-2 overflow-hidden rounded-full bg-black/40"><div className={`h-full ${reconCoverage === 100 ? 'bg-emerald-500' : 'bg-amber-500'}`} style={{ width: `${Math.min(100, reconCoverage)}%` }} /></div>
        </article>
      </div>

      <div className="grid gap-5 lg:grid-cols-2">
        <article className="rounded-3xl border border-white/10 bg-zinc-900/40 p-6"><h2 className="font-black">Luồng điểm lũy kế</h2><p className="mt-1 text-xs text-zinc-500">Chỉ số từ sổ giao dịch, không phải số dư hiện tại.</p><div className="mt-5 grid grid-cols-3 gap-3 text-center"><div><p className="text-xl font-black text-emerald-400">{number(stats.totalPointsEarned)}</p><p className="mt-1 text-[10px] uppercase text-zinc-600">Đã phát hành</p></div><div><p className="text-xl font-black text-amber-400">{number(stats.totalPointsRedeemed)}</p><p className="mt-1 text-[10px] uppercase text-zinc-600">Đã sử dụng</p></div><div><p className="text-xl font-black text-zinc-300">{number(stats.totalPointsExpired)}</p><p className="mt-1 text-[10px] uppercase text-zinc-600">Đã hết hạn</p></div></div></article>
        <article className="rounded-3xl border border-white/10 bg-zinc-900/40 p-6"><div className="flex items-center justify-between"><div><h2 className="font-black">Phân bố hạng hiện tại</h2><p className="mt-1 text-xs text-zinc-500">Theo dữ liệu hạng hiện tại trên tài khoản điểm.</p></div><Users className="text-zinc-600" size={20} /></div><div className="mt-5 space-y-3">{tierRows.map(([label, value, color]) => { const percent = Number(stats.totalMembers) ? Math.round((Number(value || 0) / Number(stats.totalMembers)) * 100) : 0; return <div key={label} className="grid grid-cols-[72px_1fr_52px] items-center gap-3 text-xs"><span className="font-bold text-zinc-300">{label}</span><div className="h-1.5 overflow-hidden rounded-full bg-black/40"><div className={`h-full ${color}`} style={{ width: `${percent}%` }} /></div><span className="text-right text-zinc-500">{number(value)}</span></div>; })}</div><Link to="/admin/scores/tiers" className="mt-5 inline-flex items-center gap-2 text-xs font-black text-zinc-300 hover:text-white">Xem chính sách hạng <ArrowRight size={14} /></Link></article>
      </div>
    </section>
  );
}
