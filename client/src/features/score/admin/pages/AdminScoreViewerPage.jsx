import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useOutletContext, useSearchParams } from 'react-router-dom';
import {
  AlertTriangle, Award, Clock3, FileClock,
  LockKeyhole, RefreshCcw, Search, ShieldCheck, UnlockKeyhole, UserRound, WalletCards
} from 'lucide-react';
import useAdminScore from '../hooks/useAdminScore';
import ExpiringPointsSection from '@/features/score/customer/components/ExpiringPointsSection';
import TierHistoryTimeline from '@/features/score/customer/components/TierHistoryTimeline';

const n = value => Number(value ?? 0).toLocaleString('vi-VN');
const when = value => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—';
const TYPE_LABEL = {
  EARN: 'Tích điểm', EARN_BY_BOOKING: 'Tích điểm', HOLD: 'Tạm giữ', COMMIT: 'Đã sử dụng',
  REDEEM: 'Đã sử dụng', REDEEM_FOR_BOOKING: 'Đã sử dụng', RELEASE: 'Hoàn tạm giữ',
  REFUND_REDEEM: 'Hoàn điểm đã dùng', REVOKE_EARN: 'Thu hồi điểm tích',
  REVOKE_EARN_BY_REFUND: 'Thu hồi do hoàn tiền', EXPIRED: 'Hết hạn', MANUAL_ADD: 'Cộng thủ công',
  MANUAL_DEDUCT: 'Trừ thủ công', REVERSE_ADJUSTMENT: 'Đảo điều chỉnh',
};

function SummaryCard({ label, value, hint, icon: Icon, tone = 'text-white' }) {
  return <div className="rounded-2xl border border-white/10 bg-zinc-900/50 p-4"><div className="flex items-center justify-between"><p className="text-[10px] font-black uppercase tracking-[0.14em] text-zinc-500">{label}</p><Icon size={17} className="text-zinc-600" /></div><p className={`mt-3 text-2xl font-black ${tone}`}>{value}</p><p className="mt-1 text-xs leading-5 text-zinc-500">{hint}</p></div>;
}

