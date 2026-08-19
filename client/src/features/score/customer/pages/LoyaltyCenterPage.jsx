import { useMemo, useState } from 'react';
import {
  AlertTriangle,
  Award,
  CalendarClock,
  CheckCircle2,
  ChevronDown,
  Clock3,
  Crown,
  Gift,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  WalletCards
} from 'lucide-react';
import useCustomerScore from '@/features/score/customer/hooks/useCustomerScore';
import ScoreHistoryTable from '@/features/score/customer/components/ScoreHistoryTable';

const friendlyTierName = value => String(value || 'Thành viên')
  .replace(/\b(vip|member|membership)\b/gi, '')
  .replace(/\s+/g, ' ')
  .trim() || 'Thành viên';

const formatDate = value => {
  if (!value) return 'Chưa có';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Chưa có';
  return date.toLocaleDateString('vi-VN');
};

function MetricCard({ icon: Icon, label, value, note, accent = 'orange' }) {
  const colors = accent === 'green'
    ? 'bg-emerald-500/10 text-emerald-400'
    : accent === 'blue'
      ? 'bg-sky-500/10 text-sky-400'
      : 'bg-brand-orange/10 text-brand-orange';
  return (
    <article className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5">
      <span className={`flex h-10 w-10 items-center justify-center rounded-xl ${colors}`}>
        <Icon className="h-5 w-5" />
      </span>
      <p className="mt-4 text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600">{label}</p>
      <p className="mt-1 text-2xl font-black tracking-tight text-white">{value}</p>
      <p className="mt-2 text-xs leading-5 text-zinc-500">{note}</p>
    </article>
  );
}

function TierProgress({ scoreData }) {
  const currentTier = scoreData?.currentTier || {};
  const nextTier = scoreData?.nextTier;
  const accumulated = Number(scoreData?.accumulatedPoints || 0);
  const currentMin = Number(currentTier?.minAccumulatedPoints || 0);
  const target = Number(nextTier?.minAccumulatedPoints || currentMin);
  const required = Number(nextTier?.pointsRequired ?? Math.max(0, target - accumulated));
  const range = Math.max(1, target - currentMin);
  const progress = nextTier ? Math.min(100, Math.max(0, ((accumulated - currentMin) / range) * 100)) : 100;

  return (
    <section className="rounded-2xl border border-brand-orange/20 bg-gradient-to-br from-brand-orange/[0.10] via-zinc-900/60 to-zinc-950 p-5 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-[10px] font-black uppercase tracking-[0.16em] text-brand-orange">Hạng hiện tại</p>
          <h2 className="mt-1 flex items-center gap-2 text-2xl font-black text-white">
            <Crown className="h-6 w-6 text-brand-orange" />
            {friendlyTierName(currentTier?.tierName)}
          </h2>
        </div>
        <span className="rounded-full border border-zinc-700 bg-zinc-950/60 px-3 py-2 text-xs font-bold text-zinc-400">
          {accumulated.toLocaleString('vi-VN')} điểm xét hạng
        </span>
      </div>

      <div className="mt-7">
        <div className="mb-2 flex justify-between gap-4 text-xs font-bold">
          <span className="text-zinc-400">
            {nextTier ? `Tiến độ lên ${friendlyTierName(nextTier.tierName)}` : 'Bạn đang ở hạng cao nhất'}
          </span>
          <span className="text-white">{Math.round(progress)}%</span>
        </div>
        <div className="h-2.5 overflow-hidden rounded-full bg-zinc-800">
          <div className="h-full rounded-full bg-gradient-to-r from-brand-orange to-amber-300 transition-all" style={{ width: `${progress}%` }} />
        </div>
        <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs text-zinc-500">
          <span>{currentMin.toLocaleString('vi-VN')} điểm</span>
          <span className="font-bold text-zinc-300">
            {nextTier ? `Cần thêm ${required.toLocaleString('vi-VN')} điểm` : 'Đã hoàn thành mọi mốc hạng'}
          </span>
          <span>{nextTier ? `${target.toLocaleString('vi-VN')} điểm` : accumulated.toLocaleString('vi-VN')}</span>
        </div>
      </div>
    </section>
  );
}

