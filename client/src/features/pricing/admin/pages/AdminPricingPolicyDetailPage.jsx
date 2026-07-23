import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, ChevronLeft, ChevronRight, Copy, Edit, Power, PowerOff, RefreshCw } from 'lucide-react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import adminPricingService from '../services/adminPricingService';

const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);

export default function AdminPricingPolicyDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const [policy, setPolicy] = useState(null);
  const [usage, setUsage] = useState(null);
  const [usagePage, setUsagePage] = useState(0);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const [policyResponse, usageResponse] = await Promise.all([
        adminPricingService.getPolicy(id),
        adminPricingService.getUsage(id, { page: usagePage, size: 20 }),
      ]);
      setPolicy(policyResponse?.data);
      setUsage(usageResponse?.data);
    } catch (error) {
      triggerToast?.(error.response?.data?.message || 'Không thể tải chính sách giá', 'error');
    }
  }, [id, triggerToast, usagePage]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const act = async action => {
    if (!policy || busy) return;
    setBusy(true);
    try {
      let response;
      if (action === 'activate') {
        const accepted = await triggerConfirm?.('Kích hoạt sẽ khóa vĩnh viễn nội dung và quy tắc của phiên bản này.');
        if (accepted === false) return;
        response = await adminPricingService.activatePolicy(id, policy.version);
      } else if (action === 'deactivate') {
        const reason = window.prompt('Lý do ngừng áp dụng chính sách:');
        if (!reason?.trim()) return;
        response = await adminPricingService.deactivatePolicy(id, policy.version, reason.trim());
      } else {
        const name = window.prompt('Tên bản nháp mới:', `${policy.name} - Bản sao`);
        if (!name?.trim()) return;
        response = await adminPricingService.copyPolicy(id, policy.version, name.trim());
        navigate(`/admin/pricing/${response.data.publicId}/edit`);
        return;
      }
      setPolicy(response.data);
      triggerToast?.('Đã cập nhật vòng đời chính sách giá', 'success');
      load();
    } catch (error) {
      triggerToast?.(error.response?.data?.message || 'Không thể cập nhật chính sách', 'error');
    } finally {
      setBusy(false);
    }
  };

  if (!policy) return <div className="p-12 text-center text-zinc-500">Đang tải chính sách giá…</div>;

  return (
    <div className="space-y-6 bg-zinc-950 text-white">
      <div className="flex flex-col gap-4 border-b border-zinc-800 pb-5 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-3">
          <button type="button" onClick={() => navigate('/admin/pricing')} className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800"><ArrowLeft className="h-5 w-5" /></button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-black">{policy.name}</h1>
              <span className="rounded-full border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 text-xs font-black text-amber-300">{policy.displayStatus}</span>
            </div>
            <p className="mt-1 font-mono text-xs text-zinc-500">{policy.publicId} · v{policy.version}</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={load} className="rounded-xl border border-zinc-700 p-2.5 text-zinc-400"><RefreshCw className="h-4 w-4" /></button>
          {policy.storedStatus === 'DRAFT' && <button type="button" onClick={() => navigate(`/admin/pricing/${id}/edit`)} className="flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-sm font-bold"><Edit className="h-4 w-4" /> Sửa</button>}
          {policy.storedStatus === 'DRAFT' && <button disabled={busy || policy.conflicts?.length > 0} type="button" onClick={() => act('activate')} className="flex items-center gap-2 rounded-xl bg-emerald-500 px-3 py-2 text-sm font-black text-zinc-950 disabled:opacity-40"><Power className="h-4 w-4" /> Kích hoạt</button>}
          {policy.storedStatus === 'ACTIVE' && <button disabled={busy} type="button" onClick={() => act('deactivate')} className="flex items-center gap-2 rounded-xl bg-red-500 px-3 py-2 text-sm font-black text-white"><PowerOff className="h-4 w-4" /> Ngừng áp dụng</button>}
          <button disabled={busy} type="button" onClick={() => act('copy')} className="flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-sm font-bold"><Copy className="h-4 w-4" /> Sao chép</button>
        </div>
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5 lg:col-span-2">
          <h2 className="font-black">Thông tin phiên bản</h2>
          <dl className="mt-4 grid gap-4 text-sm md:grid-cols-2">
            <div><dt className="text-xs text-zinc-500">Rạp</dt><dd className="mt-1 font-bold">{policy.cinemaName}</dd></div>
            <div><dt className="text-xs text-zinc-500">Ưu tiên</dt><dd className="mt-1 font-bold">{policy.priority}</dd></div>
            <div><dt className="text-xs text-zinc-500">Hiệu lực</dt><dd className="mt-1">{policy.effectiveFrom} → {policy.effectiveTo || 'Không giới hạn'}</dd></div>
            <div><dt className="text-xs text-zinc-500">Tiền tệ</dt><dd className="mt-1">{policy.currency}</dd></div>
            <div><dt className="text-xs text-zinc-500">Kích hoạt</dt><dd className="mt-1">{policy.activatedAt || 'Chưa kích hoạt'} {policy.activatedBy ? `· #${policy.activatedBy}` : ''}</dd></div>
            <div><dt className="text-xs text-zinc-500">Ngừng áp dụng</dt><dd className="mt-1">{policy.deactivatedAt || '—'} {policy.deactivationReason ? `· ${policy.deactivationReason}` : ''}</dd></div>
          </dl>
        </section>
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5">
          <h2 className="font-black">Mức sử dụng</h2>
          <p className="mt-5 text-3xl font-black text-amber-400">{usage?.snapshotShowtimeCount ?? 0}</p>
          <p className="text-xs text-zinc-500">Suất chiếu đã chụp từ chính sách</p>
          <p className="mt-5 text-3xl font-black text-zinc-200">{usage?.futureDraftShowtimeCount ?? 0}</p>
          <p className="text-xs text-zinc-500">Suất chiếu nháp tương lai bị ảnh hưởng</p>
        </section>
      </div>

      {policy.conflicts?.length > 0 && (
        <section className="rounded-2xl border border-red-500/30 bg-red-500/10 p-5">
          <h2 className="font-black text-red-300">Phải xử lý xung đột trước khi kích hoạt</h2>
          {policy.conflicts.map(item => <p key={`${item.firstRuleId}-${item.secondRuleId}`} className="mt-2 text-xs text-red-200">{item.firstRuleId} ↔ {item.secondRuleId}: {item.message}</p>)}
        </section>
      )}

      <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/30">
        <div className="border-b border-zinc-800 p-5"><h2 className="font-black">Quy tắc giá</h2></div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-zinc-950 text-xs uppercase text-zinc-500"><tr><th className="p-4">Loại ghế</th><th className="p-4">Phạm vi</th><th className="p-4">Ngày / giờ</th><th className="p-4 text-right">Giá</th></tr></thead>
            <tbody className="divide-y divide-zinc-800">
              {policy.rules.map(rule => (
                <tr key={rule.publicId}>
                  <td className="p-4 font-bold">{rule.seatTypeName} <span className="ml-1 text-xs text-zinc-500">{rule.seatTypeCode}</span></td>
                  <td className="p-4 text-zinc-400">{rule.auditoriumName || rule.screenType || 'Toàn rạp'}</td>
                  <td className="p-4 text-zinc-400">{rule.dayType} · {rule.timeBandStart ? `${rule.timeBandStart}–${rule.timeBandEnd}` : 'Cả ngày'}</td>
                  <td className="p-4 text-right font-black text-emerald-400">{money(rule.price)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {usage?.affectedFutureShowtimes?.length > 0 && (
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5">
          <div className="flex items-center justify-between">
            <h2 className="font-black">Suất chiếu nháp tương lai</h2>
            {usage.affectedTotalPages > 1 && (
              <div className="flex items-center gap-2 text-xs text-zinc-400">
                <button type="button" disabled={usage.affectedPage === 0} onClick={() => setUsagePage(page => Math.max(0, page - 1))} className="rounded-lg border border-zinc-700 p-1.5 disabled:opacity-30"><ChevronLeft className="h-4 w-4" /></button>
                <span>{usage.affectedPage + 1}/{usage.affectedTotalPages}</span>
                <button type="button" disabled={usage.affectedLast} onClick={() => setUsagePage(page => page + 1)} className="rounded-lg border border-zinc-700 p-1.5 disabled:opacity-30"><ChevronRight className="h-4 w-4" /></button>
              </div>
            )}
          </div>
          <div className="mt-3 divide-y divide-zinc-800">
            {usage.affectedFutureShowtimes.map(item => (
              <button key={item.showtimeId} type="button" onClick={() => navigate(`/admin/showtimes/${item.showtimeId}/pricing`)} className="flex w-full justify-between py-3 text-left text-sm hover:text-amber-300">
                <span>{item.auditoriumName}</span><span className="font-mono text-zinc-500">{item.startTime}</span>
              </button>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
