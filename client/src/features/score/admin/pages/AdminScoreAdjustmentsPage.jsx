import { useEffect, useState } from 'react';
import { Link, useOutletContext, useSearchParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, MinusCircle, PlusCircle, RotateCcw } from 'lucide-react';
import useAdminScore from '../hooks/useAdminScore';
import CustomerScoreSearch from '../components/CustomerScoreSearch';

const n = value => Number(value ?? 0).toLocaleString('vi-VN');
const newCaseId = () => `CASE-SCORE-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`;
const typeLabel = { MANUAL_ADD: 'Cộng điểm có kiểm soát', MANUAL_DEDUCT: 'Trừ điểm có kiểm soát', REVERSE_ADJUSTMENT: 'Đảo điều chỉnh', EARN: 'Tích điểm', REDEEM: 'Dùng điểm', HOLD: 'Tạm giữ điểm', COMMIT: 'Hoàn tất dùng điểm', RELEASE: 'Hoàn điểm tạm giữ', REFUND_REDEEM: 'Hoàn điểm đã dùng', REVOKE_EARN: 'Thu hồi điểm tích' };

export default function AdminScoreAdjustmentsPage() {
  const [params, setParams] = useSearchParams();
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const confirm = outlet?.triggerConfirm || (async () => false);
  const {
    userScore, userHistory, errorUserScore, fetchUserScore,
    fetchUserHistory, adjustScore, reverseAdjustment, isLoadingOperations,
  } = useAdminScore();
  const [accountId, setAccountId] = useState(params.get('accountId') || '');
  const [selectedCustomer, setSelectedCustomer] = useState({ fullName: params.get('name') || '', customerCode: params.get('customerCode') || '' });
  const [tab, setTab] = useState('adjust');
  const [adjust, setAdjust] = useState({ type: 'ADD', points: '', reason: '', caseId: newCaseId(), affectAccumulatedPoints: false });
  const [reverse, setReverse] = useState({ historyId: '', reason: '', caseId: newCaseId() });

  const load = async (id, customer = selectedCustomer) => {
    if (!/^\d+$/.test(String(id))) return;
    setParams({ accountId: String(id), ...(customer.fullName ? { name: customer.fullName } : {}), ...(customer.customerCode ? { customerCode: customer.customerCode } : {}) });
    await Promise.allSettled([fetchUserScore(id, { forceRefresh: true }), fetchUserHistory(id, { page: 0, size: 15 }, { forceRefresh: true })]);
  };

  useEffect(() => { const id = params.get('accountId'); if (id) load(id); /* initial deep-link only */ }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const selectCustomer = customer => {
    setAccountId(customer.accountId);
    setSelectedCustomer(customer);
    load(customer.accountId, customer);
  };

  const submitAdjust = async event => {
    event.preventDefault();
    const points = Number(adjust.points);
    if (!userScore || points < 1 || adjust.reason.trim().length < 5 || !adjust.caseId.trim()) return;
    const available = Number(userScore.availablePoints ?? Number(userScore.currentPoints) - Number(userScore.heldPoints));
    if (adjust.type === 'DEDUCT' && points > available) { notify('Không thể trừ quá điểm khả dụng. Hệ thống không tạo số dư âm bằng thao tác thủ công.', 'error'); return; }
    const approved = await confirm({
      title: adjust.type === 'ADD' ? 'Xác nhận cộng điểm' : 'Xác nhận trừ điểm',
      message: `${adjust.type === 'ADD' ? 'Cộng' : 'Trừ'} ${n(points)} điểm cho ${selectedCustomer.fullName || `tài khoản điểm ${accountId}`}. ${adjust.affectAccumulatedPoints ? 'Điểm hạng và hạng thành viên cũng có thể thay đổi.' : 'Điểm hạng không thay đổi.'} Case: ${adjust.caseId}.`,
      confirmLabel: adjust.type === 'ADD' ? 'Cộng điểm' : 'Trừ điểm', tone: adjust.type === 'DEDUCT' ? 'danger' : 'warning',
    });
    if (!approved) return;
    try {
      const requestId = `REQ-${adjust.caseId}-${adjust.type}`;
      await adjustScore(accountId, { type: adjust.type, points, reason: adjust.reason.trim(), caseId: adjust.caseId.trim(), requestId, affectAccumulatedPoints: adjust.affectAccumulatedPoints, allowNegative: false });
      notify(`Đã xử lý ${adjust.caseId}.`);
      setAdjust(value => ({ ...value, points: '', reason: '', caseId: newCaseId(), affectAccumulatedPoints: false }));
      await Promise.all([fetchUserScore(accountId, { forceRefresh: true }), fetchUserHistory(accountId, { page: 0, size: 15 }, { forceRefresh: true })]);
    } catch (error) { notify(error?.response?.data?.message || 'Không thể điều chỉnh điểm.', 'error'); }
  };

  const submitReverse = async event => {
    event.preventDefault();
    if (!userScore || !reverse.historyId || reverse.reason.trim().length < 5 || !reverse.caseId.trim()) return;
    const approved = await confirm({ title: 'Đảo giao dịch điều chỉnh', message: `Đảo giao dịch #${reverse.historyId} của ${selectedCustomer.fullName || `tài khoản điểm ${accountId}`}. Hệ thống chỉ cho đảo một lần và sẽ ghi nhật ký theo ${reverse.caseId}.`, confirmLabel: 'Đảo giao dịch', tone: 'danger' });
    if (!approved) return;
    try {
      await reverseAdjustment(accountId, { historyId: Number(reverse.historyId), reason: reverse.reason.trim(), caseId: reverse.caseId.trim(), requestId: `REQ-${reverse.caseId}-REVERSE` });
      notify(`Đã đảo giao dịch theo ${reverse.caseId}.`);
      setReverse({ historyId: '', reason: '', caseId: newCaseId() });
      await Promise.all([fetchUserScore(accountId, { forceRefresh: true }), fetchUserHistory(accountId, { page: 0, size: 15 }, { forceRefresh: true })]);
    } catch (error) { notify(error?.response?.data?.message || 'Không thể đảo giao dịch.', 'error'); }
  };

  const history = userHistory?.content || [];
  const available = Number(userScore?.availablePoints ?? Number(userScore?.currentPoints || 0) - Number(userScore?.heldPoints || 0));

  return (
    <section className="mx-auto max-w-6xl space-y-6 text-white">
      <header className="rounded-3xl border border-white/10 bg-zinc-900/50 p-6"><div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"><div><p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Thao tác có kiểm soát</p><h1 className="mt-2 text-2xl font-black">Điều chỉnh điểm có kiểm soát</h1><p className="mt-2 text-sm text-zinc-400">Dùng cho kết luận hỗ trợ đã được xác minh. Admin chỉ nhập mã case, lý do và hướng xử lý; mã chống gửi trùng được hệ thống tự sinh.</p></div>{userScore ? <Link to={`/admin/scores/viewer?accountId=${accountId}&name=${encodeURIComponent(selectedCustomer.fullName || '')}&customerCode=${encodeURIComponent(selectedCustomer.customerCode || '')}`} className="inline-flex items-center gap-2 text-xs font-black text-zinc-400 hover:text-white"><ArrowLeft size={15} /> Về hồ sơ điểm</Link> : null}</div></header>

      <article className="rounded-3xl border border-white/10 bg-zinc-900/40 p-5">
        <CustomerScoreSearch initialQuery={selectedCustomer.fullName || selectedCustomer.customerCode || ''} onSelect={selectCustomer} />
        {errorUserScore && !userScore ? <p className="mt-3 text-xs text-red-400">{errorUserScore}</p> : null}
        {userScore ? <div className="mt-5 grid gap-3 sm:grid-cols-4"><div className="rounded-xl bg-black/30 p-3"><p className="text-[10px] uppercase text-zinc-600">Khả dụng</p><p className="mt-1 text-xl font-black text-emerald-400">{n(available)}</p></div><div className="rounded-xl bg-black/30 p-3"><p className="text-[10px] uppercase text-zinc-600">Tạm giữ</p><p className="mt-1 text-xl font-black text-sky-400">{n(userScore.heldPoints)}</p></div><div className="rounded-xl bg-black/30 p-3"><p className="text-[10px] uppercase text-zinc-600">Điểm hạng</p><p className="mt-1 text-xl font-black">{n(userScore.accumulatedPoints)}</p></div><div className="rounded-xl bg-black/30 p-3"><p className="text-[10px] uppercase text-zinc-600">Trạng thái điểm</p><p className="mt-1 text-sm font-black">{userScore.status === 'LOCKED' ? 'Đang đóng băng' : 'Đang hoạt động'}</p></div></div> : null}
      </article>

      {userScore ? <>
        <div className="flex gap-2 border-b border-white/10"><button onClick={() => setTab('adjust')} className={`px-4 py-3 text-xs font-black ${tab === 'adjust' ? 'border-b-2 border-brand-orange text-brand-orange' : 'text-zinc-500'}`}>Cộng / trừ có kiểm soát</button><button onClick={() => setTab('reverse')} className={`px-4 py-3 text-xs font-black ${tab === 'reverse' ? 'border-b-2 border-red-400 text-red-400' : 'text-zinc-500'}`}>Đảo điều chỉnh</button></div>

        {tab === 'adjust' ? <form onSubmit={submitAdjust} className="space-y-5 rounded-3xl border border-white/10 bg-zinc-900/40 p-6">
          <div className="grid gap-3 sm:grid-cols-2"><button type="button" onClick={() => setAdjust(v => ({ ...v, type: 'ADD' }))} className={`rounded-xl border p-4 text-sm font-black ${adjust.type === 'ADD' ? 'border-emerald-500/40 bg-emerald-500/10 text-emerald-300' : 'border-white/10 text-zinc-500'}`}><PlusCircle size={17} className="mr-2 inline" />Cộng điểm bù</button><button type="button" onClick={() => setAdjust(v => ({ ...v, type: 'DEDUCT' }))} className={`rounded-xl border p-4 text-sm font-black ${adjust.type === 'DEDUCT' ? 'border-red-500/40 bg-red-500/10 text-red-300' : 'border-white/10 text-zinc-500'}`}><MinusCircle size={17} className="mr-2 inline" />Trừ điểm sai</button></div>
          <div className="grid gap-4 md:grid-cols-2"><label className="text-xs font-bold text-zinc-400">Số điểm *<input type="number" min="1" max="1000000" required value={adjust.points} onChange={event => setAdjust(v => ({ ...v, points: event.target.value }))} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-black/30 px-4 text-sm text-white outline-none focus:border-brand-orange" /></label><label className="text-xs font-bold text-zinc-400">Mã case *<input required value={adjust.caseId} onChange={event => setAdjust(v => ({ ...v, caseId: event.target.value }))} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-black/30 px-4 font-mono text-sm text-white outline-none focus:border-brand-orange" /></label></div>
          <label className="block text-xs font-bold text-zinc-400">Lý do và căn cứ xử lý *<textarea required minLength={5} maxLength={255} rows={3} value={adjust.reason} onChange={event => setAdjust(v => ({ ...v, reason: event.target.value }))} placeholder="Mô tả đặt vé, thanh toán, hoàn tiền liên quan và kết luận xác minh…" className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-4 text-sm text-white outline-none focus:border-brand-orange" /></label>
          <label className="flex cursor-pointer gap-3 rounded-xl border border-amber-500/20 bg-amber-500/[0.06] p-4"><input type="checkbox" checked={adjust.affectAccumulatedPoints} onChange={event => setAdjust(v => ({ ...v, affectAccumulatedPoints: event.target.checked }))} className="mt-1" /><span><b className="text-sm text-amber-200">Điều chỉnh cả điểm hạng</b><span className="mt-1 block text-xs leading-5 text-zinc-500">Chỉ chọn khi kết luận giao dịch gốc phải làm thay đổi thành tích xét hạng; có thể gây thăng hoặc giáng hạng.</span></span></label>
          <div className="flex items-center justify-between border-t border-white/10 pt-5"><p className="text-xs text-zinc-500">Số dư dự kiến: <b className="text-white">{n(adjust.type === 'ADD' ? available + Number(adjust.points || 0) : available - Number(adjust.points || 0))}</b></p><button disabled={isLoadingOperations} className={`rounded-xl px-5 py-2.5 text-xs font-black disabled:opacity-50 ${adjust.type === 'ADD' ? 'bg-emerald-500 text-black' : 'bg-red-500 text-white'}`}>Xem lại và xác nhận</button></div>
        </form> : <form onSubmit={submitReverse} className="space-y-5 rounded-3xl border border-red-500/20 bg-zinc-900/40 p-6">
          <div className="rounded-xl border border-red-500/20 bg-red-500/[0.06] p-4 text-xs leading-5 text-zinc-400"><AlertTriangle size={16} className="mr-2 inline text-red-400" />Chỉ đảo giao dịch cộng/trừ có kiểm soát hợp lệ. Giao dịch đã đảo không thể đảo lần hai.</div>
          <div className="grid gap-4 md:grid-cols-2"><label className="text-xs font-bold text-zinc-400">Mã giao dịch cần đảo *<input type="number" min="1" required value={reverse.historyId} onChange={event => setReverse(v => ({ ...v, historyId: event.target.value }))} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-black/30 px-4 text-sm text-white outline-none focus:border-red-400" /></label><label className="text-xs font-bold text-zinc-400">Mã case *<input required value={reverse.caseId} onChange={event => setReverse(v => ({ ...v, caseId: event.target.value }))} className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-black/30 px-4 font-mono text-sm text-white outline-none focus:border-red-400" /></label></div>
          <label className="block text-xs font-bold text-zinc-400">Lý do đảo *<textarea required minLength={5} maxLength={255} rows={3} value={reverse.reason} onChange={event => setReverse(v => ({ ...v, reason: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-4 text-sm text-white outline-none focus:border-red-400" /></label>
          <div className="flex justify-end"><button disabled={isLoadingOperations} className="rounded-xl bg-red-500 px-5 py-2.5 text-xs font-black text-white disabled:opacity-50"><RotateCcw size={15} className="mr-2 inline" />Xem lại và đảo</button></div>
        </form>}

        <article className="overflow-hidden rounded-3xl border border-white/10 bg-zinc-900/40"><div className="border-b border-white/10 p-5"><h2 className="font-black">Giao dịch gần đây</h2><p className="mt-1 text-xs text-zinc-500">Chọn nhanh giao dịch cộng/trừ có kiểm soát khi cần đảo.</p></div>{history.length ? <div className="overflow-x-auto"><table className="min-w-full text-left text-xs"><thead className="bg-white/[0.025] text-[10px] uppercase text-zinc-600"><tr><th className="px-5 py-3">Mã giao dịch</th><th className="px-5 py-3">Nghiệp vụ</th><th className="px-5 py-3">Biến động</th><th className="px-5 py-3">Case / lý do</th><th className="px-5 py-3"></th></tr></thead><tbody className="divide-y divide-white/5">{history.map(item => { const manual = ['MANUAL_ADD','MANUAL_DEDUCT'].includes(item.transactionType); return <tr key={item.historyId}><td className="px-5 py-4 font-mono text-brand-orange">#{item.historyId}</td><td className="px-5 py-4 font-bold">{typeLabel[item.transactionType] || 'Giao dịch điểm'}</td><td className="px-5 py-4">{Number(item.pointChange) > 0 ? '+' : ''}{n(item.pointChange)}</td><td className="max-w-sm px-5 py-4 text-zinc-500">{item.caseId || item.reason || '—'}</td><td className="px-5 py-4 text-right">{manual ? <button type="button" onClick={() => { setTab('reverse'); setReverse(v => ({ ...v, historyId: String(item.historyId) })); }} className="text-xs font-black text-red-300 hover:text-red-200">Chọn để đảo</button> : null}</td></tr>; })}</tbody></table></div> : <p className="p-8 text-center text-sm text-zinc-500">Chưa có giao dịch.</p>}</article>
      </> : null}
    </section>
  );
}