function LoyaltyRules({ earningRate = 0.05 }) {
  const [open, setOpen] = useState(false);
  const ratePercent = Number(earningRate || 0) * 100;
  return (
    <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/45">
      <button type="button" aria-expanded={open} onClick={() => setOpen(value => !value)} className="flex w-full items-center justify-between gap-4 px-5 py-5 text-left sm:px-6">
        <span>
          <span className="block text-sm font-black text-white">Điểm được tính thế nào?</span>
          <span className="mt-1 block text-xs text-zinc-500">Quy tắc tích điểm, sử dụng điểm và điều kiện lên hạng.</span>
        </span>
        <ChevronDown className={`h-5 w-5 shrink-0 text-zinc-500 transition-transform ${open ? 'rotate-180 text-brand-orange' : ''}`} />
      </button>
      {open && (
        <div className="grid gap-4 border-t border-zinc-800 px-5 py-5 sm:grid-cols-2 sm:px-6">
          <div className="flex gap-3">
            <Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-brand-orange" />
            <div><h3 className="text-xs font-black text-white">Tích điểm</h3><p className="mt-1 text-xs leading-5 text-zinc-500">Điểm nhận = giá trị thanh toán hợp lệ × {ratePercent.toLocaleString('vi-VN')}% ÷ 1.000đ/điểm, làm tròn xuống. Điểm được ghi sau khi đơn hoàn tất.</p></div>
          </div>
          <div className="flex gap-3">
            <TrendingUp className="mt-0.5 h-4 w-4 shrink-0 text-brand-orange" />
            <div><h3 className="text-xs font-black text-white">Xét hạng</h3><p className="mt-1 text-xs leading-5 text-zinc-500">Hạng thành viên dựa trên tổng điểm bạn đã tích lũy trọn đời. Dùng điểm không làm giảm tiến độ hạng.</p></div>
          </div>
          <div className="flex gap-3">
            <WalletCards className="mt-0.5 h-4 w-4 shrink-0 text-brand-orange" />
            <div><h3 className="text-xs font-black text-white">Sử dụng điểm</h3><p className="mt-1 text-xs leading-5 text-zinc-500">1 điểm = 1.000đ. Điểm khả dụng có thể dùng khi thanh toán; điểm gần hết hạn được ưu tiên trước.</p></div>
          </div>
          <div className="flex gap-3">
            <Clock3 className="mt-0.5 h-4 w-4 shrink-0 text-brand-orange" />
            <div><h3 className="text-xs font-black text-white">Điểm tạm giữ</h3><p className="mt-1 text-xs leading-5 text-zinc-500">Điểm dùng cho đơn chưa hoàn tất được tạm giữ và sẽ tự hoàn lại nếu giao dịch không thành công.</p></div>
          </div>
        </div>
      )}
    </section>
  );
}

