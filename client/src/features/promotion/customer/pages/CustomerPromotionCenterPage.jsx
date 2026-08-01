import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  BadgeCheck,
  Check,
  Gift,
  Loader2,
  RefreshCw,
  Tag,
  TicketPercent,
  WalletCards,
} from 'lucide-react';
import customerPromotionService from '../services/customerPromotionService';
import {
  badgeClass,
  conditionSummary,
  formatDateTime,
  friendlyPromotionError,
  labelFor,
  voucherDiscountSummary,
} from '../../shared/promotionPresentation';

const contentOf = payload => Array.isArray(payload) ? payload : (payload?.content || []);

function PromotionCard({ promotion, wallet = false, busy, onClaim }) {
  const available = promotion.status === 'ACTIVE';
  return (
    <article className="grid gap-4 rounded-lg border border-zinc-800 bg-zinc-900/60 p-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <TicketPercent className="h-4 w-4 text-emerald-400" />
          <h2 className="break-words text-sm font-black text-white">{promotion.name || 'Ưu đãi LoraFilm'}</h2>
          <span className={`rounded border px-2 py-0.5 text-[10px] font-bold ${badgeClass(promotion.status)}`}>
            {labelFor(promotion.status)}
          </span>
        </div>
        {promotion.code && <p className="mt-2 break-all font-mono text-xs font-bold text-amber-300">{promotion.code}</p>}
        <p className="mt-2 text-base font-black text-emerald-400">{voucherDiscountSummary(promotion)}</p>
        {promotion.description && <p className="mt-1 text-xs leading-5 text-zinc-400">{promotion.description}</p>}
        <p className="mt-2 text-[11px] leading-5 text-zinc-500">{conditionSummary(promotion.conditionsJson)}</p>
        <p className="mt-1 text-[10px] font-semibold text-zinc-600">Hiệu lực đến {formatDateTime(promotion.validTo)}</p>
      </div>
      {wallet ? (
        <span className="inline-flex h-9 items-center gap-2 self-center rounded-lg border border-emerald-500/25 bg-emerald-500/10 px-3 text-xs font-bold text-emerald-300">
          <BadgeCheck className="h-4 w-4" /> Trong ví
        </span>
      ) : (
        <button
          type="button"
          disabled={busy || !available}
          onClick={() => onClaim(promotion.publicId)}
          className="inline-flex h-9 min-w-28 items-center justify-center gap-2 rounded-lg bg-emerald-500 px-3 text-xs font-black text-zinc-950 transition-colors hover:bg-emerald-400 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
        >
          {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <Gift className="h-4 w-4" />}
          Nhận voucher
        </button>
      )}
    </article>
  );
}

