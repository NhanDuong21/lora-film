import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft, RefreshCw } from 'lucide-react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import { getPricingReasonPresentation } from '../utils/pricingPresentation';

const money = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(value || 0);

export default function AdminShowtimePricingPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { triggerToast } = useOutletContext() || {};
  const [showtime, setShowtime] = useState(null);
  const [pricing, setPricing] = useState(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const [detailResponse, pricingResponse] = await Promise.all([
        adminShowtimeService.getShowtimeDetail(id),
        adminShowtimeService.getPricing(id),
      ]);
      setShowtime(detailResponse?.data);
      setPricing(pricingResponse?.data);
    } catch (error) {
      triggerToast?.(error.response?.data?.message || 'Không thể tải chẩn đoán giá', 'error');
    }
  }, [id, triggerToast]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const resolve = async () => {
    setBusy(true);
    try {
      const response = await adminShowtimeService.resolvePricing(id, showtime.version);
      setPricing(response.data);
      triggerToast?.(response.data.complete ? 'Đã phân giải đủ giá' : 'Vẫn còn loại ghế thiếu hoặc mơ hồ', response.data.complete ? 'success' : 'error');
    } catch (error) {
      triggerToast?.(error.response?.data?.message || 'Không thể phân giải giá', 'error');
    } finally {
      setBusy(false);
    }
  };

  if (!showtime || !pricing) return <div className="p-12 text-center text-zinc-500">Đang tải chẩn đoán giá…</div>;

  return (
    <div className="space-y-6 bg-zinc-950 text-white">
      <div className="flex items-center justify-between border-b border-zinc-800 pb-5">
        <div className="flex items-center gap-3">
          <button type="button" onClick={() => navigate(`/admin/showtimes/${id}`)} className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800"><ArrowLeft className="h-5 w-5" /></button>
          <div><p className="text-xs font-bold uppercase tracking-[0.2em] text-amber-400">Showtime snapshot</p><h1 className="text-2xl font-black">Nguồn và chẩn đoán giá</h1></div>
        </div>
        {showtime.status === 'DRAFT' && <button disabled={busy} type="button" onClick={resolve} className="flex items-center gap-2 rounded-xl bg-amber-500 px-4 py-2.5 text-sm font-black text-zinc-950 disabled:opacity-40"><RefreshCw className={`h-4 w-4 ${busy ? 'animate-spin' : ''}`} /> Phân giải lại</button>}
      </div>

      <section className={`rounded-2xl border p-5 ${pricing.complete ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-red-500/30 bg-red-500/10'}`}>
        <h2 className={`text-lg font-black ${pricing.complete ? 'text-emerald-300' : 'text-red-300'}`}>{pricing.complete ? 'Snapshot đầy đủ' : 'Snapshot chưa đầy đủ'}</h2>
        <p className="mt-1 text-sm text-zinc-400">{showtime.movie?.title} · {showtime.cinema?.name} · {showtime.auditorium?.name}</p>
      </section>

      {(pricing.missingSeatTypes?.length > 0 || pricing.ambiguousSeatTypes?.length > 0) && (
        <section className="grid gap-4 md:grid-cols-2">
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5"><h2 className="font-black">{getPricingReasonPresentation('PRICING_INCOMPLETE').label}</h2>{pricing.missingSeatTypes?.map(item => <p key={item.seatTypeId} className="mt-2 text-sm text-zinc-400">{item.seatTypeName} ({item.seatTypeCode})</p>)}</div>
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5"><h2 className="font-black">{getPricingReasonPresentation('PRICING_AMBIGUOUS').label}</h2>{pricing.ambiguousSeatTypes?.map(item => <div key={item.seatTypeId} className="mt-2 text-sm text-zinc-400"><p>{item.seatTypeName}</p><details className="mt-1 text-xs text-zinc-500"><summary className="cursor-pointer">Chi tiết kỹ thuật</summary><p className="font-mono">{item.candidateRuleIds?.join(', ')}</p></details></div>)}</div>
        </section>
      )}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {pricing.prices?.map(line => (
          <article key={line.seatTypeId} className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5">
            <div className="flex items-start justify-between"><div><h2 className="font-black">{line.seatTypeName}</h2><p className="text-xs text-zinc-500">{line.seatTypeCode}</p></div><span className="rounded-full border border-zinc-700 px-2 py-1 text-[10px] font-black text-zinc-300">{line.pricingSource}</span></div>
            <p className="mt-4 text-2xl font-black text-emerald-400">{money(line.price, pricing.currency)}</p>
            <dl className="mt-4 space-y-2 text-xs"><div><dt className="text-zinc-500">Policy</dt><dd className="break-all text-zinc-300">{line.sourcePolicyName || 'Legacy / manual'} {line.sourcePolicyId ? `(${line.sourcePolicyId})` : ''}</dd></div><div><dt className="text-zinc-500">Rule</dt><dd className="break-all font-mono text-zinc-300">{line.sourceRuleId || '—'}</dd></div><div><dt className="text-zinc-500">Resolved</dt><dd className="text-zinc-300">{line.resolvedAt || '—'} · {line.resolutionTimezone || '—'}</dd></div></dl>
          </article>
        ))}
      </div>
    </div>
  );
}
