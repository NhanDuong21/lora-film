import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ArrowRight,
  BadgeCheck,
  CalendarClock,
  Gift,
  Loader2,
  RefreshCw,
  TicketPercent,
} from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import customerPromotionService from "@/features/promotion/customer/services/customerPromotionService";
import {
  conditionSummary,
  formatDateTime,
  friendlyPromotionError,
  voucherDiscountSummary,
} from "@/features/promotion/shared/promotionPresentation";

const contentOf = (payload) =>
  Array.isArray(payload) ? payload : payload?.content || [];

export default function EventSection() {
  const { isAuthenticated, isInitializing } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [promotions, setPromotions] = useState([]);
  const [ownedIds, setOwnedIds] = useState(() => new Set());
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    if (isInitializing) return;
    setLoading(true);
    setError("");
    try {
      const [publicPage, walletPage] = await Promise.all([
        customerPromotionService.getPublicPromotions({
          page: 0,
          size: 6,
          sort: "priority,asc",
        }),
        isAuthenticated
          ? customerPromotionService.getMyPromotions({
              page: 0,
              size: 100,
              sort: "validTo,asc",
            })
          : Promise.resolve({ content: [] }),
      ]);
      setPromotions(
        contentOf(publicPage).filter(
          (item) => item.promotionType === "VOUCHER",
        ),
      );
      setOwnedIds(
        new Set(
          contentOf(walletPage)
            .map((item) => item.promotionPublicId)
            .filter(Boolean),
        ),
      );
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

  const claim = async (promotion) => {
    if (!isAuthenticated) {
      navigate("/login", { state: { from: location.pathname } });
      return;
    }
    const publicId = promotion.promotionPublicId || promotion.publicId;
    setBusyId(publicId);
    setError("");
    try {
      await customerPromotionService.claimVoucher(publicId);
      setOwnedIds((current) => new Set([...current, publicId]));
    } catch (requestError) {
      setError(friendlyPromotionError(requestError));
    } finally {
      setBusyId("");
    }
  };

  return (
    <section
      id="events-section"
      className="w-full border-y border-zinc-900 bg-zinc-950 py-14 text-zinc-100"
    >
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-end justify-between gap-3 border-b border-zinc-800 pb-4">
          <div>
            <div className="flex items-center gap-2 text-[10px] font-black uppercase text-emerald-400">
              <Gift className="h-4 w-4" /> Voucher sự kiện
            </div>
            <h2 className="mt-2 text-lg font-black uppercase text-white md:text-xl">
              Nhận ưu đãi đang diễn ra
            </h2>
          </div>
          <Link
            to="/promotions"
            className="flex items-center gap-1 text-xs font-bold text-zinc-400 transition-colors hover:text-white"
          >
            <span>Xem ví ưu đãi</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        {error && (
          <div
            role="alert"
            className="mt-4 flex items-center gap-3 border-l-2 border-red-500 bg-red-500/[0.06] px-3 py-2 text-xs text-red-300"
          >
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
          <div className="flex min-h-64 flex-col items-center justify-center border-b border-zinc-900 text-center">
            <TicketPercent className="h-9 w-9 text-zinc-800" />
            <p className="mt-3 text-sm font-black text-zinc-400">
              Chưa có voucher sự kiện đang mở
            </p>
            <p className="mt-1 text-xs text-zinc-600">
              Ưu đãi mới sẽ xuất hiện tại đây khi chiến dịch được kích hoạt.
            </p>
          </div>
        ) : (
          <div className="grid gap-px bg-zinc-800 sm:grid-cols-2 lg:grid-cols-3">
            {visible.map((promotion) => {
              const publicId =
                promotion.promotionPublicId || promotion.publicId;
              const owned = ownedIds.has(publicId);
              const remaining =
                promotion.maxRedemptions == null
                  ? null
                  : Math.max(
                      0,
                      Number(promotion.maxRedemptions) -
                        Number(promotion.redemptionCount || 0),
                    );
              return (
                <article
                  key={publicId}
                  className="flex min-h-72 flex-col bg-zinc-950 px-5 py-6"
                >
                  <div className="flex items-start justify-between gap-3">
                    <span className="inline-flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-400">
                      <TicketPercent className="h-5 w-5" />
                    </span>
                    <span className="rounded border border-emerald-500/25 bg-emerald-500/10 px-2 py-1 text-[9px] font-black uppercase text-emerald-300">
                      Đang mở nhận
                    </span>
                  </div>
                  <h3 className="mt-5 break-words text-base font-black text-white">
                    {promotion.name || "Voucher LoraFilm"}
                  </h3>
                  <p className="mt-2 text-xl font-black text-emerald-400">
                    {voucherDiscountSummary(promotion)}
                  </p>
                  <p className="mt-3 line-clamp-2 text-xs leading-5 text-zinc-500">
                    {promotion.description ||
                      conditionSummary(promotion.conditionsJson)}
                  </p>
                  <div className="mt-auto pt-5">
                    <div className="mb-3 flex flex-wrap gap-x-3 gap-y-1 text-[10px] font-bold text-zinc-600">
                      <span className="flex items-center gap-1">
                        <CalendarClock className="h-3.5 w-3.5" /> Đến{" "}
                        {formatDateTime(promotion.validTo)}
                      </span>
                      {remaining !== null && <span>Còn {remaining} lượt</span>}
                    </div>
                    <button
                      type="button"
                      disabled={owned || busyId === publicId || remaining === 0}
                      onClick={() => void claim(promotion)}
                      className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg bg-emerald-500 text-xs font-black text-zinc-950 transition-colors hover:bg-emerald-400 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
                    >
                      {busyId === publicId ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : owned ? (
                        <BadgeCheck className="h-4 w-4" />
                      ) : (
                        <Gift className="h-4 w-4" />
                      )}
                      {owned
                        ? "Đã có trong ví"
                        : remaining === 0
                          ? "Đã hết lượt"
                          : isAuthenticated
                            ? "Nhận vào ví"
                            : "Đăng nhập để nhận"}
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