export default function CustomerPromotionCenterPage() {
  const [tab, setTab] = useState('public');
  const [publicPromotions, setPublicPromotions] = useState([]);
  const [wallet, setWallet] = useState([]);
  const [couponCode, setCouponCode] = useState('');
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState('');
  const [message, setMessage] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setMessage(null);
    try {
      const [publicPage, walletPage] = await Promise.all([
        customerPromotionService.getPublicPromotions({ page: 0, size: 100, sort: 'priority,asc' }),
        customerPromotionService.getMyPromotions({ page: 0, size: 100, sort: 'validTo,asc' }),
      ]);
      setPublicPromotions(contentOf(publicPage));
      setWallet(contentOf(walletPage));
    } catch (error) {
      setMessage({ kind: 'error', text: friendlyPromotionError(error) });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const claimedPromotionIds = useMemo(
    () => new Set(wallet.map(item => item.promotionPublicId).filter(Boolean)),
    [wallet]
  );
  const unclaimed = useMemo(
    () => publicPromotions.filter(item => !claimedPromotionIds.has(item.publicId)),
    [claimedPromotionIds, publicPromotions]
  );

  const claim = async publicId => {
    setBusyId(publicId);
    setMessage(null);
    try {
      await customerPromotionService.claimVoucher(publicId);
      await load();
      setMessage({ kind: 'success', text: 'Voucher đã được thêm vào ví.' });
      setTab('wallet');
    } catch (error) {
      setMessage({ kind: 'error', text: friendlyPromotionError(error) });
    } finally {
      setBusyId('');
    }
  };

  const redeem = async event => {
    event.preventDefault();
    const code = couponCode.trim();
    if (!code) return;
    setBusyId('coupon');
    setMessage(null);
    try {
      await customerPromotionService.redeemCoupon(code);
      setCouponCode('');
      await load();
      setMessage({ kind: 'success', text: 'Coupon hợp lệ đã được thêm vào ví.' });
      setTab('wallet');
    } catch (error) {
      setMessage({ kind: 'error', text: friendlyPromotionError(error) });
    } finally {
      setBusyId('');
    }
  };

  const rows = tab === 'public' ? unclaimed : wallet;

  return (
    <main className="min-h-screen bg-zinc-950 px-4 py-10 text-zinc-100 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-6xl">
        <header className="flex flex-wrap items-end justify-between gap-4 border-b border-zinc-800 pb-6">
          <div>
            <div className="flex items-center gap-2 text-xs font-black uppercase text-emerald-400">
              <WalletCards className="h-4 w-4" /> Promotion Center
            </div>
            <h1 className="mt-2 text-2xl font-black text-white sm:text-3xl">Khuyến mãi của bạn</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
              Nhận voucher công khai, nhập coupon và quản lý các ưu đãi có thể dùng khi đặt vé.
            </p>
          </div>
          <button type="button" onClick={() => void load()} disabled={loading}
            title="Tải lại khuyến mãi" aria-label="Tải lại khuyến mãi"
            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-zinc-700 text-zinc-400 hover:bg-zinc-800 hover:text-white disabled:opacity-50">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </header>

        <form onSubmit={redeem} className="grid gap-3 border-b border-zinc-800 py-5 sm:grid-cols-[minmax(0,1fr)_auto]">
          <label className="relative">
            <Tag className="pointer-events-none absolute left-3 top-3 h-4 w-4 text-zinc-600" />
            <input value={couponCode} onChange={event => setCouponCode(event.target.value.toUpperCase())}
              maxLength={64} placeholder="Nhập mã coupon"
              className="h-10 w-full rounded-lg border border-zinc-700 bg-zinc-900 pl-10 pr-3 text-sm font-bold text-white outline-none placeholder:text-zinc-600 focus:border-amber-500" />
          </label>
          <button type="submit" disabled={!couponCode.trim() || busyId === 'coupon'}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-amber-500 px-5 text-xs font-black text-zinc-950 hover:bg-amber-400 disabled:cursor-not-allowed disabled:opacity-40">
            {busyId === 'coupon' ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />}
            Áp dụng mã
          </button>
        </form>

        {message && (
          <div role="alert" className={`mt-5 flex items-start gap-2 border-l-2 px-3 py-2 text-sm ${message.kind === 'error' ? 'border-red-500 bg-red-500/10 text-red-300' : 'border-emerald-500 bg-emerald-500/10 text-emerald-300'}`}>
            {message.kind === 'error' ? <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" /> : <BadgeCheck className="mt-0.5 h-4 w-4 shrink-0" />}
            <span>{message.text}</span>
          </div>
        )}

        <div className="mt-6 flex gap-1 border-b border-zinc-800">
          {[['public', Gift, `Voucher công khai (${unclaimed.length})`], ['wallet', WalletCards, `Ví của tôi (${wallet.length})`]].map(([key, Icon, text]) => (
            <button key={key} type="button" onClick={() => setTab(key)}
              className={`flex items-center gap-2 border-b-2 px-4 py-3 text-xs font-bold ${tab === key ? 'border-emerald-500 text-white' : 'border-transparent text-zinc-500 hover:text-zinc-200'}`}>
              <Icon className="h-4 w-4" /> {text}
            </button>
          ))}
        </div>

        <section className="mt-5 space-y-3" aria-live="polite">
          {loading ? (
            <div className="flex min-h-48 items-center justify-center gap-2 text-sm font-semibold text-zinc-500">
              <Loader2 className="h-5 w-5 animate-spin" /> Đang tải khuyến mãi...
            </div>
          ) : rows.length === 0 ? (
            <div className="flex min-h-48 flex-col items-center justify-center border-y border-zinc-800 text-center">
              <Gift className="h-8 w-8 text-zinc-700" />
              <p className="mt-3 text-sm font-black text-zinc-300">{tab === 'public' ? 'Bạn đã nhận hết voucher hiện có' : 'Ví khuyến mãi đang trống'}</p>
              <p className="mt-1 text-xs text-zinc-600">Các ưu đãi mới sẽ xuất hiện tại đây.</p>
            </div>
          ) : rows.map(item => (
            <PromotionCard key={item.publicId} promotion={item} wallet={tab === 'wallet'} busy={busyId === item.publicId} onClaim={claim} />
          ))}
        </section>
      </div>
    </main>
  );
}
