import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  BadgeCheck,
  CalendarClock,
  Gift,
  Loader2,
  RefreshCw,
  Sparkles,
  TicketPercent,
} from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import customerPromotionService from '@/features/promotion/customer/services/customerPromotionService';
import {
  conditionSummary,
  formatDateTime,
  friendlyPromotionError,
  voucherDiscountSummary,
} from '@/features/promotion/shared/promotionPresentation';

const contentOf = payload => (Array.isArray(payload) ? payload : payload?.content || []);

const promotionMarketingName = promotion => {
  const name = String(promotion?.name || '').trim();
  const normalized = name.toLocaleLowerCase('vi');
  if (normalized.includes('welcome') || normalized.includes('new member')) {
    return 'Chào thành viên mới – giảm 10%';
  }
  if (normalized.includes('50k') || normalized.includes('50.000')) {
    return 'Giảm ngay 50.000đ';
  }
  if (normalized.includes('combo night')) {
    return 'Combo tối – giảm 20%';
  }
  return name || `Ưu đãi ${voucherDiscountSummary(promotion)}`;
};

const promotionConditionText = promotion => {
  const summary = conditionSummary(promotion?.conditionsJson);
  return summary === 'Không có điều kiện bổ sung.'
    ? 'Áp dụng cho đơn hàng đủ điều kiện trong thời gian diễn ra chương trình.'
    : summary;
};

const legacyOffer = promotion => ({
  campaignPublicId: promotion.campaignPublicId || promotion.publicId,
  headline: promotionMarketingName(promotion),
  summary: promotionConditionText(promotion),
  coverImageUrl: null,
  imageAltText: '',
  validTo: promotion.validTo,
  primaryPromotion: promotion,
  legacy: true,
});

function OfferAction({ offer, ownedIds, busyId, isAuthenticated, onClaim, compact = false }) {
  const promotion = offer.primaryPromotion;
  const publicId = promotion?.promotionPublicId || promotion?.publicId;
  const claimable = Boolean(publicId)
    && promotion?.promotionType === 'VOUCHER'
    && promotion?.publicVisible !== false;
  const owned = publicId ? ownedIds.has(publicId) : false;
  const remaining = promotion?.maxRedemptions == null
    ? null
    : Math.max(
        0,
        Number(promotion.maxRedemptions) - Number(promotion.redemptionCount || 0),
      );

  if (!claimable) {
    return (
      <Link
        to="/promotions"
        aria-label="Xem chi tiết ưu đãi"
        className={`inline-flex items-center justify-center gap-2 rounded-xl bg-brand-orange font-black text-white shadow-lg shadow-orange-950/30 transition-colors hover:bg-orange-600 ${compact ? 'h-8 px-2.5 text-[10px]' : 'h-11 px-5 text-sm'}`}
      >
        {compact ? 'Chi tiết' : 'Xem chi tiết'} <ArrowRight className="h-4 w-4" />
      </Link>
    );
  }

  return (
    <button
      type="button"
      aria-label={owned
        ? 'Đã có trong ví'
        : remaining === 0
          ? 'Đã hết lượt'
          : isAuthenticated
            ? 'Nhận ưu đãi'
            : 'Đăng nhập để nhận'}
      disabled={owned || busyId === publicId || remaining === 0}
      onClick={() => void onClaim(promotion)}
      className={`inline-flex items-center justify-center gap-2 rounded-xl bg-brand-orange font-black text-white shadow-lg shadow-orange-950/30 transition-colors hover:bg-orange-600 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500 ${compact ? 'h-8 px-2.5 text-[10px]' : 'h-11 px-5 text-sm'}`}
    >
      {busyId === publicId ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : owned ? (
        <BadgeCheck className="h-4 w-4 text-emerald-400" />
      ) : (
        <Gift className="h-4 w-4" />
      )}
      {compact
        ? owned ? 'Đã nhận' : remaining === 0 ? 'Hết lượt' : 'Nhận'
        : owned
          ? 'Đã có trong ví'
          : remaining === 0
            ? 'Đã hết lượt'
            : isAuthenticated
              ? 'Nhận ưu đãi'
              : 'Đăng nhập để nhận'}
    </button>
  );
}

