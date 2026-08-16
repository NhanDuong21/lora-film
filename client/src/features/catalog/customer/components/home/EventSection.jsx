import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  BadgeCheck,
  CalendarClock,
  Gift,
  Loader2,
  RefreshCw,
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
  const summary = conditionSummary(promotion.conditionsJson);
  return summary === 'Không có điều kiện bổ sung.'
    ? 'Áp dụng cho đơn hàng đủ điều kiện trong thời gian diễn ra chương trình.'
    : summary;
};

export default function EventSection() {
  const { isAuthenticated, isInitializing } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
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
      const [publicPage, walletPage] = await Promise.all([
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

  const visible = useMemo(() => promotions.slice(0, 3), [promotions]);

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
    <section id="events-section" className="w-full border-y border-zinc-900 bg-[#0a0a0c] py-16 text-zinc-100">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <header className="flex flex-wrap items-end justify-between gap-4 border-b border-zinc-800 pb-5">
          <div>
            <div className="flex items-center gap-2 text-[11px] font-black uppercase tracking-[0.22em] text-brand-orange">
              <Gift className="h-4 w-4" /> Ưu đãi dành cho bạn
            </div>
            <h2 className="mt-2 text-xl font-black uppercase text-white md:text-2xl">
              Săn ưu đãi, xem phim tiết kiệm hơn
            </h2>
            <p className="mt-2 max-w-2xl text-sm text-zinc-500">
              Nhận voucher đang mở và sử dụng trực tiếp khi thanh toán vé.
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
            <button
              type="button"
              onClick={() => void load()}
              title="Thử tải lại"
              aria-label="Thử tải lại"
              className="p-1 text-red-300 hover:text-white"
            >
              <RefreshCw className="h-4 w-4" />
            </button>
          </div>
        )}

        {loading ? (
          <div className="flex min-h-64 items-center justify-center gap-2 text-sm font-bold text-zinc-600">
            <Loader2 className="h-5 w-5 animate-spin" /> Đang tải ưu đãi...
          </div>
        ) : visible.length === 0 ? (
          <div className="flex min-h-64 flex-col items-center justify-center text-center">
            <TicketPercent className="h-10 w-10 text-zinc-800" />
            <p className="mt-3 text-sm font-black text-zinc-400">Chưa có ưu đãi đang mở nhận</p>
            <p className="mt-1 text-xs text-zinc-600">Chương trình mới sẽ xuất hiện tại đây.</p>
          </div>
        ) : (
          <div className="grid gap-6 pt-9 sm:grid-cols-2 lg:grid-cols-3">
            {visible.map(promotion => {
              const publicId = promotion.promotionPublicId || promotion.publicId;
              const owned = ownedIds.has(publicId);
              const remaining = promotion.maxRedemptions == null
                ? null
                : Math.max(
                    0,
                    Number(promotion.maxRedemptions) - Number(promotion.redemptionCount || 0),
                  );
              return (
                <article
                  key={publicId}
                  className="relative flex min-h-80 flex-col overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/70 px-6 py-6 shadow-xl before:absolute before:-left-3 before:top-[55%] before:h-6 before:w-6 before:rounded-full before:border before:border-zinc-800 before:bg-[#0a0a0c] after:absolute after:-right-3 after:top-[55%] after:h-6 after:w-6 after:rounded-full after:border after:border-zinc-800 after:bg-[#0a0a0c]"
                >
                  <div className="flex items-start justify-between gap-3">
                    <span className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-brand-orange/10 text-brand-orange">
                      <TicketPercent className="h-5 w-5" />
                    </span>
                    <span className="rounded-full border border-emerald-500/25 bg-emerald-500/10 px-2.5 py-1 text-[10px] font-black uppercase tracking-wide text-emerald-300">
                      Đang nhận
                    </span>
                  </div>
                  <h3 className="mt-5 line-clamp-2 min-h-12 text-lg font-black leading-6 text-white">
                    {promotionMarketingName(promotion)}
                  </h3>
                  <p className="mt-2 text-2xl font-black text-brand-orange">
                    {voucherDiscountSummary(promotion)}
                  </p>
                  <p className="mt-3 line-clamp-2 text-sm leading-5 text-zinc-400">
                    {promotionConditionText(promotion)}
                  </p>

                  <div className="mt-6 border-t border-dashed border-zinc-700 pt-5">
                    <div className="mb-4 space-y-2 text-xs font-semibold text-zinc-500">
                      <span className="flex items-center gap-2">
                        <CalendarClock className="h-4 w-4 text-zinc-600" />
                        Hạn nhận: {formatDateTime(promotion.validTo)}
                      </span>
                      {remaining !== null && <span className="block">Còn {remaining} lượt nhận</span>}
                    </div>
                    <button
                      type="button"
                      disabled={owned || busyId === publicId || remaining === 0}
                      onClick={() => void claim(promotion)}
                      className="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-brand-orange text-sm font-black text-white shadow-lg shadow-orange-950/30 transition-colors hover:bg-orange-600 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
                    >
                      {busyId === publicId ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : owned ? (
                        <BadgeCheck className="h-4 w-4 text-emerald-400" />
                      ) : (
                        <Gift className="h-4 w-4" />
                      )}
                      {owned
                        ? 'Đã có trong ví'
                        : remaining === 0
                          ? 'Đã hết lượt'
                          : isAuthenticated
                            ? 'Nhận ưu đãi'
                            : 'Đăng nhập để nhận'}
                    </button>
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