export default function AdminScoreViewerPage() {
  const [params, setParams] = useSearchParams();
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const confirm = outlet?.triggerConfirm || (async () => false);
  const prompt = outlet?.triggerPrompt || (async () => null);
  const {
    userScore, userHistory, expiringPoints, tierHistory, isLoadingUserScore, errorUserScore,
    isLoadingOperations, fetchUserScore, fetchUserHistory, fetchUserExpiringPoints,
    fetchUserTierHistory, recalculateTier, updateScoreAccountStatus,
  } = useAdminScore();
  const [searchId, setSearchId] = useState(params.get('accountId') || '');
  const [filter, setFilter] = useState('ALL');
  const initialLoaded = useRef(false);
  const customerName = params.get('name');
  const customerCode = params.get('customerCode');

  const loadAccount = useCallback(async (accountId, historyFilter = 'ALL', page = 0) => {
    if (!/^\d+$/.test(String(accountId))) return;
    const historyParams = { page, size: 10, ...(historyFilter === 'ALL' ? {} : { transactionType: historyFilter }) };
    await Promise.allSettled([
      fetchUserScore(accountId, { forceRefresh: true }), fetchUserHistory(accountId, historyParams, { forceRefresh: true }),
      fetchUserExpiringPoints(accountId, { forceRefresh: true }), fetchUserTierHistory(accountId, { forceRefresh: true }),
    ]);
  }, [fetchUserExpiringPoints, fetchUserHistory, fetchUserScore, fetchUserTierHistory]);

  useEffect(() => {
    const accountId = params.get('accountId');
    if (accountId && !initialLoaded.current) { initialLoaded.current = true; loadAccount(accountId); }
  }, [loadAccount, params]);

  const submitSearch = event => {
    event.preventDefault();
    const value = searchId.trim();
    if (!/^\d+$/.test(value)) return;
    setFilter('ALL');
    setParams({ accountId: value });
    loadAccount(value);
  };

  const changeFilter = type => { setFilter(type); loadAccount(searchId, type); };
  const changePage = page => loadAccount(searchId, filter, page);

  const handleRecalculate = async () => {
    const accepted = await confirm({ title: 'Tính lại hạng thành viên', message: `Tính lại hạng của Account ID ${searchId} theo tổng điểm hạng hiện tại? Thao tác được ghi audit.`, confirmLabel: 'Tính lại hạng' });
    if (!accepted) return;
    try { await recalculateTier(searchId); notify('Đã tính lại hạng thành viên.'); }
    catch (error) { notify(error?.response?.data?.message || 'Không thể tính lại hạng.', 'error'); }
  };

  const handleStatus = async () => {
    const freezing = userScore?.status !== 'LOCKED';
    const reason = await prompt({
      title: freezing ? 'Đóng băng tài khoản điểm' : 'Mở lại tài khoản điểm',
      message: freezing ? 'Chỉ chặn tích/dùng điểm; khách hàng vẫn đăng nhập và xem hồ sơ bình thường.' : 'Khôi phục quyền tích và dùng điểm. Quyền đăng nhập không thay đổi.',
      label: 'Lý do xử lý', placeholder: 'Ví dụ: Đang xác minh khiếu nại hoàn tiền…', confirmLabel: freezing ? 'Đóng băng điểm' : 'Mở lại điểm',
    });
    if (!reason) return;
    const caseId = `SCORE-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`;
    try { await updateScoreAccountStatus(searchId, { status: freezing ? 'LOCKED' : 'ACTIVE', reason, caseId }); notify(`${freezing ? 'Đã đóng băng' : 'Đã mở lại'} tài khoản điểm · ${caseId}`); }
    catch (error) { notify(error?.response?.data?.message || 'Không thể cập nhật trạng thái điểm.', 'error'); }
  };

  const history = userHistory?.content || [];
  const page = Number(userHistory?.page ?? userHistory?.number ?? 0);
  const totalPages = Number(userHistory?.totalPages ?? 0);
  const rate = Number(userScore?.currentTier?.earningRate ?? 0) * 100;

  return (
    <section className="mx-auto max-w-7xl space-y-6 text-white">
      <header className="rounded-3xl border border-white/10 bg-zinc-900/50 p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between"><div><p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Customer score workspace</p><h1 className="mt-2 text-2xl font-black">Hồ sơ điểm thưởng khách hàng</h1><p className="mt-2 text-sm text-zinc-400">Điều tra số dư, hold, điểm hạng, expiration và thao tác hỗ trợ từ một hồ sơ.</p></div>{userScore ? <div className="text-right"><p className="text-sm font-black">{customerName || `Account ID ${userScore.userId}`}</p><p className="mt-1 font-mono text-xs text-zinc-500">{customerCode || `ACCOUNT-${userScore.userId}`}</p></div> : null}</div>
        <form onSubmit={submitSearch} className="mt-5 flex flex-col gap-2 sm:flex-row"><label className="relative flex-1"><Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} /><input value={searchId} onChange={event => setSearchId(event.target.value)} inputMode="numeric" aria-label="Account ID khách hàng" placeholder="Nhập Account ID từ hồ sơ khách hàng" className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-brand-orange" /></label><button className="h-11 rounded-xl bg-brand-orange px-6 text-xs font-black text-black hover:bg-orange-400">Mở hồ sơ</button></form>
        <p className="mt-2 text-[11px] text-zinc-600">Tìm theo tên/email tại <Link to="/admin/members" className="font-bold text-zinc-400 hover:text-white">Trung tâm khách hàng</Link>, sau đó chọn “Mở hồ sơ điểm thưởng”.</p>
      </header>

      {isLoadingUserScore ? <div className="rounded-2xl border border-white/10 p-10 text-center text-sm text-zinc-500">Đang tổng hợp hồ sơ điểm…</div> : null}
      {errorUserScore && !userScore ? <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">{errorUserScore}</div> : null}

      {userScore ? <>
        {userScore.status === 'LOCKED' ? <div className="flex gap-3 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4"><AlertTriangle className="shrink-0 text-amber-400" size={20} /><div><p className="text-sm font-black text-amber-200">Tài khoản điểm đang đóng băng</p><p className="mt-1 text-xs text-zinc-400">Khách hàng không thể tích hoặc dùng điểm; trạng thái đăng nhập là một kiểm soát riêng tại Trung tâm khách hàng.</p></div></div> : null}

        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <SummaryCard label="Khả dụng" value={n(userScore.availablePoints ?? Number(userScore.currentPoints) - Number(userScore.heldPoints))} hint="Có thể dùng ngay" icon={WalletCards} tone="text-emerald-400" />
          <SummaryCard label="Tạm giữ" value={n(userScore.heldPoints)} hint="Đang reserve cho booking" icon={Clock3} tone="text-sky-400" />
          <SummaryCard label="Điểm hạng" value={n(userScore.accumulatedPoints)} hint="Dùng xét hạng, không tiêu" icon={Award} tone="text-violet-300" />
          <SummaryCard label="Dư nợ" value={n(userScore.outstandingPoints)} hint="Cần thu hồi sau refund" icon={AlertTriangle} tone={Number(userScore.outstandingPoints) ? 'text-red-400' : 'text-zinc-300'} />
          <SummaryCard label="Hạng hiện tại" value={userScore.currentTier?.tierName || userScore.currentTier?.tierCode || '—'} hint={`${rate.toLocaleString('vi-VN')}% trên giá trị hợp lệ`} icon={ShieldCheck} tone="text-amber-300" />
        </div>

        <article className="rounded-3xl border border-white/10 bg-zinc-900/40 p-5">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"><div><p className="text-sm font-black">Công thức đang áp dụng</p><p className="mt-1 text-xs leading-5 text-zinc-400">Điểm nhận = giá trị thanh toán hợp lệ × {rate.toLocaleString('vi-VN')}% ÷ 1.000đ/điểm, làm tròn xuống. 1 điểm dùng = 1.000đ.</p><p className="mt-1 text-[11px] text-zinc-600">Cập nhật hồ sơ: {when(userScore.updatedAt)} · Tích gần nhất: {when(userScore.lastEarnAt)} · Dùng gần nhất: {when(userScore.lastRedeemAt)}</p></div><div className="flex flex-wrap gap-2"><button type="button" onClick={handleRecalculate} disabled={isLoadingOperations} className="rounded-xl border border-white/10 px-3 py-2 text-xs font-black text-zinc-300 hover:bg-white/5"><RefreshCcw size={14} className="mr-1.5 inline" />Tính lại hạng</button><button type="button" onClick={handleStatus} disabled={isLoadingOperations} className={`rounded-xl px-3 py-2 text-xs font-black ${userScore.status === 'LOCKED' ? 'bg-emerald-500/15 text-emerald-300' : 'bg-amber-500/15 text-amber-300'}`}>{userScore.status === 'LOCKED' ? <UnlockKeyhole size={14} className="mr-1.5 inline" /> : <LockKeyhole size={14} className="mr-1.5 inline" />}{userScore.status === 'LOCKED' ? 'Mở lại điểm' : 'Đóng băng điểm'}</button><Link to={`/admin/scores/adjustments?accountId=${searchId}`} className="rounded-xl bg-brand-orange px-3 py-2 text-xs font-black text-black">Tạo xử lý khiếu nại</Link></div></div>
        </article>

        <article className="overflow-hidden rounded-3xl border border-white/10 bg-zinc-900/40">
          <div className="flex flex-col gap-3 border-b border-white/10 p-5 md:flex-row md:items-center md:justify-between"><div><h2 className="font-black">Ledger giao dịch</h2><p className="mt-1 text-xs text-zinc-500">Snapshot trước/sau giúp giải thích chính xác từng biến động.</p></div><div className="flex flex-wrap gap-1">{[['ALL','Tất cả'],['EARN','Tích'],['HOLD','Tạm giữ'],['COMMIT','Đã dùng'],['REFUND_REDEEM','Hoàn'],['MANUAL_ADD','Thủ công']].map(([id,label]) => <button key={id} type="button" onClick={() => changeFilter(id)} className={`rounded-lg px-2.5 py-1.5 text-[11px] font-bold ${filter === id ? 'bg-white/10 text-white' : 'text-zinc-500 hover:text-zinc-300'}`}>{label}</button>)}</div></div>
          {history.length ? <div className="overflow-x-auto"><table className="min-w-full text-left text-xs"><thead className="bg-white/[0.025] text-[10px] uppercase tracking-wider text-zinc-600"><tr><th className="px-5 py-3">Thời gian / nguồn</th><th className="px-5 py-3">Nghiệp vụ</th><th className="px-5 py-3">Thay đổi</th><th className="px-5 py-3">Số dư</th><th className="px-5 py-3">Tham chiếu</th><th className="px-5 py-3">Lý do / case</th></tr></thead><tbody className="divide-y divide-white/5">{history.map(item => { const delta = Number(item.pointChange ?? 0); const operation = TYPE_LABEL[item.transactionType] || item.transactionType || 'Giao dịch tự động'; return <tr key={item.historyId} className="align-top hover:bg-white/[0.02]"><td className="px-5 py-4 text-zinc-400">{when(item.createdAt)}<p className="mt-1 text-[10px] text-zinc-600">{item.sourceService || 'SCORE_SERVICE'}</p></td><td className="px-5 py-4 font-bold text-zinc-200">{operation}</td><td className={`px-5 py-4 font-black ${delta >= 0 ? 'text-emerald-400' : 'text-amber-400'}`}>{delta > 0 ? '+' : ''}{n(delta)}<p className="mt-1 font-normal text-zinc-600">hold {n(item.heldBefore)} → {n(item.heldAfter)}</p></td><td className="px-5 py-4 text-zinc-300">{n(item.balanceBefore)} → <b>{n(item.balanceAfter)}</b><p className="mt-1 text-zinc-600">hạng {n(item.accumulatedBefore)} → {n(item.accumulatedAfter)}</p></td><td className="px-5 py-4 font-mono text-zinc-500">{item.bookingId ? `Booking ID ${item.bookingId}` : item.eventId || '—'}</td><td className="max-w-xs px-5 py-4 text-zinc-400">{item.reason || `${operation}${item.bookingId ? ` cho Booking ID ${item.bookingId}` : ''}`}{item.caseId ? <p className="mt-1 font-mono text-[10px] text-brand-orange">{item.caseId}</p> : null}</td></tr>; })}</tbody></table></div> : <div className="p-10 text-center text-sm text-zinc-500">{filter === 'ALL' ? 'Tài khoản chưa phát sinh giao dịch điểm.' : 'Không có giao dịch thuộc nhóm đang lọc.'}</div>}
          {totalPages > 1 ? <div className="flex items-center justify-between border-t border-white/10 p-4 text-xs text-zinc-500"><span>Trang {page + 1}/{totalPages}</span><div className="flex gap-2"><button disabled={page <= 0} onClick={() => changePage(page - 1)} className="rounded-lg border border-white/10 px-3 py-1.5 disabled:opacity-30">Trước</button><button disabled={page >= totalPages - 1} onClick={() => changePage(page + 1)} className="rounded-lg border border-white/10 px-3 py-1.5 disabled:opacity-30">Sau</button></div></div> : null}
        </article>

        <div className="grid gap-5 xl:grid-cols-2"><ExpiringPointsSection expiringPoints={expiringPoints} isLoading={isLoadingUserScore} /><TierHistoryTimeline tierHistory={tierHistory} isLoading={isLoadingUserScore} /></div>
        <div className="flex flex-wrap gap-4 text-xs"><Link to={`/admin/scores/audit-logs?userId=${searchId}`} className="inline-flex items-center gap-2 font-bold text-zinc-400 hover:text-white"><FileClock size={15} /> Nhật ký thao tác admin</Link><Link to="/admin/members" className="inline-flex items-center gap-2 font-bold text-zinc-400 hover:text-white"><UserRound size={15} /> Hồ sơ và quyền đăng nhập</Link></div>
      </> : null}
    </section>
  );
}