export default function LoyaltyCenterPage({ embedded = false }) {
  const [historyFilters, setHistoryFilters] = useState({});
  const {
    scoreData,
    history,
    expiringPoints,
    tierHistory,
    isLoading,
    isHistoryLoading,
    error,
    fetchHistory
  } = useCustomerScore();

  const activeExpiring = useMemo(
    () => (expiringPoints || []).filter(item => Number(item?.remainingPoints) > 0),
    [expiringPoints]
  );
  const expiringTotal = activeExpiring.reduce((sum, item) => sum + Number(item.remainingPoints || 0), 0);
  const nearestExpiration = activeExpiring
    .map(item => item.expirationDate)
    .filter(Boolean)
    .sort()[0];
  const currentPoints = Math.max(0, Number(scoreData?.currentPoints || 0) - Number(scoreData?.heldPoints || 0));
  const heldPoints = Number(scoreData?.heldPoints || 0);
  const accumulatedPoints = Number(scoreData?.accumulatedPoints || 0);

  const handlePageChange = newPage => {
    fetchHistory({ page: newPage, size: 10, ...historyFilters });
  };
  const handleFilterChange = filters => {
    setHistoryFilters(filters);
    fetchHistory({ page: 0, size: 10, ...filters });
  };

  const content = (
    <div className="space-y-6">
      {error && (
        <div className="flex items-start gap-3 rounded-xl border border-red-500/20 bg-red-500/[0.06] p-4 text-sm text-red-300">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" /> {error}
        </div>
      )}

      {Number(scoreData?.outstandingPoints) > 0 && (
        <div className="flex items-start gap-3 rounded-xl border border-amber-500/20 bg-amber-500/[0.06] p-4">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
          <div><p className="text-sm font-black text-white">Điểm cần đối soát</p><p className="mt-1 text-xs leading-5 text-zinc-400">{Number(scoreData.outstandingPoints).toLocaleString('vi-VN')} điểm đang được điều chỉnh từ một giao dịch hoàn hoặc hủy. Điểm mới sẽ được bù tự động.</p></div>
        </div>
      )}

      {scoreData?.status === 'LOCKED' && (
        <div className="flex items-start gap-3 rounded-xl border border-amber-500/20 bg-amber-500/[0.06] p-4"><AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" /><div><p className="text-sm font-black text-white">Tài khoản điểm đang được xác minh</p><p className="mt-1 text-xs leading-5 text-zinc-400">Bạn vẫn có thể đăng nhập và xem lịch sử, nhưng tạm thời chưa thể tích hoặc sử dụng điểm. Vui lòng liên hệ hỗ trợ nếu cần thêm thông tin.</p></div></div>
      )}

      {isLoading && !scoreData ? (
        <div className="grid animate-pulse gap-4 md:grid-cols-3"><div className="h-40 rounded-2xl bg-zinc-900" /><div className="h-40 rounded-2xl bg-zinc-900" /><div className="h-40 rounded-2xl bg-zinc-900" /></div>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-3">
            <MetricCard icon={WalletCards} label="Điểm khả dụng" value={`${currentPoints.toLocaleString('vi-VN')} điểm`} note="Có thể sử dụng cho đơn hàng đủ điều kiện." />
            <MetricCard icon={ShieldCheck} label="Điểm đang tạm giữ" value={`${heldPoints.toLocaleString('vi-VN')} điểm`} note="Sẽ được dùng hoặc hoàn lại khi đơn hàng hoàn tất." accent="blue" />
            <MetricCard icon={Award} label="Tổng điểm đã tích lũy" value={`${accumulatedPoints.toLocaleString('vi-VN')} điểm`} note="Đây là số điểm được dùng để xét hạng thành viên." accent="green" />
          </div>
          <TierProgress scoreData={scoreData} />
        </>
      )}

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 sm:p-6">
          <div className="flex items-center gap-3"><Gift className="h-5 w-5 text-brand-orange" /><h2 className="text-sm font-black text-white">Quyền lợi hạng hiện tại</h2></div>
          <ul className="mt-5 space-y-3 text-xs leading-5 text-zinc-400">
            <li className="flex gap-2"><CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />Nhận điểm cho mỗi giao dịch vé và bắp nước hợp lệ.</li>
            <li className="flex gap-2"><CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />Tiếp cận ưu đãi dành riêng cho hạng {friendlyTierName(scoreData?.currentTier?.tierName)}.</li>
            <li className="flex gap-2"><CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />Được ưu tiên thông báo khi có chương trình thành viên mới.</li>
          </ul>
        </section>

        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 sm:p-6">
          <div className="flex items-center gap-3"><CalendarClock className="h-5 w-5 text-brand-orange" /><h2 className="text-sm font-black text-white">Điểm sắp hết hạn</h2></div>
          {activeExpiring.length ? (
            <div className="mt-5"><p className="text-2xl font-black text-white">{expiringTotal.toLocaleString('vi-VN')} điểm</p><p className="mt-2 text-xs text-zinc-500">Mốc hết hạn gần nhất: <strong className="text-amber-300">{formatDate(nearestExpiration)}</strong></p><p className="mt-4 text-xs leading-5 text-zinc-400">Khi sử dụng điểm, hệ thống tự ưu tiên phần điểm gần hết hạn trước.</p></div>
          ) : (
            <div className="mt-5 flex gap-3 rounded-xl bg-emerald-500/[0.06] p-4"><ShieldCheck className="h-5 w-5 shrink-0 text-emerald-400" /><div><p className="text-xs font-black text-white">Chưa có điểm sắp hết hạn</p><p className="mt-1 text-xs leading-5 text-zinc-500">Bạn chưa cần lưu ý mốc hết hạn nào ở thời điểm này.</p></div></div>
          )}
        </section>
      </div>

      {tierHistory?.length > 0 && (
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900/45 p-5 sm:p-6">
          <div className="flex items-center gap-3"><Award className="h-5 w-5 text-brand-orange" /><h2 className="text-sm font-black text-white">Lịch sử thay đổi hạng</h2></div>
          <div className="mt-4 divide-y divide-zinc-800">
            {tierHistory.slice(0, 3).map((item, index) => <div key={item.id || index} className="flex flex-wrap items-center justify-between gap-3 py-3 text-xs"><span className="font-bold text-zinc-300">{friendlyTierName(item.oldTierCode)} → {friendlyTierName(item.newTierCode)}</span><span className="text-zinc-600">{formatDate(item.createdAt)}</span></div>)}
          </div>
        </section>
      )}

      <LoyaltyRules earningRate={scoreData?.currentTier?.earningRate} />
      <ScoreHistoryTable history={history} isLoading={isHistoryLoading} onPageChange={handlePageChange} onFilterChange={handleFilterChange} />
    </div>
  );

  if (embedded) return content;
  return (
    <main className="min-h-screen bg-zinc-950 px-4 py-10 text-white sm:px-6 lg:px-8">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8"><p className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.18em] text-brand-orange"><Sparkles className="h-4 w-4" /> Thành viên LoraFilm</p><h1 className="mt-2 text-3xl font-black">Điểm & hạng thành viên</h1></div>
        {content}
      </div>
    </main>
  );
}