export default function EventSection() {
  const { isAuthenticated, isInitializing } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [offers, setOffers] = useState([]);
  const [promotions, setPromotions] = useState([]);
  const [ownedIds, setOwnedIds] = useState(() => new Set());
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (isInitializing) return;
    setLoading(true);
    setError('');
    try {
      const offersRequest = customerPromotionService.getPublicOffers
        ? customerPromotionService.getPublicOffers({ placement: 'HOME', page: 0, size: 3 })
            .catch(() => ({ content: [] }))
        : Promise.resolve({ content: [] });
      const [offerPage, publicPage, walletPage] = await Promise.all([
        offersRequest,
        customerPromotionService.getPublicPromotions({
          page: 0,
          size: 6,
          sort: 'priority,asc',
        }),
        isAuthenticated
          ? customerPromotionService.getMyPromotions({
              page: 0,
              size: 100,
              sort: 'validTo,asc',
            })
          : Promise.resolve({ content: [] }),
      ]);
      setOffers(contentOf(offerPage));
      setPromotions(contentOf(publicPage).filter(item => item.promotionType === 'VOUCHER'));
      setOwnedIds(new Set(
        contentOf(walletPage).map(item => item.promotionPublicId).filter(Boolean),
      ));
    } catch (requestError) {
      setError(friendlyPromotionError(requestError));
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, isInitializing]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const visible = useMemo(
    () => offers.length
      ? offers.slice(0, 3)
      : promotions.slice(0, 3).map(legacyOffer),
    [offers, promotions],
  );

  const claim = async promotion => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location.pathname } });
      return;
    }
    const publicId = promotion.promotionPublicId || promotion.publicId;
    setBusyId(publicId);
    setError('');
    try {
      await customerPromotionService.claimVoucher(publicId);
      setOwnedIds(current => new Set([...current, publicId]));
    } catch (requestError) {
      setError(friendlyPromotionError(requestError));
    } finally {
      setBusyId('');
    }
  };

  return (
    <section id="events-section" className="w-full border-y border-zinc-900 bg-[#08080a] py-16 text-zinc-100">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header className="flex flex-wrap items-end justify-between gap-4 border-b border-zinc-800 pb-5">
          <div>
            <div className="flex items-center gap-2 text-[11px] font-black uppercase tracking-[0.22em] text-brand-orange">
              <Sparkles className="h-4 w-4" /> Sự kiện & ưu đãi hot
            </div>
            <h2 className="mt-2 text-xl font-black uppercase text-white md:text-2xl">
              Săn ưu đãi, xem phim tiết kiệm hơn
            </h2>
            <p className="mt-2 max-w-2xl text-sm text-zinc-500">
              Khám phá chương trình nổi bật và lưu voucher vào ví trước khi đặt vé.
            </p>
          </div>
          <Link
            to="/promotions"
            className="flex items-center gap-2 text-sm font-bold text-zinc-400 transition-colors hover:text-brand-orange"
          >
            <span>{isAuthenticated ? 'Xem ví ưu đãi' : 'Xem tất cả ưu đãi'}</span>
            <ArrowRight className="h-4 w-4" />
          </Link>
        </header>

        {error && (
          <div role="alert" className="mt-5 flex items-center gap-3 rounded-xl border border-red-500/20 bg-red-500/[0.06] px-4 py-3 text-xs text-red-300">
            <span className="flex-1">{error}</span>
            <button type="button" onClick={() => void load()} title="Thử tải lại" aria-label="Thử tải lại" className="p-1 text-red-300 hover:text-white">
              <RefreshCw className="h-4 w-4" />
            </button>
          </div>
        )}

        {loading ? (
          <div className="flex min-h-80 items-center justify-center gap-2 text-sm font-bold text-zinc-600">
            <Loader2 className="h-5 w-5 animate-spin" /> Đang tải ưu đãi...
          </div>
        ) : visible.length === 0 ? (
          <div className="flex min-h-72 flex-col items-center justify-center text-center">
            <TicketPercent className="h-10 w-10 text-zinc-800" />
            <p className="mt-3 text-sm font-black text-zinc-400">Chưa có chương trình đang mở</p>
            <p className="mt-1 text-xs text-zinc-600">Ưu đãi mới sẽ xuất hiện tại đây sau khi được phát hành.</p>
          </div>
        ) : (
          <div className="grid gap-5 pt-8 lg:grid-cols-3 lg:grid-rows-2">
            {visible.map((offer, index) => {
              const promotion = offer.primaryPromotion;
              const discount = promotion ? voucherDiscountSummary(promotion) : null;
              const featured = index === 0;
              const key = offer.campaignPublicId || promotion?.publicId || index;
              if (featured) {
                return (
                  <article
                    key={key}
                    className={`group relative flex min-h-[390px] overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-950 ${visible.length === 1 ? 'lg:col-span-3 lg:row-span-2' : 'lg:col-span-2 lg:row-span-2'}`}
                  >
                    {offer.coverImageUrl ? (
                      <img src={offer.coverImageUrl} alt={offer.imageAltText || offer.headline} className="absolute inset-0 h-full w-full object-cover transition duration-700 group-hover:scale-[1.02]" />
                    ) : (
                      <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_20%,rgba(249,115,22,0.28),transparent_35%),linear-gradient(135deg,#27272a,#09090b_65%)]" />
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-black via-black/45 to-black/5" />
                    <div className="relative z-10 mt-auto flex w-full flex-col gap-4 p-6 sm:p-8">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-brand-orange px-3 py-1.5 text-[10px] font-black uppercase tracking-wide text-white">
                          <Gift className="h-3.5 w-3.5" /> Khuyến mãi mới
                        </span>
                        <span className="inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-black/45 px-3 py-1.5 text-[10px] font-bold text-zinc-200 backdrop-blur">
                          <CalendarClock className="h-3.5 w-3.5" /> Đến {formatDateTime(offer.validTo || promotion?.validTo)}
                        </span>
                      </div>
                      <div>
                        <h3 className="max-w-3xl text-2xl font-black leading-tight text-white sm:text-3xl">{offer.headline}</h3>
                        {discount && <p className="mt-2 text-lg font-black text-orange-300">{discount}</p>}
                        <p className="mt-3 max-w-3xl line-clamp-2 text-sm leading-6 text-zinc-200">{offer.summary}</p>
                      </div>
                      <div>
                        <OfferAction offer={offer} ownedIds={ownedIds} busyId={busyId} isAuthenticated={isAuthenticated} onClaim={claim} />
                      </div>
                    </div>
                  </article>
                );
              }

              return (
                <article key={key} className="group grid min-h-[185px] grid-cols-[116px_1fr] overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/55 shadow-xl">
                  <div className="relative overflow-hidden bg-zinc-950">
                    {offer.coverImageUrl ? (
                      <img src={offer.coverImageUrl} alt={offer.imageAltText || offer.headline} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
                    ) : (
                      <div className="flex h-full items-center justify-center bg-gradient-to-br from-orange-500/25 via-zinc-900 to-black text-orange-300">
                        <TicketPercent className="h-8 w-8" />
                      </div>
                    )}
                    <div className="absolute inset-0 bg-gradient-to-r from-transparent to-zinc-950/35" />
                  </div>
                  <div className="flex min-w-0 flex-col p-4">
                    <span className="text-[10px] font-black uppercase tracking-wider text-brand-orange">Ưu đãi nổi bật</span>
                    <h3 className="mt-2 line-clamp-2 text-base font-black leading-5 text-white">{offer.headline}</h3>
                    {discount && <p className="mt-1 text-sm font-black text-orange-300">{discount}</p>}
                    <p className="mt-2 line-clamp-2 text-xs leading-5 text-zinc-400">{offer.summary}</p>
                    <div className="mt-auto flex items-end justify-between gap-2 pt-3">
                      <span className="text-[10px] font-semibold text-zinc-500">Đến {formatDateTime(offer.validTo || promotion?.validTo)}</span>
                      <OfferAction offer={offer} ownedIds={ownedIds} busyId={busyId} isAuthenticated={isAuthenticated} onClaim={claim} compact />
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
