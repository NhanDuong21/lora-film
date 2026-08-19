import { useEffect, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { AlertTriangle, CheckCircle2, ChevronDown, Play, RefreshCw, ShieldAlert } from 'lucide-react';
import useAdminScore from '../hooks/useAdminScore';

const n = value => Number(value ?? 0).toLocaleString('vi-VN');
const when = value => value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—';

export default function AdminScoreReconciliationPage() {
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const confirm = outlet?.triggerConfirm || (async () => false);
  const {
    reconciliationRuns, reconciliationDetails, fetchReconciliationRuns,
    fetchReconciliationDetails, runReconciliation, isLoadingOperations,
  } = useAdminScore();
  const [selectedRun, setSelectedRun] = useState(null);
  const [detailStatus, setDetailStatus] = useState('MISMATCH');
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [remark, setRemark] = useState('');

  useEffect(() => { fetchReconciliationRuns({ page: 0, size: 20 }); }, [fetchReconciliationRuns]);
  useEffect(() => {
    const latest = reconciliationRuns?.content?.[0];
    if (latest && !selectedRun) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedRun(latest);
      fetchReconciliationDetails({ runId: latest.id, page: 0, size: 50, status: 'MISMATCH' });
    }
  }, [fetchReconciliationDetails, reconciliationRuns, selectedRun]);

  const selectRun = async run => {
    setSelectedRun(run);
    await fetchReconciliationDetails({ runId: run.id, page: 0, size: 50, ...(detailStatus ? { status: detailStatus } : {}) });
  };

  const switchStatus = async status => {
    setDetailStatus(status);
    if (selectedRun) await fetchReconciliationDetails({ runId: selectedRun.id, page: 0, size: 50, ...(status ? { status } : {}) });
  };

  const execute = async event => {
    event.preventDefault();
    if (remark.trim().length < 5) return;
    const batchCode = `RECON-${new Date().toISOString().replace(/\D/g, '').slice(0, 14)}`;
    const accepted = await confirm({ title: 'Chạy đối soát toàn bộ', message: `Đối chiếu toàn bộ tài khoản điểm với ledger và hold đang active. Batch ${batchCode}; mục đích: ${remark.trim()}.`, confirmLabel: 'Chạy đối soát' });
    if (!accepted) return;
    try {
      const run = await runReconciliation({ batchCode, remark: remark.trim() });
      notify(`Đã hoàn tất ${run.batchCode}: ${run.mismatchedUsers} tài khoản lệch.`);
      setRemark(''); setAdvancedOpen(false); setSelectedRun(run); setDetailStatus('MISMATCH');
      await Promise.all([fetchReconciliationRuns({ page: 0, size: 20 }, { forceRefresh: true }), fetchReconciliationDetails({ runId: run.id, page: 0, size: 50, status: 'MISMATCH' }, { forceRefresh: true })]);
    } catch (error) { notify(error?.response?.data?.message || 'Không thể chạy đối soát.', 'error'); }
  };

  const runs = reconciliationRuns?.content || [];
  const details = reconciliationDetails?.content || [];
  const coverage = selectedRun?.totalUsers ? 100 : 0;

  return (
    <section className="mx-auto max-w-7xl space-y-6 text-white">
      <header className="flex flex-col gap-4 rounded-3xl border border-white/10 bg-zinc-900/50 p-6 lg:flex-row lg:items-center lg:justify-between"><div><p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Integrity control</p><h1 className="mt-2 text-2xl font-black">Hàng đợi ngoại lệ điểm thưởng</h1><p className="mt-2 text-sm text-zinc-400">Ưu tiên tài khoản lệch giữa balance, active hold và điểm hạng. Kết quả khớp được ẩn mặc định.</p></div><button onClick={() => fetchReconciliationRuns({ page: 0, size: 20 }, { forceRefresh: true })} disabled={isLoadingOperations} className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-xs font-black hover:bg-white/10 disabled:opacity-50"><RefreshCw size={15} className={isLoadingOperations ? 'animate-spin' : ''} /> Làm mới</button></header>

      {selectedRun ? <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4"><div className="rounded-2xl border border-white/10 bg-zinc-900/40 p-4"><p className="text-[10px] uppercase text-zinc-600">Scope đã kiểm</p><p className="mt-2 text-2xl font-black">{n(selectedRun.totalUsers)}</p><p className="mt-1 text-xs text-zinc-500">{coverage}% batch hoàn tất</p></div><div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/[0.05] p-4"><p className="text-[10px] uppercase text-zinc-600">Khớp</p><p className="mt-2 text-2xl font-black text-emerald-400">{n(selectedRun.matchedUsers)}</p><p className="mt-1 text-xs text-zinc-500">Không cần xử lý</p></div><div className="rounded-2xl border border-red-500/20 bg-red-500/[0.05] p-4"><p className="text-[10px] uppercase text-zinc-600">Ngoại lệ</p><p className="mt-2 text-2xl font-black text-red-400">{n(selectedRun.mismatchedUsers)}</p><p className="mt-1 text-xs text-zinc-500">Cần điều tra theo tài khoản</p></div><div className="rounded-2xl border border-white/10 bg-zinc-900/40 p-4"><p className="text-[10px] uppercase text-zinc-600">Hoàn tất</p><p className="mt-2 text-sm font-black">{when(selectedRun.finishedAt || selectedRun.startedAt)}</p><p className="mt-1 truncate font-mono text-[10px] text-zinc-600">{selectedRun.batchCode}</p></div></div> : null}

      <div className="grid gap-5 xl:grid-cols-[340px_1fr]">
        <article className="overflow-hidden rounded-3xl border border-white/10 bg-zinc-900/40"><div className="border-b border-white/10 p-5"><h2 className="font-black">Các lần đối soát</h2><p className="mt-1 text-xs text-zinc-500">Mới nhất trước</p></div><div className="max-h-[580px] divide-y divide-white/5 overflow-y-auto">{runs.length ? runs.map(run => <button type="button" key={run.id} onClick={() => selectRun(run)} className={`w-full p-4 text-left hover:bg-white/[0.03] ${selectedRun?.id === run.id ? 'bg-white/[0.05]' : ''}`}><div className="flex items-center justify-between gap-3"><span className="truncate font-mono text-xs font-black text-zinc-200">{run.batchCode}</span>{Number(run.mismatchedUsers) ? <span className="rounded-full bg-red-500/10 px-2 py-1 text-[10px] font-black text-red-400">{run.mismatchedUsers} lệch</span> : <CheckCircle2 size={16} className="text-emerald-400" />}</div><p className="mt-2 text-[11px] text-zinc-500">{when(run.startedAt)} · {n(run.totalUsers)} tài khoản</p><p className="mt-1 truncate text-[11px] text-zinc-600">{run.remark || 'Không có ghi chú'}</p></button>) : <p className="p-8 text-center text-sm text-zinc-500">Chưa có lần đối soát.</p>}</div></article>

        <article className="overflow-hidden rounded-3xl border border-white/10 bg-zinc-900/40"><div className="flex flex-col gap-3 border-b border-white/10 p-5 md:flex-row md:items-center md:justify-between"><div><h2 className="font-black">{detailStatus === 'MISMATCH' ? 'Tài khoản cần xử lý' : detailStatus === 'MATCHED' ? 'Tài khoản đã khớp' : 'Toàn bộ kết quả'}</h2><p className="mt-1 text-xs text-zinc-500">{selectedRun ? selectedRun.batchCode : 'Chọn một batch để xem'}</p></div><div className="flex rounded-xl border border-white/10 bg-black/20 p-1">{[['MISMATCH','Ngoại lệ'],['MATCHED','Đã khớp'],['','Tất cả']].map(([id,label]) => <button key={label} onClick={() => switchStatus(id)} className={`rounded-lg px-3 py-1.5 text-[11px] font-black ${detailStatus === id ? 'bg-white/10 text-white' : 'text-zinc-500'}`}>{label}</button>)}</div></div>
          {!selectedRun ? <p className="p-10 text-center text-sm text-zinc-500">Chọn một lần đối soát.</p> : details.length ? <div className="overflow-x-auto"><table className="min-w-full text-left text-xs"><thead className="bg-white/[0.025] text-[10px] uppercase tracking-wider text-zinc-600"><tr><th className="px-5 py-3">Tài khoản</th><th className="px-5 py-3">Số dư</th><th className="px-5 py-3">Tạm giữ</th><th className="px-5 py-3">Điểm hạng</th><th className="px-5 py-3">Phân loại</th><th className="px-5 py-3"></th></tr></thead><tbody className="divide-y divide-white/5">{details.map(item => <tr key={item.id} className="align-top hover:bg-white/[0.02]"><td className="px-5 py-4"><p className="font-mono font-black">Account {item.userId}</p><p className="mt-1 text-[10px] text-zinc-600">Detail #{item.id}</p></td><td className="px-5 py-4"><p className="text-zinc-400">Hiện tại {n(item.currentBalance)}</p><p className="mt-1 text-zinc-600">Ledger {n(item.ledgerBalance)}</p><b className={Number(item.balanceDifference) ? 'text-red-400' : 'text-emerald-400'}>Δ {n(item.balanceDifference)}</b></td><td className="px-5 py-4"><p className="text-zinc-400">Hiện tại {n(item.currentHeldPoints)}</p><p className="mt-1 text-zinc-600">Active hold {n(item.ledgerHeldPoints)}</p><b className={Number(item.heldDifference) ? 'text-red-400' : 'text-emerald-400'}>Δ {n(item.heldDifference)}</b></td><td className="px-5 py-4"><p className="text-zinc-400">Hiện tại {n(item.currentAccumulated)}</p><p className="mt-1 text-zinc-600">Ledger {n(item.ledgerAccumulated)}</p><b className={Number(item.accumulatedDifference) ? 'text-red-400' : 'text-emerald-400'}>Δ {n(item.accumulatedDifference)}</b></td><td className="max-w-xs px-5 py-4"><span className={`rounded-full px-2 py-1 text-[10px] font-black ${item.status === 'MATCHED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'}`}>{item.status === 'MATCHED' ? 'KHỚP' : 'CẦN KIỂM TRA'}</span><p className="mt-2 font-mono text-[10px] leading-5 text-zinc-500">{item.remark}</p></td><td className="px-5 py-4 text-right"><Link to={`/admin/scores/viewer?accountId=${item.userId}`} className="text-xs font-black text-brand-orange hover:text-orange-300">Điều tra →</Link></td></tr>)}</tbody></table></div> : <div className="p-12 text-center">{detailStatus === 'MISMATCH' ? <><CheckCircle2 className="mx-auto text-emerald-400" size={28} /><p className="mt-3 text-sm font-black">Không có ngoại lệ trong batch này</p><p className="mt-1 text-xs text-zinc-500">Balance, active hold và điểm hạng đều khớp ledger.</p></> : <p className="text-sm text-zinc-500">Không có kết quả thuộc bộ lọc.</p>}</div>}
        </article>
      </div>

      <article className="rounded-3xl border border-white/10 bg-zinc-900/40"><button type="button" onClick={() => setAdvancedOpen(value => !value)} className="flex w-full items-center justify-between p-5 text-left"><div><h2 className="font-black">Chạy thủ công (nâng cao)</h2><p className="mt-1 text-xs text-zinc-500">Dùng sau deploy, migration, incident hoặc khi batch tự động quá hạn.</p></div><ChevronDown size={18} className={`text-zinc-500 transition ${advancedOpen ? 'rotate-180' : ''}`} /></button>{advancedOpen ? <form onSubmit={execute} className="border-t border-white/10 p-5"><div className="flex gap-3 rounded-xl border border-amber-500/20 bg-amber-500/[0.06] p-4 text-xs leading-5 text-zinc-400"><AlertTriangle size={17} className="shrink-0 text-amber-400" />Đợt chạy hiện kiểm toàn bộ Score account và so sánh ledger balance, active hold, tier-points ledger. Không tự sửa số dư.</div><div className="mt-4 flex flex-col gap-3 md:flex-row"><label className="flex-1 text-xs font-bold text-zinc-400">Mục đích chạy *<input required minLength={5} maxLength={500} value={remark} onChange={event => setRemark(event.target.value)} placeholder="Ví dụ: Kiểm tra sau deploy score-service #142" className="mt-2 h-11 w-full rounded-xl border border-white/10 bg-black/30 px-4 text-sm text-white outline-none focus:border-brand-orange" /></label><button disabled={isLoadingOperations || remark.trim().length < 5} className="mt-auto h-11 rounded-xl bg-brand-orange px-5 text-xs font-black text-black disabled:opacity-40"><Play size={14} className="mr-2 inline" />Xem lại và chạy</button></div></form> : null}</article>

      <div className="flex gap-3 rounded-2xl border border-sky-500/20 bg-sky-500/[0.05] p-4 text-xs leading-5 text-zinc-400"><ShieldAlert size={18} className="shrink-0 text-sky-400" /><p>Đối soát này chứng minh ledger nội bộ khớp projection/hold; chưa thay thế đối chiếu chéo sự kiện gốc từ Booking, Payment và Refund. Khi điều tra ngoại lệ, admin cần kiểm tra cả mã booking/payment trong hồ sơ giao dịch.</p></div>
    </section>
  );
}
